package no.nav.helse.spesialist.api.graphql.query

import io.mockk.every
import io.mockk.verify
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.spesialist.api.AbstractGraphQLApiTest
import no.nav.helse.spesialist.api.graphql.schema.AntallArbeidsforhold
import no.nav.helse.spesialist.api.graphql.schema.Mottaker
import no.nav.helse.spesialist.api.graphql.schema.OppgaveTilBehandling
import no.nav.helse.spesialist.api.graphql.schema.Oppgavesortering
import no.nav.helse.spesialist.api.graphql.schema.Periodetype
import no.nav.helse.spesialist.api.graphql.schema.Personnavn
import no.nav.helse.spesialist.api.graphql.schema.Sorteringsnokkel
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import no.nav.helse.spesialist.api.graphql.schema.Oppgavetype as OppgavetypeForApi

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class OppgaverQueryTest : AbstractGraphQLApiTest() {

    @Test
    fun `henter behandlede oppgaver`() {
        opprettSaksbehandler()
        val vedtakRef = opprettVedtaksperiode(opprettPerson(), opprettArbeidsgiver())
        val oppgaveRef = opprettOppgave(vedtakRef = vedtakRef)
        tildelOppgave(oppgaveRef, SAKSBEHANDLER.oid)
        ferdigstillOppgave(vedtakRef)

        val body = runQuery(
            """
            {
                behandledeOppgaver(
                    behandletAvOid: "${SAKSBEHANDLER.oid}", 
                    fom: "${LocalDateTime.now().minusDays(1)}"
                ) {
                    ferdigstiltAv
                }
            }
        """
        )

        assertEquals("Jan Banan", body["data"]["behandledeOppgaver"].first()["ferdigstiltAv"].asText())
    }

    @Test
    fun `oppgaver query uten parametere returnerer oppgave`() {
        opprettVedtaksperiode(opprettPerson(), opprettArbeidsgiver())
        every { oppgavehåndterer.oppgaver(any(), any(), any(), any()) } returns listOf(oppgaveTilBehandling())

        val body = runQuery("""{ oppgaver { id } }""")
        val antallOppgaver = body["data"]["oppgaver"].size()

        verify(exactly = 1) { oppgavehåndterer.oppgaver(any(), 0, Int.MAX_VALUE, any()) }
        assertEquals(1, antallOppgaver)
    }

    @Test
    fun `oppgaver query med parametere returnerer oppgave`() {
        opprettVedtaksperiode(opprettPerson(), opprettArbeidsgiver())
        every { oppgavehåndterer.oppgaver(any(), any(), any(), any()) } returns listOf(oppgaveTilBehandling())

        val body = runQuery("""{ oppgaver(startIndex: 2, pageSize: 5, sortering: [{nokkel: TILDELT_TIL, stigende: true}]) { id } }""")
        val antallOppgaver = body["data"]["oppgaver"].size()

        verify(exactly = 1) { oppgavehåndterer.oppgaver(any(), 2, 5, listOf(Oppgavesortering(Sorteringsnokkel.TILDELT_TIL, true))) }
        assertEquals(1, antallOppgaver)
    }

    private fun oppgaveTilBehandling() = OppgaveTilBehandling(
        id = UUID.randomUUID().toString(),
        opprettet = LocalDateTime.now().toString(),
        opprinneligSoknadsdato = LocalDateTime.now().toString(),
        vedtaksperiodeId = UUID.randomUUID().toString(),
        navn = Personnavn(
            fornavn = "Aage",
            etternavn = "Kurt",
            mellomnavn = null,
        ),
        aktorId = "1017011111111",
        tildeling = null,
        egenskaper = emptyList(),
        periodetype = Periodetype.FORSTEGANGSBEHANDLING,
        oppgavetype = OppgavetypeForApi.SOKNAD,
        mottaker = Mottaker.SYKMELDT,
        antallArbeidsforhold = AntallArbeidsforhold.ET_ARBEIDSFORHOLD
    )
}
