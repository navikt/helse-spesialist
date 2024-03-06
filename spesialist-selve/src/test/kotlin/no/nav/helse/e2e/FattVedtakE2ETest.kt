package no.nav.helse.e2e

import AbstractE2ETest
import com.fasterxml.jackson.module.kotlin.convertValue
import java.time.LocalDate
import java.util.UUID
import no.nav.helse.AvviksvurderingTestdata
import no.nav.helse.GodkjenningsbehovTestdata
import no.nav.helse.TestRapidHelpers.hendelser
import no.nav.helse.TestRapidHelpers.meldinger
import no.nav.helse.januar
import no.nav.helse.mediator.asUUID
import no.nav.helse.objectMapper
import no.nav.helse.rapids_rivers.asLocalDate
import no.nav.helse.rapids_rivers.asLocalDateTime
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

internal class FattVedtakE2ETest: AbstractE2ETest() {

    @Test
    fun `Fatt vedtak for auu-periode`() {
        val spleisBehandlingId = UUID.randomUUID()
        håndterSøknad()
        håndterVedtaksperiodeOpprettet()
        håndterAvsluttetUtenVedtak(spleisBehandlingId = spleisBehandlingId)
        val hendelser = inspektør.hendelser("vedtak_fattet")
        assertEquals(1, hendelser.size)
        val hendelse = hendelser.single()
        assertEquals("vedtak_fattet", hendelse["@event_name"].asText())
        assertEquals(0, hendelse["begrunnelser"].size())
        assertNull(hendelse["utbetalingId"])
        assertNull(hendelse["sykepengegrunnlagsfakta"])
        assertEquals(0.0, hendelse["grunnlagForSykepengegrunnlag"].asDouble())
        assertEquals(
            emptyMap<String, Double>(),
            objectMapper.convertValue<Map<String, Double>>(hendelse["grunnlagForSykepengegrunnlagPerArbeidsgiver"])
        )
        assertEquals("VET_IKKE", hendelse["begrensning"].asText())
        assertEquals(0.0, hendelse["inntekt"].asDouble())
        assertEquals(0.0, hendelse["sykepengegrunnlag"].asDouble())
        assertEquals(1.januar, hendelse["fom"].asLocalDate())
        assertEquals(11.januar, hendelse["tom"].asLocalDate())
        assertEquals(AKTØR, hendelse["aktørId"].asText())
        assertEquals(LocalDate.now(), hendelse["vedtakFattetTidspunkt"].asLocalDateTime().toLocalDate())
        assertEquals(spleisBehandlingId, hendelse["behandlingId"].asUUID())
    }

    @Test
    fun `Fatt vedtak for periode der SP er fastsatt etter hovedregel`() {
        fremTilSaksbehandleroppgave()
        håndterSaksbehandlerløsning()
        håndterAvsluttetMedVedtak(fastsattType = "EtterHovedregel")
        val sisteHendelse = inspektør.meldinger().last()
        assertEquals("vedtak_fattet", sisteHendelse["@event_name"].asText())
        assertEquals(0, sisteHendelse["begrunnelser"].size())
        assertEquals("EtterHovedregel", sisteHendelse["sykepengegrunnlagsfakta"]["fastsatt"].asText())
    }

    @Test
    fun `Fatt vedtak for periode der SP er fastsatt etter skjønn`() {
        fremTilSaksbehandleroppgave()
        håndterSkjønnsfastsattSykepengegrunnlag()
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

    @Test
    fun `Fatt vedtak for periode der SP er fastsatt i Infotrygd`() {
        fremTilSaksbehandleroppgave()
        håndterSkjønnsfastsattSykepengegrunnlag()
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
        fremTilSaksbehandleroppgave(
            avviksvurderingTestdata = AvviksvurderingTestdata(
                avviksprosent = avviksprosent,
                sammenligningsgrunnlag = sammenligningsgrunnlag,
                skjæringstidspunkt = 1.januar,
            )
        )
        håndterSaksbehandlerløsning()
        håndterAvsluttetMedVedtak(fastsattType = "EtterHovedregel", settInnAvviksvurderingFraSpleis = false)

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
        val tagsFraGodkjenningsbehovet = listOf("Arbeidsgiverutbetaling", "Personutbetaling")
        val tagsFraAvsluttetMedVedtak = listOf("SykepengegrunnlagUnder2G", "IngenNyArbeidsgiverperiode")
        val godkjenningsbehov = GodkjenningsbehovTestdata(
            fødselsnummer = FØDSELSNUMMER,
            aktørId = AKTØR,
            organisasjonsnummer = ORGNR,
            vedtaksperiodeId = VEDTAKSPERIODE_ID,
            utbetalingId = UTBETALING_ID,
            spleisBehandlingId = spleisBehandlingId,
            tags = tagsFraGodkjenningsbehovet
        )
        fremTilSaksbehandleroppgave(godkjenningsbehovTestdata = godkjenningsbehov)
        håndterSaksbehandlerløsning()
        håndterAvsluttetMedVedtak(tags = tagsFraAvsluttetMedVedtak, spleisBehandlingId = spleisBehandlingId)

        val sisteHendelse = inspektør.meldinger().last()
        assertEquals("vedtak_fattet", sisteHendelse["@event_name"].asText())
        assertEquals(spleisBehandlingId, UUID.fromString(sisteHendelse["behandlingId"].asText()))
        assertEquals(tagsFraGodkjenningsbehovet + tagsFraAvsluttetMedVedtak, sisteHendelse["tags"].map { it.asText() })
    }

}
