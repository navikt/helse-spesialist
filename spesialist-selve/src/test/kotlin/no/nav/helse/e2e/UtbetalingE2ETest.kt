package no.nav.helse.e2e

import AbstractE2ETest
import java.util.UUID
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.Meldingssender.sendPersonUtbetalingEndret
import no.nav.helse.Meldingssender.sendUtbetalingEndret
import no.nav.helse.Testdata.FØDSELSNUMMER
import no.nav.helse.Testdata.ORGNR
import no.nav.helse.Testdata.SNAPSHOT_UTEN_WARNINGS
import no.nav.helse.Testdata.UTBETALING_ID
import no.nav.helse.Testdata.VEDTAKSPERIODE_ID
import no.nav.helse.modell.utbetaling.Utbetalingsstatus.ANNULLERT
import no.nav.helse.modell.utbetaling.Utbetalingsstatus.FORKASTET
import no.nav.helse.modell.utbetaling.Utbetalingsstatus.GODKJENT
import no.nav.helse.modell.utbetaling.Utbetalingsstatus.IKKE_GODKJENT
import no.nav.helse.modell.utbetaling.Utbetalingsstatus.OVERFØRT
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow

internal class UtbetalingE2ETest : AbstractE2ETest() {

    private companion object {
        private const val arbeidsgiverFagsystemId = "ASDJ12IA312KLS"
    }

    @Test
    fun `utbetaling endret`() {
        settOppBruker()
        sendUtbetalingEndret("UTBETALING", GODKJENT, ORGNR, arbeidsgiverFagsystemId, utbetalingId = UTBETALING_ID)
        sendUtbetalingEndret("ETTERUTBETALING", OVERFØRT, ORGNR, arbeidsgiverFagsystemId, utbetalingId = UTBETALING_ID)
        sendUtbetalingEndret("ANNULLERING", ANNULLERT, ORGNR, arbeidsgiverFagsystemId, utbetalingId = UTBETALING_ID)
        assertEquals(3, utbetalinger().size)
    }

    @Test
    fun `utbetaling endret uten at vi kjenner arbeidsgiver`() {
        val ET_ORGNR = "1"
        val ET_ANNET_ORGNR = "2"
        vedtaksperiode(FØDSELSNUMMER, ET_ORGNR, VEDTAKSPERIODE_ID, false, SNAPSHOT_UTEN_WARNINGS, UTBETALING_ID)
        assertDoesNotThrow {
            sendUtbetalingEndret(
                "UTBETALING",
                GODKJENT,
                ET_ANNET_ORGNR,
                arbeidsgiverFagsystemId,
                utbetalingId = UTBETALING_ID
            )
        }
        assertEquals(0, utbetalinger().size)
        assertEquals(1, feilendeMeldinger().size)
    }

    @Test
    fun `lagrer utbetaling etter utbetaling_endret når utbetalingen har vært til godkjenning og vi kjenner arbeidsgiver`() {
        settOppBruker()
        assertDoesNotThrow {
            sendUtbetalingEndret(
                "UTBETALING",
                GODKJENT,
                ORGNR,
                arbeidsgiverFagsystemId,
                utbetalingId = UTBETALING_ID
            )
        }
        assertEquals(1, utbetalinger().size)
        assertEquals(0, feilendeMeldinger().size)
    }

    @Test
    fun `lagrer utbetaling med annen type enn UTBETALING, forventer ikke at utbetalingen har vært til godkjenning`() {
        settOppBruker()
        assertDoesNotThrow {
            sendUtbetalingEndret(
                "FERIEPENGER", GODKJENT, ORGNR, arbeidsgiverFagsystemId, utbetalingId = UUID.randomUUID()
            )
            sendUtbetalingEndret(
                "ETTERUTBETALING", GODKJENT, ORGNR, arbeidsgiverFagsystemId, utbetalingId = UUID.randomUUID()
            )
            sendUtbetalingEndret(
                "ANNULLERING", GODKJENT, ORGNR, arbeidsgiverFagsystemId, utbetalingId = UUID.randomUUID()
            )
            sendUtbetalingEndret(
                "REVURDERING", GODKJENT, ORGNR, arbeidsgiverFagsystemId, utbetalingId = UUID.randomUUID()
            )
        }
        assertEquals(4, utbetalinger().size)
        assertEquals(0, feilendeMeldinger().size)
    }

