package no.nav.helse.mediator

import com.fasterxml.jackson.databind.JsonNode
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.util.UUID
import no.nav.helse.mediator.dokument.DokumentMediator
import no.nav.helse.modell.dokument.DokumentDao
import no.nav.helse.modell.kommando.TestMelding
import no.nav.helse.objectMapper
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class DokumentMediatorTest {
    private companion object {
        private const val FNR = "12345678911"
        private val VEDTAKSPERIODE_ID = UUID.randomUUID()
        private val HENDELSE_ID = UUID.randomUUID()
        private val TESTHENDELSE = TestMelding(HENDELSE_ID, VEDTAKSPERIODE_ID, FNR)
        private val DOKUMENTID = UUID.randomUUID()
        private const val DOKUMENTTYPE = "SØKNAD"
        private const val RETRIES = 2
    }

    private val dokumentDao = mockk<DokumentDao>(relaxed = true)
    private val testRapid = TestRapid()

    private val mediator = DokumentMediator(
        dokumentDao = dokumentDao,
        rapidsConnection = testRapid,
        retries = RETRIES,
    )
    @BeforeEach
    fun setup() {
        clearMocks(dokumentDao)
        testRapid.reset()
    }

    @Test
    fun `Prøver å hente dokumentet {retries + 1} ganger`() {
        every { dokumentDao.hent(any(), any()) } returns null
        mediator.håndter(TESTHENDELSE.fødselsnummer(), DOKUMENTID, DOKUMENTTYPE)
        verify(exactly = RETRIES+1) {
            dokumentDao.hent(
                any(), any()
            )
        }
    }

    @Test
    fun `Sender behov dersom dokumentet ikke finnes i databasen`() {
        every { dokumentDao.hent(any(), any()) } returns null
        mediator.håndter(TESTHENDELSE.fødselsnummer(), DOKUMENTID, DOKUMENTTYPE)
        assertEquals(1, testRapid.inspektør.size)
        assertDokumentevent(0, "hent-dokument", DOKUMENTID)
    }

    @Test
    fun `Sender nytt behov dersom dokumentet i databasen er tomt`() {
        every { dokumentDao.hent(any(), any()) } returns objectMapper.createObjectNode()
        mediator.håndter(TESTHENDELSE.fødselsnummer(), DOKUMENTID, DOKUMENTTYPE)
        assertEquals(1, testRapid.inspektør.size)
        assertDokumentevent(0, "hent-dokument", DOKUMENTID)
    }

    @Test
    fun `Sender nytt behov dersom dokumentet i databasen ikke har 404 error`() {
        every { dokumentDao.hent(any(), any()) } returns objectMapper.createObjectNode().put("error", 403)
        mediator.håndter(TESTHENDELSE.fødselsnummer(), DOKUMENTID, DOKUMENTTYPE)
        assertEquals(1, testRapid.inspektør.size)
        assertDokumentevent(0, "hent-dokument", DOKUMENTID)
    }

    @Test
    fun `Sender ikke nytt behov dersom dokumentet i databasen har 404 error`() {
        every { dokumentDao.hent(any(), any()) } returns objectMapper.createObjectNode().put("error", 404)
        mediator.håndter(TESTHENDELSE.fødselsnummer(), DOKUMENTID, DOKUMENTTYPE)
        assertEquals(0, testRapid.inspektør.size)
    }

    @Test
    fun `Sender ikke behov dersom dokumentet finnes i databasen`() {
        every { dokumentDao.hent(any(), any()) } returns objectMapper.readTree("""{"ikkeTom":"harVerdi"}""")

        mediator.håndter(TESTHENDELSE.fødselsnummer(), DOKUMENTID, DOKUMENTTYPE)
        verify(exactly = 1) {
            dokumentDao.hent(
                any(), any()
            )
        }

        assertEquals(0, testRapid.inspektør.size)
    }

    private fun assertDokumentevent(
        indeks: Int,
        navn: String,
        dokumentId: UUID,
        dokumentType: String = "SØKNAD",
        assertBlock: (JsonNode) -> Unit = {},
    ) {
        testRapid.inspektør.message(indeks).also {
            assertEquals(navn, it.path("@event_name").asText())
            assertEquals(dokumentId, UUID.fromString(it.path("dokumentId").asText()))
            assertEquals(dokumentType, it.path("dokumentType").asText())
            assertBlock(it)
        }
    }
}
