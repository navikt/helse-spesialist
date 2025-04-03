package no.nav.helse.e2e

import no.nav.helse.AvviksvurderingTestdata
import no.nav.helse.GodkjenningsbehovTestdata
import no.nav.helse.TestRapidHelpers.meldinger
import no.nav.helse.Testdata.skjønnsvurdering
import no.nav.helse.util.januar
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.UUID

class FattVedtakE2ETest: AbstractE2ETest() {
    @Test
    fun `Fatt vedtak for periode der SP er fastsatt etter skjønn`() {
        vedtaksløsningenMottarNySøknad()
        spleisOppretterNyBehandling()
        spesialistBehandlerGodkjenningsbehovFremTilOppgave()
        håndterSkjønnsfastsattSykepengegrunnlag()
        spesialistBehandlerGodkjenningsbehovFremTilOppgave(
            harRisikovurdering = true,
            harOppdatertMetadata = true,
            godkjenningsbehovTestdata = godkjenningsbehovTestdata.copy(utbetalingId = UUID.randomUUID())
        )
        håndterSaksbehandlerløsning()

        håndterAvsluttetMedVedtak(fastsattType = "EtterSkjønn")
        val sisteHendelse = inspektør.meldinger().last()
        assertEquals("vedtak_fattet", sisteHendelse["@event_name"].asText())
        assertEquals(3, sisteHendelse["begrunnelser"].size())
        assertEquals("SkjønnsfastsattSykepengegrunnlagMal", sisteHendelse["begrunnelser"][0]["type"].asText())
        assertEquals("SkjønnsfastsattSykepengegrunnlagFritekst", sisteHendelse["begrunnelser"][1]["type"].asText())
        assertEquals("SkjønnsfastsattSykepengegrunnlagKonklusjon", sisteHendelse["begrunnelser"][2]["type"].asText())
        assertEquals("EtterSkjønn", sisteHendelse["sykepengegrunnlagsfakta"]["fastsatt"].asText())
    }

    /*
    Denne testen er litt wonky, fordi den tester et case som i virkelighetens verden involverer en spouting fra oss
    utviklere. Testriggen støtter ikke å sende inn et frittstående godkjenningsbehov (tilsvarende når spleis har blitt
    påminnet), så vi må kjøre fyll "reberegningsløype".
     */
    @Test
    fun `Velger nyeste skjønnsmessig fastsettelse`() {
        vedtaksløsningenMottarNySøknad()
        spleisOppretterNyBehandling()
        spesialistBehandlerGodkjenningsbehovFremTilOppgave()
        håndterSkjønnsfastsattSykepengegrunnlag(
            arbeidsgivere = listOf(skjønnsvurdering().copy(begrunnelseFritekst = "første tekst"))
        )
        spesialistBehandlerGodkjenningsbehovFremTilOppgave(
            harRisikovurdering = true,
            harOppdatertMetadata = true,
            godkjenningsbehovTestdata = godkjenningsbehovTestdata.copy(utbetalingId = UUID.randomUUID())
        )
        håndterSkjønnsfastsattSykepengegrunnlag(
            arbeidsgivere = listOf(skjønnsvurdering().copy(begrunnelseFritekst = "andre tekst"))
        )
        // Skulle ha vært et frittstående (påminnet) godkjenningsbehov
        spesialistBehandlerGodkjenningsbehovFremTilOppgave(
            harRisikovurdering = true,
            harOppdatertMetadata = true,
            godkjenningsbehovTestdata = godkjenningsbehovTestdata.copy(utbetalingId = UUID.randomUUID())
        )
        håndterSaksbehandlerløsning()
        håndterAvsluttetMedVedtak(fastsattType = "EtterSkjønn")

        val vedtakFattetMelding = inspektør.meldinger().last { it["@event_name"].asText() == "vedtak_fattet" }
        assertEquals("andre tekst", vedtakFattetMelding["begrunnelser"][1]["begrunnelse"].asText())
    }

    @Test
    fun `Fatt vedtak for periode der SP er fastsatt i Infotrygd`() {
        vedtaksløsningenMottarNySøknad()
        spleisOppretterNyBehandling()
        spesialistBehandlerGodkjenningsbehovFremTilOppgave()
        håndterSkjønnsfastsattSykepengegrunnlag()
        spesialistBehandlerGodkjenningsbehovFremTilOppgave(
            harRisikovurdering = true,
            harOppdatertMetadata = true,
            godkjenningsbehovTestdata = godkjenningsbehovTestdata.copy(utbetalingId = UUID.randomUUID())
        )
        håndterSaksbehandlerløsning()

        håndterAvsluttetMedVedtak(fastsattType = "IInfotrygd")
        val sisteHendelse = inspektør.meldinger().last()
        assertEquals("vedtak_fattet", sisteHendelse["@event_name"].asText())
        assertEquals(0, sisteHendelse["begrunnelser"].size())
        assertEquals("IInfotrygd", sisteHendelse["sykepengegrunnlagsfakta"]["fastsatt"].asText())
    }

