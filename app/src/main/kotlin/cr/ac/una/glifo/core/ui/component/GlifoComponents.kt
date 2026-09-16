package cr.ac.una.glifo.core.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cr.ac.una.glifo.R
import cr.ac.una.glifo.core.ui.theme.GlifoTheme
import cr.ac.una.glifo.domain.model.GlifoRole
import kotlinx.coroutines.delay

// ── Glifo Logo Drawing ────────────────────────────────────────────────────────
@Composable
fun GlifoLogo(
    modifier: Modifier = Modifier,
    size: Dp = 88.dp,
    showRing: Boolean = true,
    color: Color = GlifoTheme.colors.accentText
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val scale = w / 88f

        if (showRing) {
            drawCircle(
                color = color.copy(alpha = 0.45f),
                radius = 39f * scale,
                center = Offset(44f * scale, 44f * scale),
                style = Stroke(width = 1.2f * scale)
            )
        }

        // Stylized 'G' glyph path
        val glyphPath = Path().apply {
            moveTo(30f * scale, 58f * scale)
            cubicTo(
                30f * scale, 34f * scale,
                58f * scale, 34f * scale,
                58f * scale, 48f * scale
            )
            cubicTo(
                58f * scale, 58f * scale,
                42f * scale, 60f * scale,
                38f * scale, 50f * scale
            )
            cubicTo(
                35f * scale, 43f * scale,
                46f * scale, 38f * scale,
                52f * scale, 44f * scale
            )
        }

        drawPath(
            path = glyphPath,
            color = color,
            style = Stroke(
                width = 3.4f * scale,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )

        if (showRing) {
            drawCircle(
                color = color,
                radius = 2.8f * scale,
                center = Offset(56.5f * scale, 29.5f * scale)
            )
        }
    }
}

// ── Coverage Bar (CovBar) ─────────────────────────────────────────────────────
@Composable
fun CovBar(
    v: Float, // verified %
    r: Float, // repaired %
    e: Float, // escalated %
    u: Float, // uncertain %
    modifier: Modifier = Modifier,
    height: Dp = 5.dp
) {
    val colors = GlifoTheme.colors
    val total = (v + r + e + u).coerceAtLeast(0.001f)
    val vW = v / total
    val rW = r / total
    val eW = e / total
    val uW = u / total

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(999.dp))
    ) {
        if (vW > 0) {
            Box(
                modifier = Modifier
                    .weight(vW)
                    .fillMaxHeight()
                    .background(colors.verified)
            )
        }
        if (rW > 0) {
            Box(
                modifier = Modifier
                    .weight(rW)
                    .fillMaxHeight()
                    .background(colors.repaired)
            )
        }
        if (eW > 0) {
            Box(
                modifier = Modifier
                    .weight(eW)
                    .fillMaxHeight()
                    .background(colors.escalated)
            )
        }
        if (uW > 0) {
            Box(
                modifier = Modifier
                    .weight(uW)
                    .fillMaxHeight()
                    .background(colors.uncertain)
            )
        }
    }
}

// ── Badge & Pill ──────────────────────────────────────────────────────────────
@Composable
fun GlifoBadge(
    text: String,
    modifier: Modifier = Modifier,
    backgroundColor: Color = GlifoTheme.colors.accentSoft,
    textColor: Color = GlifoTheme.colors.accentText
) {
    Box(
        modifier = modifier
            .background(backgroundColor, RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = textColor,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.5.sp
        )
    }
}

// ── Thumbnail ─────────────────────────────────────────────────────────────────
@Composable
fun GlifoThumb(
    modifier: Modifier = Modifier,
    size: Dp = 42.dp,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .size(size)
            .background(GlifoTheme.colors.surfaceHigh, RoundedCornerShape(7.dp))
            .border(1.dp, GlifoTheme.colors.border, RoundedCornerShape(7.dp)),
        contentAlignment = Alignment.Center,
        content = content
    )
}

// ── Glifo Card ────────────────────────────────────────────────────────────────
@Composable
fun GlifoCard(
    modifier: Modifier = Modifier,
    backgroundColor: Color = GlifoTheme.colors.surface,
    borderColor: Color = GlifoTheme.colors.border,
    tight: Boolean = false,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(10.dp)
    val cardModifier = modifier
        .fillMaxWidth()
        .background(backgroundColor, shape)
        .border(1.dp, borderColor, shape)
        .clip(shape)
        .then(
            if (onClick != null) {
                Modifier.clickable(onClick = onClick)
            } else Modifier
        )
        .then(
            if (!tight) Modifier.padding(14.dp) else Modifier
        )

    Column(
        modifier = cardModifier,
        content = content
    )
}

