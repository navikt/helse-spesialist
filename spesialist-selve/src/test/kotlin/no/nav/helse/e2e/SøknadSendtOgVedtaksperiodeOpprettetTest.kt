package no.nav.helse.e2e

import AbstractE2ETest
import org.junit.jupiter.api.Test


internal class SøknadSendtOgVedtaksperiodeOpprettetTest : AbstractE2ETest() {

    @Test
    fun `Oppretter minimal person og arbeidsgiver ved mottatt søknad og minimal vedtaksperiode ved vedtaksperiode opprettet`() {
        assertPersonEksistererIkke(FØDSELSNUMMER, AKTØR)

        håndterSøknad(AKTØR, FØDSELSNUMMER, ORGNR)
        assertPersonEksisterer(FØDSELSNUMMER, AKTØR)
        assertArbeidsgiverEksisterer(ORGNR)
        håndterSøknad(AKTØR, FØDSELSNUMMER, ORGNR)
        håndterVedtaksperiodeOpprettet(
            aktørId = AKTØR,
            fødselsnummer = FØDSELSNUMMER,
            organisasjonsnummer = ORGNR,
            vedtaksperiodeId = VEDTAKSPERIODE_ID
        )
        assertVedtaksperiodeEksisterer(VEDTAKSPERIODE_ID)

        håndterSøknad(AKTØR, FØDSELSNUMMER, ORGNR)
        assertPersonEksisterer(FØDSELSNUMMER, AKTØR)
        assertArbeidsgiverEksisterer(ORGNR)
    }
}
