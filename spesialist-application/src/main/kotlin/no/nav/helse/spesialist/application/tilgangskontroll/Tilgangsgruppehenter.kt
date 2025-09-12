package no.nav.helse.spesialist.application.tilgangskontroll

import java.util.UUID

interface Tilgangsgruppehenter {
    suspend fun hentTilgangsgrupper(
        oid: UUID,
        gruppeIder: List<UUID>,
    ): Set<UUID>

    suspend fun hentTilgangsgrupper(oid: UUID): Set<Tilgangsgruppe>
}
