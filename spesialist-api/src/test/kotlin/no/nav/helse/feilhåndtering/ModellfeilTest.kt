package no.nav.helse.feilhåndtering

import io.ktor.http.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith

internal class ModellfeilTest {
    @Test
    fun testfeil() {
        val exception = assertFailsWith<Testfeil> {
            throw Testfeil()
        }
        assertEquals("testmelding", exception.message)
        assertEquals(HttpStatusCode.NotFound, exception.httpkode)

    }

    private class Testfeil: Modellfeil() {
        override val eksternKontekst: Map<String, Any>
            get() = mapOf("testdata" to "testdata")
        override val melding: String = "testmelding"
        override val httpkode: HttpStatusCode = HttpStatusCode.NotFound
    }
}
