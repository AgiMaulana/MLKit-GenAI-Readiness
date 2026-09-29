package io.github.agimaulana.genaireadiness

import org.junit.Assert.assertEquals
import org.junit.Test

class ReadinessClassifierTest {

    @Test
    fun `status maps to the matching readiness`() {
        // FeatureStatus: UNAVAILABLE=0, DOWNLOADABLE=1, DOWNLOADING=2, AVAILABLE=3
        assertEquals(Readiness.AVAILABLE, readinessForStatus(3))
        assertEquals(Readiness.DOWNLOADABLE, readinessForStatus(1))
        assertEquals(Readiness.DOWNLOADING, readinessForStatus(2))
        assertEquals(Readiness.UNAVAILABLE, readinessForStatus(0))
    }

    @Test
    fun `unknown status is reported as unsupported`() {
        assertEquals(Readiness.UNAVAILABLE, readinessForStatus(99))
    }

    @Test
    fun `feature not provisioned is unsupported rather than error`() {
        // AICore raw code, the common case on most devices.
        assertEquals(Readiness.UNAVAILABLE, readinessForErrorCode(606))
    }

    @Test
    fun `unsupported error codes map to unsupported`() {
        // ErrorCode.NOT_AVAILABLE, NOT_SUPPORTED, NEEDS_SYSTEM_UPDATE, AICORE_INCOMPATIBLE
        assertEquals(Readiness.UNAVAILABLE, readinessForErrorCode(8))
        assertEquals(Readiness.UNAVAILABLE, readinessForErrorCode(16))
        assertEquals(Readiness.UNAVAILABLE, readinessForErrorCode(604))
        assertEquals(Readiness.UNAVAILABLE, readinessForErrorCode(-101))
    }

    @Test
    fun `transient and unexpected error codes map to error`() {
        // BINDING_FAILURE, BUSY, UNKNOWN
        assertEquals(Readiness.ERROR, readinessForErrorCode(601))
        assertEquals(Readiness.ERROR, readinessForErrorCode(9))
        assertEquals(Readiness.ERROR, readinessForErrorCode(0))
    }
}
