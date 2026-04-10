package io.github.c1921.mosswriter.ui.setup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.c1921.mosswriter.data.settings.SettingsRepository

@Composable
fun SetupScreen(
    settingsRepo: SettingsRepository,
    onProjectCreated: () -> Unit
) {
    var projectName by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Moss Writer", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))
        Text("创建你的第一个项目", style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(40.dp))

        OutlinedTextField(
            value = projectName,
            onValueChange = {
                projectName = it
                isError = false
            },
            label = { Text("项目名称") },
            placeholder = { Text("如：小说1、日记、笔记") },
            isError = isError,
            supportingText = if (isError) { { Text("项目名称不能为空") } } else null,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(Modifier.height(24.dp))

        Button(
            onClick = {
                val name = projectName.trim()
                if (name.isBlank()) {
                    isError = true
                    return@Button
                }
                settingsRepo.saveProjectName(name)
                onProjectCreated()
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("开始")
        }
    }
}
