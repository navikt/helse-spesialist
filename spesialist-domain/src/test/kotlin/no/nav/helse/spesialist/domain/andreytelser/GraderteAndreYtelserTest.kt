package no.nav.helse.spesialist.domain.andreytelser

import no.nav.helse.spesialist.domain.Periode.Companion.tilOgMed
import no.nav.helse.spesialist.domain.TotrinnsvurderingId
import no.nav.helse.spesialist.domain.andreytelser.AndreYtelserPeriode.GraderteAndreYtelserPeriode
import no.nav.helse.spesialist.domain.testfixtures.feb
import no.nav.helse.spesialist.domain.testfixtures.jan
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagIdentitetsnummer
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagSaksbehandler
import kotlin.random.Random
import kotlin.test.*

class GraderteAndreYtelserTest {
    @Test
    fun `kan opprette graderte andre ytelser`() {
        // Given:
        val identitetsnummer = lagIdentitetsnummer()
        val saksbehandlerIdent = lagSaksbehandler().ident
        val perioder =
            listOf(
                GraderteAndreYtelserPeriode(
                    periode = (1 jan 2024) tilOgMed (31 jan 2024),
                    grad = 50,
                ),
            )

        // When:
        val graderteAndreYtelser =
            GraderteAndreYtelser.ny(
                identitetsnummer = identitetsnummer,
                saksbehandlerIdent = saksbehandlerIdent,
                notatTilBeslutter = "et notat til beslutter",
                totrinnsvurderingId = TotrinnsvurderingId(Random.nextLong()),
                graderteAndreYtelserPerioder = perioder,
                graderteAndreYtelserType = GraderteAndreYtelserType.FORELDREPENGER,
            )

        // Then:
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
        assertFalse(graderteAndreYtelser.fjernet)
        assertEquals(1, graderteAndreYtelser.versjon)
    }

    @Test
    fun `kan endre graderte andre ytelser`() {
        // Given:
        val originalPerioder =
            listOf(
                GraderteAndreYtelserPeriode(
                    periode = (1 jan 2024) tilOgMed (31 jan 2024),
                    grad = 50,
                ),
            )
        val graderteAndreYtelser =
            GraderteAndreYtelser.ny(
                identitetsnummer = lagIdentitetsnummer(),
                saksbehandlerIdent = lagSaksbehandler().ident,
                notatTilBeslutter = "et notat til beslutter",
                totrinnsvurderingId = TotrinnsvurderingId(Random.nextLong()),
                graderteAndreYtelserPerioder = originalPerioder,
                graderteAndreYtelserType = GraderteAndreYtelserType.FORELDREPENGER,
            )

        val nyePerioder =
            listOf(
                GraderteAndreYtelserPeriode(
                    periode = (1 feb 2024) tilOgMed (29 feb 2024),
                    grad = 80,
                ),
            )

        // When:
        val saksbehandlerIdent = lagSaksbehandler().ident
        graderteAndreYtelser.endreTil(
            graderteAndreYtelserPerioder = nyePerioder,
            graderteAndreYtelserType = GraderteAndreYtelserType.PLEIEPENGER,
            saksbehandlerIdent = saksbehandlerIdent,
            notatTilBeslutter = "nytt notat",
            totrinnsvurderingId = TotrinnsvurderingId(Random.nextLong()),
        )

        // Then:
        assertEquals(2, graderteAndreYtelser.events.size)
        assertEquals(GraderteAndreYtelserEndretEvent::class, graderteAndreYtelser.events.last()::class)
        val endretEvent = graderteAndreYtelser.events.last() as GraderteAndreYtelserEndretEvent
        assertEquals(originalPerioder, endretEvent.endringer.graderteAndreYtelserPerioder?.fra)
        assertEquals(nyePerioder, endretEvent.endringer.graderteAndreYtelserPerioder?.til)
        assertEquals(GraderteAndreYtelserType.FORELDREPENGER, endretEvent.endringer.graderteAndreYtelserType?.fra)
        assertEquals(GraderteAndreYtelserType.PLEIEPENGER, endretEvent.endringer.graderteAndreYtelserType?.til)
        assertEquals("nytt notat", endretEvent.metadata.notatTilBeslutter)
        assertEquals(saksbehandlerIdent, endretEvent.metadata.utførtAvSaksbehandlerIdent)
        assertEquals(2, endretEvent.metadata.sekvensnummer)

        assertEquals(nyePerioder, graderteAndreYtelser.perioder)
        assertEquals(GraderteAndreYtelserType.PLEIEPENGER, graderteAndreYtelser.graderteAndreYtelserType)
        assertFalse(graderteAndreYtelser.fjernet)
        assertEquals(2, graderteAndreYtelser.versjon)
    }

