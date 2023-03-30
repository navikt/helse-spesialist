package no.nav.helse.e2e

import AbstractE2ETestV2
import java.util.UUID
import kotliquery.Row
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.Testdata.VEDTAKSPERIODE_ID
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class UtbetalingEndretE2ETest: AbstractE2ETestV2() {
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

        håndterUtbetalingOpprettet(arbeidsgiverbeløp = 20000, personbeløp = 20000)

        assertEquals(saksbehandlerOid, finnOidForTildeling(oppgaveId))
    }

    @Test
    fun `beholder påVent-flagget ved gjentildeling`() {
        val saksbehandlerOid = UUID.randomUUID()
        fremTilSaksbehandleroppgave()
        opprettSaksbehandler(saksbehandlerOid, "Behandler, Saks", "saks.behandler@nav.no")

        val oppgaveId = oppgaveIdFor(VEDTAKSPERIODE_ID)
        tildelOppgave(oppgaveId, saksbehandlerOid, påVent = true)

        håndterUtbetalingOpprettet(arbeidsgiverbeløp = 20000, personbeløp = 20000)

        val (oid, påVent) = finnOidOgPåVentForTildeling(oppgaveId)!!
        assertEquals(saksbehandlerOid, oid)
        assertEquals(true, påVent) { "Ny oppgave skal være lagt på vent etter reberegning" }
    }


    private fun oppgaveIdFor(vedtaksperiodeId: UUID): Long = sessionOf(dataSource).use { session ->
        @Language("PostgreSQL")
        val query = "SELECT id FROM oppgave WHERE vedtak_ref = (SELECT id FROM vedtak WHERE vedtaksperiode_id = ?);"
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
        epost: String
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