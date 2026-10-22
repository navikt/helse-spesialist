package no.nav.helse.spesialist.api

import io.ktor.utils.io.ConnectionClosedException
import tools.jackson.databind.DatabindException
import java.io.Closeable
import java.util.concurrent.CancellationException
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal class ClientDisconnectTest {
    private class MyDatabindException(
        message: String,
        cause: Throwable? = null,
    ) : DatabindException(null as Closeable?, message, cause)

    @Test
    fun `CancellationException som årsak gjenkjennes som klientfrakobling`() {
        val cause = CancellationException("Call context was cancelled by `HttpRequestLifecycle` plugin")
        assertTrue(MyDatabindException(cause.message!!, cause).erKlientFrakoblingUnderSerialisering())
    }

    @Test
    fun `ConnectionClosedException dypt i årsakskjeden gjenkjennes som klientfrakobling`() {
        val cause = RuntimeException("innpakket", ConnectionClosedException("Connection was closed"))
        assertTrue(MyDatabindException("noko", cause).erKlientFrakoblingUnderSerialisering())
    }

    @Test
    fun `sykel i årsakskjeden gir ikke evig løkke`() {
        val a = RuntimeException("a")
        val b = RuntimeException("b", a)
        a.initCause(b)
        assertFalse(MyDatabindException("noko", a).erKlientFrakoblingUnderSerialisering())
    }

    @Test
    fun `annen årsak gjenkjennes ikke som klientfrakobling`() {
        assertFalse(MyDatabindException("noko", IllegalStateException("feil")).erKlientFrakoblingUnderSerialisering())
    }

    @Test
    fun `Broken pipe gjenkjennes som klientfrakobling`() {
        assertTrue(MyDatabindException("Broken pipe").erKlientFrakoblingUnderSerialisering())
    }

    @Test
    fun `Connection reset by peer gjenkjennes som klientfrakobling`() {
        assertTrue(MyDatabindException("Connection reset by peer").erKlientFrakoblingUnderSerialisering())
    }

    @Test
    fun `andre databind exceptions gjenkjennes ikke som klientfrakobling`() {
        assertFalse(MyDatabindException("noko gale").erKlientFrakoblingUnderSerialisering())
    }
}
