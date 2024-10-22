package no.nav.helse.mediator.oppgave

import kotliquery.sessionOf
import no.nav.helse.HelseDao
import no.nav.helse.db.AntallOppgaverFraDatabase
import no.nav.helse.db.BehandletOppgaveFraDatabaseForVisning
import no.nav.helse.db.EgenskapForDatabase
import no.nav.helse.db.OppgaveFraDatabase
import no.nav.helse.db.OppgaveFraDatabaseForVisning
import no.nav.helse.db.OppgaveRepository
import no.nav.helse.db.OppgavesorteringForDatabase
import no.nav.helse.db.TransactionalOppgaveDao
import no.nav.helse.modell.gosysoppgaver.OppgaveDataForAutomatisering
import no.nav.helse.modell.oppgave.Egenskap
import java.util.UUID
import javax.sql.DataSource

class OppgaveDao(private val dataSource: DataSource) : HelseDao(dataSource), OppgaveRepository {
    override fun reserverNesteId(): Long {
        return sessionOf(dataSource).use { session ->
            TransactionalOppgaveDao(session).reserverNesteId()
        }
    }

    override fun finnOppgave(id: Long): OppgaveFraDatabase? {
        return sessionOf(dataSource).use { session ->
            TransactionalOppgaveDao(session).finnOppgave(id)
        }
    }

    override fun finnOppgaverForVisning(
        ekskluderEgenskaper: List<String>,
        saksbehandlerOid: UUID,
        offset: Int,
        limit: Int,
        sortering: List<OppgavesorteringForDatabase>,
        egneSakerPåVent: Boolean,
        egneSaker: Boolean,
        tildelt: Boolean?,
        grupperteFiltrerteEgenskaper: Map<Egenskap.Kategori, List<EgenskapForDatabase>>?,
    ): List<OppgaveFraDatabaseForVisning> {
        return sessionOf(dataSource).use { session ->
            TransactionalOppgaveDao(session).finnOppgaverForVisning(
                ekskluderEgenskaper,
                saksbehandlerOid,
                offset,
                limit,
                sortering,
                egneSakerPåVent,
                egneSaker,
                tildelt,
                grupperteFiltrerteEgenskaper,
            )
        }
    }

    override fun finnEgenskaper(
        vedtaksperiodeId: UUID,
        utbetalingId: UUID,
    ): Set<EgenskapForDatabase>? =
        sessionOf(dataSource).use { session ->
            TransactionalOppgaveDao(session).finnEgenskaper(vedtaksperiodeId, utbetalingId)
        }

    override fun finnAntallOppgaver(saksbehandlerOid: UUID): AntallOppgaverFraDatabase {
        return sessionOf(dataSource).use { session ->
            TransactionalOppgaveDao(session).finnAntallOppgaver(saksbehandlerOid)
        }
    }

    override fun finnBehandledeOppgaver(
        behandletAvOid: UUID,
        offset: Int,
        limit: Int,
    ): List<BehandletOppgaveFraDatabaseForVisning> =
        sessionOf(dataSource).use { session ->
            TransactionalOppgaveDao(session).finnBehandledeOppgaver(behandletAvOid, offset, limit)
        }

    override fun finnUtbetalingId(oppgaveId: Long) =
        sessionOf(dataSource).use { session ->
            TransactionalOppgaveDao(session).finnUtbetalingId(oppgaveId)
        }

    override fun finnGenerasjonId(oppgaveId: Long): UUID {
        return sessionOf(dataSource).use { session ->
            TransactionalOppgaveDao(session).finnGenerasjonId(oppgaveId)
        }
    }

    override fun finnSpleisBehandlingId(oppgaveId: Long) =
        sessionOf(dataSource).use { session ->
            TransactionalOppgaveDao(session).finnSpleisBehandlingId(oppgaveId)
        }

    override fun finnIdForAktivOppgave(vedtaksperiodeId: UUID) =
        sessionOf(dataSource).use { session ->
            TransactionalOppgaveDao(session).finnIdForAktivOppgave(vedtaksperiodeId)
        }

