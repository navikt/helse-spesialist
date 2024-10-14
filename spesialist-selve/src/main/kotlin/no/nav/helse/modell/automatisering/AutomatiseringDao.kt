package no.nav.helse.modell.automatisering

import kotliquery.sessionOf
import no.nav.helse.db.AutomatiseringRepository
import no.nav.helse.db.TransactionalAutomatiseringDao
import java.util.UUID
import javax.sql.DataSource

internal class AutomatiseringDao(val dataSource: DataSource) : AutomatiseringRepository {
    override fun manuellSaksbehandling(
        problems: List<String>,
        vedtaksperiodeId: UUID,
        hendelseId: UUID,
        utbetalingId: UUID,
    ) = sessionOf(dataSource).use { session ->
        session.transaction { transactionalSession ->
            TransactionalAutomatiseringDao(transactionalSession).manuellSaksbehandling(problems, vedtaksperiodeId, hendelseId, utbetalingId)
        }
    }

    override fun automatisert(
        vedtaksperiodeId: UUID,
        hendelseId: UUID,
        utbetalingId: UUID,
    ) = sessionOf(dataSource).use { session ->
        session.transaction { transactionalSession ->
            TransactionalAutomatiseringDao(transactionalSession).automatisert(vedtaksperiodeId, hendelseId, utbetalingId)
        }
    }

    override fun stikkprøve(
        vedtaksperiodeId: UUID,
        hendelseId: UUID,
        utbetalingId: UUID,
    ) = sessionOf(dataSource).use { session ->
        session.transaction { transactionalSession ->
            TransactionalAutomatiseringDao(transactionalSession).stikkprøve(vedtaksperiodeId, hendelseId, utbetalingId)
        }
    }

    override fun settAutomatiseringInaktiv(
        vedtaksperiodeId: UUID,
        hendelseId: UUID,
    ) {
        sessionOf(dataSource).use { session ->
            session.transaction { transactionalSession ->
                TransactionalAutomatiseringDao(transactionalSession).settAutomatiseringInaktiv(vedtaksperiodeId, hendelseId)
            }
        }
    }

    override fun settAutomatiseringProblemInaktiv(
        vedtaksperiodeId: UUID,
        hendelseId: UUID,
    ) {
        sessionOf(dataSource).use { session ->
            session.transaction { transactionalSession ->
                TransactionalAutomatiseringDao(transactionalSession).settAutomatiseringProblemInaktiv(vedtaksperiodeId, hendelseId)
            }
        }
    }

    override fun plukketUtTilStikkprøve(
        vedtaksperiodeId: UUID,
        hendelseId: UUID,
    ) = sessionOf(dataSource).use { session ->
        session.transaction { transactionalSession ->
            TransactionalAutomatiseringDao(transactionalSession).plukketUtTilStikkprøve(vedtaksperiodeId, hendelseId)
        }
    }

    override fun hentAktivAutomatisering(
        vedtaksperiodeId: UUID,
        hendelseId: UUID,
    ) = sessionOf(dataSource).use { session ->
        session.transaction { transactionalSession ->
            TransactionalAutomatiseringDao(transactionalSession).hentAktivAutomatisering(vedtaksperiodeId, hendelseId)
        }
    }

    override fun finnAktiveProblemer(
        vedtaksperiodeRef: Long,
        hendelseId: UUID,
    ) = sessionOf(dataSource).use { session ->
        session.transaction { transactionalSession ->
            TransactionalAutomatiseringDao(transactionalSession).finnAktiveProblemer(vedtaksperiodeRef, hendelseId)
        }
    }

    override fun finnVedtaksperiode(vedtaksperiodeId: UUID): Long? =
        sessionOf(dataSource).use { session ->
            session.transaction { transactionalSession ->
                TransactionalAutomatiseringDao(transactionalSession).finnVedtaksperiode(vedtaksperiodeId)
            }
        }
}

data class AutomatiseringDto(
    val automatisert: Boolean,
    val vedtaksperiodeId: UUID,
    val hendelseId: UUID,
    val problemer: List<String>,
    val utbetalingId: UUID?,
)
