package no.nav.helse.spesialist.domain.andreytelser

import no.nav.helse.spesialist.domain.TotrinnsvurderingId
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

        // When:
        val graderteAndreYtelser =
            GraderteAndreYtelser.ny(
                identitetsnummer = identitetsnummer,
                saksbehandlerIdent = saksbehandlerIdent,
                notatTilBeslutter = "et notat til beslutter",
                totrinnsvurderingId = TotrinnsvurderingId(Random.nextLong()),
                graderteAndreYtelserType = GraderteAndreYtelserType.FORELDREPENGER,
            )

        // Then:
        assertEquals(1, graderteAndreYtelser.events.size)
        assertEquals(GraderteAndreYtelserOpprettetEvent::class, graderteAndreYtelser.events.last()::class)
        val opprettetEvent = graderteAndreYtelser.events.last() as GraderteAndreYtelserOpprettetEvent

        assertEquals(identitetsnummer.value, opprettetEvent.fødselsnummer)
        assertEquals(GraderteAndreYtelserType.FORELDREPENGER, opprettetEvent.graderteAndreYtelserType)
        assertEquals("et notat til beslutter", opprettetEvent.metadata.notatTilBeslutter)
        assertEquals(saksbehandlerIdent, opprettetEvent.metadata.utførtAvSaksbehandlerIdent)
        assertEquals(1, opprettetEvent.metadata.sekvensnummer)
        assertNotNull(opprettetEvent.metadata.graderteAndreYtelserId)

        assertEquals(identitetsnummer, graderteAndreYtelser.identitetsnummer)
        assertEquals(GraderteAndreYtelserType.FORELDREPENGER, graderteAndreYtelser.graderteAndreYtelserType)
        assertEquals(1, graderteAndreYtelser.versjon)
    }

    @Test
    fun `ny genererer unik id for hver instans`() {
        // Given:
        val identitetsnummer = lagIdentitetsnummer()

        // When:
        val første =
            GraderteAndreYtelser.ny(
                identitetsnummer = identitetsnummer,
                saksbehandlerIdent = lagSaksbehandler().ident,
                notatTilBeslutter = "notat",
                totrinnsvurderingId = TotrinnsvurderingId(Random.nextLong()),
                graderteAndreYtelserType = GraderteAndreYtelserType.FORELDREPENGER,
            )
        val andre =
            GraderteAndreYtelser.ny(
                identitetsnummer = identitetsnummer,
                saksbehandlerIdent = lagSaksbehandler().ident,
                notatTilBeslutter = "notat",
                totrinnsvurderingId = TotrinnsvurderingId(Random.nextLong()),
                graderteAndreYtelserType = GraderteAndreYtelserType.FORELDREPENGER,
            )

        // Then:
        assertNotNull(første.id)
        assertNotNull(andre.id)
        assertEquals(false, første.id == andre.id)
    }
}
