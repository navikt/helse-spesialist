package no.nav.helse.db

import no.nav.helse.spesialist.domain.Saksbehandler
import java.time.LocalDateTime
import java.util.UUID

interface SaksbehandlerDao {
    fun hent(ident: String): Saksbehandler?

    fun hentAlleAktiveSisteTreMnder(): List<Saksbehandler>

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
