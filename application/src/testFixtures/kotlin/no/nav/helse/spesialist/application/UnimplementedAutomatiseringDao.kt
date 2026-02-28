package no.nav.helse.spesialist.application

import no.nav.helse.db.AutomatiseringDao
import no.nav.helse.db.AutomatiseringDto
import java.util.UUID

class UnimplementedAutomatiseringDao : AutomatiseringDao {
    override fun plukketUtTilStikkprøve(
        vedtaksperiodeId: UUID,
        hendelseId: UUID
    ): Boolean {
        TODO("Not yet implemented")
    }

    override fun settAutomatiseringInaktiv(vedtaksperiodeId: UUID, hendelseId: UUID) {
        TODO("Not yet implemented")
    }

    override fun settAutomatiseringProblemInaktiv(vedtaksperiodeId: UUID, hendelseId: UUID) {
        TODO("Not yet implemented")
    }

    override fun automatisert(
        vedtaksperiodeId: UUID,
        hendelseId: UUID,
        utbetalingId: UUID
    ) {
        TODO("Not yet implemented")
    }

    override fun manuellSaksbehandling(
        problems: List<String>,
        vedtaksperiodeId: UUID,
        hendelseId: UUID,
        utbetalingId: UUID
    ) {
        TODO("Not yet implemented")
    }

    override fun stikkprøve(
        vedtaksperiodeId: UUID,
        hendelseId: UUID,
        utbetalingId: UUID
    ) {
        TODO("Not yet implemented")
    }

    override fun hentAktivAutomatisering(
        vedtaksperiodeId: UUID,
        hendelseId: UUID
    ): AutomatiseringDto? {
        TODO("Not yet implemented")
    }

    override fun finnAktiveProblemer(
        vedtaksperiodeRef: Long,
        hendelseId: UUID
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
