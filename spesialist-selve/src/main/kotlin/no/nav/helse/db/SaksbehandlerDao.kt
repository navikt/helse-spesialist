package no.nav.helse.db

import no.nav.helse.modell.saksbehandler.Saksbehandler
import no.nav.helse.modell.saksbehandler.Tilgangskontroll
import java.time.LocalDateTime
import java.util.UUID

interface SaksbehandlerDao {
    fun finnSaksbehandler(oid: UUID): SaksbehandlerFraDatabase?

    fun finnSaksbehandler(
        oid: UUID,
        tilgangskontroll: Tilgangskontroll,
    ): Saksbehandler?

    fun opprettEllerOppdater(
        oid: UUID,
        navn: String,
        epost: String,
        ident: String,
    ): Int

    fun oppdaterSistObservert(
        oid: UUID,
        sisteHandlingUtført: LocalDateTime = LocalDateTime.now(),
    ): Int
}
