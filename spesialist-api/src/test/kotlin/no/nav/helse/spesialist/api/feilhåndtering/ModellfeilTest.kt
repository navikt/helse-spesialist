package no.nav.helse.spesialist.api.feilhåndtering

import io.ktor.http.HttpStatusCode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

internal class ModellfeilTest {
    @Test
    fun testfeil() {

        val exception = assertThrows(
            Testfeil::class.java,
            { throw Testfeil() },
            "Testfeil"
        )
        assertEquals("testmelding", exception.message)
        assertEquals(HttpStatusCode.NotFound, exception.httpkode)

    }

    private class Testfeil : Modellfeil() {
        override val eksternKontekst: Map<String, Any>
            get() = mapOf("testdata" to "testdata")
        override val feilkode: String = "testmelding"
        override val httpkode: HttpStatusCode = HttpStatusCode.NotFound
    }
}
