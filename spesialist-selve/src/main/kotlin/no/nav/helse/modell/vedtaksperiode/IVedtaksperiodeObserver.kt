package no.nav.helse.modell.vedtaksperiode

import java.time.LocalDate
import java.util.UUID

interface IVedtaksperiodeObserver {

    fun tidslinjeOppdatert(generasjonId: UUID, fom: LocalDate, tom: LocalDate, skjæringstidspunkt: LocalDate) {}
    fun generasjonOpprettet(generasjonId: UUID, vedtaksperiodeId: UUID, hendelseId: UUID, fom: LocalDate?, tom: LocalDate?, skjæringstidspunkt: LocalDate?) {}
    fun nyUtbetaling(generasjonId: UUID, utbetalingId: UUID) {}
    fun utbetalingForkastet(generasjonId: UUID, utbetalingId: UUID) {}
    fun vedtakFattet(generasjonId: UUID, hendelseId: UUID) {}
    fun førsteGenerasjonOpprettet(generasjonId: UUID, vedtaksperiodeId: UUID, hendelseId: UUID, fom: LocalDate?, tom: LocalDate?, skjæringstidspunkt: LocalDate?) {}
}