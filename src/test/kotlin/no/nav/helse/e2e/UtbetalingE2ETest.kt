package no.nav.helse.e2e

import AbstractE2ETest
import io.mockk.every
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.helse.modell.vedtak.Saksbehandleroppgavetype
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test
import java.util.*
import kotlin.test.assertEquals

internal class UtbetalingE2ETest : AbstractE2ETest() {
    private companion object {
        private const val ORGNR = "987654321"
        private const val arbeidsgiverFagsystemId = "ASDJ12IA312KLS"

        private val VEDTAKSPERIODE_ID = UUID.randomUUID()
        private const val SNAPSHOTV1 = """{"version": "this_is_version_1"}"""
    }

    @Test
    fun `utbetaling endret`() {
        vedtaksperiode()
        sendUtbetalingEndret("UTBETALING", "GODKJENT", ORGNR, arbeidsgiverFagsystemId)
        sendUtbetalingEndret("ETTERUTBETALING", "OVERFØRT", ORGNR, arbeidsgiverFagsystemId)
        sendUtbetalingEndret("ANNULLERING", "ANNULLERT", ORGNR, arbeidsgiverFagsystemId)
        assertEquals(3, utbetalinger().size)
    }

    @Test
    fun `utbetaling forkastet`() {
        vedtaksperiode()
        sendUtbetalingEndret("UTBETALING", "FORKASTET", ORGNR, arbeidsgiverFagsystemId, forrigeStatus = "IKKE_UTBETALT")
        assertEquals(0, utbetalinger().size)
        sendUtbetalingEndret("UTBETALING", "FORKASTET", ORGNR, arbeidsgiverFagsystemId, forrigeStatus = "GODKJENT")
        assertEquals(1, utbetalinger().size)
    }

    private fun utbetalinger(): List<String> {
        @Language("PostgreSQL")
        val statement = """
            SELECT u.*
            FROM utbetaling u
            INNER JOIN utbetaling_id ui ON (ui.id = u.utbetaling_id_ref)
            INNER JOIN person p ON (p.id = ui.person_ref)
            INNER JOIN arbeidsgiver a ON (a.id = ui.arbeidsgiver_ref)
            INNER JOIN oppdrag o1 ON (o1.id = ui.arbeidsgiver_fagsystem_id_ref)
            INNER JOIN oppdrag o2 ON (o2.id = ui.person_fagsystem_id_ref)
            WHERE p.fodselsnummer = :fodselsnummer AND a.orgnummer = :orgnummer
            """
        return using(sessionOf(dataSource)) {
            it.run(queryOf(statement, mapOf(
                "fodselsnummer" to UNG_PERSON_FNR_2018.toLong(),
                "orgnummer" to ORGNR.toLong()
            )).map { row -> row.string("utbetaling_id_ref") }.asList)
        }
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
        sendArbeidsgiverinformasjonløsning(
            hendelseId = godkjenningsmeldingId,
            orgnr = ORGNR,
            vedtaksperiodeId = VEDTAKSPERIODE_ID
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
