package no.nav.helse.spesialist.api.graphql.query

import io.mockk.every
import io.mockk.verify
import no.nav.helse.spesialist.api.AbstractGraphQLApiTest
import no.nav.helse.spesialist.api.graphql.schema.AntallArbeidsforhold
import no.nav.helse.spesialist.api.graphql.schema.AntallOppgaver
import no.nav.helse.spesialist.api.graphql.schema.BehandledeOppgaver
import no.nav.helse.spesialist.api.graphql.schema.BehandletOppgave
import no.nav.helse.spesialist.api.graphql.schema.Egenskap
import no.nav.helse.spesialist.api.graphql.schema.Filtrering
import no.nav.helse.spesialist.api.graphql.schema.Kategori
import no.nav.helse.spesialist.api.graphql.schema.Mottaker
import no.nav.helse.spesialist.api.graphql.schema.OppgaveTilBehandling
import no.nav.helse.spesialist.api.graphql.schema.Oppgaveegenskap
import no.nav.helse.spesialist.api.graphql.schema.OppgaverTilBehandling
import no.nav.helse.spesialist.api.graphql.schema.Oppgavesortering
import no.nav.helse.spesialist.api.graphql.schema.Periodetype
import no.nav.helse.spesialist.api.graphql.schema.Personnavn
import no.nav.helse.spesialist.api.graphql.schema.Sorteringsnokkel
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.spesialist.api.graphql.schema.Oppgavetype as OppgavetypeForApi

internal class OppgaverQueryTest : AbstractGraphQLApiTest() {
    override val useGraphQLServerWithSeparateMocks = true

    @Test
    fun `oppgaver query uten parametere returnerer oppgave`() {
        every {
            oppgavehåndterer.oppgaver(any(), any(), any(), any(), any())
        } returns OppgaverTilBehandling(oppgaver = listOf(oppgaveTilBehandling()), totaltAntallOppgaver = 1)

        val body =
            runQuery(
                """{
                oppgaveFeed(
                    offset: 0,
                    limit: 14,
                    sortering: [],
                    filtrering: {
                        egenskaper: []
                        egneSaker: false
                        egneSakerPaVent: false
                        ingenUkategoriserteEgenskaper: false
                    }
                ) { oppgaver { id } }
            }""",
            )
        val antallOppgaver = body["data"]["oppgaveFeed"].size()

        verify(exactly = 1) { oppgavehåndterer.oppgaver(any(), 0, 14, any(), any()) }
        assertEquals(1, antallOppgaver)
    }

    @Test
    fun `oppgaver query med parametere returnerer oppgave`() {
        every {
            oppgavehåndterer.oppgaver(any(), any(), any(), any(), any())
        } returns OppgaverTilBehandling(oppgaver = listOf(oppgaveTilBehandling()), totaltAntallOppgaver = 1)

        val body =
            runQuery(
                """{ 
                oppgaveFeed(
                    offset: 14, 
                    limit: 14,
                    sortering: [{nokkel: TILDELT_TIL, stigende: true}],
                    filtrering: {
                        egenskaper: [{egenskap: DELVIS_REFUSJON, kategori: Mottaker}]
                        egneSaker: true
                        egneSakerPaVent: false
                        ingenUkategoriserteEgenskaper: false
                    }
                )  { oppgaver { id } }
        }""",
            )
        val antallOppgaver = body["data"]["oppgaveFeed"].size()

        verify(exactly = 1) {
            oppgavehåndterer.oppgaver(
                saksbehandlerFraApi = any(),
                offset = 14,
                limit = 14,
                sortering = listOf(Oppgavesortering(Sorteringsnokkel.TILDELT_TIL, true)),
                filtrering =
                    Filtrering(
                        egenskaper = listOf(Oppgaveegenskap(Egenskap.DELVIS_REFUSJON, Kategori.Mottaker)),
                        egneSaker = true,
                    ),
            )
        }
        assertEquals(1, antallOppgaver)
    }

