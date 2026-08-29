package com.example.startapp.ui.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.startapp.R
import com.example.startapp.ui.viewmodel.CounterViewModel

@Composable
fun SettingsScreen(viewModel: CounterViewModel) {
    val maxHistoryDays by viewModel.maxHistoryDays.collectAsState()

    var maxHistoryDaysText by remember { mutableStateOf("") }
    var showSavedMessage by remember { mutableStateOf(false) }

    LaunchedEffect(maxHistoryDays) {
        if (maxHistoryDaysText.isBlank()) {
            maxHistoryDaysText = maxHistoryDays.toString()
        }
    }

    val parsedMaxHistoryDays = maxHistoryDaysText.toIntOrNull()?.takeIf { it > 0 }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = stringResource(R.string.settings_title),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = stringResource(R.string.settings_section_retention),
                    style = MaterialTheme.typography.titleSmall
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = maxHistoryDaysText,
                    onValueChange = {
                        maxHistoryDaysText = it
                        showSavedMessage = false
                    },
                    label = { Text(stringResource(R.string.max_history_days)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                if (maxHistoryDaysText.isNotBlank() && parsedMaxHistoryDays == null) {
                    Text(
                        text = stringResource(R.string.max_history_days_invalid),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.max_history_days_hint),
                    style = MaterialTheme.typography.bodySmall
                )

                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    enabled = parsedMaxHistoryDays != null,
                    onClick = {
                        val days = parsedMaxHistoryDays ?: return@Button
                        viewModel.updateMaxHistoryDays(days)
                        showSavedMessage = true
                    }
                ) {
                    Text(stringResource(R.string.save))
                }
                if (showSavedMessage) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.retention_saved),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = stringResource(R.string.settings_section_data),
                    style = MaterialTheme.typography.titleSmall
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.settings_data_note),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}
