package cr.ac.una.glifo.feature.sync.presentation

import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cr.ac.una.glifo.core.data.dummy.DummyAiCostProvider
import cr.ac.una.glifo.core.ui.component.*
import cr.ac.una.glifo.core.ui.theme.GlifoTheme
import kotlinx.coroutines.launch

@Composable
fun AiCostScreen(
    role: GlifoRole = GlifoRole.Student,
    onNavigateBackOrMenu: () -> Unit,
    onNavigateDrawer: (String) -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onLogout: () -> Unit
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val colors = GlifoTheme.colors
    val context = LocalContext.current
    val stats = DummyAiCostProvider.getStats(role)

    val title = when (role) {
        GlifoRole.Admin -> "Consumo del sistema"
        GlifoRole.Teacher -> "Consumo del curso"
        GlifoRole.Student -> "Consumo de IA"
    }

    var animateIn by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { animateIn = true }
    val quotaAnim by animateFloatAsState(
        targetValue = if (animateIn) (stats.quotaPercent ?: 0) / 100f else 0f,
        animationSpec = tween(700),
        label = "quota"
    )

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
                    role = role,
                    currentScreen = "ai_cost",
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
                title = title,
                onNavigationClick = { scope.launch { drawerState.open() } },
                actions = {
                    IconButton(onClick = {
                        Toast.makeText(context, "Por ahora solo hay datos de agosto.", Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(imageVector = Icons.Default.CalendarToday, contentDescription = "Calendario", tint = colors.textSecondary, modifier = Modifier.size(20.dp))
                    }
                }
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .navigationBarsPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(14.dp)
            ) {
                // Main Highlight Card
                GlifoCard(
                    borderColor = colors.verifiedLine
                ) {
                    Text(
                        text = stats.periodLabel,
                        fontSize = 12.sp,
                        color = colors.textSecondary
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "${stats.headlinePercent}%",
                            fontSize = 34.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (stats.isSystemWide) colors.accentText else colors.verified
                        )
                        Text(
                            text = "de las regiones se resolvieron\nsin llamadas de pago",
                            fontSize = 14.sp,
                            lineHeight = 18.sp,
                            color = colors.textPrimary
                        )
                    }

                    if (stats.quotaUsed != null && stats.quotaTotal != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .padding(top = 12.dp)
                                .clip(RoundedCornerShape(999.dp))
                                .background(colors.surfaceHigh)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(quotaAnim)
                                    .background(colors.accent)
                            )
                        }
                        Text(
                            text = "${stats.quotaUsed} de ${stats.quotaTotal} llamadas de nivel 2 · cuota al ${stats.quotaPercent}%",
                            fontSize = 12.sp,
                            color = colors.textSecondary,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }

                Text(
                    text = "DÓNDE SE RESOLVIÓ CADA REGIÓN",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.6.sp,
                    color = colors.textSecondary,
                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                )

                GlifoCard {
                    stats.breakdown.forEach { row ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            GlifoConfidenceChip(state = row.state)
                            Text(text = row.label, fontSize = 14.sp, color = colors.textPrimary, modifier = Modifier.weight(1f))
                            Box(
                                modifier = Modifier
                                    .width(64.dp)
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(colors.surfaceHigh)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .fillMaxWidth(row.percent / 100f)
                                        .background(
                                            when (row.state) {
                                                ConfidenceState.Verified -> colors.verified
                                                ConfidenceState.Repaired -> colors.repaired
                                                ConfidenceState.Escalated -> colors.escalated
                                                ConfidenceState.Uncertain -> colors.uncertain
                                            }
                                        )
                                )
                            }
                            Text(
                                text = row.count,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                color = colors.textSecondary,
                                modifier = Modifier.width(34.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Right
                            )
                        }
                    }
                }

                Text(
                    text = "LLAMADAS AL MODELO DE PAGO",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.6.sp,
                    color = colors.textSecondary,
                    modifier = Modifier.padding(top = 18.dp, bottom = 8.dp)
                )

                GlifoCard(tight = true) {
                    stats.paidCalls.forEach { call ->
                        GlifoItem(compact = true) {
                            Text(text = call.label, fontSize = 15.sp, color = colors.textPrimary, modifier = Modifier.weight(1f))
                            Text(text = call.count, fontFamily = FontFamily.Monospace, fontSize = 14.sp, color = colors.textSecondary)
                        }
                    }
                    GlifoItem(compact = true, showBottomBorder = false, backgroundColor = colors.surfaceHigh) {
                        Text(text = "Total", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = colors.textPrimary, modifier = Modifier.weight(1f))
                        Text(text = "${stats.totalPaidCalls}", fontFamily = FontFamily.Monospace, fontSize = 16.sp, color = colors.accentText)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(imageVector = Icons.Default.GpsFixed, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(18.dp).padding(top = 1.dp))
                    Text(
                        text = stats.footerNote,
                        fontSize = 12.sp,
                        color = colors.textSecondary,
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}
