package no.nav.helse.e2e

import AbstractE2ETest
import no.nav.helse.TestRapidHelpers.meldinger
import no.nav.helse.januar
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

internal class FattVedtakE2ETest: AbstractE2ETest() {

    @Test
    fun `Fatt vedtak for auu-periode`() {
        håndterSøknad()
        håndterVedtaksperiodeOpprettet()
        håndterUtkastTilVedtak()
        val hendelser = inspektør.meldinger()
        assertEquals(1, hendelser.size)
        val hendelse = hendelser.single()
        assertEquals("vedtak_fattet", hendelse["@event_name"].asText())
        assertEquals(0, hendelse["begrunnelser"].size())
        assertNull(hendelse["utbetalingId"])
    }

    @Test
    fun `Fatt vedtak for periode der SP er fastsatt etter hovedregel`() {
        fremTilSaksbehandleroppgave()
        håndterSaksbehandlerløsning()
        håndterUtkastTilVedtak(fastsattType = "EtterHovedregel")
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

        håndterUtkastTilVedtak(fastsattType = "EtterSkjønn")
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

        håndterUtkastTilVedtak(fastsattType = "IInfotrygd")
        val sisteHendelse = inspektør.meldinger().last()
        assertEquals("vedtak_fattet", sisteHendelse["@event_name"].asText())
        assertEquals(0, sisteHendelse["begrunnelser"].size())
        assertEquals("IInfotrygd", sisteHendelse["sykepengegrunnlagsfakta"]["fastsatt"].asText())
    }

    @Test
    fun `Finn sykepengegrunnlagdata i databasen`() {
        val sammenligningsgrunnlag = 420_000.0
        val avviksprosent = 42.0
        håndterAvviksvurdering(avviksprosent = avviksprosent, sammenligningsgrunnlag = sammenligningsgrunnlag, skjæringstidspunkt = 1.januar)
        fremTilSaksbehandleroppgave()
        håndterSaksbehandlerløsning()
        håndterUtkastTilVedtak(fastsattType = "EtterHovedregel", inkluderSpleisverdier = false)
        val sisteHendelse = inspektør.meldinger().last()
        assertEquals("vedtak_fattet", sisteHendelse["@event_name"].asText())
        assertEquals("EtterHovedregel", sisteHendelse["sykepengegrunnlagsfakta"]["fastsatt"].asText())
        assertEquals(avviksprosent, sisteHendelse["sykepengegrunnlagsfakta"]["avviksprosent"].asDouble())
        assertEquals(sammenligningsgrunnlag, sisteHendelse["sykepengegrunnlagsfakta"]["innrapportertÅrsinntekt"].asDouble())
    }
}
