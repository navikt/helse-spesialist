package no.nav.helse.spesialist.client.spiskammerset

import org.junit.jupiter.api.assertThrows
import kotlin.test.Test
import kotlin.test.assertEquals

class ClientUtilsTest {

    val backoffDelay = 0L

    @Test
    fun `kjører 1 gang om ingen retries`() {
        var count = 0
        ClientUtils.retryMedBackoff(maxRetries = 2, initialDelayMs = backoffDelay) {
            count += 1
        }
        assertEquals(1, count)
    }

    @Test
    fun `kjører 2 ganger når første runde feiler`() {
        var count = 0
        ClientUtils.retryMedBackoff(maxRetries = 2, initialDelayMs = backoffDelay) {
            if (count == 0) {
                count += 1
                throw RetryableException("Retryable")
            } else count += 1
        }
        assertEquals(2, count)
    }

    @Test
    fun `kaster exception hvis alle kall feiler`() {
        val maxRetries = 3
        var count = 0
        assertThrows<RetryableException> {
            ClientUtils.retryMedBackoff(maxRetries = maxRetries, initialDelayMs = backoffDelay) {
                count += 1
                throw RetryableException("Retryable")
            }
        }
        assertEquals(maxRetries, count)
    }
}