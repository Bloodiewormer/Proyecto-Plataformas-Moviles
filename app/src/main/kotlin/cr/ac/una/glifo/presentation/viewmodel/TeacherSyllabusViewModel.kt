package cr.ac.una.glifo.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cr.ac.una.glifo.core.common.AppError
import cr.ac.una.glifo.core.common.UiState
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
    val fileName: String,
    val units: List<SyllabusUnitRow>,
    val isPublished: Boolean = false
)

@HiltViewModel
class TeacherSyllabusViewModel @Inject constructor(
    private val teacherRepository: TeacherRepository
) : ViewModel() {

    private val _state = MutableStateFlow<UiState<TeacherSyllabusUiData>>(UiState.Loading)
    val state: StateFlow<UiState<TeacherSyllabusUiData>> = _state.asStateFlow()

    private val pendingTopics = mutableListOf<String>()
    private var currentFileName: String = ""
    private var isPublished: Boolean = false

    init { load() }

    fun load() {
        viewModelScope.launch {
            _state.value = UiState.Loading
            val fileName = when (val fileNameResult = teacherRepository.getSyllabusFileName()) {
                is Result.Failure -> {
                    _state.value = UiState.Error(fileNameResult.error)
                    return@launch
                }
                is Result.Success -> fileNameResult.value
            }

            val published = when (val publishedResult = teacherRepository.isSyllabusPublished()) {
                is Result.Failure -> {
                    _state.value = UiState.Error(publishedResult.error)
                    return@launch
                }
                is Result.Success -> publishedResult.value
            }

            if (currentFileName.isNotEmpty() && currentFileName != fileName) {
                pendingTopics.clear()
            }
            currentFileName = fileName
            isPublished = published

            if (isPublished) {
                pendingTopics.clear()
                when (val unitsResult = teacherRepository.getSyllabusUnits()) {
                    is Result.Failure -> _state.value = UiState.Error(unitsResult.error)
                    is Result.Success -> _state.value = UiState.Success(
                        TeacherSyllabusUiData(
                            fileName = currentFileName,
                            units = unitsResult.value,
                            isPublished = true
                        )
                    )
                }
            } else {
                _state.value = UiState.Success(
                    TeacherSyllabusUiData(
                        fileName = currentFileName,
                        units = pendingTopics.toPendingUnitRows(),
                        isPublished = false
                    )
                )
            }
        }
    }

    fun addTopic(label: String) {
        val trimmed = label.trim()
        if (trimmed.isBlank()) return
        if (!isPublished) {
            pendingTopics.add(trimmed)
            _state.value = UiState.Success(
                TeacherSyllabusUiData(
                    fileName = currentFileName,
                    units = pendingTopics.toPendingUnitRows(),
                    isPublished = false
                )
            )
            return
        }
        viewModelScope.launch {
            when (val result = teacherRepository.addSyllabusTopic(trimmed)) {
                is Result.Success -> load()
                is Result.Failure -> _state.value = UiState.Error(result.error)
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
        return teacherRepository.publishSyllabus(pendingTopics.toList()).also { result ->
            if (result is Result.Success) {
                pendingTopics.clear()
                isPublished = true
                load()
            }
        }
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
        const val FIELD_ALREADY_PUBLISHED = "already_published"
        const val FIELD_EMPTY_TOPICS = "empty_topics"
    }
}

