package no.nav.helse.spesialist.application

import no.nav.helse.db.AutomatiseringDao
import no.nav.helse.db.AutomatiseringDto
import java.util.UUID

class InMemoryAutomatiseringDao : AutomatiseringDao {
    val automatisert = mutableListOf<UUID>()
    val manuellSaksbehandling = mutableListOf<ManuellSaksbehandling>()
    val stikkprøver = mutableListOf<UUID>()

    data class ManuellSaksbehandling(
        val problemer: List<String>,
        val vedtaksperiodeId: UUID,
        val hendelseId: UUID,
        val utbetalingId: UUID,
    )

    override fun plukketUtTilStikkprøve(
        vedtaksperiodeId: UUID,
        hendelseId: UUID,
    ): Boolean {
        TODO("Not yet implemented")
    }

    override fun settAutomatiseringInaktiv(
        vedtaksperiodeId: UUID,
        hendelseId: UUID,
    ) {
        TODO("Not yet implemented")
    }

    override fun settAutomatiseringProblemInaktiv(
        vedtaksperiodeId: UUID,
        hendelseId: UUID,
    ) {
        TODO("Not yet implemented")
    }

    override fun automatisert(
        vedtaksperiodeId: UUID,
        hendelseId: UUID,
        utbetalingId: UUID,
    ) {
        automatisert.add(utbetalingId)
    }

    override fun manuellSaksbehandling(
        problems: List<String>,
        vedtaksperiodeId: UUID,
        hendelseId: UUID,
        utbetalingId: UUID,
    ) {
        manuellSaksbehandling.add(ManuellSaksbehandling(problems, vedtaksperiodeId, hendelseId, utbetalingId))
    }

    override fun stikkprøve(
        vedtaksperiodeId: UUID,
        hendelseId: UUID,
        utbetalingId: UUID,
    ) {
        stikkprøver.add(utbetalingId)
    }

    override fun hentAktivAutomatisering(
        vedtaksperiodeId: UUID,
        hendelseId: UUID,
    ): AutomatiseringDto? {
        TODO("Not yet implemented")
    }

    override fun finnAktiveProblemer(
        vedtaksperiodeRef: Long,
        hendelseId: UUID,
    ): List<String> {
        TODO("Not yet implemented")
    }

    override fun finnVedtaksperiode(vedtaksperiodeId: UUID): Long? {
        TODO("Not yet implemented")
    }

    override fun skalTvingeAutomatisering(vedtaksperiodeId: UUID): Boolean {
        TODO("Not yet implemented")
    }
}
