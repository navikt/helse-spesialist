package no.nav.helse.db

import no.nav.helse.modell.saksbehandler.Saksbehandler
import no.nav.helse.modell.saksbehandler.Tilgangskontroll
import java.util.UUID

interface SaksbehandlerRepository {
    fun finnSaksbehandler(oid: UUID): SaksbehandlerFraDatabase?

    fun finnSaksbehandler(
        oid: UUID,
        tilgangskontroll: Tilgangskontroll,
    ): Saksbehandler?
}
