package no.nav.helse.e2e

import AbstractE2ETest
import io.mockk.every
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.Meldingssender.sendGodkjenningsbehov
import no.nav.helse.Meldingssender.sendPersoninfoløsningComposite
import no.nav.helse.Testdata.AKTØR
import no.nav.helse.Testdata.FØDSELSNUMMER
import no.nav.helse.Testdata.ORGNR
import no.nav.helse.Testdata.VEDTAKSPERIODE_ID
import no.nav.helse.Testdata.snapshot
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class EndretSkjermetinfoTest : AbstractE2ETest() {

    @Test
    fun `Ignorerer hendelsen for ukjente personer`() {
        sendEndretSkjermetinfo(true)
        assertNull(egenAnsattDao.erEgenAnsatt(FØDSELSNUMMER))
    }

    @Test
    fun `Ignorerer hendelsen for fødselsnummer som ikke lar seg caste til long`() {
        sendEndretSkjermetinfo(true, "123456789XX")
        assertNull(egenAnsattDao.erEgenAnsatt(FØDSELSNUMMER))
    }

    @Test
    fun `Etterspør skjermetinfo for kjente personer hvor skjermetinfo mangler i basen`() {
        val godkjenningsmeldingId = sendGodkjenningsbehov(AKTØR, FØDSELSNUMMER, ORGNR, VEDTAKSPERIODE_ID, UUID.randomUUID())
        sendPersoninfoløsningComposite(godkjenningsmeldingId, ORGNR, VEDTAKSPERIODE_ID)

        sendEndretSkjermetinfo(false)
        assertFalse(egenAnsattDao.erEgenAnsatt(FØDSELSNUMMER)!!)
        sendEndretSkjermetinfo(true)
        assertTrue(egenAnsattDao.erEgenAnsatt(FØDSELSNUMMER)!!)
    }

    @BeforeEach
    fun setup() {
        every { snapshotClient.hentSnapshot(FØDSELSNUMMER) } returns snapshot()
    }

    private fun sendEndretSkjermetinfo(skjermet: Boolean, fødselsnummer: String = FØDSELSNUMMER) {
        @Language("JSON")
        val json = """{
          "@event_name": "endret_skjermetinfo",
          "@id": "${UUID.randomUUID()}",
          "fødselsnummer": "$fødselsnummer",
          "skjermet": "$skjermet",
          "@opprettet": "${LocalDateTime.now()}"
        }"""
        testRapid.sendTestMessage(json)
    }

}
