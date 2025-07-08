package no.nav.helse.spesialist.api.graphql.query

import io.mockk.every
import io.mockk.verify
import no.nav.helse.spesialist.api.AbstractGraphQLApiTest
import no.nav.helse.spesialist.api.graphql.schema.ApiAntallArbeidsforhold
import no.nav.helse.spesialist.api.graphql.schema.ApiMottaker
import no.nav.helse.spesialist.api.graphql.schema.ApiOppgaveTilBehandling
import no.nav.helse.spesialist.api.graphql.schema.ApiOppgaverTilBehandling
import no.nav.helse.spesialist.api.graphql.schema.ApiPeriodetype
import no.nav.helse.spesialist.api.graphql.schema.ApiPersonnavn
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.spesialist.api.graphql.schema.ApiOppgavetype as OppgavetypeForApi

class TildelteOppgaverQueryHandlerTest : AbstractGraphQLApiTest() {

    @Test
    fun `tildelteOppgaver query uten parametere returnerer oppgave`() {
        every {
            apiOppgaveService.tildelteOppgaver(any(), any(), any(), any())
        } returns ApiOppgaverTilBehandling(oppgaver = listOf(oppgaveTilBehandling()), totaltAntallOppgaver = 1)

        val body = runQuery(
            """
            {
                tildelteOppgaverFeed(
                    offset: 0,
                    limit: 14,
                    oppslattSaksbehandler:  {
                        oid: "fe72c646-42b7-4d1d-b0f5-48bdd6c499bf",
                        navn: "Ola Nordmann",
                        ident: "N815493",
                    }
                ) { oppgaver { id } }
            }
            """.trimIndent(),
        )
        val antallOppgaver = body["data"]["tildelteOppgaverFeed"].size()

        verify(exactly = 1) { apiOppgaveService.tildelteOppgaver(any(), any(), 0, 14) }
        assertEquals(1, antallOppgaver)
    }

    private fun oppgaveTilBehandling() =
        ApiOppgaveTilBehandling(
            id = UUID.randomUUID().toString(),
            opprettet = LocalDateTime.now(),
            opprinneligSoknadsdato = LocalDateTime.now(),
            tidsfrist = LocalDate.now(),
            vedtaksperiodeId = UUID.randomUUID(),
            navn =
                ApiPersonnavn(
                    fornavn = "Aage",
                    etternavn = "Kurt",
                    mellomnavn = null,
                ),
            aktorId = "1017011111111",
            tildeling = null,
            egenskaper = emptyList(),
            periodetype = ApiPeriodetype.FORSTEGANGSBEHANDLING,
            oppgavetype = OppgavetypeForApi.SOKNAD,
            mottaker = ApiMottaker.SYKMELDT,
            antallArbeidsforhold = ApiAntallArbeidsforhold.ET_ARBEIDSFORHOLD,
            paVentInfo = null,
        )
}
