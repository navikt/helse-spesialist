package no.nav.helse.modell.vedtaksperiode

import java.time.LocalDate
import java.util.UUID

interface IVedtaksperiodeObserver {

    fun tidslinjeOppdatert(vedtaksperiodeId: UUID, fom: LocalDate, tom: LocalDate, skjæringstidspunkt: LocalDate) {}
}