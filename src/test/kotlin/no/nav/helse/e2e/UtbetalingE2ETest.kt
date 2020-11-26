package no.nav.helse.e2e

import AbstractE2ETest
import io.mockk.every
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.helse.modell.vedtak.Saksbehandleroppgavetype
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.*
import kotlin.test.assertEquals

class UtbetalingE2ETest : AbstractE2ETest() {
    private companion object {
        private const val ORGNR = "987654321"
        private const val arbeidsgiverFagsystemId = "ASDJ12IA312KLS"
        private const val personFagsystemId = "LKJHGFXD256"

        private val VEDTAKSPERIODE_ID = UUID.randomUUID()
        private const val SNAPSHOTV1 = """{"version": "this_is_version_1"}"""
    }

    @Test
    fun `utbetaling endret`() {
        vedtaksperiode()
        sendUtbetalingEndret("UTBETALING", "GODKJENT")
        sendUtbetalingEndret("ETTERUTBETALING", "OVERFØRT")
        sendUtbetalingEndret("ANNULLERING", "ANNULLERT")
        assertEquals(3, utbetalinger().size)
    }

    private fun utbetalinger(): List<String> {
        @Language("PostgreSQL")
        val statement = """
            SELECT u.*
            FROM utbetaling u
            INNER JOIN person p ON (p.id = u.person_ref)
            INNER JOIN arbeidsgiver a ON (a.id = u.arbeidsgiver_ref)
            INNER JOIN utbetaling_id uid ON (uid.id = u.utbetaling_id_ref)
            INNER JOIN oppdrag o1 ON (o1.id = u.arbeidsgiver_fagsystem_id_ref)
            INNER JOIN oppdrag o2 ON (o2.id = u.person_fagsystem_id_ref)
            WHERE p.fodselsnummer = :fodselsnummer AND a.orgnummer = :orgnummer
            """
        return using(sessionOf(dataSource)) {
            it.run(queryOf(statement, mapOf(
                "fodselsnummer" to UNG_PERSON_FNR_2018.toLong(),
                "orgnummer" to ORGNR.toLong()
            )).map { it.string("utbetaling_id_ref") }.asList)
        }
    }

    private fun sendUtbetalingEndret(type: String, status: String) {
        @Language("JSON")
        val json = """
{
    "@event_name": "utbetaling_endret",
    "@id": "${UUID.randomUUID()}",
    "@opprettet": "${LocalDateTime.now()}",
    "utbetalingId": "${UUID.randomUUID()}",
    "fødselsnummer": "$UNG_PERSON_FNR_2018",
    "type": "$type",
    "gjeldendeStatus": "$status",
    "organisasjonsnummer": "$ORGNR",
    "arbeidsgiverOppdrag": {
      "fagsystemId": "$arbeidsgiverFagsystemId"
    },
    "personOppdrag": {
      "fagsystemId": "$personFagsystemId"
    }
}"""

        testRapid.sendTestMessage(json)
    }

    fun vedtaksperiode(vedtaksperiodeId: UUID = VEDTAKSPERIODE_ID, snapshot: String = SNAPSHOTV1) {
        every { restClient.hentSpeilSpapshot(UNG_PERSON_FNR_2018) } returns snapshot
        val godkjenningsmeldingId = sendGodkjenningsbehov(
            orgnr = ORGNR,
            vedtaksperiodeId = vedtaksperiodeId,
            periodetype = Saksbehandleroppgavetype.FORLENGELSE
        )
        sendPersoninfoløsning(
            orgnr = ORGNR,
            vedtaksperiodeId = vedtaksperiodeId,
            hendelseId = godkjenningsmeldingId
        )
        sendEgenAnsattløsning(
            godkjenningsmeldingId = godkjenningsmeldingId,
            erEgenAnsatt = false
        )
        sendDigitalKontaktinformasjonløsning(
            godkjenningsmeldingId = godkjenningsmeldingId,
            erDigital = true
        )
        sendÅpneGosysOppgaverløsning(
            godkjenningsmeldingId = godkjenningsmeldingId
        )
        sendRisikovurderingløsning(
            godkjenningsmeldingId = godkjenningsmeldingId,
            vedtaksperiodeId = vedtaksperiodeId
        )
    }
}
