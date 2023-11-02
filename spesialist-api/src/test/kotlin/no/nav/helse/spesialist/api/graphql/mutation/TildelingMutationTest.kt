package no.nav.helse.spesialist.api.graphql.mutation

import io.mockk.clearMocks
import io.mockk.every
import io.mockk.verify
import java.util.UUID
import no.nav.helse.spesialist.api.AbstractGraphQLApiTest
import no.nav.helse.spesialist.api.feilhåndtering.OppgaveIkkeTildelt
import no.nav.helse.spesialist.api.feilhåndtering.OppgaveTildeltNoenAndre
import no.nav.helse.spesialist.api.saksbehandler.handlinger.AvmeldOppgave
import no.nav.helse.spesialist.api.saksbehandler.handlinger.FjernOppgaveFraPåVent
import no.nav.helse.spesialist.api.saksbehandler.handlinger.LeggOppgavePåVent
import no.nav.helse.spesialist.api.saksbehandler.handlinger.TildelOppgave
import no.nav.helse.spesialist.api.tildeling.TildelingApiDto
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class TildelingMutationTest : AbstractGraphQLApiTest() {

    @BeforeEach
    fun beforeEach() {
        clearMocks(oppgavehåndterer, saksbehandlerhåndterer)
    }

    @Test
    fun `oppretter tildeling`() {
        val oppgaveId = 1L

        val body = runQuery(
            """
                mutation OpprettTildeling {
                    opprettTildeling(
                        oppgaveId: "$oppgaveId",
                    ) {
                        navn, oid, epost, paaVent
                    }
                }
            """
        )

        verify(exactly = 1) { saksbehandlerhåndterer.håndter(TildelOppgave(oppgaveId), any()) }

        assertEquals(SAKSBEHANDLER.oid, UUID.fromString(body["data"]["opprettTildeling"]["oid"].asText()))
    }

    @Test
    fun `kan ikke tildele allerede tildelt oppgave`() {
        val oppgaveId = 1L

        every { saksbehandlerhåndterer.håndter(any<TildelOppgave>(), any()) } throws OppgaveTildeltNoenAndre(TildelingApiDto("navn", "epost", UUID.randomUUID(), false))

        val body = runQuery(
            """
                mutation OpprettTildeling {
                    opprettTildeling(
                        oppgaveId: "$oppgaveId",
                    ) {
                        navn, oid, epost, paaVent
                    }
                }
            """
        )

        assertEquals(409, body["errors"].first()["extensions"]["code"]["value"].asInt())
    }

    @Test
    fun `kan fjerne tildeling`() {
        opprettSaksbehandler()
        opprettVedtaksperiode(opprettPerson(), opprettArbeidsgiver())
        val oppgaveId = finnOppgaveIdFor(PERIODE.id)
        tildelOppgave(oppgaveId, SAKSBEHANDLER.oid)

        val body = runQuery(
            """
                mutation FjernTildeling {
                    fjernTildeling(
                        oppgaveId: "$oppgaveId"
                    )
                }
            """
        )

        assertTrue(body["data"]["fjernTildeling"].booleanValue())
    }

    @Test
    fun `returnerer false hvis oppgaven ikke er tildelt`() {
        val oppgaveId = 1L
        every { saksbehandlerhåndterer.håndter(any<AvmeldOppgave>(), any()) } throws OppgaveIkkeTildelt(oppgaveId)
        val body = runQuery(
            """
                mutation FjernTildeling {
                    fjernTildeling(
                        oppgaveId: "$oppgaveId"
                    )
                }
            """
        )

        assertFalse(body["data"]["fjernTildeling"].booleanValue())
    }

    @Test
    fun `returnerer false hvis oppgaven ikke finnes`() {
        every { saksbehandlerhåndterer.håndter(any<AvmeldOppgave>(), any()) } throws IllegalStateException()

        val body = runQuery(
            """
                mutation FjernTildeling {
                    fjernTildeling(
                        oppgaveId: "999"
                    )
                }
            """
        )

        assertFalse(body["data"]["fjernTildeling"].booleanValue())
    }

    @Test
    fun `legg på vent`() {

        val body = runQuery(
            """
                mutation LeggPaaVent {
                    leggPaaVent(
                        oppgaveId: "1",
                        notatTekst: "Dett er et notat",
                        notatType: PaaVent
                    ) {
                        navn, oid, epost, paaVent
                    }
                }
            """
        )

        assertTrue(body["data"]["leggPaaVent"]["paaVent"].asBoolean())
    }

    @Test
    fun `kan ikke legge på vent hvis oppgaven ikke er tildelt`() {
        opprettSaksbehandler()
        opprettVedtaksperiode(opprettPerson(), opprettArbeidsgiver())
        val oppgaveId = finnOppgaveIdFor(PERIODE.id)

        every { saksbehandlerhåndterer.håndter(any<LeggOppgavePåVent>(), any()) } throws OppgaveIkkeTildelt(oppgaveId)

        val body = runQuery(
            """
                mutation LeggPaaVent {
                    leggPaaVent(
                        oppgaveId: "$oppgaveId",
                        notatTekst: "Dett er et notat",
                        notatType: PaaVent
                    ) {
                        navn, oid, epost, paaVent
                    }
                }
            """
        )

        assertEquals(424, body["errors"].first()["extensions"]["code"]["value"].asInt())
    }

    @Test
    fun `kan ikke legge på vent hvis oppgaven er tildelt noen andre`() {
        val oppgaveId = 1L

        every { saksbehandlerhåndterer.håndter(any<LeggOppgavePåVent>(), any()) } throws OppgaveTildeltNoenAndre(TildelingApiDto("navn", "epost", UUID.randomUUID(), false))

        val body = runQuery(
            """
                mutation LeggPaaVent {
                    leggPaaVent(
                        oppgaveId: "$oppgaveId",
                        notatTekst: "Dett er et notat",
                        notatType: PaaVent
                    ) {
                        navn, oid, epost, paaVent
                    }
                }
            """
        )

        assertEquals(409, body["errors"].first()["extensions"]["code"]["value"].asInt())
    }

    @Test
    fun `fjern på vent`() {
        val oppgaveId = 1L

        val body = runQuery(
            """
                mutation FjernPaaVent {
                    fjernPaaVent(
                        oppgaveId: "$oppgaveId"
                    ) {
                        navn, oid, epost, paaVent
                    }
                }
            """
        )

        assertFalse(body["data"]["fjernPaaVent"]["paaVent"].booleanValue())
    }

    @Test
    fun `kan ikke fjerne oppgave fra på vent hvis den ikke er tildelt`() {
        val oppgaveId = 1L

        every { saksbehandlerhåndterer.håndter(any<FjernOppgaveFraPåVent>(), any()) } throws OppgaveIkkeTildelt(oppgaveId)

        val body = runQuery(
            """
                mutation FjernPaaVent {
                    fjernPaaVent(
                        oppgaveId: "$oppgaveId"
                    ) {
                        navn, oid, epost, paaVent
                    }
                }
            """
        )

        assertEquals(424, body["errors"].first()["extensions"]["code"]["value"].asInt())
    }

    @Test
    fun `kan ikke fjerne oppgave fra på vent hvis oppgaven er tildelt noen andre`() {
        val oppgaveId = 1L

        every { saksbehandlerhåndterer.håndter(any<FjernOppgaveFraPåVent>(), any()) } throws OppgaveTildeltNoenAndre(TildelingApiDto("navn", "epost", UUID.randomUUID(), false))

        val body = runQuery(
            """
                mutation FjernPaaVent {
                    fjernPaaVent(
                        oppgaveId: "$oppgaveId"
                    ) {
                        navn, oid, epost, paaVent
                    }
                }
            """
        )

        assertEquals(409, body["errors"].first()["extensions"]["code"]["value"].asInt())
    }
}
