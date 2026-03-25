package no.nav.helse.db

import java.util.UUID

interface PåVentDao {
    fun erPåVent(vedtaksperiodeId: UUID): Boolean

    fun slettPåVent(oppgaveId: Long): Int?
}
