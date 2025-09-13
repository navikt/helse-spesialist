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
import no.nav.helse.spesialist.domain.SaksbehandlerOid
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.spesialist.api.graphql.schema.ApiOppgavetype as OppgavetypeForApi

class TildelteOppgaverQueryHandlerTest : AbstractGraphQLApiTest() {

    @Test
    fun `tildelteOppgaver query uten parametere returnerer oppgave`() {
        every { saksbehandlerDao.hent("N815493") } returns no.nav.helse.spesialist.domain.Saksbehandler(
            id = SaksbehandlerOid(UUID.randomUUID()),
            navn = "Ola Nordmann",
            ident = "N815493",
            epost = "ola@nordmann.no"
        )
        every {
            apiOppgaveService.tildelteOppgaver(any(), any(), any(), any(), any())
        } returns ApiOppgaverTilBehandling(oppgaver = listOf(oppgaveTilBehandling()), totaltAntallOppgaver = 1)

        val body = runQuery(
            """
            {
                tildelteOppgaverFeed(
                    offset: 0,
                    limit: 14,
                    oppslattSaksbehandler:  {
                        ident: "N815493",
                        navn: "Ola Nordmann"
                    }
                ) { oppgaver { id } }
            }
            """.trimIndent(),
        )
        val antallOppgaver = body["data"]["tildelteOppgaverFeed"].size()

        verify(exactly = 1) { apiOppgaveService.tildelteOppgaver(any(), any(), any(), 0, 14) }
        assertEquals(1, antallOppgaver)
    }

    @Test
    fun `tildelteOppgaver query med manglende saksbehandler-ident gir 404`() {
        val body = runQuery(
            """
            {
                tildelteOppgaverFeed(
                    offset: 0,
                    limit: 14,
                    oppslattSaksbehandler:  {
                        ident: null,
                        navn: "Ola Nordmann"
                    }
                ) { oppgaver { id } }
            }
            """.trimIndent(),
        )
        val error = body["errors"].first()

        verify(exactly = 0) { saksbehandlerDao.hent(any()) }
        verify(exactly = 0) { apiOppgaveService.tildelteOppgaver(any(), any(), any(), any(),  any()) }

        assertTrue(error["message"].asText().contains("Saksbehandler mangler ident"))
        assertEquals(404, error["extensions"]["code"].asInt())
    }

    @Test
    fun `tildelteOppgaver query med ukjent saksbehandler-ident gir 404`() {
        every { saksbehandlerDao.hent(any()) } returns null
        val body = runQuery(
            """
            {
                tildelteOppgaverFeed(
                    offset: 0,
                    limit: 14,
                    oppslattSaksbehandler:  {
                        ident: "E123456",
                        navn: "Ola Nordmann"
                    }
                ) { oppgaver { id } }
            }
            """.trimIndent(),
        )
        val error = body["errors"].first()

        verify(exactly = 0) { apiOppgaveService.tildelteOppgaver(any(), any(), any(), any(),  any()) }

        assertTrue(error["message"].asText().contains("Finner ikke saksbehandler"))
        assertEquals(404, error["extensions"]["code"].asInt())
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