    @Test
    fun `Finn sykepengegrunnlagdata i databasen`() {
        val sammenligningsgrunnlag = 420_000.0
        val avviksprosent = 42.0
        vedtaksløsningenMottarNySøknad()
        spleisOppretterNyBehandling()
        spesialistBehandlerGodkjenningsbehovFremTilOppgave(
            avviksvurderingTestdata = AvviksvurderingTestdata(
                avviksprosent = avviksprosent,
                sammenligningsgrunnlag = sammenligningsgrunnlag,
                skjæringstidspunkt = 1.januar,
            )
        )
        håndterSaksbehandlerløsning()
        håndterAvsluttetMedVedtak(
            fastsattType = "EtterHovedregel",
            settInnAvviksvurderingFraSpleis = false
        )

        val sisteHendelse = inspektør.meldinger().last()
        assertEquals("vedtak_fattet", sisteHendelse["@event_name"].asText())
        val sykepengegrunnlagsfakta = sisteHendelse["sykepengegrunnlagsfakta"]
        assertEquals("EtterHovedregel", sykepengegrunnlagsfakta["fastsatt"].asText())
        assertEquals(avviksprosent, sykepengegrunnlagsfakta["avviksprosent"].asDouble())
        assertEquals(sammenligningsgrunnlag, sykepengegrunnlagsfakta["innrapportertÅrsinntekt"].asDouble())
    }

    @Test
    fun `behandlingsinformasjon fra godkjenningsbehovet skal sendes på vedtak_fattet`() {
        val spleisBehandlingId = UUID.randomUUID()
        val tagsFraGodkjenningsbehovet = listOf(
            "Arbeidsgiverutbetaling",
            "Personutbetaling",
            "SykepengegrunnlagUnder2G",
            "IngenNyArbeidsgiverperiode",
            "6GBegrenset",
            "SykepengegrunnlagUnder2G",
            "IngenNyArbeidsgiverperiode"
        )
        val godkjenningsbehov = GodkjenningsbehovTestdata(
            fødselsnummer = FØDSELSNUMMER,
            aktørId = AKTØR,
            organisasjonsnummer = ORGNR,
            vedtaksperiodeId = VEDTAKSPERIODE_ID,
            utbetalingId = UTBETALING_ID,
            spleisBehandlingId = spleisBehandlingId,
            tags = tagsFraGodkjenningsbehovet
        )
        vedtaksløsningenMottarNySøknad()
        spleisOppretterNyBehandling(spleisBehandlingId = spleisBehandlingId)
        spesialistBehandlerGodkjenningsbehovFremTilOppgave(godkjenningsbehovTestdata = godkjenningsbehov)
        håndterSaksbehandlerløsning()
        håndterAvsluttetMedVedtak(spleisBehandlingId = spleisBehandlingId)

        val sisteHendelse = inspektør.meldinger().last()
        assertEquals("vedtak_fattet", sisteHendelse["@event_name"].asText())
        assertEquals(spleisBehandlingId, UUID.fromString(sisteHendelse["behandlingId"].asText()))
        val tagsSomSkalISykepengegrunnlagsfakta = listOf("6GBegrenset")
        val forventet = (tagsFraGodkjenningsbehovet - tagsSomSkalISykepengegrunnlagsfakta).toSet()
        assertEquals(forventet, sisteHendelse["tags"].map { it.asText() }.toSet())
        val sykepengegrunnlagsfakta = sisteHendelse["sykepengegrunnlagsfakta"]
        assertEquals(tagsSomSkalISykepengegrunnlagsfakta, sykepengegrunnlagsfakta["tags"].map { it.asText() } )
    }

    @Test
    fun `behandlingsinformasjon fra godkjenningsbehovet skal sendes på vedtak_fattet - uten tag på sykepengegrunnlagsfakta`() {
        val spleisBehandlingId = UUID.randomUUID()
        val tagsFraGodkjenningsbehovet = listOf(
            "Arbeidsgiverutbetaling",
            "Personutbetaling",
            "SykepengegrunnlagUnder2G",
            "IngenNyArbeidsgiverperiode",
            "SykepengegrunnlagUnder2G",
            "IngenNyArbeidsgiverperiode"
        )
        val godkjenningsbehov = GodkjenningsbehovTestdata(
            fødselsnummer = FØDSELSNUMMER,
            aktørId = AKTØR,
            organisasjonsnummer = ORGNR,
            vedtaksperiodeId = VEDTAKSPERIODE_ID,
            utbetalingId = UTBETALING_ID,
            spleisBehandlingId = spleisBehandlingId,
            tags = tagsFraGodkjenningsbehovet
        )
        vedtaksløsningenMottarNySøknad()
        spleisOppretterNyBehandling(spleisBehandlingId = spleisBehandlingId)
        spesialistBehandlerGodkjenningsbehovFremTilOppgave(godkjenningsbehovTestdata = godkjenningsbehov)
        håndterSaksbehandlerløsning()
        håndterAvsluttetMedVedtak(spleisBehandlingId = spleisBehandlingId)

        val sisteHendelse = inspektør.meldinger().last()
        assertEquals("vedtak_fattet", sisteHendelse["@event_name"].asText())
        assertEquals(spleisBehandlingId, UUID.fromString(sisteHendelse["behandlingId"].asText()))
        val forventet = tagsFraGodkjenningsbehovet.toSet()
        assertEquals(forventet, sisteHendelse["tags"].map { it.asText() }.toSet())
        val sykepengegrunnlagsfakta = sisteHendelse["sykepengegrunnlagsfakta"]
        assertTrue(sykepengegrunnlagsfakta["tags"].isEmpty)
    }
}
