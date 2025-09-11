package no.nav.helse

import java.util.UUID

interface Gruppekontroll {
    suspend fun hentGrupper(
        oid: UUID,
        gruppeIder: List<UUID>,
    ): Set<UUID>
}
