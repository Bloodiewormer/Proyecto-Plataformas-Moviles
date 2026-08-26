package cr.ac.una.glifo.feature.note.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cr.ac.una.glifo.core.data.dummy.DummyNoteContentProvider
import cr.ac.una.glifo.core.ui.component.*
import cr.ac.una.glifo.core.ui.theme.GlifoTheme

@Composable
fun NoteDetailScreen(
    noteId: Long,
    onNavigateBack: () -> Unit,
    onNavigateToCorrect: () -> Unit,
    onNavigateToFormula: () -> Unit,
    onNavigateToProvenance: () -> Unit
) {
    val colors = GlifoTheme.colors
    val content = DummyNoteContentProvider.get(noteId)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        GlifoTopAppBar(
            title = content.note.title,
            isBack = true,
            onNavigationClick = onNavigateBack,
            actions = {
                IconButton(onClick = onNavigateToProvenance) {
                    Icon(imageVector = Icons.Default.Image, contentDescription = "Procedencia", tint = colors.textSecondary, modifier = Modifier.size(22.dp))
                }
                IconButton(onClick = onNavigateToCorrect) {
                    Icon(imageVector = Icons.Default.Edit, contentDescription = "Editar", tint = colors.textSecondary, modifier = Modifier.size(22.dp))
                }
            }
        )

        GlifoBanner(
            text = "",
            content = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    GlifoConfidenceChip(state = ConfidenceState.Verified)
                    GlifoConfidenceChip(state = ConfidenceState.Repaired)
                    GlifoConfidenceChip(state = ConfidenceState.Escalated)
                    GlifoConfidenceChip(state = ConfidenceState.Uncertain)
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${content.verifiedCount} verificados · ${content.repairedCount} reparados · " +
                        "${content.escalatedCount} escalados · ${content.uncertainCount} inciertos",
                    fontSize = 12.sp,
                    color = colors.textPrimary
                )
            }
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(14.dp)
        ) {
            Text(
                text = content.pageLabel,
                fontSize = 11.sp,
                color = colors.textSecondary,
                modifier = Modifier.padding(bottom = 10.dp)
            )

            // Section Title with Verified Underline
            Text(
                text = content.heading,
                fontSize = 17.sp,
                fontWeight = FontWeight.Medium,
                color = colors.textPrimary,
                modifier = Modifier
                    .drawBehind {
                        val strokeWidth = 2.dp.toPx()
                        val y = size.height
                        drawLine(colors.verified, Offset(0f, y), Offset(size.width, y), strokeWidth)
                    }
                    .padding(bottom = 2.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Paragraph with Confidence Underlines
            Text(
                text = buildAnnotatedString {
                    append(content.bodyBeforeHighlight)
                    withStyle(SpanStyle(color = colors.escalated, background = colors.escalatedFaint)) {
                        append(content.highlighted)
                    }
                    append(content.bodyAfterHighlight)
                },
                fontSize = 14.sp,
                lineHeight = 24.sp,
                color = colors.textPrimary,
                modifier = Modifier
                    .clickable(onClick = onNavigateToProvenance)
                    .padding(bottom = 12.dp)
            )

            // Formula Box (Repaired N1.5)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.surfaceHigh, RoundedCornerShape(7.dp))
                    .border(1.dp, colors.border, RoundedCornerShape(7.dp))
                    .drawBehind {
                        drawLine(colors.repaired, Offset(0f, 0f), Offset(0f, size.height), strokeWidth = 6.dp.toPx())
                    }
                    .clip(RoundedCornerShape(7.dp))
                    .clickable(onClick = onNavigateToFormula)
                    .padding(12.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        GlifoBadge(
                            text = "REPARADO · ${content.formula.tier}",
                            backgroundColor = colors.repairedSoft,
                            textColor = colors.repaired
                        )
                        Text(text = "confianza ${content.formula.confidence}", fontSize = 11.sp, color = colors.textSecondary)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = content.formula.display,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Medium,
                        color = colors.textPrimary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Image, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(15.dp))
                        Text("Ver el recorte original", fontSize = 11.sp, color = colors.textSecondary)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Paragraph 2
            Text(
                text = buildAnnotatedString {
                    append(content.secondParagraphBefore)
                    withStyle(SpanStyle(background = colors.uncertainSoft)) {
                        append(content.secondParagraphHighlighted)
                    }
                    append(".")
                },
                fontSize = 14.sp,
                lineHeight = 24.sp,
                color = colors.textPrimary
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Uncertain warning card
            GlifoCard(
                backgroundColor = colors.uncertainFaint,
                borderColor = colors.uncertainLine,
                onClick = onNavigateToCorrect
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    GlifoConfidenceChip(state = ConfidenceState.Uncertain)
                    Text(content.uncertainFragmentLabel, fontSize = 13.sp, color = colors.textPrimary, modifier = Modifier.weight(1f))
                    Icon(imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}