    override fun finnOppgaveIdUansettStatus(fødselsnummer: String) =
        sessionOf(dataSource).use { session ->
            TransactionalOppgaveDao(session).finnOppgaveIdUansettStatus(fødselsnummer)
        }

    override fun finnVedtaksperiodeId(fødselsnummer: String) =
        sessionOf(dataSource).use { session ->
            TransactionalOppgaveDao(session).finnVedtaksperiodeId(fødselsnummer)
        }

    override fun finnOppgaveId(fødselsnummer: String) =
        sessionOf(dataSource).use { session ->
            TransactionalOppgaveDao(session).finnOppgaveId(fødselsnummer)
        }

    override fun finnOppgaveId(utbetalingId: UUID) =
        sessionOf(dataSource).use { session ->
            TransactionalOppgaveDao(session).finnOppgaveId(utbetalingId)
        }

    fun finnVedtaksperiodeId(oppgaveId: Long) =
        requireNotNull(
            asSQL(
                """ SELECT v.vedtaksperiode_id
            FROM vedtak v
            INNER JOIN oppgave o on v.id = o.vedtak_ref
            WHERE o.id = :oppgaveId
        """,
                mapOf("oppgaveId" to oppgaveId),
            ).single { row -> row.uuid("vedtaksperiode_id") },
        )

    override fun opprettOppgave(
        id: Long,
        commandContextId: UUID,
        egenskaper: List<EgenskapForDatabase>,
        vedtaksperiodeId: UUID,
        utbetalingId: UUID,
        kanAvvises: Boolean,
    ) = sessionOf(dataSource, returnGeneratedKey = true).use { session ->
        TransactionalOppgaveDao(session).opprettOppgave(
            id,
            commandContextId,
            egenskaper,
            vedtaksperiodeId,
            utbetalingId,
            kanAvvises,
        )
    }

    override fun updateOppgave(
        oppgaveId: Long,
        oppgavestatus: String,
        ferdigstiltAv: String?,
        oid: UUID?,
        egenskaper: List<EgenskapForDatabase>,
    ) = sessionOf(dataSource).use { session ->
        TransactionalOppgaveDao(session).updateOppgave(oppgaveId, oppgavestatus, ferdigstiltAv, oid, egenskaper)
    }

    override fun finnHendelseId(id: Long) =
        sessionOf(dataSource).use { session ->
            TransactionalOppgaveDao(session).finnHendelseId(id)
        }

    override fun harGyldigOppgave(utbetalingId: UUID) =
        requireNotNull(
            asSQL(
                """ SELECT COUNT(1) AS oppgave_count FROM oppgave
            WHERE utbetaling_id = :utbetalingId AND status IN('AvventerSystem'::oppgavestatus, 'AvventerSaksbehandler'::oppgavestatus, 'Ferdigstilt'::oppgavestatus)
        """,
                mapOf("utbetalingId" to utbetalingId),
            ).single { it.int("oppgave_count") },
        ) > 0

    override fun harFerdigstiltOppgave(vedtaksperiodeId: UUID) =
        sessionOf(dataSource).use { session ->
            TransactionalOppgaveDao(session).harFerdigstiltOppgave(vedtaksperiodeId)
        }

    override fun venterPåSaksbehandler(oppgaveId: Long) =
        sessionOf(dataSource).use { session ->
            TransactionalOppgaveDao(session).venterPåSaksbehandler(oppgaveId)
        }

    override fun finnFødselsnummer(oppgaveId: Long) =
        sessionOf(dataSource).use { session ->
            TransactionalOppgaveDao(session).finnFødselsnummer(oppgaveId)
        }

    override fun oppgaveDataForAutomatisering(oppgaveId: Long): OppgaveDataForAutomatisering? {
        return sessionOf(dataSource).use { session ->
            TransactionalOppgaveDao(session).oppgaveDataForAutomatisering(oppgaveId)
        }
    }

    override fun invaliderOppgaveFor(fødselsnummer: String) {
        sessionOf(dataSource).use { session ->
            TransactionalOppgaveDao(session).invaliderOppgaveFor(fødselsnummer)
        }
    }
}