    @Test
    fun `kan endre graderte andre ytelser fra en til tre perioder`() {
        val opprinneligePerioder =
            listOf(
                GraderteAndreYtelserPeriode(
                    periode = (1 jan 2024) tilOgMed (31 jan 2024),
                    grad = 50,
                ),
            )
        val nyePerioder =
            listOf(
                GraderteAndreYtelserPeriode(
                    periode = (1 jan 2024) tilOgMed (10 jan 2024),
                    grad = 20,
                ),
                GraderteAndreYtelserPeriode(
                    periode = (11 jan 2024) tilOgMed (20 jan 2024),
                    grad = 40,
                ),
                GraderteAndreYtelserPeriode(
                    periode = (21 jan 2024) tilOgMed (31 jan 2024),
                    grad = 60,
                ),
            )

        assertEndreTilOppdatererPerioder(
            opprinneligePerioder = opprinneligePerioder,
            nyePerioder = nyePerioder,
        )
    }

    @Test
    fun `kan gjenopprette graderte andre ytelser`() {
        // Given:
        val originalPerioder =
            listOf(
                GraderteAndreYtelserPeriode(
                    periode = (1 jan 2024) tilOgMed (31 jan 2024),
                    grad = 50,
                ),
            )
        val graderteAndreYtelser =
            GraderteAndreYtelser.ny(
                identitetsnummer = lagIdentitetsnummer(),
                saksbehandlerIdent = lagSaksbehandler().ident,
                notatTilBeslutter = "et notat til beslutter",
                totrinnsvurderingId = TotrinnsvurderingId(Random.nextLong()),
                graderteAndreYtelserPerioder = originalPerioder,
                graderteAndreYtelserType = GraderteAndreYtelserType.FORELDREPENGER,
            )

        graderteAndreYtelser.fjern(
            saksbehandlerIdent = lagSaksbehandler().ident,
            notatTilBeslutter = "fjern",
            totrinnsvurderingId = TotrinnsvurderingId(Random.nextLong()),
        )

        val nyePerioder =
            listOf(
                GraderteAndreYtelserPeriode(
                    periode = (1 feb 2024) tilOgMed (29 feb 2024),
                    grad = 80,
                ),
            )

        // When:
        val saksbehandlerIdent = lagSaksbehandler().ident
        graderteAndreYtelser.gjenopprett(
            graderteAndreYtelserPerioder = nyePerioder,
            graderteAndreYtelserType = GraderteAndreYtelserType.PLEIEPENGER,
            saksbehandlerIdent = saksbehandlerIdent,
            notatTilBeslutter = "gjenopprett",
            totrinnsvurderingId = TotrinnsvurderingId(Random.nextLong()),
        )

        // Then:
        assertEquals(3, graderteAndreYtelser.events.size)
        assertEquals(GraderteAndreYtelserGjenopprettetEvent::class, graderteAndreYtelser.events.last()::class)
        val gjenopprettetEvent = graderteAndreYtelser.events.last() as GraderteAndreYtelserGjenopprettetEvent
        assertEquals(originalPerioder, gjenopprettetEvent.endringer.graderteAndreYtelserPerioder?.fra)
        assertEquals(nyePerioder, gjenopprettetEvent.endringer.graderteAndreYtelserPerioder?.til)
        assertEquals(GraderteAndreYtelserType.FORELDREPENGER, gjenopprettetEvent.endringer.graderteAndreYtelserType?.fra)
        assertEquals(GraderteAndreYtelserType.PLEIEPENGER, gjenopprettetEvent.endringer.graderteAndreYtelserType?.til)
        assertEquals("gjenopprett", gjenopprettetEvent.metadata.notatTilBeslutter)
        assertEquals(saksbehandlerIdent, gjenopprettetEvent.metadata.utførtAvSaksbehandlerIdent)
        assertEquals(3, gjenopprettetEvent.metadata.sekvensnummer)

        assertEquals(nyePerioder, graderteAndreYtelser.perioder)
        assertEquals(GraderteAndreYtelserType.PLEIEPENGER, graderteAndreYtelser.graderteAndreYtelserType)
        assertFalse(graderteAndreYtelser.fjernet)
        assertEquals(3, graderteAndreYtelser.versjon)
    }

