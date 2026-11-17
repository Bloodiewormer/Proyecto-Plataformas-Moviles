package cr.ac.una.glifo.presentation.ui.screen

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import cr.ac.una.glifo.R
import cr.ac.una.glifo.core.common.AppError
import cr.ac.una.glifo.core.common.UiState
import cr.ac.una.glifo.domain.model.ConfidenceState
import cr.ac.una.glifo.domain.model.GlifoRole
import cr.ac.una.glifo.core.ui.component.*
import cr.ac.una.glifo.core.ui.theme.GlifoTheme
import cr.ac.una.glifo.presentation.viewmodel.SessionViewModel
import cr.ac.una.glifo.presentation.viewmodel.TeacherCoursesViewModel
import cr.ac.una.glifo.presentation.viewmodel.TeacherGapsViewModel
import cr.ac.una.glifo.presentation.viewmodel.TeacherGlossaryViewModel
import cr.ac.una.glifo.presentation.viewmodel.TeacherNewCourseViewModel
import cr.ac.una.glifo.presentation.viewmodel.TeacherSyllabusViewModel
import cr.ac.una.glifo.presentation.viewmodel.toMessage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

// ── Screen I1: Cursos Docente ─────────────────────────────────────────────────
@Composable
fun TeacherCoursesScreen(
    onNavigateBackOrMenu: () -> Unit,
    onNavigateToNewCourse: () -> Unit,
    onNavigateToSyllabus: () -> Unit,
    onNavigateDrawer: (String) -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onLogout: () -> Unit,
    viewModel: TeacherCoursesViewModel = hiltViewModel(),
    sessionViewModel: SessionViewModel = hiltViewModel()
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val colors = GlifoTheme.colors
    val context = LocalContext.current
    val user by sessionViewModel.currentUser.collectAsState()
    val uiState by viewModel.state.collectAsState()
    val activeCourseId by viewModel.activeCourseId.collectAsState()

    LaunchedEffect(Unit) { viewModel.load() }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = true,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = colors.surface,
                drawerContentColor = colors.textPrimary,
                drawerShape = RoundedCornerShape(topEnd = 0.dp, bottomEnd = 0.dp),
                windowInsets = WindowInsets(0, 0, 0, 0),
                modifier = Modifier
                    .width(280.dp)
                    .fillMaxHeight()
            ) {
                GlifoDrawerContent(
                    role = GlifoRole.Teacher,
                    currentScreen = "teacher_courses",
                    userName = user?.fullName.orEmpty(),
                    userEmail = user?.email.orEmpty(),
                    userInitials = user?.initials.orEmpty(),
                    onNavigate = { route ->
                        scope.launch { drawerState.close() }
                        onNavigateDrawer(route)
                    },
                    onProfileClick = {
                        scope.launch { drawerState.close() }
                        onNavigateToProfile()
                    },
                    onSettingsClick = {
                        scope.launch { drawerState.close() }
                        onNavigateToSettings()
                    },
                    onLogoutClick = {
                        scope.launch { drawerState.close() }
                        sessionViewModel.logout()
                        onLogout()
                    }
                )
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.background)
        ) {
            GlifoTopAppBar(
                title = stringResource(R.string.courses_title),
                onNavigationClick = { scope.launch { drawerState.open() } },
                actions = {
                    IconButton(onClick = onNavigateToNewCourse) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = stringResource(R.string.teacher_courses_create_description), tint = colors.textSecondary, modifier = Modifier.size(22.dp))
                    }
                }
            )

            when (val state = uiState) {
                is UiState.Loading -> GlifoLoadingBox()
                is UiState.Empty -> GlifoEmptyBox(
                    message = stringResource(R.string.courses_empty_no_courses),
                    actionText = stringResource(R.string.teacher_courses_create_new),
                    onAction = onNavigateToNewCourse
                )
                is UiState.Error -> GlifoErrorBox(state.error.toMessage(context), onRetry = viewModel::load)
                is UiState.Success -> {
                    val coursesWithStats = state.data
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .navigationBarsPadding()
                            .verticalScroll(rememberScrollState())
                            .padding(14.dp)
                    ) {
                        coursesWithStats.forEachIndexed { i, entry ->
                            val course = entry.course
                            val stats = entry.stats
                            val codeTermText = if (course.term.isNotBlank()) {
                                stringResource(R.string.teacher_courses_code_term, course.code, course.term)
                            } else {
                                course.code
                            }
                            GlifoStaggeredEntry(index = i) {
                                if (stats.hasSyllabus) {
                                    GlifoCard(
                                        onClick = { viewModel.select(course); onNavigateToSyllabus() },
                                        modifier = Modifier.padding(bottom = 10.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(text = course.name, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = colors.textPrimary)
                                            if (course.id == activeCourseId) GlifoBadge(text = stringResource(R.string.courses_active_badge))
                                        }
                                        Text(codeTermText, fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = colors.textSecondary, modifier = Modifier.padding(top = 2.dp))
                                        CovBar(v = stats.verified, r = stats.repaired, e = stats.escalated, u = stats.uncertain, modifier = Modifier.padding(vertical = 8.dp))
                                        Text(stringResource(R.string.teacher_courses_coverage_students, stats.coveragePercent, stats.studentCount), fontSize = 12.sp, color = colors.textSecondary)
                                    }
                                } else {
                                    GlifoCard(
                                        onClick = { viewModel.select(course); onNavigateToSyllabus() },
                                        borderColor = colors.alertLine,
                                        modifier = Modifier.padding(bottom = 14.dp)
                                    ) {
                                        Text(text = course.name, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = colors.textPrimary)
                                        Text(codeTermText, fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = colors.textSecondary, modifier = Modifier.padding(top = 2.dp))
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(top = 10.dp)
                                                .background(colors.alertFaint, RoundedCornerShape(6.dp))
                                                .padding(horizontal = 10.dp, vertical = 8.dp)
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Icon(Icons.Default.Warning, contentDescription = null, tint = colors.alert, modifier = Modifier.size(14.dp))
                                                Text(stringResource(R.string.teacher_courses_no_syllabus_warning), fontSize = 12.sp, color = colors.alert)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        GlifoButton(
                            text = stringResource(R.string.teacher_courses_create_new),
                            variant = GlifoButtonVariant.Secondary,
                            icon = Icons.Default.Add,
                            onClick = onNavigateToNewCourse
                        )
                    }
                }
            }
        }
    }
}