// ── List Item ─────────────────────────────────────────────────────────────────
@Composable
fun GlifoItem(
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    showBottomBorder: Boolean = true,
    backgroundColor: Color = Color.Transparent,
    onClick: (() -> Unit)? = null,
    content: @Composable RowScope.() -> Unit
) {
    val borderMod = if (showBottomBorder) {
        Modifier.drawBottomBorder(GlifoTheme.colors.border)
    } else Modifier

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .then(
                if (onClick != null) {
                    Modifier.clickable(onClick = onClick)
                } else Modifier
            )
            .then(borderMod)
            .padding(horizontal = 14.dp, vertical = if (compact) 8.dp else 10.dp)
            .defaultMinSize(minHeight = if (compact) 48.dp else 64.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        content = content
    )
}

fun Modifier.drawBottomBorder(color: Color): Modifier = this.then(
    Modifier.drawWithContent {
        drawContent()
        val strokeWidth = 1.dp.toPx()
        val y = size.height - strokeWidth / 2
        drawLine(
            color = color,
            start = Offset(0f, y),
            end = Offset(size.width, y),
            strokeWidth = strokeWidth
        )
    }
)

fun Modifier.drawRightBorder(color: Color): Modifier = this.then(
    Modifier.drawWithContent {
        drawContent()
        val strokeWidth = 1.dp.toPx()
        val x = size.width - strokeWidth / 2
        drawLine(
            color = color,
            start = Offset(x, 0f),
            end = Offset(x, size.height),
            strokeWidth = strokeWidth
        )
    }
)

// ── Glifo Top AppBar ──────────────────────────────────────────────────────────
@Composable
fun GlifoTopAppBar(
    title: String? = null,
    subtitle: String? = null,
    onNavigationClick: (() -> Unit)? = null,
    isBack: Boolean = false,
    hasDropdown: Boolean = false,
    actions: @Composable RowScope.() -> Unit = {},
    customTitle: (@Composable () -> Unit)? = null
) {
    val colors = GlifoTheme.colors
    Surface(
        color = colors.surface,
        modifier = Modifier
            .fillMaxWidth()
            .drawBottomBorder(colors.border)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .height(56.dp)
                .padding(start = 4.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Navigation icon (3 bars or back arrow) with 48x48dp touch target
            if (onNavigationClick != null) {
                IconButton(
                    onClick = onNavigationClick,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = if (isBack) Icons.AutoMirrored.Filled.ArrowBack else Icons.Default.Menu,
                        contentDescription = if (isBack) stringResource(R.string.common_nav_back) else stringResource(R.string.common_nav_menu),
                        tint = colors.textPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            } else {
                Spacer(modifier = Modifier.width(12.dp))
            }

            // Title area
            if (customTitle != null) {
                customTitle()
            } else if (subtitle != null && title != null) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 2.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = subtitle,
                        fontSize = 11.sp,
                        color = colors.textSecondary,
                        letterSpacing = 0.3.sp,
                        maxLines = 1
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Text(
                            text = title,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = colors.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (hasDropdown) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                tint = colors.textPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            } else if (title != null) {
                Text(
                    text = title,
                    fontSize = if (title.length > 14) 16.sp else 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = colors.textPrimary,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 2.dp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            } else {
                Spacer(modifier = Modifier.weight(1f))
            }

            // Right actions
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                content = actions
            )
        }
    }
}

// ── Banner ────────────────────────────────────────────────────────────────────
@Composable
fun GlifoBanner(
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    isWarning: Boolean = false,
    content: (@Composable RowScope.() -> Unit)? = null
) {
    val colors = GlifoTheme.colors
    val bg = if (isWarning) colors.alertSoft else colors.accentSoft
    val fg = if (isWarning) colors.alert else colors.textPrimary

    Row(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 36.dp)
            .background(bg)
            .padding(horizontal = 14.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (content != null) {
            content()
        } else {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = fg,
                    modifier = Modifier.size(16.dp)
                )
            }
            Text(
                text = text,
                fontSize = 13.sp,
                color = fg,
                lineHeight = 16.sp
            )
        }
    }
}

// ── Buttons ───────────────────────────────────────────────────────────────────
enum class GlifoButtonVariant {
    Primary, Secondary, Text, Danger
}