    @Test
    fun `utbetaling forkastet`() {
        vedtaksperiode(FØDSELSNUMMER, ORGNR, VEDTAKSPERIODE_ID, true, SNAPSHOT_UTEN_WARNINGS, UTBETALING_ID)
        sendUtbetalingEndret(
            "UTBETALING",
            FORKASTET,
            ORGNR,
            arbeidsgiverFagsystemId,
            forrigeStatus = IKKE_GODKJENT,
            utbetalingId = UTBETALING_ID
        )
        assertEquals(1, utbetalinger().size)
        sendUtbetalingEndret(
            "UTBETALING",
            FORKASTET,
            ORGNR,
            arbeidsgiverFagsystemId,
            forrigeStatus = GODKJENT,
            utbetalingId = UTBETALING_ID
        )
        assertEquals(2, utbetalinger().size)
    }

    @Test
    fun `feriepengeutbetalinger har riktig type på utbetaling`() {
        vedtaksperiode(utbetalingId = UTBETALING_ID)
        sendUtbetalingEndret("FERIEPENGER", OVERFØRT, ORGNR, arbeidsgiverFagsystemId, utbetalingId = UTBETALING_ID)

        utbetalinger().first().let {
            assertNotNull(it)
            assertEquals("FERIEPENGER", it.type)
        }
    }

    @Test
    fun `ved endringer i utbetalinger skal kun nyeste vises`() {
        val nyUtbetalingId = UUID.randomUUID()
        vedtaksperiode(utbetalingId = UTBETALING_ID)
        vedtaksperiode(utbetalingId = nyUtbetalingId)
        sendUtbetalingEndret("FERIEPENGER", OVERFØRT, ORGNR, arbeidsgiverFagsystemId, utbetalingId = UTBETALING_ID)
        sendUtbetalingEndret("FERIEPENGER", OVERFØRT, ORGNR, arbeidsgiverFagsystemId, utbetalingId = nyUtbetalingId)

        assertEquals(2, utbetalinger().size)
        assertTrue { utbetalinger().find { it.id == nyUtbetalingId } != null }
    }

    @Test
    fun `forstår utbetaling til bruker`() {
        val nyUtbetalingId = UUID.randomUUID()
        vedtaksperiode(utbetalingId = nyUtbetalingId)
        sendPersonUtbetalingEndret("UTBETALING", OVERFØRT, ORGNR, utbetalingId = nyUtbetalingId)

        assertEquals(1, utbetalinger().size)
        assertTrue { utbetalinger().find { it.id == nyUtbetalingId } != null }
    }

    private fun utbetalinger(): List<UtbetalingTestDto> {
        @Language("PostgreSQL")
        val statement = """
            SELECT u.*, ui.utbetaling_id, ui.type
            FROM utbetaling u
            INNER JOIN utbetaling_id ui ON (ui.id = u.utbetaling_id_ref)
            INNER JOIN person p ON (p.id = ui.person_ref)
            INNER JOIN arbeidsgiver a ON (a.id = ui.arbeidsgiver_ref)
            INNER JOIN oppdrag o1 ON (o1.id = ui.arbeidsgiver_fagsystem_id_ref)
            INNER JOIN oppdrag o2 ON (o2.id = ui.person_fagsystem_id_ref)
            WHERE p.fodselsnummer = :fodselsnummer AND a.orgnummer = :orgnummer
            """
        return sessionOf(dataSource).use { session ->
            session.run(
                queryOf(
                    statement, mapOf(
                        "fodselsnummer" to FØDSELSNUMMER.toLong(),
                        "orgnummer" to ORGNR.toLong()
                    )
                ).map {
                    UtbetalingTestDto(
                        id = it.uuid("utbetaling_id"),
                        idRef = it.long("utbetaling_id_ref"),
                        type = it.string("type")
                    )
                }.asList
            )
        }
    }

    private fun feilendeMeldinger(): List<UUID> {
        @Language("PostgreSQL")
        val statement = "SELECT id FROM feilende_meldinger"
        return sessionOf(dataSource).use { session ->
            session.run(queryOf(statement).map { UUID.fromString(it.string("id")) }.asList)
        }
    }
}

private data class UtbetalingTestDto(
    val id: UUID,
    val idRef: Long,
    val type: String,
)