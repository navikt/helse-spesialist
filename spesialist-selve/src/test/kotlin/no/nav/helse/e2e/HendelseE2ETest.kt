package no.nav.helse.e2e

import AbstractE2ETest
import AbstractE2ETest.Kommandokjedetilstand.AVBRUTT
import AbstractE2ETest.Kommandokjedetilstand.NY
import AbstractE2ETest.Kommandokjedetilstand.SUSPENDERT
import java.util.UUID
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.Testdata
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class HendelseE2ETest : AbstractE2ETest() {

    @Test
    fun `ignorerer behov uten tilhørende command`() {
        håndterVedtaksperiodeEndret()
        assertIkkeHendelse(sisteMeldingId)
    }

    @Test
    fun `vedtaksperiode forkastet`() {
        håndterVedtaksperiodeForkastet()
        assertIkkeHendelse(sisteMeldingId)
        assertVedtaksperiodeEksistererIkke(Testdata.VEDTAKSPERIODE_ID)
    }

    @Test
    fun `gjør ingen ting om man får tilbake løsning på en avbrutt command context`() {
        håndterSøknad()
        håndterVedtaksperiodeOpprettet()
        håndterGodkjenningsbehov()
        håndterVedtaksperiodeForkastet()
        håndterPersoninfoløsningUtenValidering()
        assertKommandokjedetilstander(sisteGodkjenningsbehovId, NY, SUSPENDERT, AVBRUTT)
    }


    private fun assertIkkeHendelse(hendelseId: UUID) {
        assertEquals(0, sessionOf(dataSource).use {
            it.run(queryOf("SELECT COUNT(1) FROM hendelse WHERE id = ?", hendelseId).map { row -> row.int(1) }.asSingle)
        })
    }
}
