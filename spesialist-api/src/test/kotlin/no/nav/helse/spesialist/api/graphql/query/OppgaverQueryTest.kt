package no.nav.helse.spesialist.api.graphql.query

import io.mockk.every
import io.mockk.verify
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.spesialist.api.AbstractGraphQLApiTest
import no.nav.helse.spesialist.api.graphql.schema.AntallArbeidsforhold
import no.nav.helse.spesialist.api.graphql.schema.Egenskap
import no.nav.helse.spesialist.api.graphql.schema.Fane
import no.nav.helse.spesialist.api.graphql.schema.Kategori
import no.nav.helse.spesialist.api.graphql.schema.Mottaker
import no.nav.helse.spesialist.api.graphql.schema.OppgaveTilBehandling
import no.nav.helse.spesialist.api.graphql.schema.Oppgaveegenskap
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
    fun `oppgaver query uten parametere returnerer oppgave`() {
        every { oppgavehåndterer.oppgaver(any(), any(), any(), any(), any(), any()) } returns listOf(oppgaveTilBehandling())

        val body = runQuery("""{ oppgaver { id } }""")
        val antallOppgaver = body["data"]["oppgaver"].size()

        verify(exactly = 1) { oppgavehåndterer.oppgaver(any(), 0, Int.MAX_VALUE, any(), any(), any()) }
        assertEquals(1, antallOppgaver)
    }

    @Test
    fun `oppgaver query med parametere returnerer oppgave`() {
        every { oppgavehåndterer.oppgaver(any(), any(), any(), any(), any(), any()) } returns listOf(oppgaveTilBehandling())

        val body = runQuery("""{ 
            oppgaver(
                startIndex: 2, 
                pageSize: 5, 
                sortering: [{nokkel: TILDELT_TIL, stigende: true}], 
                filtrerteEgenskaper: [{egenskap: DELVIS_REFUSJON, kategori: Mottaker}],
                fane: MINE_SAKER
            ) { 
                id 
            } 
        }""")
        val antallOppgaver = body["data"]["oppgaver"].size()

        verify(exactly = 1) { oppgavehåndterer.oppgaver(
            saksbehandlerFraApi = any(),
            startIndex = 2,
            pageSize = 5,
            sortering = listOf(Oppgavesortering(Sorteringsnokkel.TILDELT_TIL, true)),
            egenskaper = listOf(Oppgaveegenskap(Egenskap.DELVIS_REFUSJON, Kategori.Mottaker)),
            fane = Fane.MINE_SAKER
        ) }
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