    @Test
    fun `kan fjerne graderte andre ytelser`() {
        // Given:
        val graderteAndreYtelser =
            GraderteAndreYtelser.ny(
                identitetsnummer = lagIdentitetsnummer(),
                saksbehandlerIdent = lagSaksbehandler().ident,
                notatTilBeslutter = "et notat til beslutter",
                totrinnsvurderingId = TotrinnsvurderingId(Random.nextLong()),
                graderteAndreYtelserPerioder =
                    listOf(
                        GraderteAndreYtelserPeriode(
                            periode = (1 jan 2024) tilOgMed (31 jan 2024),
                            grad = 50,
                        ),
                    ),
                graderteAndreYtelserType = GraderteAndreYtelserType.FORELDREPENGER,
            )

        // When:
        val saksbehandlerIdent = lagSaksbehandler().ident
        graderteAndreYtelser.fjern(
            saksbehandlerIdent = saksbehandlerIdent,
            notatTilBeslutter = "remove",
            totrinnsvurderingId = TotrinnsvurderingId(Random.nextLong()),
        )

        // Then:
        assertEquals(2, graderteAndreYtelser.events.size)
        assertEquals(GraderteAndreYtelserFjernetEvent::class, graderteAndreYtelser.events.last()::class)
        val fjernetEvent = graderteAndreYtelser.events.last() as GraderteAndreYtelserFjernetEvent

        assertEquals("remove", fjernetEvent.metadata.notatTilBeslutter)
        assertEquals(saksbehandlerIdent, fjernetEvent.metadata.utførtAvSaksbehandlerIdent)
        assertEquals(2, fjernetEvent.metadata.sekvensnummer)
        assertTrue(graderteAndreYtelser.fjernet)
        assertEquals(2, graderteAndreYtelser.versjon)
    }

    @Test
    fun `ny genererer unik id for hver instans`() {
        // Given:
        val identitetsnummer = lagIdentitetsnummer()
        val perioder =
            listOf(
                GraderteAndreYtelserPeriode(
                    periode = (1 jan 2024) tilOgMed (31 jan 2024),
                    grad = 50,
                ),
            )

        // When:
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

        // Then:
        assertNotNull(første.id)
        assertNotNull(andre.id)
        assertEquals(false, første.id == andre.id)
    }

