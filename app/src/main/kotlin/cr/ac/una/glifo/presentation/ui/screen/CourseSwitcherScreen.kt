package cr.ac.una.glifo.presentation.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import cr.ac.una.glifo.R
import cr.ac.una.glifo.core.common.UiState
import cr.ac.una.glifo.core.ui.component.*
import cr.ac.una.glifo.core.ui.theme.GlifoTheme
import cr.ac.una.glifo.domain.model.GlifoRole
import cr.ac.una.glifo.presentation.viewmodel.CourseSwitcherViewModel
import cr.ac.una.glifo.presentation.viewmodel.toMessage

/**
 * Selector del curso activo. Se abre desde "Cambiar" en el menú lateral o tocando el nombre
 * del curso en el inicio; elegir un curso lo vuelve el activo en toda la app y regresa.
 */
@Composable
fun CourseSwitcherScreen(
    onNavigateBack: () -> Unit,
    onJoinCourse: () -> Unit,
    onCreateCourse: () -> Unit,
    viewModel: CourseSwitcherViewModel = hiltViewModel()
) {
    val colors = GlifoTheme.colors
    val context = LocalContext.current
    val uiState by viewModel.state.collectAsState()

    Column(modifier = Modifier.fillMaxSize().background(colors.background)) {
        GlifoTopAppBar(
            title = stringResource(R.string.course_switcher_title),
            isBack = true,
            onNavigationClick = onNavigateBack
        )

        val role = (uiState as? UiState.Success)?.data?.role
        val addCourse: @Composable () -> Unit = {
            GlifoButton(
                text = stringResource(
                    if (role == GlifoRole.Teacher) R.string.course_switcher_create else R.string.course_switcher_join
                ),
                icon = Icons.Default.Add,
                variant = GlifoButtonVariant.Secondary,
                modifier = Modifier.fillMaxWidth().padding(14.dp),
                onClick = if (role == GlifoRole.Teacher) onCreateCourse else onJoinCourse
            )
        }

        when (val state = uiState) {
            is UiState.Loading -> GlifoLoadingBox()
            is UiState.Error -> GlifoErrorBox(state.error.toMessage(context), onRetry = viewModel::load)
            is UiState.Empty -> Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(stringResource(R.string.course_switcher_empty), fontSize = 14.sp, color = colors.textSecondary, textAlign = TextAlign.Center)
                addCourse()
            }
            is UiState.Success -> Column(
                modifier = Modifier.weight(1f).navigationBarsPadding().verticalScroll(rememberScrollState())
            ) {
                Text(
                    stringResource(R.string.course_switcher_hint),
                    fontSize = 12.sp,
                    color = colors.textSecondary,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                )
                val courses = state.data.courses
                courses.forEachIndexed { i, course ->
                    val active = course.id == state.data.activeCourseId
                    GlifoItem(
                        showBottomBorder = i < courses.size - 1,
                        backgroundColor = if (active) colors.accentFaint else androidx.compose.ui.graphics.Color.Transparent,
                        onClick = {
                            viewModel.select(course)
                            onNavigateBack()
                        }
                    ) {
                        GlifoThumb {
                            Icon(Icons.Default.School, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(20.dp))
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(course.name, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = colors.textPrimary)
                            Text(
                                stringResource(R.string.course_switcher_code, course.code, course.joinCode),
                                fontSize = 12.sp,
                                color = colors.textSecondary
                            )
                        }
                        if (active) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = stringResource(R.string.course_switcher_active),
                                tint = colors.accentText,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
                addCourse()
            }
        }
    }
}
