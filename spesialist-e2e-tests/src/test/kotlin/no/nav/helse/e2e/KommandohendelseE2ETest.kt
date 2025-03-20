package no.nav.helse.e2e

import no.nav.helse.e2e.AbstractE2ETest.Kommandokjedetilstand.AVBRUTT
import no.nav.helse.e2e.AbstractE2ETest.Kommandokjedetilstand.NY
import no.nav.helse.e2e.AbstractE2ETest.Kommandokjedetilstand.SUSPENDERT
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.UUID

class KommandohendelseE2ETest : AbstractE2ETest() {

    @Test
    fun `lagrer melding som starter en command context`() {
        vedtaksløsningenMottarNySøknad()
        assertHendelseLagret(sisteMeldingId)
    }

    @Test
    fun `gjør ingen ting om man får tilbake løsning på en avbrutt command context`() {
        vedtaksløsningenMottarNySøknad()
        spleisOppretterNyBehandling()
        håndterGodkjenningsbehov()
        håndterVedtaksperiodeForkastet()
        håndterPersoninfoløsningUtenValidering()
        assertKommandokjedetilstander(sisteGodkjenningsbehovId, NY, SUSPENDERT, SUSPENDERT, AVBRUTT)
        assertHendelseIkkeLagret(sisteMeldingId)
    }

    private fun assertHendelseIkkeLagret(hendelseId: UUID) {
        assertEquals(0, antallHendelser(hendelseId))
    }

    private fun assertHendelseLagret(hendelseId: UUID) {
        assertEquals(1, antallHendelser(hendelseId))
    }

    private fun antallHendelser(hendelseId: UUID) = dbQuery.single(
        "select count(1) from hendelse where id = :hendelseId",
        "hendelseId" to hendelseId
    ) { it.int(1) }
}
