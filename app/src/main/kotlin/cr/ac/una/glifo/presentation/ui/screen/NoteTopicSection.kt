package cr.ac.una.glifo.presentation.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cr.ac.una.glifo.R
import cr.ac.una.glifo.core.common.AppError
import cr.ac.una.glifo.core.ui.component.GlifoBanner
import cr.ac.una.glifo.core.ui.component.GlifoButton
import cr.ac.una.glifo.core.ui.component.GlifoButtonVariant
import cr.ac.una.glifo.core.ui.component.GlifoCard
import cr.ac.una.glifo.core.ui.theme.GlifoTheme
import cr.ac.una.glifo.domain.model.SyllabusTopic
import cr.ac.una.glifo.domain.model.TopicPickerItem
import cr.ac.una.glifo.domain.model.buildTopicPickerItems
import cr.ac.una.glifo.domain.model.displayLabel
import cr.ac.una.glifo.domain.model.unitHeaderLabel
import cr.ac.una.glifo.presentation.viewmodel.toTopicUpdateMessage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteTopicSection(
    linkedTopicId: Long?,
    suggestedTopicId: Long?,
    courseTopics: List<SyllabusTopic>,
    isTopicsLoading: Boolean,
    isUpdatingTopic: Boolean,
    topicError: AppError?,
    onSelectTopic: (Long?) -> Unit,
    onAcceptSuggestion: () -> Unit,
    onRefreshTopics: () -> Unit,
    onClearError: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = GlifoTheme.colors
    val context = LocalContext.current
    var showPicker by remember { mutableStateOf(false) }

    val topicsById = remember(courseTopics) { courseTopics.associateBy { it.id } }
    val linkedTopic = linkedTopicId?.let { topicsById[it] }
    val linkedLabel = when {
        linkedTopic != null -> linkedTopic.displayLabel()
        linkedTopicId != null -> stringResource(R.string.note_topic_fallback_label)
        else -> stringResource(R.string.note_topic_unlinked)
    }

    val showSuggestion = suggestedTopicId != null && linkedTopicId == null
    val suggestedLabel = suggestedTopicId?.let { id ->
        topicsById[id]?.displayLabel() ?: stringResource(R.string.note_topic_fallback_label)
    }

    Column(modifier = modifier.fillMaxWidth()) {
        GlifoCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.note_topic_section_title),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.5.sp,
                        color = colors.textSecondary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = linkedLabel,
                        fontSize = 14.sp,
                        fontWeight = if (linkedTopicId != null) FontWeight.Medium else FontWeight.Normal,
                        color = if (linkedTopicId != null) colors.textPrimary else colors.textSecondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (isUpdatingTopic) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = colors.accent
                    )
                } else {
                    GlifoButton(
                        text = stringResource(
                            if (linkedTopicId != null) R.string.note_topic_change else R.string.note_topic_select
                        ),
                        onClick = {
                            onClearError()
                            if (courseTopics.isEmpty()) onRefreshTopics()
                            showPicker = true
                        },
                        variant = GlifoButtonVariant.Secondary,
                        small = true,
                        fillMaxWidth = false
                    )
                }
            }
        }

        if (showSuggestion && suggestedLabel != null) {
            Spacer(modifier = Modifier.height(8.dp))
            GlifoCard(
                backgroundColor = colors.accentSoft,
                borderColor = colors.accent
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = stringResource(R.string.note_topic_suggested_label, suggestedLabel),
                        fontSize = 13.sp,
                        color = colors.textPrimary,
                        modifier = Modifier.weight(1f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    GlifoButton(
                        text = stringResource(R.string.note_topic_suggested_accept),
                        onClick = {
                            onClearError()
                            onAcceptSuggestion()
                        },
                        enabled = !isUpdatingTopic,
                        small = true,
                        fillMaxWidth = false
                    )
                }
            }
        }

        if (topicError != null) {
            Spacer(modifier = Modifier.height(8.dp))
            GlifoBanner(
                text = topicError.toTopicUpdateMessage(context),
                isWarning = true
            )
        }
    }

    if (showPicker) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val pickerItems = remember(courseTopics) { buildTopicPickerItems(courseTopics) }

        ModalBottomSheet(
            onDismissRequest = { showPicker = false },
            sheetState = sheetState,
            containerColor = colors.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(bottom = 16.dp)
            ) {
                Text(
                    text = stringResource(R.string.note_topic_picker_title),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textPrimary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
                HorizontalDivider(color = colors.border)

                when {
                    isTopicsLoading && pickerItems.isEmpty() -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp,
                                color = colors.accent
                            )
                        }
                    }

                    pickerItems.isEmpty() -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.note_topic_picker_empty),
                                fontSize = 13.sp,
                                color = colors.textSecondary
                            )
                            GlifoButton(
                                text = stringResource(R.string.note_topic_picker_retry),
                                onClick = onRefreshTopics,
                                variant = GlifoButtonVariant.Secondary,
                                small = true,
                                fillMaxWidth = false
                            )
                        }
                    }

                    else -> {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 420.dp)
                        ) {
                            items(
                                items = pickerItems,
                                key = { item ->
                                    when (item) {
                                        is TopicPickerItem.UnitHeader -> "unit-${item.topic.id}"
                                        is TopicPickerItem.TopicOption -> "topic-${item.topic.id}"
                                    }
                                }
                            ) { item ->
                                when (item) {
                                    is TopicPickerItem.UnitHeader -> {
                                        Text(
                                            text = item.topic.unitHeaderLabel(),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            letterSpacing = 0.5.sp,
                                            color = colors.textSecondary,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(
                                                    start = 16.dp,
                                                    end = 16.dp,
                                                    top = 14.dp,
                                                    bottom = 6.dp
                                                )
                                        )
                                    }

                                    is TopicPickerItem.TopicOption -> {
                                        val selected = item.topic.id == linkedTopicId
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    showPicker = false
                                                    onSelectTopic(item.topic.id)
                                                }
                                                .padding(
                                                    start = if (item.indented) 28.dp else 16.dp,
                                                    end = 16.dp,
                                                    top = 11.dp,
                                                    bottom = 11.dp
                                                ),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Text(
                                                text = item.topic.displayLabel(),
                                                fontSize = 14.sp,
                                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                                color = if (selected) colors.accentText else colors.textPrimary,
                                                modifier = Modifier.weight(1f)
                                            )
                                            if (selected) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = null,
                                                    tint = colors.accent,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                        HorizontalDivider(color = colors.border.copy(alpha = 0.5f))
                                    }
                                }
                            }

                            if (linkedTopicId != null) {
                                item(key = "clear-topic") {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                showPicker = false
                                                onSelectTopic(null)
                                            }
                                            .padding(horizontal = 16.dp, vertical = 14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = stringResource(R.string.note_topic_picker_clear),
                                            fontSize = 14.sp,
                                            color = colors.alert
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
