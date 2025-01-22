package no.nav.helse.db.api

import java.util.UUID

interface AbonnementDao {
    fun opprettAbonnement(
        saksbehandlerId: UUID,
        aktørId: String,
    )

    fun registrerSistekvensnummer(
        saksbehandlerIdent: UUID,
        sisteSekvensId: Int,
    ): Int
}
