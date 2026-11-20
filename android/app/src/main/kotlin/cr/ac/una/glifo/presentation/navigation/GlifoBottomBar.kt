package cr.ac.una.glifo.presentation.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.Functions
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.outlined.DonutLarge
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import cr.ac.una.glifo.R
import cr.ac.una.glifo.core.ui.theme.GlifoTheme
import cr.ac.una.glifo.domain.model.GlifoRole

/** A top-level destination. [route] is the nav route pattern, so it also matches the current destination. */
data class GlifoTab(val route: String, @StringRes val label: Int, val icon: ImageVector)

fun tabsFor(role: GlifoRole): List<GlifoTab> = when (role) {
    GlifoRole.Student -> listOf(
        GlifoTab(Screen.Home.route, R.string.drawer_item_home, Icons.Outlined.Home),
        GlifoTab(Screen.NoteList.route, R.string.notes_title, Icons.AutoMirrored.Filled.Notes),
        GlifoTab(Screen.Coverage.route, R.string.course_detail_coverage, Icons.Outlined.DonutLarge),
        GlifoTab(Screen.StudyHub.route, R.string.course_detail_study, Icons.Default.Layers),
        GlifoTab(Screen.Profile.route, R.string.profile_title, Icons.Outlined.Person)
    )
    GlifoRole.Teacher -> listOf(
        GlifoTab(Screen.HomeTeacher.route, R.string.drawer_item_home, Icons.Outlined.Home),
        GlifoTab(Screen.TeacherCourses.route, R.string.nav_tab_courses, Icons.Default.School),
        GlifoTab(Screen.TeacherGaps.route, R.string.nav_tab_gaps, Icons.AutoMirrored.Filled.ShowChart),
        GlifoTab(Screen.TeacherGlossary.route, R.string.nav_tab_glossary, Icons.Default.Functions),
        GlifoTab(Screen.Profile.route, R.string.profile_title, Icons.Outlined.Person)
    )
    GlifoRole.Admin -> listOf(
        GlifoTab(Screen.HomeAdmin.route, R.string.drawer_item_home, Icons.Outlined.Home),
        GlifoTab(Screen.AdminUsers.route, R.string.admin_users_title, Icons.Default.Group),
        GlifoTab(Screen.AdminCourses.route, R.string.admin_courses_title, Icons.Default.School),
        GlifoTab(Screen.AdminRoles.route, R.string.admin_roles_title, Icons.Default.Security),
        GlifoTab(Screen.Profile.route, R.string.profile_title, Icons.Outlined.Person)
    )
}

@Composable
fun GlifoBottomBar(tabs: List<GlifoTab>, currentRoute: String?, onSelect: (GlifoTab) -> Unit) {
    val colors = GlifoTheme.colors
    NavigationBar(containerColor = colors.surface) {
        tabs.forEach { tab ->
            NavigationBarItem(
                selected = tab.route == currentRoute,
                onClick = { onSelect(tab) },
                icon = { Icon(tab.icon, contentDescription = null) },
                label = { Text(stringResource(tab.label), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = colors.accentText,
                    selectedTextColor = colors.accentText,
                    indicatorColor = colors.accentSoft,
                    unselectedIconColor = colors.textSecondary,
                    unselectedTextColor = colors.textSecondary
                )
            )
        }
    }
}
