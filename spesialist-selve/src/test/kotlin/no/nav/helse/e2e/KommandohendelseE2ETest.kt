package no.nav.helse.e2e

import AbstractE2ETest
import AbstractE2ETest.Kommandokjedetilstand.AVBRUTT
import AbstractE2ETest.Kommandokjedetilstand.NY
import AbstractE2ETest.Kommandokjedetilstand.SUSPENDERT
import kotliquery.queryOf
import kotliquery.sessionOf
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.UUID

internal class KommandohendelseE2ETest : AbstractE2ETest() {

    @Test
    fun `ignorerer behov uten tilhørende command`() {
        håndterVedtaksperiodeEndret()
        assertIkkeHendelse(sisteMeldingId)
    }

    @Test
    fun `vedtaksperiode forkastet`() {
        håndterVedtaksperiodeForkastet()
        assertIkkeHendelse(sisteMeldingId)
        assertVedtaksperiodeEksistererIkke(VEDTAKSPERIODE_ID)
    }

    @Test
    fun `gjør ingen ting om man får tilbake løsning på en avbrutt command context`() {
        vedtaksløsningenMottarNySøknad()
        spleisOppretterNyBehandling()
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