    @Test
    fun `oppgaver query sortert på tidsfrist`() {
        every {
            oppgavehåndterer.oppgaver(any(), any(), any(), any(), any())
        } returns OppgaverTilBehandling(oppgaver = listOf(oppgaveTilBehandling()), totaltAntallOppgaver = 1)

        val body =
            runQuery(
                """{ 
                oppgaveFeed(
                    offset: 14, 
                    limit: 14,
                    sortering: [{nokkel: TIDSFRIST, stigende: true}],
                    filtrering: {
                        egenskaper: [{egenskap: DELVIS_REFUSJON, kategori: Mottaker}]
                        egneSaker: true
                        egneSakerPaVent: false
                        ingenUkategoriserteEgenskaper: false
                    }
                )  { oppgaver { id } }
        }""",
            )
        val antallOppgaver = body["data"]["oppgaveFeed"].size()

        verify(exactly = 1) {
            oppgavehåndterer.oppgaver(
                saksbehandlerFraApi = any(),
                offset = 14,
                limit = 14,
                sortering = listOf(Oppgavesortering(Sorteringsnokkel.TIDSFRIST, true)),
                filtrering =
                    Filtrering(
                        egenskaper = listOf(Oppgaveegenskap(Egenskap.DELVIS_REFUSJON, Kategori.Mottaker)),
                        egneSaker = true,
                    ),
            )
        }
        assertEquals(1, antallOppgaver)
    }

    @Test
    fun `behandledeOppgaverFeed uten parametere returnerer oppgave`() {
        every {
            oppgavehåndterer.behandledeOppgaver(any(), any(), any())
        } returns BehandledeOppgaver(oppgaver = listOf(behandletOppgave()), totaltAntallOppgaver = 1)

        val body =
            runQuery(
                """{
                behandledeOppgaverFeed(
                    offset: 0,
                    limit: 14,
                ) { oppgaver { id } }
            }""",
            )
        val antallOppgaver = body["data"]["behandledeOppgaverFeed"].size()

        verify(exactly = 1) { oppgavehåndterer.behandledeOppgaver(any(), 0, 14) }
        assertEquals(1, antallOppgaver)
    }

    @Test
    fun `behandledeOppgaverFeed med parametere returnerer oppgave`() {
        every {
            oppgavehåndterer.behandledeOppgaver(any(), any(), any())
        } returns BehandledeOppgaver(oppgaver = listOf(behandletOppgave()), totaltAntallOppgaver = 1)

        val body =
            runQuery(
                """{ 
                behandledeOppgaverFeed(
                    offset: 14, 
                    limit: 14,
                )  { oppgaver { id } }
        }""",
            )
        val antallOppgaver = body["data"]["behandledeOppgaverFeed"].size()

        verify(exactly = 1) {
            oppgavehåndterer.behandledeOppgaver(
                saksbehandlerFraApi = any(),
                offset = 14,
                limit = 14,
            )
        }
        assertEquals(1, antallOppgaver)
    }

    @Test
    fun `antallOppgaver returnerer antall oppgaver`() {
        every { oppgavehåndterer.antallOppgaver(any()) } returns AntallOppgaver(antallMineSaker = 2, antallMineSakerPaVent = 1)

        val body =
            runQuery(
                """{
            antallOppgaver {
                antallMineSaker
                antallMineSakerPaVent
            }
        }""",
            )

        val antallMineSaker = body["data"]["antallOppgaver"]["antallMineSaker"].asInt()
        val antallMineSakerPåVent = body["data"]["antallOppgaver"]["antallMineSakerPaVent"].asInt()

        verify(exactly = 1) {
            oppgavehåndterer.antallOppgaver(
                saksbehandlerFraApi = any(),
            )
        }
        assertEquals(2, antallMineSaker)
        assertEquals(1, antallMineSakerPåVent)
    }

    private fun oppgaveTilBehandling() =
        OppgaveTilBehandling(
            id = UUID.randomUUID().toString(),
            opprettet = LocalDateTime.now(),
            opprinneligSoknadsdato = LocalDateTime.now(),
            tidsfrist = LocalDate.now(),
            vedtaksperiodeId = UUID.randomUUID(),
            navn =
                Personnavn(
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
            antallArbeidsforhold = AntallArbeidsforhold.ET_ARBEIDSFORHOLD,
        )

    private fun behandletOppgave() =
        BehandletOppgave(
            id = UUID.randomUUID().toString(),
            aktorId = "1017011111111",
            oppgavetype = OppgavetypeForApi.SOKNAD,
            periodetype = Periodetype.FORSTEGANGSBEHANDLING,
            antallArbeidsforhold = AntallArbeidsforhold.ET_ARBEIDSFORHOLD,
            ferdigstiltTidspunkt = LocalDateTime.now(),
            ferdigstiltAv = "SAKSBEHANDLER",
            personnavn =
                Personnavn(
                    fornavn = "Aage",
                    etternavn = "Kurt",
                    mellomnavn = null,
                ),
        )
}