@Composable
fun GlifoButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: GlifoButtonVariant = GlifoButtonVariant.Primary,
    small: Boolean = false,
    icon: ImageVector? = null,
    enabled: Boolean = true
) {
    val colors = GlifoTheme.colors
    val height = if (small) 36.dp else 44.dp
    val fontSize = if (small) 13.sp else 15.sp
    val shape = RoundedCornerShape(7.dp)

    val (bgColor, contentColor, border) = when (variant) {
        GlifoButtonVariant.Primary -> Triple(colors.accent, colors.onAccent, null)
        GlifoButtonVariant.Secondary -> Triple(Color.Transparent, colors.btnSecText, BorderStroke(1.dp, colors.btnSecBorder))
        GlifoButtonVariant.Text -> Triple(Color.Transparent, colors.btnSecText, null)
        GlifoButtonVariant.Danger -> Triple(Color.Transparent, colors.alert, BorderStroke(1.dp, colors.alert))
    }

    Surface(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(height),
        enabled = enabled,
        shape = shape,
        color = bgColor,
        contentColor = contentColor,
        border = border
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier
                        .size(if (small) 16.dp else 18.dp)
                        .padding(end = 4.dp)
                )
            }
            Text(
                text = text,
                fontSize = fontSize,
                fontWeight = FontWeight.Medium,
                color = contentColor
            )
        }
    }
}

// ── Outlined & Secondary button backward compatibility ────────────────────────
@Composable
fun GlifoOutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null
) {
    GlifoButton(
        text = text,
        onClick = onClick,
        modifier = modifier,
        variant = GlifoButtonVariant.Secondary,
        icon = icon
    )
}

// ── Field ─────────────────────────────────────────────────────────────────────
@Composable
fun GlifoField(
    label: String,
    value: String = "",
    placeholder: String = "",
    modifier: Modifier = Modifier,
    onValueChange: ((String) -> Unit)? = null,
    isFocused: Boolean = false,
    isError: Boolean = false
) {
    val colors = GlifoTheme.colors
    val borderColor = when {
        isError -> colors.alert
        isFocused -> colors.accentLine
        else -> colors.border
    }
    val labelColor = when {
        isError -> colors.alert
        isFocused -> colors.accentText
        else -> colors.textSecondary
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 50.dp)
            .background(colors.surfaceHigh, RoundedCornerShape(7.dp))
            .border(if (isFocused || isError) 2.dp else 1.dp, borderColor, RoundedCornerShape(7.dp))
            .padding(horizontal = 12.dp, vertical = 7.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = label,
                fontSize = 10.sp,
                color = labelColor,
                letterSpacing = 0.3.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            if (onValueChange != null) {
                androidx.compose.foundation.text.BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    textStyle = androidx.compose.ui.text.TextStyle(
                        color = colors.textPrimary,
                        fontSize = 15.sp,
                        fontFamily = FontFamily.SansSerif
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    decorationBox = { innerTextField ->
                        if (value.isEmpty() && placeholder.isNotEmpty()) {
                            Text(
                                text = placeholder,
                                fontSize = 15.sp,
                                color = colors.textSecondary
                            )
                        }
                        innerTextField()
                    }
                )
            } else {
                Text(
                    text = value.ifEmpty { placeholder },
                    fontSize = 15.sp,
                    color = if (value.isNotEmpty()) colors.textPrimary else colors.textSecondary
                )
            }
        }
    }
}

// ── Glifo Camera FAB ──────────────────────────────────────────────────────────
@Composable
fun GlifoCameraFab(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = GlifoTheme.colors
    Box(
        modifier = modifier
            .size(50.dp)
            .background(colors.accent, RoundedCornerShape(16.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = colors.onAccent),
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.CameraAlt,
            contentDescription = stringResource(R.string.common_camera_description),
            tint = colors.onAccent,
            modifier = Modifier.size(22.dp)
        )
    }
}

// ── Drawer Content ────────────────────────────────────────────────────────────
// GlifoRole moved to domain.model.GlifoRole (pure domain concept, not UI-only).

data class GlifoDrawerItem(
    val id: String,
    val label: String,
    val icon: ImageVector? = null,
    val route: String,
    val isActive: Boolean = false,
    val isSubItem: Boolean = false
)

