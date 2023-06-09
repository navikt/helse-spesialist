package no.nav.helse.e2e

import AbstractE2ETestV2
import java.util.UUID
import kotliquery.Row
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.Testdata.VEDTAKSPERIODE_ID
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class UtbetalingEndretE2ETest : AbstractE2ETestV2() {

    @Test
    fun `Lagrer personbeløp og arbeidsgiverbeløp ved innlesing av utbetaling_endret`() {
        håndterSøknad()
        håndterVedtaksperiodeOpprettet()
        håndterVedtaksperiodeNyUtbetaling()
        håndterUtbetalingOpprettet(arbeidsgiverbeløp = 20000, personbeløp = 20000)
        assertUtbetaling(20000, 20000)
    }

    @Test
    fun `tildeler andre rundes oppgave til saksbehandler`() {
        val saksbehandlerOid = UUID.randomUUID()
        fremTilSaksbehandleroppgave()
        opprettSaksbehandler(saksbehandlerOid, "Behandler, Saks", "saks.behandler@nav.no")

        val oppgaveId = oppgaveIdFor(VEDTAKSPERIODE_ID)
        tildelOppgave(oppgaveId, saksbehandlerOid)

        håndterUtbetalingErstattet(arbeidsgiverbeløp = 20000, personbeløp = 20000)
        håndterVedtaksperiodeReberegnet()
        fremTilSaksbehandleroppgave(harOppdatertMetadata = true, harRisikovurdering = true)

        val oppgaveId2 = finnNyOppgaveId(forrigeOppgaveId = oppgaveId)
        assertEquals(saksbehandlerOid, finnOidForTildeling(oppgaveId2))
    }

    @Test
    fun `beholder påVent-flagget ved gjentildeling`() {
        val saksbehandlerOid = UUID.randomUUID()
        fremTilSaksbehandleroppgave()
        opprettSaksbehandler(saksbehandlerOid, "Behandler, Saks", "saks.behandler@nav.no")

        val oppgaveId = oppgaveIdFor(VEDTAKSPERIODE_ID)
        tildelOppgave(oppgaveId, saksbehandlerOid, påVent = false)

        håndterUtbetalingErstattet(arbeidsgiverbeløp = 20000, personbeløp = 20000)
        håndterVedtaksperiodeReberegnet()
        fremTilSaksbehandleroppgave(harOppdatertMetadata = true, harRisikovurdering = true)

        val oppgaveId2 = finnNyOppgaveId(forrigeOppgaveId = oppgaveId)
        val (oid, oppgave2PåVent) = finnOidOgPåVentForTildeling(oppgaveId2)!!
        assertEquals(saksbehandlerOid, oid)
        assertFalse(oppgave2PåVent) { "Ny oppgave skal ikke være på vent etter reberegning, siden forrige ikke var det" }

        leggPåVent(oppgaveId2)
        håndterUtbetalingErstattet(arbeidsgiverbeløp = 20000, personbeløp = 20000)
        håndterVedtaksperiodeReberegnet()
        fremTilSaksbehandleroppgave(harOppdatertMetadata = true, harRisikovurdering = true)

        val oppgaveId3 = finnNyOppgaveId(forrigeOppgaveId = oppgaveId2)
        val (_, oppgave3PåVent) = finnOidOgPåVentForTildeling(oppgaveId3)!!
        assertTrue(oppgave3PåVent) { "Ny oppgave skal være på vent etter reberegning siden forrige var det" }
    }

    private fun finnNyOppgaveId(forrigeOppgaveId: Long) = oppgaveIdFor(VEDTAKSPERIODE_ID).also { nyOppgaveId ->
        assertNotEquals(forrigeOppgaveId, nyOppgaveId) {
            "Det er meningen at det skal ha blitt opprettet en ny oppgave"
        }
    }

    private fun oppgaveIdFor(vedtaksperiodeId: UUID): Long = sessionOf(dataSource).use { session ->
        @Language("PostgreSQL")
        val query =
            "SELECT id FROM oppgave WHERE vedtak_ref = (SELECT id FROM vedtak WHERE vedtaksperiode_id = ?) ORDER BY id DESC;"
        requireNotNull(session.run(queryOf(query, vedtaksperiodeId).map { it.long(1) }.asSingle))
    }

    private fun tildelOppgave(oppgaveId: Long, saksbehandlerOid: UUID, påVent: Boolean = false) {
        sessionOf(dataSource).use {
            it.run(
                queryOf(
                    "INSERT INTO tildeling(oppgave_id_ref, saksbehandler_ref, på_vent) VALUES(:oppgave_id_ref, :saksbehandler_ref, :paa_vent);",
                    mapOf(
                        "oppgave_id_ref" to oppgaveId,
                        "saksbehandler_ref" to saksbehandlerOid,
                        "paa_vent" to påVent,
                    )
                ).asUpdate
            )
        }
    }

    private fun leggPåVent(oppgaveId: Long) {
        sessionOf(dataSource).use {
            it.run(
                queryOf(
                    " UPDATE tildeling SET på_vent = true WHERE oppgave_id_ref = :oppgave_id_ref; ",
                    mapOf("oppgave_id_ref" to oppgaveId)
                ).asUpdate
            )
        }
    }

    private fun finnOidForTildeling(oppgaveId: Long) = hentFraTildeling<UUID?>(oppgaveId) {
        it.uuid("saksbehandler_ref")
    }

    private fun finnOidOgPåVentForTildeling(oppgaveId: Long) =
        hentFraTildeling<Pair<UUID, Boolean>?>(oppgaveId) {
            it.uuid("saksbehandler_ref") to it.boolean("på_vent")
        }

    private fun <T> hentFraTildeling(oppgaveId: Long, mapping: (Row) -> T) =
        sessionOf(dataSource).use { session ->
            session.run(
                queryOf(
                    "SELECT * FROM tildeling WHERE oppgave_id_ref=?;", oppgaveId
                ).map(mapping).asSingle
            )
        }

    private fun opprettSaksbehandler(
        oid: UUID,
        navn: String,
        epost: String,
    ) {
        sessionOf(dataSource).use {
            val opprettSaksbehandlerQuery = "INSERT INTO saksbehandler(oid, navn, epost) VALUES (:oid, :navn, :epost)"
            it.run(
                queryOf(
                    opprettSaksbehandlerQuery,
                    mapOf<String, Any>(
                        "oid" to oid, "navn" to navn, "epost" to epost
                    )
                ).asUpdate
            )
        }
    }
}
