package no.nav.helse.db

import no.nav.helse.modell.automatisering.AutomatiseringDto
import java.util.UUID

internal interface AutomatiseringRepository {
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
}
