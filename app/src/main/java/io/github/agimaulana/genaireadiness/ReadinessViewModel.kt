package io.github.agimaulana.genaireadiness

import android.content.Context
import android.os.Build
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.mlkit.genai.common.FeatureStatus
import com.google.mlkit.genai.imagedescription.ImageDescriberOptions
import com.google.mlkit.genai.imagedescription.ImageDescription
import com.google.mlkit.genai.prompt.Generation
import com.google.mlkit.genai.proofreading.ProofreaderOptions
import com.google.mlkit.genai.proofreading.Proofreading
import com.google.mlkit.genai.rewriting.RewriterOptions
import com.google.mlkit.genai.rewriting.Rewriting
import com.google.mlkit.genai.speechrecognition.SpeechRecognition
import com.google.mlkit.genai.speechrecognition.SpeechRecognizerOptions
import com.google.mlkit.genai.speechrecognition.speechRecognizerOptions
import com.google.mlkit.genai.summarization.Summarization
import com.google.mlkit.genai.summarization.SummarizerOptions
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.launch
import java.util.Locale

const val AICORE_PACKAGE = "com.google.android.aicore"

enum class Readiness(val label: String) {
    CHECKING("Checking…"),
    AVAILABLE("Ready"),
    DOWNLOADABLE("Needs download"),
    DOWNLOADING("Downloading"),
    UNAVAILABLE("Unsupported"),
    ERROR("Error")
}

data class FeatureResult(
    val name: String,
    val description: String,
    val readiness: Readiness = Readiness.CHECKING,
    val detail: String? = null
)

data class UiState(
    val device: String = "${Build.MANUFACTURER} ${Build.MODEL} " +
        "(Android ${Build.VERSION.RELEASE}, API ${Build.VERSION.SDK_INT})",
    val aiCoreInstalled: Boolean? = null,
    val aiCoreVersion: String? = null,
    val features: List<FeatureResult> = emptyList(),
    val loading: Boolean = false
)

private val FEATURES = listOf(
    "Prompt API" to "Free-form text / multimodal prompts (Gemini Nano)",
    "Summarization" to "Summarize articles or chats",
    "Proofreading" to "Grammar and spelling fixes",
    "Rewriting" to "Rephrase text in a different tone",
    "Image Description" to "Short captions for images",
    "Speech Recognition (basic)" to "On-device speech model, most devices on API 31+",
    "Speech Recognition (advanced)" to "GenAI transcription model, Pixel 10 and Pixel 11 only"
)

class ReadinessViewModel : ViewModel() {

    var state by mutableStateOf(UiState())
        private set

    private fun update(block: (UiState) -> UiState) {
        state = block(state)
    }

    fun runChecks(context: Context) {
        val ctx = context.applicationContext
        viewModelScope.launch {
            val aiCore = runCatching {
                ctx.packageManager.getPackageInfo(AICORE_PACKAGE, 0)
            }.getOrNull()

            update {
                it.copy(
                    loading = true,
                    aiCoreInstalled = aiCore != null,
                    aiCoreVersion = aiCore?.versionName,
                    features = FEATURES.map { (name, description) -> FeatureResult(name, description) }
                )
            }

            val checks: List<suspend () -> Int> = listOf(
                ::checkPromptApi,
                { checkSummarization(ctx) },
                { checkProofreading(ctx) },
                { checkRewriting(ctx) },
                { checkImageDescription(ctx) },
                ::checkSpeechRecognitionBasic,
                ::checkSpeechRecognitionAdvanced
            )

            checks.forEachIndexed { index, check ->
                val (name, description) = FEATURES[index]
                val result = try {
                    FeatureResult(name, description, check().toReadiness())
                } catch (t: Throwable) {
                    FeatureResult(name, description, Readiness.ERROR, t.message ?: t.javaClass.simpleName)
                }
                update { current ->
                    current.copy(features = current.features.toMutableList().also { it[index] = result })
                }
            }

            update { it.copy(loading = false) }
        }
    }

    private suspend fun checkPromptApi(): Int {
        val model = Generation.getClient()
        return try {
            model.checkStatus()
        } finally {
            model.close()
        }
    }

    private suspend fun checkSummarization(ctx: Context): Int {
        val options = SummarizerOptions.builder(ctx)
            .setInputType(SummarizerOptions.InputType.ARTICLE)
            .setOutputType(SummarizerOptions.OutputType.ONE_BULLET)
            .setLanguage(SummarizerOptions.Language.ENGLISH)
            .build()
        val summarizer = Summarization.getClient(options)
        return try {
            summarizer.checkFeatureStatus().await()
        } finally {
            summarizer.close()
        }
    }

    private suspend fun checkProofreading(ctx: Context): Int {
        val options = ProofreaderOptions.builder(ctx)
            .setInputType(ProofreaderOptions.InputType.KEYBOARD)
            .setLanguage(ProofreaderOptions.Language.ENGLISH)
            .build()
        val proofreader = Proofreading.getClient(options)
        return try {
            proofreader.checkFeatureStatus().await()
        } finally {
            proofreader.close()
        }
    }

    private suspend fun checkRewriting(ctx: Context): Int {
        val options = RewriterOptions.builder(ctx)
            .setOutputType(RewriterOptions.OutputType.FRIENDLY)
            .setLanguage(RewriterOptions.Language.ENGLISH)
            .build()
        val rewriter = Rewriting.getClient(options)
        return try {
            rewriter.checkFeatureStatus().await()
        } finally {
            rewriter.close()
        }
    }

    private suspend fun checkImageDescription(ctx: Context): Int {
        val options = ImageDescriberOptions.builder(ctx).build()
        val imageDescriber = ImageDescription.getClient(options)
        return try {
            imageDescriber.checkFeatureStatus().await()
        } finally {
            imageDescriber.close()
        }
    }

    private suspend fun checkSpeechRecognitionBasic(): Int {
        val options = speechRecognizerOptions {
            locale = Locale.US
            preferredMode = SpeechRecognizerOptions.Mode.MODE_BASIC
        }
        val recognizer = SpeechRecognition.getClient(options)
        return recognizer.use { recognizer ->
            recognizer.checkStatus()
        }
    }

    private suspend fun checkSpeechRecognitionAdvanced(): Int {
        val options = speechRecognizerOptions {
            locale = Locale.US
            preferredMode = SpeechRecognizerOptions.Mode.MODE_ADVANCED
        }
        val recognizer = SpeechRecognition.getClient(options)
        return recognizer.use { recognizer ->
            recognizer.checkStatus()
        }
    }
}

private fun Int.toReadiness(): Readiness = when (this) {
    FeatureStatus.AVAILABLE -> Readiness.AVAILABLE
    FeatureStatus.DOWNLOADABLE -> Readiness.DOWNLOADABLE
    FeatureStatus.DOWNLOADING -> Readiness.DOWNLOADING
    else -> Readiness.UNAVAILABLE
}