    @Test
    fun `fraLagring rekonstruerer riktig tilstand fra historikken`() {
        // Given:
        val original =
            GraderteAndreYtelser.ny(
                identitetsnummer = lagIdentitetsnummer(),
                saksbehandlerIdent = lagSaksbehandler().ident,
                notatTilBeslutter = "notat",
                totrinnsvurderingId = TotrinnsvurderingId(Random.nextLong()),
                graderteAndreYtelserPerioder =
                    listOf(
                        GraderteAndreYtelserPeriode(
                            periode = (1 jan 2024) tilOgMed (31 jan 2024),
                            grad = 60,
                        ),
                    ),
                graderteAndreYtelserType = GraderteAndreYtelserType.SVANGERSKAPSPENGER,
            )

        val oppdatertePerioder =
            listOf(
                GraderteAndreYtelserPeriode(
                    periode = (1 feb 2024) tilOgMed (29 feb 2024),
                    grad = 80,
                ),
            )
        original.endreTil(
            graderteAndreYtelserPerioder = oppdatertePerioder,
            graderteAndreYtelserType = GraderteAndreYtelserType.PLEIEPENGER,
            saksbehandlerIdent = lagSaksbehandler().ident,
            notatTilBeslutter = "endre",
            totrinnsvurderingId = TotrinnsvurderingId(Random.nextLong()),
        )
        original.fjern(
            saksbehandlerIdent = lagSaksbehandler().ident,
            notatTilBeslutter = "fjern",
            totrinnsvurderingId = TotrinnsvurderingId(Random.nextLong()),
        )
        original.gjenopprett(
            graderteAndreYtelserPerioder = oppdatertePerioder,
            graderteAndreYtelserType = GraderteAndreYtelserType.PLEIEPENGER,
            saksbehandlerIdent = lagSaksbehandler().ident,
            notatTilBeslutter = "gjenopprett",
            totrinnsvurderingId = TotrinnsvurderingId(Random.nextLong()),
        )

        // When:
        val rekonstruert = GraderteAndreYtelser.fraLagring(original.events)

        // Then:
        assertEquals(original.id, rekonstruert.id)
        assertEquals(original.identitetsnummer, rekonstruert.identitetsnummer)
        assertEquals(oppdatertePerioder, rekonstruert.perioder)
        assertEquals(GraderteAndreYtelserType.PLEIEPENGER, rekonstruert.graderteAndreYtelserType)
        assertFalse(rekonstruert.fjernet)
        assertEquals(4, rekonstruert.versjon)
        assertEquals(4, rekonstruert.events.size)
    }

    private fun assertEndreTilOppdatererPerioder(
        opprinneligePerioder: List<GraderteAndreYtelserPeriode>,
        nyePerioder: List<GraderteAndreYtelserPeriode>,
    ) {
        val graderteAndreYtelser =
            GraderteAndreYtelser.ny(
                identitetsnummer = lagIdentitetsnummer(),
                saksbehandlerIdent = lagSaksbehandler().ident,
                notatTilBeslutter = "et notat til beslutter",
                totrinnsvurderingId = TotrinnsvurderingId(Random.nextLong()),
                graderteAndreYtelserPerioder = opprinneligePerioder,
                graderteAndreYtelserType = GraderteAndreYtelserType.FORELDREPENGER,
            )

        graderteAndreYtelser.endreTil(
            graderteAndreYtelserPerioder = nyePerioder,
            graderteAndreYtelserType = GraderteAndreYtelserType.FORELDREPENGER,
            saksbehandlerIdent = lagSaksbehandler().ident,
            notatTilBeslutter = "oppdaterer perioder",
            totrinnsvurderingId = TotrinnsvurderingId(Random.nextLong()),
        )

        assertEquals(2, graderteAndreYtelser.events.size)
        assertEquals(GraderteAndreYtelserEndretEvent::class, graderteAndreYtelser.events.last()::class)
        val endretEvent = graderteAndreYtelser.events.last() as GraderteAndreYtelserEndretEvent
        assertEquals(opprinneligePerioder, endretEvent.endringer.graderteAndreYtelserPerioder?.fra)
        assertEquals(nyePerioder, endretEvent.endringer.graderteAndreYtelserPerioder?.til)
        assertEquals(null, endretEvent.endringer.graderteAndreYtelserType)
        assertEquals(nyePerioder, graderteAndreYtelser.perioder)
        assertEquals(GraderteAndreYtelserType.FORELDREPENGER, graderteAndreYtelser.graderteAndreYtelserType)
        assertEquals(2, graderteAndreYtelser.versjon)
    }
}
