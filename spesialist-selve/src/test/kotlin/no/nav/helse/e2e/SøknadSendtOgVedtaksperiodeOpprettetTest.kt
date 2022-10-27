package no.nav.helse.e2e

import AbstractE2ETest
import no.nav.helse.Meldingssender.sendSøknadSendt
import no.nav.helse.Meldingssender.sendVedtaksperiodeEndret
import no.nav.helse.Testdata.FØDSELSNUMMER
import no.nav.helse.Testdata.ORGNR
import no.nav.helse.Testdata.VEDTAKSPERIODE_ID
import org.junit.jupiter.api.Test


internal class SøknadSendtOgVedtaksperiodeOpprettetTest : AbstractE2ETest() {

    @Test
    fun `Oppretter minimal person og arbeidsgiver ved mottatt søknad og minial vedtaksperiode ved vedtaksperiode endret`() {
        assertPersonEksistererIkke(FØDSELSNUMMER)

        sendSøknadSendt()
        assertPersonEksisterer(FØDSELSNUMMER)
        assertArbeidsgiver(ORGNR)
        sendSøknadSendt()
        sendVedtaksperiodeEndret(orgnr = ORGNR, vedtaksperiodeId = VEDTAKSPERIODE_ID, forrigeTilstand = "START")
        assertVedtak(VEDTAKSPERIODE_ID)
        assertIngenOppgave()

        sendSøknadSendt()
        assertPersonEksisterer(FØDSELSNUMMER)
        assertArbeidsgiver(ORGNR)
    }
}
