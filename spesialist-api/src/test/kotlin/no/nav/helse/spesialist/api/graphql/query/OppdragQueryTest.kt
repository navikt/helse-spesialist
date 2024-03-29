package no.nav.helse.spesialist.api.graphql.query

import io.mockk.clearMocks
import io.mockk.every
import java.util.UUID
import no.nav.helse.spesialist.api.AbstractGraphQLApiTest
import no.nav.helse.spesialist.api.graphql.schema.Utbetalingtype
import no.nav.helse.spesialist.api.person.Adressebeskyttelse
import no.nav.helse.spesialist.api.utbetaling.OppdragApiDto
import no.nav.helse.spesialist.api.utbetaling.UtbetalingApiDto
import no.nav.helse.spesialist.api.utbetaling.Utbetalingsstatus
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class OppdragQueryTest : AbstractGraphQLApiTest() {

    @AfterEach
    fun clear() {
        clearMocks(utbetalingApiDao, egenAnsattApiDao)
    }

    @Test
    fun `henter oppdrag`() {
        opprettVedtaksperiode(opprettPerson(), opprettArbeidsgiver())
        mockUtbetalinger()

        val body = runQuery("""{ oppdrag(fnr: "$FØDSELSNUMMER") { status } }""")
        val oppdrag = body["data"]["oppdrag"]

        assertEquals(1, oppdrag.size())
        assertEquals(Utbetalingsstatus.UTBETALT.name, oppdrag.first()["status"].asText())
    }

    @Test
    fun `får 404-feil ved oppslag av person som ikke finnes`() {
        val body = runQuery("""{ oppdrag(fnr: "$FØDSELSNUMMER") { status } }""")

        assertEquals(404, body["errors"].first()["extensions"]["code"].asInt())
    }

    @Test
    fun `får 403-feil ved oppslag av kode7-personer uten riktige tilganger`() {
        opprettVedtaksperiode(opprettPerson(adressebeskyttelse = Adressebeskyttelse.Fortrolig), opprettArbeidsgiver())
        mockUtbetalinger()

        val body = runQuery("""{ oppdrag(fnr: "$FØDSELSNUMMER") { status } }""")

        assertEquals(403, body["errors"].first()["extensions"]["code"].asInt())
    }

    @Test
    fun `får 403-feil ved oppslag av skjermede personer uten riktige tilganger`() {
        every { egenAnsattApiDao.erEgenAnsatt(FØDSELSNUMMER) } returns true
        opprettVedtaksperiode(opprettPerson(), opprettArbeidsgiver())
        mockUtbetalinger()

        val body = runQuery("""{ oppdrag(fnr: "$FØDSELSNUMMER") { status } }""")

        assertEquals(403, body["errors"].first()["extensions"]["code"].asInt())
    }

    private fun mockUtbetalinger() {
        every { utbetalingApiDao.findUtbetalinger(FØDSELSNUMMER) } returns listOf(
            UtbetalingApiDto(
                id = UUID.randomUUID(),
                type = Utbetalingtype.UTBETALING.name,
                status = Utbetalingsstatus.UTBETALT,
                arbeidsgiveroppdrag = OppdragApiDto(
                    fagsystemId = "arbeidsgiversFagsystemId",
                    utbetalingslinjer = emptyList(),
                    mottaker = ORGANISASJONSNUMMER,
                ),
                personoppdrag = OppdragApiDto(
                    fagsystemId = "personensFagsystemId",
                    utbetalingslinjer = emptyList(),
                    mottaker = FØDSELSNUMMER,
                ),
                annullertAvSaksbehandler = null,
                totalbeløp = 100_000,
            )
        )
    }

}