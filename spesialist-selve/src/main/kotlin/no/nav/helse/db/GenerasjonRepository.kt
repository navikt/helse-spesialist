package no.nav.helse.db

import java.time.LocalDateTime
import java.util.UUID

interface GenerasjonRepository {
    fun førsteGenerasjonVedtakFattetTidspunkt(vedtaksperiodeId: UUID): LocalDateTime?
}
