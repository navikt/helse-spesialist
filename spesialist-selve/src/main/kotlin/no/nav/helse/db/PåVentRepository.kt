package no.nav.helse.db

import java.util.UUID

interface PåVentRepository {
    fun erPåVent(vedtaksperiodeId: UUID): Boolean
}
