package no.nav.helse.e2e

import AbstractE2ETest
import java.util.UUID
import kotliquery.Row
import kotliquery.queryOf
import kotliquery.sessionOf
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test

internal class UtbetalingEndretE2ETest : AbstractE2ETest() {

    @Test
    fun `Lagrer personbeløp og arbeidsgiverbeløp ved innlesing av utbetaling_endret`() {
        vedtaksløsningenMottarNySøknad()
        spleisOppretterNyBehandling()
        håndterVedtaksperiodeNyUtbetaling()
        håndterUtbetalingOpprettet(arbeidsgiverbeløp = 20000, personbeløp = 20000)
        assertUtbetaling(20000, 20000)
    }

    @Test
    fun `tildeler andre rundes oppgave til saksbehandler`() {
        val saksbehandlerOid = UUID.randomUUID()
        vedtaksløsningenMottarNySøknad()
        spleisOppretterNyBehandling()
        spesialistBehandlerGodkjenningsbehovFremTilOppgave()
        opprettSaksbehandler(saksbehandlerOid, "Behandler, Saks", "saks.behandler@nav.no", "Z999999")

        val oppgaveId = oppgaveIdFor(VEDTAKSPERIODE_ID)
        tildelOppgave(oppgaveId, saksbehandlerOid)

        val utbetalingId2 = UUID.randomUUID()
        håndterUtbetalingErstattet(arbeidsgiverbeløp = 20000, personbeløp = 20000, utbetalingId = utbetalingId2)
        håndterVedtaksperiodeReberegnet()
        spesialistBehandlerGodkjenningsbehovFremTilOppgave(
            harOppdatertMetadata = true,
            harRisikovurdering = true,
            godkjenningsbehovTestdata = godkjenningsbehovTestdata.copy(utbetalingId = utbetalingId2)
        )

        val oppgaveId2 = finnNyOppgaveId(forrigeOppgaveId = oppgaveId)
        assertEquals(saksbehandlerOid, finnOidForTildeling(oppgaveId2))
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

    private fun tildelOppgave(oppgaveId: Long, saksbehandlerOid: UUID) {
        sessionOf(dataSource).use {
            it.run(
                queryOf(
                    "INSERT INTO tildeling(oppgave_id_ref, saksbehandler_ref) VALUES(:oppgave_id_ref, :saksbehandler_ref);",
                    mapOf(
                        "oppgave_id_ref" to oppgaveId,
                        "saksbehandler_ref" to saksbehandlerOid,
                    )
                ).asUpdate
            )
        }
    }

    private fun finnOidForTildeling(oppgaveId: Long) = hentFraTildeling<UUID?>(oppgaveId) {
        it.uuid("saksbehandler_ref")
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
        ident: String
    ) {
        sessionOf(dataSource).use {
            val opprettSaksbehandlerQuery = "INSERT INTO saksbehandler(oid, navn, epost, ident) VALUES (:oid, :navn, :epost, :ident)"
            it.run(
                queryOf(
                    opprettSaksbehandlerQuery,
                    mapOf<String, Any>(
                        "oid" to oid, "navn" to navn, "epost" to epost, "ident" to ident
                    )
                ).asUpdate
            )
        }
    }
}
