package no.nav.helse.db

import kotliquery.sessionOf
import no.nav.helse.modell.totrinnsvurdering.TotrinnsvurderingOld
import java.util.UUID
import javax.sql.DataSource

class TotrinnsvurderingDao(private val dataSource: DataSource) : TotrinnsvurderingRepository {
    override fun oppdater(totrinnsvurderingFraDatabase: TotrinnsvurderingFraDatabase) {
        sessionOf(dataSource).use { session ->
            TransactionalTotrinnsvurderingDao(session).oppdater(totrinnsvurderingFraDatabase)
        }
    }

    override fun hentAktivTotrinnsvurdering(oppgaveId: Long): TotrinnsvurderingFraDatabase? {
        return sessionOf(dataSource).use { session ->
            TransactionalTotrinnsvurderingDao(session).hentAktivTotrinnsvurdering(oppgaveId)
        }
    }

    override fun opprett(vedtaksperiodeId: UUID): TotrinnsvurderingOld =
        sessionOf(dataSource).use { session ->
            session.transaction { transaction ->
                val transactionalDao = TransactionalTotrinnsvurderingDao(transaction)
                transactionalDao.hentAktiv(vedtaksperiodeId) ?: transactionalDao.opprett(vedtaksperiodeId)
            }
        }

    override fun settBeslutter(
        oppgaveId: Long,
        saksbehandlerOid: UUID,
    ) {
        sessionOf(dataSource).use { session ->
            TransactionalTotrinnsvurderingDao(session).settBeslutter(oppgaveId, saksbehandlerOid)
        }
    }

    override fun settErRetur(vedtaksperiodeId: UUID) {
        sessionOf(dataSource).use { session ->
            TransactionalTotrinnsvurderingDao(session).settErRetur(vedtaksperiodeId)
        }
    }

    override fun ferdigstill(vedtaksperiodeId: UUID) {
        sessionOf(dataSource).use { session ->
            TransactionalTotrinnsvurderingDao(session).ferdigstill(vedtaksperiodeId)
        }
    }

    override fun hentAktiv(vedtaksperiodeId: UUID): TotrinnsvurderingOld? =
        sessionOf(dataSource).use { session ->
            TransactionalTotrinnsvurderingDao(session).hentAktiv(vedtaksperiodeId)
        }

    override fun hentAktiv(oppgaveId: Long): TotrinnsvurderingOld? =
        sessionOf(dataSource).use { session ->
            TransactionalTotrinnsvurderingDao(session).hentAktiv(oppgaveId)
        }
}
