package no.nav.helse.spesialist.domain.tilkommeninntekt

import no.nav.helse.modell.totrinnsvurdering.TotrinnsvurderingId
import no.nav.helse.spesialist.domain.Periode.Companion.tilOgMed
import no.nav.helse.spesialist.domain.testfixtures.jan
import no.nav.helse.spesialist.domain.testfixtures.lagFødselsnummer
import no.nav.helse.spesialist.domain.testfixtures.lagOrganisasjonsnummer
import no.nav.helse.spesialist.domain.testfixtures.lagSaksbehandlerident
import java.math.BigDecimal
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TilkommenInntektTest {

    @Test
    fun `kan opprette tilkommen Inntekt`() {
        //given
        val fødselsnummer = lagFødselsnummer()
        val organisasjonsnummer = lagOrganisasjonsnummer()
        val saksbehandlerIdent = lagSaksbehandlerident()

        //when
        val tilkommenInntekt = TilkommenInntekt.ny(
            periode = (1 jan 2018) tilOgMed (31 jan 2018),
            ekskluderteUkedager = setOf(1 jan 2018, 31 jan 2018),
            periodebeløp = BigDecimal("10000"),
            fødselsnummer = fødselsnummer,
            saksbehandlerIdent = saksbehandlerIdent,
            notatTilBeslutter = "et notat til beslutter",
            totrinnsvurderingId = TotrinnsvurderingId(Random.nextLong()),
            organisasjonsnummer = organisasjonsnummer
        )

        //then
        assertEquals(1, tilkommenInntekt.events.size)
        assertEquals(TilkommenInntektOpprettetEvent::class, tilkommenInntekt.events.last()::class)
        val opprettetEvent = tilkommenInntekt.events.last() as TilkommenInntektOpprettetEvent
        assertEquals(organisasjonsnummer, opprettetEvent.organisasjonsnummer)
        assertEquals(1 jan 2018, opprettetEvent.periode.fom)
        assertEquals(31 jan 2018, opprettetEvent.periode.tom)
        assertEquals(setOf(1 jan 2018, 31 jan 2018), opprettetEvent.ekskluderteUkedager)
        assertEquals(BigDecimal("10000"), opprettetEvent.periodebeløp)

        assertEquals("et notat til beslutter", opprettetEvent.metadata.notatTilBeslutter)
        assertEquals(saksbehandlerIdent, opprettetEvent.metadata.utførtAvSaksbehandlerIdent)
        assertEquals(1, opprettetEvent.metadata.sekvensnummer)

        assertEquals(organisasjonsnummer, tilkommenInntekt.organisasjonsnummer)
        assertEquals(1 jan 2018, tilkommenInntekt.periode.fom)
        assertEquals(31 jan 2018, tilkommenInntekt.periode.tom)
        assertEquals(setOf(1 jan 2018, 31 jan 2018), tilkommenInntekt.ekskluderteUkedager)
        assertEquals(BigDecimal("10000"), tilkommenInntekt.periodebeløp)
        assertFalse(tilkommenInntekt.fjernet)
    }

    @Test
    fun `kan endre tilkommen inntekt`() {
        //given
        val fødselsnummer = lagFødselsnummer()
        val organisasjonsnummer = lagOrganisasjonsnummer()
        val tilkommenInntekt = TilkommenInntekt.ny(
            periode = (1 jan 2018) tilOgMed (31 jan 2018),
            ekskluderteUkedager = setOf(1 jan 2018, 31 jan 2018),
            periodebeløp = BigDecimal("10000"),
            fødselsnummer = fødselsnummer,
            saksbehandlerIdent = lagSaksbehandlerident(),
            notatTilBeslutter = "et notat til beslutter",
            totrinnsvurderingId = TotrinnsvurderingId(Random.nextLong()),
            organisasjonsnummer = organisasjonsnummer
        )

        //when
        val endretOrganisasjonsnummer = lagOrganisasjonsnummer()
        val saksbehandlerIdent = lagSaksbehandlerident()
        tilkommenInntekt.endreTil(
            organisasjonsnummer = endretOrganisasjonsnummer,
            periode = (3 jan 2018) tilOgMed (20 jan 2018),
            periodebeløp = BigDecimal("100"),
            ekskluderteUkedager = emptySet(),
            saksbehandlerIdent = saksbehandlerIdent,
            notatTilBeslutter = "nytt notat",
            totrinnsvurderingId = TotrinnsvurderingId(Random.nextLong())
        )

        //then
        assertEquals(2, tilkommenInntekt.events.size)
        assertEquals(TilkommenInntektEndretEvent::class, tilkommenInntekt.events.last()::class)
        val endretEvent = tilkommenInntekt.events.last() as TilkommenInntektEndretEvent
        assertEquals(organisasjonsnummer, endretEvent.endringer.organisasjonsnummer?.fra)
        assertEquals(endretOrganisasjonsnummer, endretEvent.endringer.organisasjonsnummer?.til)
        assertEquals((1 jan 2018) tilOgMed (31 jan 2018), endretEvent.endringer.periode?.fra)
        assertEquals((3 jan 2018) tilOgMed (20 jan 2018), endretEvent.endringer.periode?.til)
        assertEquals(sortedSetOf(1 jan 2018, 31 jan 2018), endretEvent.endringer.ekskluderteUkedager?.fra)
        assertEquals(sortedSetOf(), endretEvent.endringer.ekskluderteUkedager?.til)
        assertEquals(BigDecimal("10000"), endretEvent.endringer.periodebeløp?.fra)
        assertEquals(BigDecimal("100"), endretEvent.endringer.periodebeløp?.til)

        assertEquals("nytt notat", endretEvent.metadata.notatTilBeslutter)
        assertEquals(saksbehandlerIdent, endretEvent.metadata.utførtAvSaksbehandlerIdent)
        assertEquals(2, endretEvent.metadata.sekvensnummer)

        assertEquals(endretOrganisasjonsnummer, tilkommenInntekt.organisasjonsnummer)
        assertEquals(3 jan 2018, tilkommenInntekt.periode.fom)
        assertEquals(20 jan 2018, tilkommenInntekt.periode.tom)
        assertEquals(emptySet(), tilkommenInntekt.ekskluderteUkedager)
        assertEquals(BigDecimal("100"), tilkommenInntekt.periodebeløp)
        assertFalse(tilkommenInntekt.fjernet)

    }

    @Test
    fun `kan gjenoprette tilkommen inntekt`() {
        //given
        val fødselsnummer = lagFødselsnummer()
        val organisasjonsnummer = lagOrganisasjonsnummer()
        val tilkommenInntekt = TilkommenInntekt.ny(
            periode = (1 jan 2018) tilOgMed (31 jan 2018),
            ekskluderteUkedager = setOf(1 jan 2018, 31 jan 2018),
            periodebeløp = BigDecimal("10000"),
            fødselsnummer = fødselsnummer,
            saksbehandlerIdent = lagSaksbehandlerident(),
            notatTilBeslutter = "et notat til beslutter",
            totrinnsvurderingId = TotrinnsvurderingId(Random.nextLong()),
            organisasjonsnummer = organisasjonsnummer
        )

        tilkommenInntekt.fjern(lagSaksbehandlerident(), "fjern", TotrinnsvurderingId(Random.nextLong()))

        //when
        val endretOrganisasjonsnummer = lagOrganisasjonsnummer()
        val saksbehandlerIdent = lagSaksbehandlerident()
        tilkommenInntekt.gjenopprett(
            organisasjonsnummer = endretOrganisasjonsnummer,
            periode = (3 jan 2018) tilOgMed (20 jan 2018),
            periodebeløp = BigDecimal("100"),
            ekskluderteUkedager = emptySet(),
            saksbehandlerIdent = saksbehandlerIdent,
            notatTilBeslutter = "nytt notat",
            totrinnsvurderingId = TotrinnsvurderingId(Random.nextLong())
        )

        //then
        assertEquals(3, tilkommenInntekt.events.size)
        assertEquals(TilkommenInntektGjenopprettetEvent::class, tilkommenInntekt.events.last()::class)
        val gjenopprettetEvent = tilkommenInntekt.events.last() as TilkommenInntektGjenopprettetEvent
        assertEquals(organisasjonsnummer, gjenopprettetEvent.endringer.organisasjonsnummer?.fra)
        assertEquals(endretOrganisasjonsnummer, gjenopprettetEvent.endringer.organisasjonsnummer?.til)
        assertEquals((1 jan 2018) tilOgMed (31 jan 2018), gjenopprettetEvent.endringer.periode?.fra)
        assertEquals((3 jan 2018) tilOgMed (20 jan 2018), gjenopprettetEvent.endringer.periode?.til)
        assertEquals(sortedSetOf(1 jan 2018, 31 jan 2018), gjenopprettetEvent.endringer.ekskluderteUkedager?.fra)
        assertEquals(sortedSetOf(), gjenopprettetEvent.endringer.ekskluderteUkedager?.til)
        assertEquals(BigDecimal("10000"), gjenopprettetEvent.endringer.periodebeløp?.fra)
        assertEquals(BigDecimal("100"), gjenopprettetEvent.endringer.periodebeløp?.til)

        assertEquals("nytt notat", gjenopprettetEvent.metadata.notatTilBeslutter)
        assertEquals(saksbehandlerIdent, gjenopprettetEvent.metadata.utførtAvSaksbehandlerIdent)
        assertEquals(3, gjenopprettetEvent.metadata.sekvensnummer)

        assertEquals(endretOrganisasjonsnummer, tilkommenInntekt.organisasjonsnummer)
        assertEquals(3 jan 2018, tilkommenInntekt.periode.fom)
        assertEquals(20 jan 2018, tilkommenInntekt.periode.tom)
        assertEquals(emptySet(), tilkommenInntekt.ekskluderteUkedager)
        assertEquals(BigDecimal("100"), tilkommenInntekt.periodebeløp)
        assertFalse(tilkommenInntekt.fjernet)

    }

    @Test
    fun `kan fjerne tilkommen inntekt`() {
        // given
        val fødselsnummer = lagFødselsnummer()
        val organisasjonsnummer = lagOrganisasjonsnummer()
        val tilkommenInntekt = TilkommenInntekt.ny(
            periode = (1 jan 2018) tilOgMed (31 jan 2018),
            ekskluderteUkedager = setOf(1 jan 2018, 31 jan 2018),
            periodebeløp = BigDecimal("10000"),
            fødselsnummer = fødselsnummer,
            saksbehandlerIdent = lagSaksbehandlerident(),
            notatTilBeslutter = "et notat til beslutter",
            totrinnsvurderingId = TotrinnsvurderingId(Random.nextLong()),
            organisasjonsnummer = organisasjonsnummer
        )

        // when
        val saksbehandlerIdent = lagSaksbehandlerident()
        tilkommenInntekt.fjern(saksbehandlerIdent, "remove", TotrinnsvurderingId(Random.nextLong()))

        // then
        assertEquals(2, tilkommenInntekt.events.size)
        assertEquals(TilkommenInntektFjernetEvent::class, tilkommenInntekt.events.last()::class)
        val fjernetEvent = tilkommenInntekt.events.last() as TilkommenInntektFjernetEvent

        assertEquals("remove", fjernetEvent.metadata.notatTilBeslutter)
        assertEquals(saksbehandlerIdent, fjernetEvent.metadata.utførtAvSaksbehandlerIdent)
        assertEquals(2, fjernetEvent.metadata.sekvensnummer)

        assertEquals(organisasjonsnummer, tilkommenInntekt.organisasjonsnummer)
        assertEquals(1 jan 2018, tilkommenInntekt.periode.fom)
        assertEquals(31 jan 2018, tilkommenInntekt.periode.tom)
        assertEquals(setOf(1 jan 2018, 31 jan 2018), tilkommenInntekt.ekskluderteUkedager)
        assertEquals(BigDecimal("10000"), tilkommenInntekt.periodebeløp)
        assertTrue(tilkommenInntekt.fjernet)
    }
}
