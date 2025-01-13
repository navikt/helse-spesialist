package no.nav.helse.e2e

import kotliquery.Row
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.TestRapidHelpers.oppgaveId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.UUID

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

        val oppgaveId = inspektør.oppgaveId()
        tildelOppgave(oppgaveId, saksbehandlerOid)

        val utbetalingId2 = UUID.randomUUID()
        håndterVedtaksperiodeReberegnet()
        håndterUtbetalingErstattet(arbeidsgiverbeløp = 20000, personbeløp = 20000, utbetalingId = utbetalingId2)
        spesialistBehandlerGodkjenningsbehovFremTilOppgave(
            harRisikovurdering = true,
            harOppdatertMetadata = true,
            godkjenningsbehovTestdata = godkjenningsbehovTestdata.copy(utbetalingId = utbetalingId2),
        )

        val oppgaveId2 = inspektør.oppgaveId()
        assertEquals(saksbehandlerOid, finnOidForTildeling(oppgaveId2))
    }

    private fun tildelOppgave(
        oppgaveId: Long,
        saksbehandlerOid: UUID,
    ) {
        sessionOf(dataSource).use {
            it.run(
                queryOf(
                    "INSERT INTO tildeling(oppgave_id_ref, saksbehandler_ref) VALUES(:oppgave_id_ref, :saksbehandler_ref);",
                    mapOf(
                        "oppgave_id_ref" to oppgaveId,
                        "saksbehandler_ref" to saksbehandlerOid,
                    ),
                ).asUpdate,
            )
        }
    }

    private fun finnOidForTildeling(oppgaveId: Long) =
        hentFraTildeling<UUID?>(oppgaveId) {
            it.uuid("saksbehandler_ref")
        }

    private fun <T> hentFraTildeling(
        oppgaveId: Long,
        mapping: (Row) -> T,
    ) = sessionOf(dataSource).use { session ->
        session.run(
            queryOf(
                "SELECT * FROM tildeling WHERE oppgave_id_ref=?;",
                oppgaveId,
            ).map(mapping).asSingle,
        )
    }

    private fun opprettSaksbehandler(
        oid: UUID,
        navn: String,
        epost: String,
        ident: String,
    ) {
        sessionOf(dataSource).use {
            val opprettSaksbehandlerQuery = "INSERT INTO saksbehandler(oid, navn, epost, ident) VALUES (:oid, :navn, :epost, :ident)"
            it.run(
                queryOf(
                    opprettSaksbehandlerQuery,
                    mapOf<String, Any>(
                        "oid" to oid,
                        "navn" to navn,
                        "epost" to epost,
                        "ident" to ident,
                    ),
                ).asUpdate,
            )
        }
    }
}
