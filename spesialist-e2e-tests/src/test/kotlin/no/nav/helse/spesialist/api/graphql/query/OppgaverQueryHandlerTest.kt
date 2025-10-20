package no.nav.helse.spesialist.api.graphql.query

import io.mockk.every
import io.mockk.verify
import no.nav.helse.spesialist.api.AbstractGraphQLApiTest
import no.nav.helse.spesialist.api.graphql.schema.ApiAntallArbeidsforhold
import no.nav.helse.spesialist.api.graphql.schema.ApiAntallOppgaver
import no.nav.helse.spesialist.api.graphql.schema.ApiBehandledeOppgaver
import no.nav.helse.spesialist.api.graphql.schema.ApiBehandletOppgave
import no.nav.helse.spesialist.api.graphql.schema.ApiPeriodetype
import no.nav.helse.spesialist.api.graphql.schema.ApiPersonnavn
import no.nav.helse.util.juni
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.spesialist.api.graphql.schema.ApiOppgavetype as OppgavetypeForApi

class OppgaverQueryHandlerTest : AbstractGraphQLApiTest() {

    @Test
    fun `behandledeOppgaverFeed med offset 0 returnerer oppgave`() {
        every {
            apiOppgaveService.behandledeOppgaver(any(), any(), any(), any(), any())
        } returns ApiBehandledeOppgaver(oppgaver = listOf(behandletOppgave()), totaltAntallOppgaver = 1)

        val body = runQuery(
            """
            {
                behandledeOppgaverFeed(
                    offset: 0,
                    limit: 14,
                    fom: "2025-06-01"
                    tom: "2025-06-02"
                ) { oppgaver { id } }
            }
            """.trimIndent(),
        )
        val antallOppgaver = body["data"]["behandledeOppgaverFeed"].size()

        verify(exactly = 1) { apiOppgaveService.behandledeOppgaver(any(), 0, 14, 1.juni(2025), 2.juni(2025)) }
        assertEquals(1, antallOppgaver)
    }

    @Test
    fun `behandledeOppgaverFeed med offset 14 returnerer oppgave`() {
        every {
            apiOppgaveService.behandledeOppgaver(any(), any(), any(), any(), any())
        } returns ApiBehandledeOppgaver(oppgaver = listOf(behandletOppgave()), totaltAntallOppgaver = 1)

        val body = runQuery(
            """
            {
                behandledeOppgaverFeed(
                    offset: 14,
                    limit: 14,
                    fom: "2025-06-10",
                    tom: "2025-06-13"
                ) { oppgaver { id } }
            }
            """.trimIndent(),
        )
        val antallOppgaver = body["data"]["behandledeOppgaverFeed"].size()

        verify(exactly = 1) {
            apiOppgaveService.behandledeOppgaver(
                saksbehandler = any(), offset = 14, limit = 14, fom = 10.juni(2025), tom = 13.juni(2025)
            )
        }
        assertEquals(1, antallOppgaver)
    }

    @Test
    fun `antallOppgaver returnerer antall oppgaver`() {
        every {
            apiOppgaveService.antallOppgaver(any())
        } returns ApiAntallOppgaver(antallMineSaker = 2, antallMineSakerPaVent = 1)

        val body = runQuery(
            """
            {
                antallOppgaver {
                    antallMineSaker
                    antallMineSakerPaVent
                }
            }
            """.trimIndent(),
        )

        val antallMineSaker = body["data"]["antallOppgaver"]["antallMineSaker"].asInt()
        val antallMineSakerPåVent = body["data"]["antallOppgaver"]["antallMineSakerPaVent"].asInt()

        verify(exactly = 1) {
            apiOppgaveService.antallOppgaver(
                saksbehandler = any(),
            )
        }
        assertEquals(2, antallMineSaker)
        assertEquals(1, antallMineSakerPåVent)
    }

    private fun behandletOppgave() =
        ApiBehandletOppgave(
            id = UUID.randomUUID().toString(),
            aktorId = "1017011111111",
            oppgavetype = OppgavetypeForApi.SOKNAD,
            periodetype = ApiPeriodetype.FORSTEGANGSBEHANDLING,
            antallArbeidsforhold = ApiAntallArbeidsforhold.ET_ARBEIDSFORHOLD,
            ferdigstiltTidspunkt = LocalDateTime.now(),
            ferdigstiltAv = "BESLUTTER",
            beslutter = "BESLUTTER",
            saksbehandler = "SAKSBEHANDLER",
            personnavn =
                ApiPersonnavn(
                    fornavn = "Aage",
                    etternavn = "Kurt",
                    mellomnavn = null,
                ),
        )
}