// ── Screen I2: Nuevo Curso ──────────────────────────────────────────────────────────
@Composable
fun TeacherNewCourseScreen(
    onNavigateBack: () -> Unit,
    onCreateSuccess: () -> Unit,
    viewModel: TeacherNewCourseViewModel = hiltViewModel()
) {
    val colors = GlifoTheme.colors
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()

    var name by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var cycle by remember { mutableStateOf("") }
    var createdJoinCode by remember { mutableStateOf<String?>(null) }
    var copied by remember { mutableStateOf(false) }
    var creating by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        GlifoTopAppBar(
            title = stringResource(R.string.teacher_new_course_title),
            isBack = true,
            onNavigationClick = onNavigateBack
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(14.dp)
        ) {
            GlifoField(label = stringResource(R.string.teacher_new_course_field_name), value = name, placeholder = stringResource(R.string.teacher_new_course_field_name_placeholder), onValueChange = { if (createdJoinCode == null) name = it })
            Spacer(modifier = Modifier.height(10.dp))
            GlifoField(label = stringResource(R.string.teacher_new_course_field_code), value = code, placeholder = stringResource(R.string.teacher_new_course_field_code_placeholder), onValueChange = { if (createdJoinCode == null) code = it })
            Spacer(modifier = Modifier.height(10.dp))
            GlifoField(label = stringResource(R.string.teacher_new_course_field_cycle), value = cycle, placeholder = stringResource(R.string.teacher_new_course_field_cycle_placeholder), onValueChange = { if (createdJoinCode == null) cycle = it })

            if (createdJoinCode != null) {
                Text(
                    text = stringResource(R.string.teacher_new_course_join_code_section),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.6.sp,
                    color = colors.textSecondary,
                    modifier = Modifier.padding(top = 14.dp, bottom = 8.dp)
                )

                GlifoCard(borderColor = colors.accentLine) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = createdJoinCode!!,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 24.sp,
                            color = colors.accentText,
                            letterSpacing = 3.sp
                        )
                        Icon(
                            imageVector = if (copied) Icons.Default.Check else Icons.Default.ContentCopy,
                            contentDescription = stringResource(R.string.teacher_new_course_copy_code_description),
                            tint = if (copied) colors.verified else colors.accentText,
                            modifier = Modifier
                                .size(18.dp)
                                .clickable {
                                    clipboard.setText(AnnotatedString(createdJoinCode!!))
                                    copied = true
                                }
                        )
                    }
                    Text(stringResource(R.string.teacher_new_course_join_code_note), fontSize = 11.sp, color = colors.textSecondary, modifier = Modifier.padding(top = 8.dp))
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            GlifoCard(
                backgroundColor = colors.accentFaint,
                borderColor = colors.accentLine
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(imageVector = Icons.Default.FormatListBulleted, contentDescription = null, tint = colors.accentText, modifier = Modifier.size(18.dp))
                    Column {
                        Text(stringResource(R.string.teacher_new_course_syllabus_later_title), fontSize = 13.sp, fontWeight = FontWeight.Medium, color = colors.textPrimary)
                        Text(stringResource(R.string.teacher_new_course_syllabus_later_description), fontSize = 11.sp, color = colors.textSecondary, modifier = Modifier.padding(top = 3.dp))
                    }
                }
            }

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(10.dp))
                GlifoBanner(text = errorMessage!!)
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (createdJoinCode == null) {
                GlifoButton(
                    text = if (creating) stringResource(R.string.teacher_new_course_button_creating) else stringResource(R.string.teacher_new_course_button_create),
                    enabled = name.isNotBlank() && code.isNotBlank() && cycle.isNotBlank() && !creating,
                    onClick = {
                        creating = true
                        errorMessage = null
                        scope.launch {
                            when (val result = viewModel.createCourse(name, code, cycle)) {
                                is cr.ac.una.glifo.domain.error.Result.Success -> {
                                    creating = false
                                    createdJoinCode = result.value.joinCode
                                }
                                is cr.ac.una.glifo.domain.error.Result.Failure -> {
                                    creating = false
                                    errorMessage = result.error.toMessage(context)
                                }
                            }
                        }
                    }
                )
            } else {
                GlifoButton(
                    text = stringResource(R.string.common_close),
                    onClick = onCreateSuccess
                )
            }
        }
    }
}

