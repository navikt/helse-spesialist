package no.nav.helse.spesialist.api

import tools.jackson.databind.DatabindException
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal class ClientDisconnectTest {
    private class MyDatabindException(
        message: String,
    ) : DatabindException(message)

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
