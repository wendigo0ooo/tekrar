package com.wendigo.tekrar.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.wendigo.tekrar.R
import com.wendigo.tekrar.core.DueFormat
import com.wendigo.tekrar.data.Word
import com.wendigo.tekrar.ui.components.ActionChip
import com.wendigo.tekrar.ui.components.EmptyState
import com.wendigo.tekrar.ui.components.ScreenHeader
import com.wendigo.tekrar.ui.components.Segmented
import com.wendigo.tekrar.ui.contracts.LibraryFilter
import com.wendigo.tekrar.ui.contracts.LibrarySort
import com.wendigo.tekrar.ui.contracts.LibraryUiState

@Composable
fun LibraryScreen(
    state: LibraryUiState,
    onQuery: (String) -> Unit,
    onFilter: (LibraryFilter) -> Unit,
    onSort: (LibrarySort) -> Unit,
    onWord: (Long) -> Unit,
    onAdd: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding()
            .padding(horizontal = 20.dp),
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        ScreenHeader(
            title = stringResource(R.string.library_title),
            subtitle = stringResource(R.string.library_count, state.words.size),
            trailing = {
                TextButton(onClick = onAdd) {
                    Text(text = stringResource(R.string.library_add))
                }
            },
        )
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = state.query,
            onValueChange = onQuery,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(text = stringResource(R.string.library_search)) },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
        )
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ActionChip(
                text = stringResource(R.string.lib_filter_all),
                selected = state.filter == LibraryFilter.ALL,
                onClick = { onFilter(LibraryFilter.ALL) },
            )
            ActionChip(
                text = stringResource(R.string.lib_filter_due),
                selected = state.filter == LibraryFilter.DUE,
                onClick = { onFilter(LibraryFilter.DUE) },
            )
            ActionChip(
                text = stringResource(R.string.lib_filter_new),
                selected = state.filter == LibraryFilter.NEW,
                onClick = { onFilter(LibraryFilter.NEW) },
            )
            ActionChip(
                text = stringResource(R.string.lib_filter_fav),
                selected = state.filter == LibraryFilter.FAVORITE,
                onClick = { onFilter(LibraryFilter.FAVORITE) },
            )
            ActionChip(
                text = stringResource(R.string.lib_filter_suspended),
                selected = state.filter == LibraryFilter.SUSPENDED,
                onClick = { onFilter(LibraryFilter.SUSPENDED) },
            )
            ActionChip(
                text = stringResource(R.string.lib_filter_leech),
                selected = state.filter == LibraryFilter.LEECH,
                onClick = { onFilter(LibraryFilter.LEECH) },
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        Segmented(
            options = listOf(
                stringResource(R.string.sort_due),
                stringResource(R.string.sort_az),
                stringResource(R.string.sort_newest),
                stringResource(R.string.sort_hardest),
            ),
            selectedIndex = when (state.sort) {
                LibrarySort.DUE -> 0
                LibrarySort.ALPHABETICAL -> 1
                LibrarySort.NEWEST -> 2
                LibrarySort.HARDEST -> 3
            },
            onSelect = { index ->
                onSort(
                    when (index) {
                        0 -> LibrarySort.DUE
                        1 -> LibrarySort.ALPHABETICAL
                        2 -> LibrarySort.NEWEST
                        else -> LibrarySort.HARDEST
                    }
                )
            },
        )
        Spacer(modifier = Modifier.height(10.dp))
        if (!state.loading && state.words.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                EmptyState(
                    title = stringResource(R.string.library_empty_title),
                    hint = stringResource(R.string.library_empty_hint),
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(bottom = 24.dp),
            ) {
                items(state.words, key = { it.id }) { word ->
                    WordRow(word = word, onClick = { onWord(word.id) })
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                    )
                }
            }
        }
    }
}

@Composable
private fun WordRow(word: Word, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                Text(
                    text = word.term,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (word.suspended) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (word.pos.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(horizontal = 7.dp, vertical = 2.dp),
                    ) {
                        Text(
                            text = word.pos,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = word.meaning,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                word.suspended -> MaterialTheme.colorScheme.outline
                                word.isLeech -> MaterialTheme.colorScheme.error
                                word.isNew -> MaterialTheme.colorScheme.secondary
                                word.dueAt <= System.currentTimeMillis() ->
                                    MaterialTheme.colorScheme.primary
                                else -> MaterialTheme.colorScheme.outline
                            }
                        ),
                )
                Text(
                    text = DueFormat.status(word),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (word.tags.isNotBlank()) {
                    Text(
                        text = "· ${word.tags}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
        if (word.favorite) {
            Text(
                text = "★",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Text(
            text = "›",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
