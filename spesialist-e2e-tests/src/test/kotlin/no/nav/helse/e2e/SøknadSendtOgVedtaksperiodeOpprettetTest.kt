package no.nav.helse.e2e

import org.junit.jupiter.api.Test


class SøknadSendtOgVedtaksperiodeOpprettetTest : AbstractE2ETest() {

    @Test
    fun `Oppretter minimal person ved mottatt søknad og minimal vedtaksperiode ved vedtaksperiode opprettet`() {
        assertPersonEksistererIkke(FØDSELSNUMMER, AKTØR)

        vedtaksløsningenMottarNySøknad(AKTØR, FØDSELSNUMMER, ORGNR)
        assertPersonEksisterer(FØDSELSNUMMER, AKTØR)
        spleisOppretterNyBehandling(AKTØR, FØDSELSNUMMER, ORGNR)
        vedtaksløsningenMottarNySøknad(AKTØR, FØDSELSNUMMER, ORGNR)
        spleisOppretterNyBehandling(
            aktørId = AKTØR,
            fødselsnummer = FØDSELSNUMMER,
            organisasjonsnummer = ORGNR,
            vedtaksperiodeId = VEDTAKSPERIODE_ID
        )
        assertVedtaksperiodeEksisterer(VEDTAKSPERIODE_ID)

        vedtaksløsningenMottarNySøknad(AKTØR, FØDSELSNUMMER, ORGNR)
        assertPersonEksisterer(FØDSELSNUMMER, AKTØR)
    }
}
