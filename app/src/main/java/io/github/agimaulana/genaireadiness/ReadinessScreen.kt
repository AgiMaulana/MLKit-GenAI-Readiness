package io.github.agimaulana.genaireadiness

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

private val Grey = Color(0xFF9E9E9E)
private val Green = Color(0xFF2E7D32)
private val Amber = Color(0xFFF9A825)
private val Blue = Color(0xFF1565C0)
private val Red = Color(0xFFC62828)

private val Readiness.color: Color
    get() = when (this) {
        Readiness.CHECKING -> Grey
        Readiness.AVAILABLE -> Green
        Readiness.DOWNLOADABLE -> Amber
        Readiness.DOWNLOADING -> Blue
        Readiness.UNAVAILABLE -> Red
        Readiness.ERROR -> Red
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReadinessScreen(vm: ReadinessViewModel = viewModel()) {
    val context = LocalContext.current
    val state = vm.state

    LaunchedEffect(Unit) { vm.runChecks(context) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("On-device AI Readiness") },
                actions = {
                    TextButton(
                        onClick = { vm.runChecks(context) },
                        enabled = !state.loading
                    ) {
                        Text("Refresh")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (state.loading) {
                item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
            }
            item { SystemCard(state) }
            item {
                Text(
                    "ML Kit GenAI features",
                    style = MaterialTheme.typography.titleMedium
                )
            }
            items(state.features) { FeatureCard(it) }
            item { Verdict(state) }
        }
    }
}

@Composable
private fun SystemCard(state: UiState) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Device", style = MaterialTheme.typography.labelMedium)
            Text(state.device, fontWeight = FontWeight.SemiBold)
            HorizontalDivider(Modifier.padding(vertical = 4.dp))
            val (label, color) = when (state.aiCoreInstalled) {
                null -> "Checking…" to Grey
                true -> "Installed${state.aiCoreVersion?.let { " (v$it)" } ?: ""}" to Green
                false -> "Not found" to Red
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("AICore app", Modifier.weight(1f))
                StatusChip(label, color)
            }
        }
    }
}

@Composable
private fun FeatureCard(feature: FeatureResult) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(feature.name, Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                if (feature.readiness == Readiness.CHECKING) {
                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    StatusChip(feature.readiness.label, feature.readiness.color)
                }
            }
            Text(feature.description, style = MaterialTheme.typography.bodySmall)
            if (feature.readiness == Readiness.ERROR) {
                feature.detail?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = Red)
                }
            }
        }
    }
}

@Composable
private fun StatusChip(text: String, color: Color) {
    Surface(color = color.copy(alpha = 0.15f), shape = RoundedCornerShape(50)) {
        Text(
            text,
            Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            color = color,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun Verdict(state: UiState) {
    val done = state.features.isNotEmpty() &&
        state.features.none { it.readiness == Readiness.CHECKING }
    if (!done) return

    val ready = state.features.count {
        it.readiness == Readiness.AVAILABLE || it.readiness == Readiness.DOWNLOADABLE
    }
    val message = if (ready > 0) {
        "$ready of ${state.features.size} ML Kit GenAI features can run on this device."
    } else {
        "No ML Kit GenAI feature is supported here. AICore alone isn't enough: the device must be on " +
            "Google's allow-list with a locked bootloader. Use a self-hosted model (LiteRT-LM / Gemma) instead."
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (ready > 0) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
        )
    ) {
        Text(message, Modifier.padding(16.dp), color = Color(0xFF212121))
    }
}