@Composable
fun GlifoDrawerContent(
    role: GlifoRole = GlifoRole.Student,
    currentScreen: String = "home",
    activeCourseName: String = if (role == GlifoRole.Teacher) "EIF411" else "Cálculo II",
    userName: String = when (role) {
        GlifoRole.Teacher -> "Maikol Guzmán"
        GlifoRole.Admin -> "Admin Glifo"
        GlifoRole.Student -> "Brandon Brenes"
    },
    userEmail: String = when (role) {
        GlifoRole.Teacher -> "maikol.guzman@una.cr"
        GlifoRole.Admin -> "admin@glifo.app"
        GlifoRole.Student -> "bbrenes@est.una.ac.cr"
    },
    userInitials: String = when (role) {
        GlifoRole.Teacher -> "MG"
        GlifoRole.Admin -> "AD"
        GlifoRole.Student -> "BB"
    },
    onNavigate: (String) -> Unit,
    onChangeCourseClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onLogoutClick: () -> Unit = {}
) {
    val colors = GlifoTheme.colors

    val drawerHome = stringResource(R.string.drawer_item_home)
    val drawerMyCourses = stringResource(R.string.courses_title)
    val drawerNotes = stringResource(R.string.notes_title)
    val drawerCoverage = stringResource(R.string.course_detail_coverage)
    val drawerStudy = stringResource(R.string.course_detail_study)
    val drawerSync = stringResource(R.string.sync_queue_title)
    val drawerAiCostStudent = stringResource(R.string.drawer_item_ai_cost)
    val drawerSyllabus = stringResource(R.string.drawer_item_syllabus)
    val drawerGlossary = stringResource(R.string.drawer_item_notation_glossary)
    val drawerGroupGaps = stringResource(R.string.drawer_item_group_gaps)
    val drawerCourseConsumption = stringResource(R.string.drawer_item_course_consumption)
    val drawerUsers = stringResource(R.string.admin_users_title)
    val drawerCourses = stringResource(R.string.admin_courses_title)
    val drawerRoles = stringResource(R.string.admin_roles_title)

    val studentItems = listOf(
        GlifoDrawerItem("home", drawerHome, Icons.Outlined.Home, "home", currentScreen == "home"),
        GlifoDrawerItem("school", drawerMyCourses, Icons.Default.School, "courses", currentScreen == "courses"),
        GlifoDrawerItem("notes", drawerNotes, Icons.AutoMirrored.Filled.Notes, "notes", currentScreen == "notes"),
        GlifoDrawerItem("coverage", drawerCoverage, Icons.Outlined.Schedule, "coverage", currentScreen == "coverage"),
        GlifoDrawerItem("study", drawerStudy, Icons.Default.Layers, "study", currentScreen == "study"),
        GlifoDrawerItem("sync", drawerSync, Icons.Default.Sync, "sync", currentScreen == "sync"),
        GlifoDrawerItem("cost", drawerAiCostStudent, Icons.Default.BarChart, "ai_cost", currentScreen == "ai_cost")
    )

    val teacherItems = listOf(
        GlifoDrawerItem("home", drawerHome, Icons.Outlined.Home, "home_teacher", currentScreen == "home_teacher"),
        GlifoDrawerItem("school", drawerMyCourses, Icons.Default.School, "teacher_courses", currentScreen == "teacher_courses"),
        GlifoDrawerItem("syllabus", drawerSyllabus, Icons.AutoMirrored.Filled.FormatListBulleted, "syllabus", currentScreen == "syllabus"),
        GlifoDrawerItem("glossary", drawerGlossary, Icons.Default.Functions, "glossary", currentScreen == "glossary"),
        GlifoDrawerItem("gaps", drawerGroupGaps, Icons.AutoMirrored.Filled.ShowChart, "gaps", currentScreen == "gaps"),
        GlifoDrawerItem("cost", drawerCourseConsumption, Icons.Default.BarChart, "ai_cost", currentScreen == "ai_cost")
    )

    val adminItems = listOf(
        GlifoDrawerItem("home", drawerHome, Icons.Outlined.Home, "home_admin", currentScreen == "home_admin"),
        GlifoDrawerItem("users", drawerUsers, Icons.Default.Group, "admin_users", currentScreen == "admin_users"),
        GlifoDrawerItem("school", drawerCourses, Icons.Default.School, "admin_courses", currentScreen == "admin_courses"),
        GlifoDrawerItem("roles", drawerRoles, null, "admin_roles", currentScreen == "admin_roles", isSubItem = true),
        GlifoDrawerItem("cost", drawerAiCostStudent, Icons.Default.BarChart, "ai_cost", currentScreen == "ai_cost")
    )

    val items = when (role) {
        GlifoRole.Student -> studentItems
        GlifoRole.Teacher -> teacherItems
        GlifoRole.Admin -> adminItems
    }

    val roleLabel = when (role) {
        GlifoRole.Student -> stringResource(R.string.profile_role_student)
        GlifoRole.Teacher -> stringResource(R.string.profile_role_teacher)
        GlifoRole.Admin -> stringResource(R.string.profile_role_admin)
    }

    val roleBadgeBg = if (role == GlifoRole.Admin) colors.alertSoft else colors.accentSoft
    val roleBadgeFg = if (role == GlifoRole.Admin) colors.alert else colors.accentText

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .background(colors.surface)
            .statusBarsPadding()
            .navigationBarsPadding()
            .drawRightBorder(colors.border)
    ) {
        // User Profile Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .drawBottomBorder(colors.border)
                .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 14.dp)
        ) {
            // Avatar Circle
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(colors.surfaceHigh, CircleShape)
                    .border(1.dp, colors.border, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = userInitials,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.accentText
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = userName,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.textPrimary
            )

            Text(
                text = userEmail,
                fontSize = 12.sp,
                color = colors.textSecondary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .background(roleBadgeBg, CircleShape)
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = roleLabel,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.6.sp,
                    color = roleBadgeFg
                )
            }
        }

        // Active Course Section (only for Student and Teacher)
        if (role != GlifoRole.Admin) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .drawBottomBorder(colors.border)
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Text(
                    text = stringResource(R.string.drawer_active_course_section),
                    fontSize = 10.sp,
                    color = colors.textSecondary,
                    letterSpacing = 0.5.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(3.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = activeCourseName,
                        fontSize = 15.sp,
                        color = colors.textPrimary,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = stringResource(R.string.drawer_change_course),
                        fontSize = 13.sp,
                        color = colors.accentText,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .clickable(onClick = onChangeCourseClick)
                            .padding(4.dp)
                    )
                }
            }
        }

        // Navigation Items List
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(vertical = 6.dp)
        ) {
            items.forEach { item ->
                val bg = if (item.isActive) colors.accentSoft else Color.Transparent
                val fg = if (item.isActive) colors.accentText else colors.textPrimary
                val iconFg = if (item.isActive) colors.accentText else colors.textSecondary

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .background(bg)
                        .clickable { onNavigate(item.route) }
                ) {
                    // Left active indicator
                    if (item.isActive) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .width(3.dp)
                                .background(colors.accent)
                                .align(Alignment.CenterStart)
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        if (item.isSubItem) {
                            Spacer(modifier = Modifier.width(20.dp))
                        } else if (item.icon != null) {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.label,
                                tint = iconFg,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Text(
                            text = item.label,
                            fontSize = 14.sp,
                            color = fg,
                            fontWeight = if (item.isActive) FontWeight.SemiBold else FontWeight.Normal
                        )
                    }
                }
            }

            // Divider
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                thickness = 1.dp,
                color = colors.border
            )

            // Perfil (Only for Student and Teacher)
            if (role != GlifoRole.Admin) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clickable(onClick = onProfileClick)
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = stringResource(R.string.profile_title),
                        tint = colors.textSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = stringResource(R.string.profile_title),
                        fontSize = 14.sp,
                        color = colors.textPrimary
                    )
                }
            }

            // Ajustes
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clickable(onClick = onSettingsClick)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.WbSunny,
                    contentDescription = stringResource(R.string.settings_title),
                    tint = colors.textSecondary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = stringResource(R.string.settings_title),
                    fontSize = 14.sp,
                    color = colors.textPrimary
                )
            }

            // Cerrar sesión
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clickable(onClick = onLogoutClick)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                    contentDescription = stringResource(R.string.profile_button_logout),
                    tint = colors.alert,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = stringResource(R.string.profile_button_logout),
                    fontSize = 14.sp,
                    color = colors.alert,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

