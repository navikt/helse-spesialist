package no.nav.helse.db.api

import java.time.LocalDate
import java.util.UUID

interface PåVentApiDao {
    fun hentAktivPåVent(vedtaksperiodeId: UUID): PaVentDto?

    data class PaVentDto(
        val frist: LocalDate?,
        val oid: UUID,
    )
}
