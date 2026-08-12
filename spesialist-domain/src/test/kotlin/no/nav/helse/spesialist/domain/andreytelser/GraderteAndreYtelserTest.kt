package no.nav.helse.spesialist.domain.andreytelser

import no.nav.helse.spesialist.domain.Periode.Companion.tilOgMed
import no.nav.helse.spesialist.domain.TotrinnsvurderingId
import no.nav.helse.spesialist.domain.andreytelser.AndreYtelserPeriode.GraderteAndreYtelserPeriode
import no.nav.helse.spesialist.domain.testfixtures.jan
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagIdentitetsnummer
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagSaksbehandler
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class GraderteAndreYtelserTest {
    @Test
    fun `kan opprette graderte andre ytelser`() {
        // given
        val identitetsnummer = lagIdentitetsnummer()
        val saksbehandlerIdent = lagSaksbehandler().ident
        val perioder =
            listOf(
                GraderteAndreYtelserPeriode(
                    periode = (1 jan 2024) tilOgMed (31 jan 2024),
                    grad = 50,
                ),
            )

        // when
        val graderteAndreYtelser =
            GraderteAndreYtelser.ny(
                identitetsnummer = identitetsnummer,
                saksbehandlerIdent = saksbehandlerIdent,
                notatTilBeslutter = "et notat til beslutter",
                totrinnsvurderingId = TotrinnsvurderingId(Random.nextLong()),
                graderteAndreYtelserPerioder = perioder,
                graderteAndreYtelserType = GraderteAndreYtelserType.FORELDREPENGER,
            )

        // then
        assertEquals(1, graderteAndreYtelser.events.size)
        assertEquals(GraderteAndreYtelserOpprettetEvent::class, graderteAndreYtelser.events.last()::class)
        val opprettetEvent = graderteAndreYtelser.events.last() as GraderteAndreYtelserOpprettetEvent

        assertEquals(identitetsnummer.value, opprettetEvent.fødselsnummer)
        assertEquals(perioder, opprettetEvent.graderteAndreYtelserPerioder)
        assertEquals(GraderteAndreYtelserType.FORELDREPENGER, opprettetEvent.graderteAndreYtelserType)

        assertEquals("et notat til beslutter", opprettetEvent.metadata.notatTilBeslutter)
        assertEquals(saksbehandlerIdent, opprettetEvent.metadata.utførtAvSaksbehandlerIdent)
        assertEquals(1, opprettetEvent.metadata.sekvensnummer)
        assertNotNull(opprettetEvent.metadata.graderteAndreYtelserId)

        assertEquals(identitetsnummer, graderteAndreYtelser.identitetsnummer)
        assertEquals(perioder, graderteAndreYtelser.perioder)
        assertEquals(GraderteAndreYtelserType.FORELDREPENGER, graderteAndreYtelser.graderteAndreYtelserType)
    }

    @Test
    fun `ny() genererer unik id for hver instans`() {
        // given
        val identitetsnummer = lagIdentitetsnummer()
        val perioder =
            listOf(
                GraderteAndreYtelserPeriode(
                    periode = (1 jan 2024) tilOgMed (31 jan 2024),
                    grad = 50,
                ),
            )

        // when
        val første =
            GraderteAndreYtelser.ny(
                identitetsnummer = identitetsnummer,
                saksbehandlerIdent = lagSaksbehandler().ident,
                notatTilBeslutter = "notat",
                totrinnsvurderingId = TotrinnsvurderingId(Random.nextLong()),
                graderteAndreYtelserPerioder = perioder,
                graderteAndreYtelserType = GraderteAndreYtelserType.FORELDREPENGER,
            )
        val andre =
            GraderteAndreYtelser.ny(
                identitetsnummer = identitetsnummer,
                saksbehandlerIdent = lagSaksbehandler().ident,
                notatTilBeslutter = "notat",
                totrinnsvurderingId = TotrinnsvurderingId(Random.nextLong()),
                graderteAndreYtelserPerioder = perioder,
                graderteAndreYtelserType = GraderteAndreYtelserType.FORELDREPENGER,
            )

        // then
        assertNotNull(første.id)
        assertNotNull(andre.id)
        assertEquals(false, første.id == andre.id)
    }

    @Test
    fun `kan opprette med flere perioder`() {
        // given
        val perioder =
            listOf(
                GraderteAndreYtelserPeriode(
                    periode = (1 jan 2024) tilOgMed (5 jan 2024),
                    grad = 50,
                ),
                GraderteAndreYtelserPeriode(
                    periode = (6 jan 2024) tilOgMed (28 jan 2024),
                    grad = 80,
                ),
            )

        // when
        val graderteAndreYtelser =
            GraderteAndreYtelser.ny(
                identitetsnummer = lagIdentitetsnummer(),
                saksbehandlerIdent = lagSaksbehandler().ident,
                notatTilBeslutter = "notat",
                totrinnsvurderingId = TotrinnsvurderingId(Random.nextLong()),
                graderteAndreYtelserPerioder = perioder,
                graderteAndreYtelserType = GraderteAndreYtelserType.PLEIEPENGER,
            )

        // then
        assertEquals(2, graderteAndreYtelser.perioder.size)
        assertEquals(50, graderteAndreYtelser.perioder[0].grad)
        assertEquals(80, graderteAndreYtelser.perioder[1].grad)
        assertEquals(GraderteAndreYtelserType.PLEIEPENGER, graderteAndreYtelser.graderteAndreYtelserType)
    }

    @Test
    fun `fraLagring rekonstruerer riktig tilstand fra events`() {
        // given
        val identitetsnummer = lagIdentitetsnummer()
        val saksbehandlerIdent = lagSaksbehandler().ident
        val perioder =
            listOf(
                GraderteAndreYtelserPeriode(
                    periode = (1 jan 2024) tilOgMed (31 jan 2024),
                    grad = 60,
                ),
            )
        val original =
            GraderteAndreYtelser.ny(
                identitetsnummer = identitetsnummer,
                saksbehandlerIdent = saksbehandlerIdent,
                notatTilBeslutter = "notat",
                totrinnsvurderingId = TotrinnsvurderingId(Random.nextLong()),
                graderteAndreYtelserPerioder = perioder,
                graderteAndreYtelserType = GraderteAndreYtelserType.SVANGERSKAPSPENGER,
            )

        // when
        val rekonstruert = GraderteAndreYtelser.fraLagring(original.events)

        // then
        assertEquals(original.id, rekonstruert.id)
        assertEquals(original.identitetsnummer, rekonstruert.identitetsnummer)
        assertEquals(original.perioder, rekonstruert.perioder)
        assertEquals(original.graderteAndreYtelserType, rekonstruert.graderteAndreYtelserType)
    }
}
