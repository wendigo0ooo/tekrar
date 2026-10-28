package com.wendigo.tekrar.ui.add

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.wendigo.tekrar.R
import com.wendigo.tekrar.ui.components.PrimaryButton
import com.wendigo.tekrar.ui.components.ScreenHeader
import com.wendigo.tekrar.ui.contracts.WordEditorUiState

@Composable
fun AddWordScreen(
    state: WordEditorUiState,
    onSave: (
        term: String,
        meaning: String,
        example: String,
        exampleTr: String,
        notes: String,
        tags: String,
        pos: String,
        onSaved: () -> Unit,
    ) -> Unit,
    onDelete: (onDeleted: () -> Unit) -> Unit,
    onClearDuplicateError: () -> Unit,
    onBack: () -> Unit,
) {
    val word = state.word
    val wordId = word?.id
    var term by remember(wordId) { mutableStateOf(word?.term ?: "") }
    var meaning by remember(wordId) { mutableStateOf(word?.meaning ?: "") }
    var example by remember(wordId) { mutableStateOf(word?.example ?: "") }
    var exampleTr by remember(wordId) { mutableStateOf(word?.exampleTr ?: "") }
    var notes by remember(wordId) { mutableStateOf(word?.notes ?: "") }
    var tags by remember(wordId) { mutableStateOf(word?.tags ?: "") }
    var pos by remember(wordId) { mutableStateOf(word?.pos ?: "") }
    var showDelete by remember { mutableStateOf(false) }

    if (state.loading) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .safeDrawingPadding(),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        ScreenHeader(
            title = stringResource(
                if (state.isEdit) R.string.edit_title else R.string.add_title
            ),
            onBack = onBack,
        )
        Spacer(modifier = Modifier.height(4.dp))
        OutlinedTextField(
            value = term,
            onValueChange = {
                term = it
                if (state.duplicateTerm) onClearDuplicateError()
            },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(text = stringResource(R.string.field_term)) },
            singleLine = true,
            isError = state.duplicateTerm,
            supportingText = if (state.duplicateTerm) {
                { Text(text = stringResource(R.string.add_duplicate_term)) }
            } else {
                null
            },
            shape = RoundedCornerShape(14.dp),
        )
        OutlinedTextField(
            value = meaning,
            onValueChange = { meaning = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(text = stringResource(R.string.field_meaning)) },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
        )
        OutlinedTextField(
            value = pos,
            onValueChange = { pos = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(text = stringResource(R.string.field_pos)) },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
        )
        OutlinedTextField(
            value = example,
            onValueChange = { example = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(text = stringResource(R.string.field_example)) },
            minLines = 2,
            shape = RoundedCornerShape(14.dp),
        )
        OutlinedTextField(
            value = exampleTr,
            onValueChange = { exampleTr = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(text = stringResource(R.string.field_example_tr)) },
            minLines = 2,
            shape = RoundedCornerShape(14.dp),
        )
        OutlinedTextField(
            value = tags,
            onValueChange = { tags = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(text = stringResource(R.string.field_tags)) },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
        )
        OutlinedTextField(
            value = notes,
            onValueChange = { notes = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(text = stringResource(R.string.field_notes)) },
            minLines = 2,
            shape = RoundedCornerShape(14.dp),
        )
        Text(
            text = stringResource(R.string.add_required_hint),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(4.dp))
        PrimaryButton(
            text = stringResource(R.string.add_save),
            onClick = {
                onSave(
                    term.trim(),
                    meaning.trim(),
                    example.trim(),
                    exampleTr.trim(),
                    notes.trim(),
                    tags.trim(),
                    pos.trim(),
                    onBack,
                )
            },
            enabled = !state.saving && term.isNotBlank() && meaning.isNotBlank(),
        )
        if (state.isEdit) {
            TextButton(
                onClick = { showDelete = true },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = stringResource(R.string.add_delete),
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
    }

    if (showDelete && word != null) {
        AlertDialog(
            onDismissRequest = { showDelete = false },
            title = { Text(text = stringResource(R.string.add_delete_title)) },
            text = { Text(text = stringResource(R.string.add_delete_text, word.term)) },
            confirmButton = {
                Button(
                    onClick = {
                        showDelete = false
                        onDelete { onBack() }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError,
                    ),
                ) {
                    Text(text = stringResource(R.string.add_delete_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDelete = false }) {
                    Text(text = stringResource(R.string.common_cancel))
                }
            },
        )
    }
}
