package no.nav.helse.spesialist.domain.andreytelser

import no.nav.helse.spesialist.domain.Periode.Companion.tilOgMed
import no.nav.helse.spesialist.domain.TotrinnsvurderingId
import no.nav.helse.spesialist.domain.andreytelser.AndreYtelserPeriode.GraderteAndreYtelserPeriode
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
        assertEquals(1, graderteAndreYtelser.versjon)
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
}
