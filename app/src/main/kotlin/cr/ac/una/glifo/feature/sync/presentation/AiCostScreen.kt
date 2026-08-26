package cr.ac.una.glifo.feature.sync.presentation

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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cr.ac.una.glifo.core.ui.component.*
import cr.ac.una.glifo.core.ui.theme.GlifoTheme
import kotlinx.coroutines.launch

@Composable
fun AiCostScreen(
    role: GlifoRole = GlifoRole.Student,
    onNavigateBackOrMenu: () -> Unit,
    onNavigateDrawer: (String) -> Unit,
    onLogout: () -> Unit
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val colors = GlifoTheme.colors

    val title = when (role) {
        GlifoRole.Admin -> "Consumo del sistema"
        GlifoRole.Teacher -> "Consumo del curso"
        GlifoRole.Student -> "Consumo de IA"
    }

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
                    IconButton(onClick = {}) {
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
                        text = if (role == GlifoRole.Admin) "Agosto · Sistema completo · 142 usuarios" else if (role == GlifoRole.Teacher) "Agosto · EIF411 · 27 estudiantes" else "Agosto · Cálculo II",
                        fontSize = 12.sp,
                        color = colors.textSecondary
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = if (role == GlifoRole.Admin) "64%" else "81%",
                            fontSize = 34.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (role == GlifoRole.Admin) colors.accentText else colors.verified
                        )
                        Text(
                            text = "de las regiones se resolvieron\nsin llamadas de pago",
                            fontSize = 14.sp,
                            lineHeight = 18.sp,
                            color = colors.textPrimary
                        )
                    }

                    if (role == GlifoRole.Admin) {
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
                                    .fillMaxWidth(0.64f)
                                    .background(colors.accent)
                            )
                        }
                        Text(
                            text = "1 842 de 2 900 llamadas de nivel 2 · cuota al 78%",
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
                    listOf(
                        Triple(ConfidenceState.Verified, "N1 · ML Kit local", Pair(68f, "612")),
                        Triple(ConfidenceState.Repaired, "N1.5 · SimpleTex gratis", Pair(13f, "117")),
                        Triple(ConfidenceState.Escalated, "N2 · visión, en lote", Pair(16f, "144")),
                        Triple(ConfidenceState.Uncertain, "Sin resolver", Pair(3f, "27"))
                    ).forEach { (state, label, meta) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            GlifoConfidenceChip(state = state)
                            Text(text = label, fontSize = 14.sp, color = colors.textPrimary, modifier = Modifier.weight(1f))
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
                                        .fillMaxWidth(meta.first / 100f)
                                        .background(
                                            when (state) {
                                                ConfidenceState.Verified -> colors.verified
                                                ConfidenceState.Repaired -> colors.repaired
                                                ConfidenceState.Escalated -> colors.escalated
                                                ConfidenceState.Uncertain -> colors.uncertain
                                            }
                                        )
                                )
                            }
                            Text(
                                text = meta.second,
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
                    listOf(
                        Pair("IA-00 · reparación por visión", "31"),
                        Pair("IA-01 · reconstrucción", "47"),
                        Pair("IA-02 · flashcards y quizzes", "9"),
                        Pair("IA-05 · adjudicación semántica", "6")
                    ).forEach { (label, count) ->
                        GlifoItem(compact = true) {
                            Text(text = label, fontSize = 15.sp, color = colors.textPrimary, modifier = Modifier.weight(1f))
                            Text(text = count, fontFamily = FontFamily.Monospace, fontSize = 14.sp, color = colors.textSecondary)
                        }
                    }
                    GlifoItem(compact = true, showBottomBorder = false, backgroundColor = colors.surfaceHigh) {
                        Text(text = "Total", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = colors.textPrimary, modifier = Modifier.weight(1f))
                        Text(text = "93", fontFamily = FontFamily.Monospace, fontSize = 16.sp, color = colors.accentText)
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
                        text = "Sin la escalera, 47 páginas habrían requerido 47 llamadas de visión de página completa.",
                        fontSize = 12.sp,
                        color = colors.textSecondary,
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}
