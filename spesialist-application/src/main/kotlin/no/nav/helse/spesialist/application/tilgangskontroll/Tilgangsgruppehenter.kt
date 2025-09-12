package no.nav.helse.spesialist.application.tilgangskontroll

import no.nav.helse.spesialist.domain.tilgangskontroll.Tilgangsgruppe
import java.util.UUID

interface Tilgangsgruppehenter {
    suspend fun hentTilgangsgrupper(
        oid: UUID,
        gruppeIder: List<UUID>,
    ): Set<UUID>

    suspend fun hentTilgangsgrupper(oid: UUID): Set<Tilgangsgruppe>
}
