package no.nav.helse.e2e

import AbstractE2ETest
import no.nav.helse.Meldingssender.sendSøknadSendt
import no.nav.helse.Meldingssender.sendVedtaksperiodeEndret
import no.nav.helse.Testdata.AKTØR
import no.nav.helse.Testdata.FØDSELSNUMMER
import no.nav.helse.Testdata.ORGNR
import no.nav.helse.Testdata.VEDTAKSPERIODE_ID
import org.junit.jupiter.api.Test


internal class SøknadSendtOgVedtaksperiodeOpprettetTest : AbstractE2ETest() {

    @Test
    fun `Oppretter minimal person og arbeidsgiver ved mottatt søknad og minial vedtaksperiode ved vedtaksperiode endret`() {
        assertPersonEksistererIkke(FØDSELSNUMMER, AKTØR)

        sendSøknadSendt(AKTØR, FØDSELSNUMMER, ORGNR)
        assertPersonEksisterer(FØDSELSNUMMER, AKTØR)
        assertArbeidsgiverEksisterer(ORGNR)
        sendSøknadSendt(AKTØR, FØDSELSNUMMER, ORGNR)
        sendVedtaksperiodeEndret(
            aktørId = AKTØR,
            fødselsnummer = FØDSELSNUMMER,
            organisasjonsnummer = ORGNR,
            vedtaksperiodeId = VEDTAKSPERIODE_ID,
            forrigeTilstand = "START"
        )
        assertVedtaksperiodeEksisterer(VEDTAKSPERIODE_ID)
        assertIngenOppgave()

        sendSøknadSendt(AKTØR, FØDSELSNUMMER, ORGNR)
        assertPersonEksisterer(FØDSELSNUMMER, AKTØR)
        assertArbeidsgiverEksisterer(ORGNR)
    }
}
