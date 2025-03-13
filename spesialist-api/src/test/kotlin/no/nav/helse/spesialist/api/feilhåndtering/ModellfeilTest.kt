package no.nav.helse.spesialist.api.feilhåndtering

import io.ktor.http.HttpStatusCode
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

internal class ModellfeilTest {
    @Test
    fun testfeil() {

        val exception = Assertions.assertThrows(
            Testfeil::class.java,
            { throw Testfeil() },
            "Testfeil"
        )
        Assertions.assertEquals("testmelding", exception.message)
        Assertions.assertEquals(HttpStatusCode.Companion.NotFound, exception.httpkode)

    }

    private class Testfeil : Modellfeil() {
        override val eksternKontekst: Map<String, Any>
            get() = mapOf("testdata" to "testdata")
        override val feilkode: String = "testmelding"
        override val httpkode: HttpStatusCode = HttpStatusCode.Companion.NotFound
    }
}