// ── Screen I3: Publicar Temario ─────────────────────────────────────────────────
@Composable
fun TeacherSyllabusScreen(
    onNavigateBack: () -> Unit,
    onPublishSuccess: () -> Unit,
    viewModel: TeacherSyllabusViewModel = hiltViewModel()
) {
    val colors = GlifoTheme.colors
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showAddTopicDialog by remember { mutableStateOf(false) }
    var publishError by remember { mutableStateOf<String?>(null) }
    val uiState by viewModel.state.collectAsState()

    val pdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        publishError = null
        scope.launch {
            when (val picked = readSyllabusPdfFromUri(context, uri)) {
                is PickedPdfResult.Success -> viewModel.uploadSyllabusPdf(picked.fileName, picked.bytes)
                is PickedPdfResult.Error -> viewModel.reportPdfError(picked.field)
            }
        }
    }

    val onUploadPdfClick: () -> Unit = {
        pdfLauncher.launch(arrayOf("application/pdf"))
    }

    val isUploading = (uiState as? UiState.Success)?.data?.isUploading == true

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        GlifoTopAppBar(
            title = stringResource(R.string.teacher_syllabus_title),
            isBack = true,
            onNavigationClick = onNavigateBack,
            actions = {
                TextButton(
                    enabled = !isUploading,
                    onClick = {
                        publishError = null
                        scope.launch {
                            when (val result = viewModel.publishSyllabus()) {
                                is cr.ac.una.glifo.domain.error.Result.Success -> onPublishSuccess()
                                is cr.ac.una.glifo.domain.error.Result.Failure -> {
                                    publishError = result.error.toSyllabusErrorMessage(context)
                                }
                            }
                        }
                    }
                ) {
                    Text(
                        stringResource(R.string.teacher_syllabus_publish_button),
                        color = if (isUploading) colors.textSecondary else colors.accentText,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        )

        if (publishError != null) {
            Box(modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)) {
                GlifoBanner(text = publishError!!, isWarning = true)
            }
        }

        when (val state = uiState) {
            is UiState.Loading -> GlifoLoadingBox()
            is UiState.Empty -> GlifoLoadingBox()
            is UiState.Error -> GlifoErrorBox(state.error.toMessage(context), onRetry = viewModel::load)
            is UiState.Success -> {
                val data = state.data
                val units = data.units
                val fileName = data.fileName
                val totalTopics = units.sumOf { it.topicCount }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .navigationBarsPadding()
                        .verticalScroll(rememberScrollState())
                        .padding(14.dp)
                ) {
                    data.actionError?.let { error ->
                        GlifoBanner(
                            text = error.toSyllabusErrorMessage(context),
                            isWarning = true,
                            modifier = Modifier
                                .padding(bottom = 12.dp)
                                .clickable { viewModel.dismissActionError() }
                        )
                    }

                    if (data.isUploading) {
                        GlifoCard(
                            backgroundColor = colors.accentFaint,
                            borderColor = colors.accentLine,
                            modifier = Modifier.padding(bottom = 12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = colors.accent
                                )
                                Text(
                                    text = stringResource(R.string.teacher_syllabus_uploading_ai),
                                    fontSize = 14.sp,
                                    color = colors.textPrimary
                                )
                            }
                        }
                    }

                    if (data.canUploadPdf && fileName == null && units.isEmpty()) {
                        GlifoCard(borderColor = colors.accentLine) {
                            Row(
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    Icons.Default.PictureAsPdf,
                                    contentDescription = null,
                                    tint = colors.accentText,
                                    modifier = Modifier.size(32.dp)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = stringResource(R.string.teacher_syllabus_empty_title),
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = colors.textPrimary
                                    )
                                    Text(
                                        text = stringResource(R.string.teacher_syllabus_empty_description),
                                        fontSize = 13.sp,
                                        color = colors.textSecondary,
                                        lineHeight = 18.sp,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                            GlifoButton(
                                text = stringResource(R.string.teacher_syllabus_upload_pdf),
                                icon = Icons.Default.UploadFile,
                                enabled = !data.isUploading,
                                onClick = onUploadPdfClick
                            )
                        }
                    } else {
                        if (fileName != null) {
                            val metaText = when {
                                data.formattedSize != null && data.parsedAtDisplay != null ->
                                    stringResource(R.string.teacher_syllabus_file_meta_full, data.formattedSize, data.parsedAtDisplay)
                                data.formattedSize != null -> data.formattedSize
                                data.parsedAtDisplay != null ->
                                    stringResource(R.string.teacher_syllabus_file_meta_date_only, data.parsedAtDisplay)
                                else -> null
                            }

                            GlifoCard(borderColor = colors.accentLine) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = colors.accentText, modifier = Modifier.size(32.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(fileName, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = colors.textPrimary)
                                        if (metaText != null) {
                                            Text(metaText, fontSize = 12.sp, color = colors.textSecondary, modifier = Modifier.padding(top = 2.dp))
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                if (data.canUploadPdf) {
                                    GlifoButton(
                                        text = stringResource(R.string.teacher_syllabus_upload_pdf),
                                        variant = GlifoButtonVariant.Secondary,
                                        small = true,
                                        icon = Icons.Default.UploadFile,
                                        enabled = !data.isUploading,
                                        onClick = onUploadPdfClick
                                    )
                                } else {
                                    Row(
                                        verticalAlignment = Alignment.Top,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Info,
                                            contentDescription = null,
                                            tint = colors.accentText,
                                            modifier = Modifier.size(16.dp).padding(top = 1.dp)
                                        )
                                        Text(
                                            text = stringResource(R.string.teacher_syllabus_published_note),
                                            fontSize = 12.sp,
                                            color = colors.textSecondary,
                                            lineHeight = 17.sp
                                        )
                                    }
                                }
                            }
                        } else if (data.canUploadPdf) {
                            GlifoButton(
                                text = stringResource(R.string.teacher_syllabus_upload_pdf),
                                variant = GlifoButtonVariant.Secondary,
                                icon = Icons.Default.UploadFile,
                                enabled = !data.isUploading,
                                onClick = onUploadPdfClick
                            )
                        } else {
                            GlifoCard(
                                backgroundColor = colors.accentFaint,
                                borderColor = colors.accentLine
                            ) {
                                Row(
                                    verticalAlignment = Alignment.Top,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = null,
                                        tint = colors.accentText,
                                        modifier = Modifier.size(16.dp).padding(top = 1.dp)
                                    )
                                    Text(
                                        text = stringResource(R.string.teacher_syllabus_published_note),
                                        fontSize = 12.sp,
                                        color = colors.textSecondary,
                                        lineHeight = 17.sp
                                    )
                                }
                            }
                        }

                        if (units.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = colors.verified, modifier = Modifier.size(18.dp).padding(top = 1.dp))
                                Text(
                                    text = stringResource(
                                        if (data.isPublished) R.string.teacher_syllabus_saved_summary else R.string.teacher_syllabus_pending_summary,
                                        units.size,
                                        totalTopics
                                    ),
                                    fontSize = 12.sp,
                                    color = colors.textSecondary,
                                    lineHeight = 18.sp
                                )
                            }

                            Text(
                                text = stringResource(R.string.teacher_syllabus_structure_section),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 0.6.sp,
                                color = colors.textSecondary,
                                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                            )

                            GlifoCard(tight = true) {
                                units.forEachIndexed { unitIdx, unit ->
                                    val unitLabel = unit.label.ifBlank { stringResource(R.string.teacher_syllabus_pending_unit_label) }
                                    GlifoItem(compact = true, backgroundColor = colors.surfaceHigh, showBottomBorder = unit.topics.isEmpty() && unitIdx < units.size - 1) {
                                        Icon(
                                            imageVector = if (unit.topics.isNotEmpty()) Icons.Default.KeyboardArrowDown else Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                            contentDescription = null,
                                            tint = colors.accentText,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(unitLabel, fontWeight = FontWeight.Medium, fontSize = 15.sp, color = colors.textPrimary, modifier = Modifier.weight(1f))
                                        Text(stringResource(R.string.course_detail_topic_count, unit.topicCount), fontSize = 12.sp, color = colors.textSecondary)
                                        Icon(Icons.Default.Edit, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(18.dp))
                                    }
                                    unit.topics.forEachIndexed { i, topic ->
                                        GlifoItem(
                                            compact = true,
                                            backgroundColor = if (topic.hasIssue) colors.alertFaint else Color.Transparent,
                                            showBottomBorder = !(unitIdx == units.size - 1 && i == unit.topics.size - 1),
                                            modifier = Modifier.padding(start = 24.dp)
                                        ) {
                                            Text(topic.label, fontSize = 15.sp, color = if (topic.hasIssue) colors.alert else colors.textPrimary, modifier = Modifier.weight(1f))
                                            Icon(Icons.Default.Edit, contentDescription = null, tint = if (topic.hasIssue) colors.alert else colors.textSecondary, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    GlifoButton(
                        text = stringResource(R.string.teacher_syllabus_add_topic_manually),
                        variant = GlifoButtonVariant.Secondary,
                        icon = Icons.Default.Add,
                        enabled = !data.isUploading,
                        onClick = { showAddTopicDialog = true }
                    )
                }
            }
        }
    }

    if (showAddTopicDialog) {
        AddTopicDialog(
            onDismiss = { showAddTopicDialog = false },
            onAdd = { label ->
                viewModel.addTopic(label)
                showAddTopicDialog = false
            }
        )
    }
}

private sealed interface PickedPdfResult {
    data class Success(val fileName: String, val bytes: ByteArray) : PickedPdfResult
    data class Error(val field: String) : PickedPdfResult
}

private suspend fun readSyllabusPdfFromUri(context: Context, uri: Uri): PickedPdfResult =
    withContext(Dispatchers.IO) {
        try {
            var displayName = "syllabus.pdf"
            var reportedSize: Long? = null
            context.contentResolver.query(
                uri,
                arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE),
                null,
                null,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIdx >= 0 && !cursor.isNull(nameIdx)) {
                        cursor.getString(nameIdx)?.trim()?.takeIf { it.isNotEmpty() }?.let { displayName = it }
                    }
                    val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (sizeIdx >= 0 && !cursor.isNull(sizeIdx)) {
                        reportedSize = cursor.getLong(sizeIdx)
                    }
                }
            }

            if ((reportedSize ?: 0L) > TeacherSyllabusViewModel.MAX_PDF_BYTES) {
                return@withContext PickedPdfResult.Error(TeacherSyllabusViewModel.FIELD_PDF_TOO_LARGE)
            }

            val stream = context.contentResolver.openInputStream(uri)
                ?: return@withContext PickedPdfResult.Error(TeacherSyllabusViewModel.FIELD_PDF_UNREADABLE)

            val bytes = stream.use { input ->
                val out = ByteArrayOutputStream()
                val buffer = ByteArray(8192)
                var total = 0L
                while (true) {
                    val read = input.read(buffer)
                    if (read == -1) break
                    total += read
                    // Corta antes de leer archivos enormes completos a memoria.
                    if (total > TeacherSyllabusViewModel.MAX_PDF_BYTES) {
                        return@withContext PickedPdfResult.Error(TeacherSyllabusViewModel.FIELD_PDF_TOO_LARGE)
                    }
                    out.write(buffer, 0, read)
                }
                out.toByteArray()
            }

            if (bytes.isEmpty()) {
                PickedPdfResult.Error(TeacherSyllabusViewModel.FIELD_PDF_UNREADABLE)
            } else {
                PickedPdfResult.Success(displayName, bytes)
            }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Exception) {
            PickedPdfResult.Error(TeacherSyllabusViewModel.FIELD_PDF_UNREADABLE)
        }
    }

internal fun AppError.toSyllabusErrorMessage(context: Context): String = when {
    this is AppError.Validation && field == TeacherSyllabusViewModel.FIELD_ALREADY_PUBLISHED ->
        context.getString(R.string.teacher_syllabus_already_published)
    this is AppError.Validation && (field == TeacherSyllabusViewModel.FIELD_EMPTY_TOPICS || field == "topics") ->
        context.getString(R.string.teacher_syllabus_empty_topics_error)
    this is AppError.Validation && field == TeacherSyllabusViewModel.FIELD_PDF_TOO_LARGE ->
        context.getString(R.string.teacher_syllabus_error_too_large)
    this is AppError.Validation && field == TeacherSyllabusViewModel.FIELD_PDF_UNREADABLE ->
        context.getString(R.string.teacher_syllabus_error_unreadable)
    this is AppError.Server && code == "409" ->
        message.ifBlank { context.getString(R.string.teacher_syllabus_already_published) }
    else -> toMessage(context)
}

@Composable
private fun AddTopicDialog(onDismiss: () -> Unit, onAdd: (String) -> Unit) {
    val colors = GlifoTheme.colors
    var label by remember { mutableStateOf("") }
    Dialog(onDismissRequest = onDismiss) {
        GlifoCard {
            Text(stringResource(R.string.teacher_add_topic_dialog_title), fontSize = 17.sp, fontWeight = FontWeight.Medium, color = colors.textPrimary)
            Spacer(modifier = Modifier.height(4.dp))
            Text(stringResource(R.string.teacher_add_topic_dialog_description), fontSize = 12.sp, color = colors.textSecondary)
            Spacer(modifier = Modifier.height(12.dp))
            GlifoField(label = stringResource(R.string.teacher_add_topic_field_label), value = label, placeholder = stringResource(R.string.teacher_add_topic_field_placeholder), onValueChange = { label = it })
            Spacer(modifier = Modifier.height(14.dp))
            GlifoButton(text = stringResource(R.string.teacher_add_button), enabled = label.isNotBlank(), onClick = { onAdd(label) })
        }
    }
}

// ── Screen I4: Glosario Docente ───────────────────────────────────────────────
@Composable
fun TeacherGlossaryScreen(
    onNavigateBackOrMenu: () -> Unit,
    onNavigateDrawer: (String) -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onLogout: () -> Unit,
    viewModel: TeacherGlossaryViewModel = hiltViewModel(),
    sessionViewModel: SessionViewModel = hiltViewModel()
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val colors = GlifoTheme.colors
    val context = LocalContext.current
    val user by sessionViewModel.currentUser.collectAsState()
    var searchOpen by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }
    val uiState by viewModel.state.collectAsState()
    val actionError by viewModel.actionError.collectAsState()

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = true,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = colors.surface,
                drawerContentColor = colors.textPrimary,
                drawerShape = RoundedCornerShape(topEnd = 0.dp, bottomEnd = 0.dp),
                windowInsets = WindowInsets(0, 0, 0, 0),
                modifier = Modifier
                    .width(280.dp)
                    .fillMaxHeight()
            ) {
                GlifoDrawerContent(
                    role = GlifoRole.Teacher,
                    currentScreen = "glossary",
                    userName = user?.fullName.orEmpty(),
                    userEmail = user?.email.orEmpty(),
                    userInitials = user?.initials.orEmpty(),
                    onNavigate = { route ->
                        scope.launch { drawerState.close() }
                        onNavigateDrawer(route)
                    },
                    onProfileClick = {
                        scope.launch { drawerState.close() }
                        onNavigateToProfile()
                    },
                    onSettingsClick = {
                        scope.launch { drawerState.close() }
                        onNavigateToSettings()
                    },
                    onLogoutClick = {
                        scope.launch { drawerState.close() }
                        sessionViewModel.logout()
                        onLogout()
                    }
                )
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.background)
        ) {
            GlifoTopAppBar(
                title = stringResource(R.string.teacher_glossary_title),
                onNavigationClick = { scope.launch { drawerState.open() } },
                actions = {
                    IconButton(onClick = { searchOpen = !searchOpen; if (!searchOpen) query = "" }) {
                        Icon(Icons.Default.Search, contentDescription = stringResource(R.string.common_search), tint = colors.textSecondary, modifier = Modifier.size(20.dp))
                    }
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = stringResource(R.string.teacher_glossary_add_description), tint = colors.textSecondary, modifier = Modifier.size(20.dp))
                    }
                }
            )

            AnimatedVisibility(visible = searchOpen) {
                Box(modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
                    GlifoField(label = stringResource(R.string.teacher_glossary_search_placeholder), value = query, onValueChange = { query = it }, isFocused = true)
                }
            }

            when (val state = uiState) {
                is UiState.Loading -> GlifoLoadingBox()
                is UiState.Empty -> GlifoEmptyBox(
                    message = stringResource(R.string.teacher_glossary_no_match),
                    actionText = stringResource(R.string.common_retry),
                    onAction = viewModel::load
                )
                is UiState.Error -> GlifoErrorBox(state.error.toMessage(context), onRetry = viewModel::load)
                is UiState.Success -> {
                    val allEntries = state.data.entries
                    val entries = allEntries.filter { query.isBlank() || it.term.contains(query, ignoreCase = true) }
                    val suggestions = state.data.suggestions

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .navigationBarsPadding()
                            .verticalScroll(rememberScrollState())
                            .padding(14.dp)
                    ) {
                        actionError?.let { error ->
                            GlifoBanner(
                                text = error.toMessage(context),
                                isWarning = true,
                                modifier = Modifier
                                    .padding(bottom = 12.dp)
                                    .clickable { viewModel.dismissActionError() }
                            )
                        }

                        GlifoCard(
                            backgroundColor = colors.accentFaint,
                            borderColor = colors.accentLine
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Icon(Icons.Default.GpsFixed, contentDescription = null, tint = colors.accentText, modifier = Modifier.size(20.dp))
                                Column {
                                    Text(stringResource(R.string.teacher_glossary_not_dictionary_title), fontSize = 15.sp, fontWeight = FontWeight.Medium, color = colors.textPrimary)
                                    Text(stringResource(R.string.teacher_glossary_not_dictionary_description), fontSize = 12.sp, color = colors.textSecondary, modifier = Modifier.padding(top = 4.dp))
                                }
                            }
                        }

                        Text(
                            text = stringResource(R.string.teacher_glossary_entries_count, entries.size),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.6.sp,
                            color = colors.textSecondary,
                            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                        )

                        GlifoCard(tight = true) {
                            entries.forEachIndexed { i, entry ->
                                GlifoItem(showBottomBorder = i < entries.size - 1) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(text = entry.term, fontSize = 16.sp, color = colors.textPrimary)
                                        Text(text = entry.notation, fontSize = 13.sp, fontFamily = FontFamily.Monospace, color = colors.textSecondary, modifier = Modifier.padding(top = 2.dp))
                                    }
                                    val bg = if (entry.isSymbol) colors.repairedSoft else colors.accentSoft
                                    val fg = if (entry.isSymbol) colors.repaired else colors.accentText
                                    GlifoBadge(text = entry.tag, backgroundColor = bg, textColor = fg)
                                }
                            }
                        }

                        if (suggestions.isNotEmpty()) {
                            Text(
                                text = stringResource(R.string.teacher_glossary_suggestions_section),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 0.6.sp,
                                color = colors.textSecondary,
                                modifier = Modifier.padding(top = 18.dp, bottom = 8.dp)
                            )

                            GlifoCard(tight = true) {
                                suggestions.forEachIndexed { i, s ->
                                    GlifoItem(compact = true, showBottomBorder = i < suggestions.size - 1) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(s.term, fontSize = 15.sp, color = colors.textPrimary)
                                            Text(s.meta, fontSize = 12.sp, color = colors.textSecondary, modifier = Modifier.padding(top = 2.dp))
                                        }
                                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                            Icon(
                                                Icons.Default.Close,
                                                contentDescription = stringResource(R.string.teacher_glossary_discard_description),
                                                tint = colors.textSecondary,
                                                modifier = Modifier.size(20.dp).clickable { viewModel.discardSuggestion(s.id) }
                                            )
                                            Icon(
                                                Icons.Default.Check,
                                                contentDescription = stringResource(R.string.teacher_glossary_accept_description),
                                                tint = colors.verified,
                                                modifier = Modifier.size(20.dp).clickable { viewModel.approveSuggestion(s.id) }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        if (entries.isEmpty()) {
                            GlifoBanner(text = stringResource(R.string.teacher_glossary_no_match), modifier = Modifier.padding(top = 12.dp))
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddGlossaryEntryDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { term, notation, isSymbol ->
                viewModel.addEntry(term, notation, isSymbol)
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun AddGlossaryEntryDialog(
    onDismiss: () -> Unit,
    onAdd: (term: String, notation: String, isSymbol: Boolean) -> Unit
) {
    val colors = GlifoTheme.colors
    var term by remember { mutableStateOf("") }
    var notation by remember { mutableStateOf("") }
    var isSymbol by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        GlifoCard {
            Text(stringResource(R.string.teacher_glossary_add_dialog_title), fontSize = 17.sp, fontWeight = FontWeight.Medium, color = colors.textPrimary)
            Spacer(modifier = Modifier.height(12.dp))
            GlifoField(label = stringResource(R.string.teacher_glossary_field_term), value = term, onValueChange = { term = it })
            Spacer(modifier = Modifier.height(8.dp))
            GlifoField(label = stringResource(R.string.teacher_glossary_field_notation), value = notation, placeholder = stringResource(R.string.teacher_glossary_field_notation_placeholder), onValueChange = { notation = it })
            Spacer(modifier = Modifier.height(10.dp))
            val termLabel = stringResource(R.string.teacher_glossary_field_term)
            val symbolLabel = stringResource(R.string.teacher_glossary_type_symbol)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(false to termLabel, true to symbolLabel).forEach { (value, label) ->
                    val selected = isSymbol == value
                    Box(
                        modifier = Modifier
                            .background(if (selected) colors.accentSoft else colors.neutralSoft, RoundedCornerShape(999.dp))
                            .clickable { isSymbol = value }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(label, fontSize = 12.sp, color = if (selected) colors.accentText else colors.textSecondary)
                    }
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
            GlifoButton(
                text = stringResource(R.string.teacher_add_button),
                enabled = term.isNotBlank() && notation.isNotBlank(),
                onClick = { onAdd(term, notation, isSymbol) }
            )
        }
    }
}

// ── Screen I5: Brechas del Grupo ──────────────────────────────────────────────
@Composable
fun TeacherGapsScreen(
    onNavigateBackOrMenu: () -> Unit,
    onNavigateToTopicDetail: (String) -> Unit,
    onNavigateDrawer: (String) -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onLogout: () -> Unit,
    viewModel: TeacherGapsViewModel = hiltViewModel(),
    sessionViewModel: SessionViewModel = hiltViewModel()
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val colors = GlifoTheme.colors
    val context = LocalContext.current
    val user by sessionViewModel.currentUser.collectAsState()
    val uiState by viewModel.state.collectAsState()
    val activeCourseName = LocalActiveCourse.current.name ?: stringResource(R.string.drawer_no_active_course)
    val shareTitle = stringResource(R.string.teacher_gaps_share_title, activeCourseName)
    val shareSummaryLineFormat = stringResource(R.string.teacher_gaps_share_summary_line)
    val shareGapLineFormat = stringResource(R.string.teacher_gaps_share_gap_line)
    val shareChooserTitle = stringResource(R.string.teacher_gaps_share_chooser_title)

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = true,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = colors.surface,
                drawerContentColor = colors.textPrimary,
                drawerShape = RoundedCornerShape(topEnd = 0.dp, bottomEnd = 0.dp),
                windowInsets = WindowInsets(0, 0, 0, 0),
                modifier = Modifier
                    .width(280.dp)
                    .fillMaxHeight()
            ) {
                GlifoDrawerContent(
                    role = GlifoRole.Teacher,
                    currentScreen = "gaps",
                    userName = user?.fullName.orEmpty(),
                    userEmail = user?.email.orEmpty(),
                    userInitials = user?.initials.orEmpty(),
                    onNavigate = { route ->
                        scope.launch { drawerState.close() }
                        onNavigateDrawer(route)
                    },
                    onProfileClick = {
                        scope.launch { drawerState.close() }
                        onNavigateToProfile()
                    },
                    onSettingsClick = {
                        scope.launch { drawerState.close() }
                        onNavigateToSettings()
                    },
                    onLogoutClick = {
                        scope.launch { drawerState.close() }
                        sessionViewModel.logout()
                        onLogout()
                    }
                )
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.background)
        ) {
            GlifoTopAppBar(
                title = activeCourseName,
                subtitle = stringResource(R.string.teacher_gaps_subtitle),
                onNavigationClick = { scope.launch { drawerState.open() } },
                actions = {
                    val currentState = uiState
                    IconButton(onClick = {
                        if (currentState is UiState.Success) {
                            val stats = currentState.data.stats
                            val gaps = currentState.data.gaps
                            val summary = buildString {
                                appendLine(shareTitle)
                                appendLine(String.format(shareSummaryLineFormat, stats.studentCount, stats.coveragePercent))
                                appendLine()
                                gaps.forEach { gap -> appendLine(String.format(shareGapLineFormat, gap.label, gap.fraction, gap.percent.toInt())) }
                            }
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, shareTitle)
                                putExtra(Intent.EXTRA_TEXT, summary)
                            }
                            context.startActivity(Intent.createChooser(shareIntent, shareChooserTitle))
                        }
                    }) {
                        Icon(Icons.Default.Upload, contentDescription = stringResource(R.string.teacher_gaps_export_description), tint = colors.textSecondary, modifier = Modifier.size(20.dp))
                    }
                }
            )

            when (val state = uiState) {
                is UiState.Loading -> GlifoLoadingBox()
                is UiState.Empty -> GlifoEmptyBox(
                    message = stringResource(R.string.courses_empty_no_courses),
                    actionText = stringResource(R.string.common_nav_back),
                    onAction = onNavigateBackOrMenu
                )
                is UiState.Error -> GlifoErrorBox(state.error.toMessage(context), onRetry = viewModel::load)
                is UiState.Success -> {
                    val stats = state.data.stats
                    val gaps = state.data.gaps

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .navigationBarsPadding()
                            .verticalScroll(rememberScrollState())
                            .padding(14.dp)
                    ) {
                        GlifoCard {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Text(stringResource(R.string.teacher_gaps_student_count, stats.studentCount), fontSize = 18.sp, fontWeight = FontWeight.Medium, color = colors.textPrimary)
                                Text("${stats.coveragePercent}%", fontSize = 24.sp, fontWeight = FontWeight.SemiBold, color = colors.accentText)
                            }
                            CovBar(v = stats.verified, r = stats.repaired, e = stats.escalated, u = stats.uncertain, modifier = Modifier.padding(vertical = 10.dp))
                            Text(stringResource(R.string.teacher_gaps_coverage_updated_today), fontSize = 12.sp, color = colors.textSecondary)
                        }

                        Text(
                            text = stringResource(R.string.teacher_home_gaps_section),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.6.sp,
                            color = colors.textSecondary,
                            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                        )

                        if (gaps.isEmpty()) {
                            Text(stringResource(R.string.teacher_gaps_empty), fontSize = 13.sp, color = colors.textSecondary)
                        } else GlifoCard(tight = true) {
                            gaps.forEachIndexed { i, gap ->
                                GlifoItem(showBottomBorder = i < gaps.size - 1, onClick = { onNavigateToTopicDetail(gap.label) }) {
                                    GlifoConfidenceChip(state = gap.state)
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(text = gap.label, fontSize = 15.sp, color = colors.textPrimary)
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(6.dp)
                                                .padding(top = 7.dp)
                                                .clip(RoundedCornerShape(999.dp))
                                                .background(colors.surfaceHigh)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxHeight()
                                                    .fillMaxWidth(gap.percent / 100f)
                                                    .background(
                                                        when (gap.state) {
                                                            ConfidenceState.Escalated -> colors.escalated
                                                            ConfidenceState.Uncertain -> colors.uncertain
                                                            else -> colors.repaired
                                                        }
                                                    )
                                            )
                                        }
                                    }
                                    Text(text = gap.fraction, fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = colors.textSecondary)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(Icons.Default.Shield, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(18.dp).padding(top = 1.dp))
                            Text(stringResource(R.string.teacher_gaps_privacy_note), fontSize = 12.sp, color = colors.textSecondary)
                        }
                    }
                }
            }
        }
    }
}
