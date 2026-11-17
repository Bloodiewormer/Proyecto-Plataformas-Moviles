package cr.ac.una.glifo.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cr.ac.una.glifo.core.common.AppError
import cr.ac.una.glifo.core.common.UiState
import cr.ac.una.glifo.data.mapper.SyllabusMapper
import cr.ac.una.glifo.domain.error.Result
import cr.ac.una.glifo.domain.model.SyllabusTopicRow
import cr.ac.una.glifo.domain.model.SyllabusUnitRow
import cr.ac.una.glifo.domain.repository.TeacherRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TeacherSyllabusUiData(
    val fileName: String? = null,
    val formattedSize: String? = null,
    val parsedAtDisplay: String? = null,
    val units: List<SyllabusUnitRow> = emptyList(),
    val isPublished: Boolean = false,
    val isUploading: Boolean = false,
    val actionError: AppError? = null
) {
    val hasTopics: Boolean
        get() = units.isNotEmpty()
}

@HiltViewModel
class TeacherSyllabusViewModel @Inject constructor(
    private val teacherRepository: TeacherRepository
) : ViewModel() {

    private val _state = MutableStateFlow<UiState<TeacherSyllabusUiData>>(UiState.Loading)
    val state: StateFlow<UiState<TeacherSyllabusUiData>> = _state.asStateFlow()

    private val pendingTopics = mutableListOf<String>()
    private var currentCourseId: Long? = null
    private var currentFileName: String? = null
    private var isPublished: Boolean = false

    init { load() }

    fun load() {
        viewModelScope.launch {
            _state.value = UiState.Loading
            when (val result = teacherRepository.getSyllabus()) {
                is Result.Failure -> _state.value = UiState.Error(result.error)
                is Result.Success -> {
                    val doc = result.value
                    val serverHasSyllabus = doc.syllabusVersion > 0 || doc.units.isNotEmpty()
                    if (currentCourseId != null && currentCourseId != doc.courseId) {
                        pendingTopics.clear()
                    }
                    currentCourseId = doc.courseId
                    currentFileName = doc.fileName
                    isPublished = serverHasSyllabus

                    if (isPublished) {
                        pendingTopics.clear()
                        _state.value = UiState.Success(
                            TeacherSyllabusUiData(
                                fileName = doc.fileName,
                                formattedSize = doc.formattedSize,
                                parsedAtDisplay = doc.parsedAtDisplay,
                                units = doc.units,
                                isPublished = true
                            )
                        )
                    } else {
                        _state.value = UiState.Success(
                            TeacherSyllabusUiData(
                                fileName = doc.fileName,
                                formattedSize = doc.formattedSize,
                                parsedAtDisplay = doc.parsedAtDisplay,
                                units = pendingTopics.toPendingUnitRows(),
                                isPublished = false
                            )
                        )
                    }
                }
            }
        }
    }

    fun uploadSyllabusPdf(fileName: String, pdfBytes: ByteArray) {
        if (pdfBytes.isEmpty()) {
            reportPdfError(FIELD_PDF_UNREADABLE)
            return
        }
        if (pdfBytes.size > MAX_PDF_BYTES) {
            reportPdfError(FIELD_PDF_TOO_LARGE)
            return
        }
        updateSuccessData { it.copy(isUploading = true, actionError = null) }
        viewModelScope.launch {
            when (val result = teacherRepository.uploadSyllabusPdf(fileName, pdfBytes)) {
                is Result.Success -> {
                    val doc = result.value
                    val resolvedFileName = doc.fileName ?: fileName.trim().ifEmpty { null }
                    val resolvedSize = doc.formattedSize ?: SyllabusMapper.formatFileSize(pdfBytes.size.toLong())
                    pendingTopics.clear()
                    currentCourseId = doc.courseId
                    currentFileName = resolvedFileName
                    isPublished = true
                    _state.value = UiState.Success(
                        TeacherSyllabusUiData(
                            fileName = resolvedFileName,
                            formattedSize = resolvedSize,
                            parsedAtDisplay = doc.parsedAtDisplay,
                            units = doc.units,
                            isPublished = true,
                            isUploading = false,
                            actionError = null
                        )
                    )
                }
                is Result.Failure -> {
                    updateSuccessData { it.copy(isUploading = false, actionError = result.error) }
                }
            }
        }
    }

    fun reportPdfError(field: String) {
        updateSuccessData {
            it.copy(
                isUploading = false,
                actionError = AppError.Validation(field = field, issue = field)
            )
        }
    }

    fun dismissActionError() {
        updateSuccessData { it.copy(actionError = null) }
    }

    fun addTopic(label: String) {
        val trimmed = label.trim()
        if (trimmed.isBlank()) return
        if (!isPublished) {
            pendingTopics.add(trimmed)
            updateSuccessData {
                it.copy(
                    units = pendingTopics.toPendingUnitRows(),
                    isPublished = false,
                    actionError = null
                )
            }
            return
        }
        viewModelScope.launch {
            when (val result = teacherRepository.addSyllabusTopic(trimmed)) {
                is Result.Success -> load()
                is Result.Failure -> updateSuccessData { it.copy(actionError = result.error) }
            }
        }
    }

    suspend fun publishSyllabus(): Result<Unit> {
        if (isPublished) {
            return Result.Failure(
                AppError.Validation(field = FIELD_ALREADY_PUBLISHED, issue = FIELD_ALREADY_PUBLISHED)
            )
        }
        if (pendingTopics.isEmpty()) {
            return Result.Failure(
                AppError.Validation(field = FIELD_EMPTY_TOPICS, issue = FIELD_EMPTY_TOPICS)
            )
        }
        return teacherRepository.publishSyllabus(
            topics = pendingTopics.toList(),
            sourceUri = currentFileName
        ).also { result ->
            if (result is Result.Success) {
                pendingTopics.clear()
                isPublished = true
                load()
            }
        }
    }

    private inline fun updateSuccessData(transform: (TeacherSyllabusUiData) -> TeacherSyllabusUiData) {
        val current = (_state.value as? UiState.Success)?.data ?: TeacherSyllabusUiData()
        _state.value = UiState.Success(transform(current))
    }

    private fun List<String>.toPendingUnitRows(): List<SyllabusUnitRow> {
        if (isEmpty()) return emptyList()
        val rows = mapIndexed { index, raw ->
            val order = index + 1
            val parts = raw.split(Regex("\\s+"), limit = 2)
            val firstToken = parts.firstOrNull().orEmpty()
            val label = if (parts.size == 2 && firstToken.any { it.isDigit() } && firstToken.length <= 20) {
                "$firstToken ${parts[1].trim()}"
            } else {
                "T$order $raw"
            }
            SyllabusTopicRow(label = label, hasIssue = false)
        }
        return listOf(
            SyllabusUnitRow(
                label = "",
                topicCount = rows.size,
                topics = rows
            )
        )
    }

    companion object {
        const val MAX_PDF_BYTES = 10L * 1024L * 1024L
        const val FIELD_ALREADY_PUBLISHED = "already_published"
        const val FIELD_EMPTY_TOPICS = "empty_topics"
        const val FIELD_PDF_TOO_LARGE = "pdf_too_large"
        const val FIELD_PDF_UNREADABLE = "pdf_unreadable"
    }
}

