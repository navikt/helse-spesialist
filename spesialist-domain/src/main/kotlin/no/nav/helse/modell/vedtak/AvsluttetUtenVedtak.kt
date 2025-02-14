package no.nav.helse.modell.vedtak

import java.util.UUID

class AvsluttetUtenVedtak(
    private val vedtaksperiodeId: UUID,
    val spleisBehandlingId: UUID,
) {
    fun vedtaksperiodeId() = vedtaksperiodeId

    fun spleisBehandlingId() = spleisBehandlingId
}
