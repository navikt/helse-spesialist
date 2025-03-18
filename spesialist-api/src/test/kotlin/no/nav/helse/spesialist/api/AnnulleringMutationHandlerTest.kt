package no.nav.helse.spesialist.api

import no.nav.helse.TestRunner.runQuery
import no.nav.helse.spesialist.api.graphql.schema.ApiAnnulleringData
import no.nav.helse.spesialist.api.saksbehandler.SaksbehandlerFraApi
import no.nav.helse.spesialist.api.testfixtures.mutation.annullerMutation
import no.nav.helse.spesialist.testhjelp.lagAktørId
import no.nav.helse.spesialist.testhjelp.lagEpostadresseFraFulltNavn
import no.nav.helse.spesialist.testhjelp.lagFødselsnummer
import no.nav.helse.spesialist.testhjelp.lagOrganisasjonsnummer
import no.nav.helse.spesialist.testhjelp.lagSaksbehandlerident
import no.nav.helse.spesialist.testhjelp.lagSaksbehandlernavn
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.UUID

class AnnulleringMutationHandlerTest {
    private val saksbehandlernavn = lagSaksbehandlernavn()
    private val saksbehandlerFraApi = SaksbehandlerFraApi(
        UUID.randomUUID(),
        saksbehandlernavn,
        lagEpostadresseFraFulltNavn(saksbehandlernavn),
        ident = lagSaksbehandlerident(),
        grupper = listOf(UUID.randomUUID())
    )

    @Test
    fun `annullering ok`() {
        runQuery(
            saksbehandlerFraApi = saksbehandlerFraApi,
            given = {},
            whenever = annullerMutation(
                ApiAnnulleringData(
                    organisasjonsnummer = lagOrganisasjonsnummer(),
                    fodselsnummer = lagFødselsnummer(),
                    aktorId = lagAktørId(),
                    utbetalingId = UUID.randomUUID(),
                    arbeidsgiverFagsystemId = "EN-FAGSYSTEMID",
                    personFagsystemId = "EN-FAGSYSTEMID",
                    vedtaksperiodeId = UUID.randomUUID(),
                    kommentar = "kommentar",
                    begrunnelser = listOf("Det første", "Det andre"),
                    arsaker = listOf(
                        ApiAnnulleringData.ApiAnnulleringArsak(
                            _key = "en key",
                            arsak = "Ferie",
                        )
                    ),
                )
            ),
            then = {_, body ->
                assertTrue(body["data"]["annuller"].asBoolean())
            }
        )
    }

    @Test
    fun `annullering av tomme verdier`() {
        runQuery(
            saksbehandlerFraApi = saksbehandlerFraApi,
            whenever = annullerMutation(
                ApiAnnulleringData(
                    organisasjonsnummer = lagOrganisasjonsnummer(),
                    fodselsnummer = lagFødselsnummer(),
                    aktorId = lagAktørId(),
                    utbetalingId = UUID.randomUUID(),
                    arbeidsgiverFagsystemId = "EN-FAGSYSTEMID",
                    personFagsystemId = "EN-FAGSYSTEMID",
                    vedtaksperiodeId = UUID.randomUUID(),
                    kommentar = "kommentar",
                    begrunnelser = emptyList(),
                    arsaker = emptyList()
                )
            ),
            then = { _, body ->
                assertTrue(body["data"]["annuller"].asBoolean())
            }
        )
    }
}
