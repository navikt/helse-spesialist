package no.nav.helse.db

import java.util.UUID

interface AutomatiseringDao {
    fun plukketUtTilStikkprøve(
        vedtaksperiodeId: UUID,
        hendelseId: UUID,
    ): Boolean

    fun settAutomatiseringInaktiv(
        vedtaksperiodeId: UUID,
        hendelseId: UUID,
    )

    fun settAutomatiseringProblemInaktiv(
        vedtaksperiodeId: UUID,
        hendelseId: UUID,
    )

    fun automatisert(
        vedtaksperiodeId: UUID,
        hendelseId: UUID,
        utbetalingId: UUID,
    )

    fun manuellSaksbehandling(
        problems: List<String>,
        vedtaksperiodeId: UUID,
        hendelseId: UUID,
        utbetalingId: UUID,
    )

    fun stikkprøve(
        vedtaksperiodeId: UUID,
        hendelseId: UUID,
        utbetalingId: UUID,
    )

    fun hentAktivAutomatisering(
        vedtaksperiodeId: UUID,
        hendelseId: UUID,
    ): AutomatiseringDto?

    fun finnAktiveProblemer(
        vedtaksperiodeRef: Long,
        hendelseId: UUID,
    ): List<String>

    fun finnVedtaksperiode(vedtaksperiodeId: UUID): Long?

    fun skalTvingeAutomatisering(vedtaksperiodeId: UUID): Boolean
}

data class AutomatiseringDto(
    val automatisert: Boolean,
    val vedtaksperiodeId: UUID,
    val hendelseId: UUID,
    val problemer: List<String>,
    val utbetalingId: UUID?,
)
