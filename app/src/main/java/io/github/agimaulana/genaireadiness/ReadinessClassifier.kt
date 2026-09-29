package io.github.agimaulana.genaireadiness

import com.google.mlkit.genai.common.FeatureStatus
import com.google.mlkit.genai.common.GenAiException

// AICore surfaces a not-provisioned feature as 606 instead of returning UNAVAILABLE. It is not
// exposed as a named ErrorCode constant. See
// https://developers.google.com/ml-kit/genai/prompt/android/get-started#common-setup-issues
private const val FEATURE_NOT_FOUND = 606

internal fun readinessForStatus(status: Int): Readiness = when (status) {
    FeatureStatus.AVAILABLE -> Readiness.AVAILABLE
    FeatureStatus.DOWNLOADABLE -> Readiness.DOWNLOADABLE
    FeatureStatus.DOWNLOADING -> Readiness.DOWNLOADING
    else -> Readiness.UNAVAILABLE
}

internal fun readinessForErrorCode(errorCode: Int): Readiness = when (errorCode) {
    FEATURE_NOT_FOUND,
    GenAiException.ErrorCode.AICORE_INCOMPATIBLE,
    GenAiException.ErrorCode.NEEDS_SYSTEM_UPDATE,
    GenAiException.ErrorCode.NOT_AVAILABLE,
    GenAiException.ErrorCode.NOT_SUPPORTED -> Readiness.UNAVAILABLE

    else -> Readiness.ERROR
}
