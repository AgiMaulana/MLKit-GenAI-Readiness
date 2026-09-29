package io.github.agimaulana.genaireadiness

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import io.github.agimaulana.genaireadiness.ui.theme.GenAIReadinessTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GenAIReadinessTheme {
                ReadinessScreen()
            }
        }
    }
}
