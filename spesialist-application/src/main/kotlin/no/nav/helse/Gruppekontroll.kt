package no.nav.helse

import java.util.UUID

interface Gruppekontroll {
    suspend fun erIGrupper(
        oid: UUID,
        gruppeIder: List<UUID>,
    ): Boolean
}
