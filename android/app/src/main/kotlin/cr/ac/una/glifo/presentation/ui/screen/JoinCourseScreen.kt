package cr.ac.una.glifo.presentation.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import cr.ac.una.glifo.R
import cr.ac.una.glifo.core.common.AppError
import cr.ac.una.glifo.core.common.UiState
import cr.ac.una.glifo.core.ui.component.*
import cr.ac.una.glifo.core.ui.theme.GlifoTheme
import cr.ac.una.glifo.presentation.viewmodel.JoinCourseViewModel
import cr.ac.una.glifo.presentation.viewmodel.toMessage

@Composable
fun JoinCourseScreen(
    onNavigateBack: () -> Unit,
    onJoinSuccess: () -> Unit,
    viewModel: JoinCourseViewModel = hiltViewModel()
) {
    val colors = GlifoTheme.colors
    val context = LocalContext.current
    var courseCode by remember { mutableStateOf("") }
    val joinState by viewModel.joinState.collectAsState()

    LaunchedEffect(joinState) {
        if (joinState is UiState.Success) {
            onJoinSuccess()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        GlifoTopAppBar(
            title = stringResource(R.string.join_course_title),
            isBack = true,
            onNavigationClick = onNavigateBack
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(14.dp)
        ) {
            Text(
                text = stringResource(R.string.join_course_instructions),
                fontSize = 13.sp,
                color = colors.textSecondary,
                lineHeight = 20.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            GlifoField(
                label = stringResource(R.string.join_course_field_label),
                value = courseCode,
                onValueChange = { courseCode = it; viewModel.reset() },
                isFocused = true
            )

            Spacer(modifier = Modifier.height(10.dp))

            GlifoButton(
                text = stringResource(R.string.join_course_join_button),
                enabled = courseCode.isNotBlank() && joinState !is UiState.Loading,
                onClick = { viewModel.joinCourse(courseCode) }
            )

            if (joinState is UiState.Loading) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(top = 18.dp),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator(color = colors.accent) }
            }

            if (joinState is UiState.Error) {
                val error = (joinState as UiState.Error).error
                val errorMsg = when {
                    error is AppError.Server && (error.code == "404" || error.code == "NOT_FOUND") ->
                        stringResource(R.string.join_course_invalid_code)
                    error is AppError.Validation ->
                        stringResource(R.string.join_course_invalid_code)
                    error is AppError.Server && (error.code == "409" || error.code == "DUPLICATE" || error.code == "CONFLICT") ->
                        stringResource(R.string.join_course_already_enrolled)
                    error is AppError.NoConnection ->
                        stringResource(R.string.error_no_connection)
                    else -> error.toMessage(context)
                }
                GlifoBanner(
                    text = errorMsg,
                    icon = Icons.Default.Warning,
                    isWarning = true,
                    modifier = Modifier.padding(top = 18.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = colors.textSecondary,
                    modifier = Modifier.size(14.dp).padding(top = 1.dp)
                )
                Text(
                    text = stringResource(R.string.join_course_privacy_note),
                    fontSize = 11.sp,
                    color = colors.textSecondary,
                    lineHeight = 16.sp
                )
            }
        }
    }
}

