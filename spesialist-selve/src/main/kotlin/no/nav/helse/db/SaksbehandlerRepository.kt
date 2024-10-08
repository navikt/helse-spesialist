package no.nav.helse.db

import java.util.UUID

interface SaksbehandlerRepository {
    fun finnSaksbehandler(oid: UUID): SaksbehandlerFraDatabase?
}