// ── Backward Compatibility Components ────────────────────────────────────────
@Composable
fun GlifoTopBar(
    title: String,
    onNavigateBack: (() -> Unit)? = null
) {
    GlifoTopAppBar(
        title = title,
        isBack = onNavigateBack != null,
        onNavigationClick = onNavigateBack
    )
}

@Composable
fun GlifoTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    errorMessage: String? = null
) {
    Column(modifier = modifier) {
        GlifoField(
            label = label,
            value = value,
            onValueChange = onValueChange,
            isError = isError
        )
        if (isError && errorMessage != null) {
            Text(
                text = errorMessage,
                color = GlifoTheme.colors.alert,
                fontSize = 12.sp,
                modifier = Modifier.padding(start = 12.dp, top = 4.dp)
            )
        }
    }
}

// ── Staggered list entrance ─────────────────────────────────────────────────
// Small shared motion used across list/card-heavy screens (courses, notes, coverage,
// admin/teacher lists, sync queue…) so content fades and slides in instead of just
// appearing, without every screen needing its own bespoke animation code.
@Composable
fun GlifoStaggeredEntry(
    index: Int,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(30L * index.coerceIn(0, 10))
        visible = true
    }
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(260)) + slideInVertically(tween(260)) { it / 6 },
        modifier = modifier
    ) {
        content()
    }
}

