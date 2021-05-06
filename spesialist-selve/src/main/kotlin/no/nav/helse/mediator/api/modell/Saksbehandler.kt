package no.nav.helse.mediator.api.modell

import io.ktor.auth.jwt.*
import no.nav.helse.saksbehandler.SaksbehandlerDao
import java.util.*

internal class Saksbehandler(
    private val epostadresse: String,
    private val oid: UUID,
    private val navn: String
) {
    companion object {
        fun fraOnBehalfOfToken(jwtPrincipal: JWTPrincipal) = Saksbehandler(
            epostadresse = jwtPrincipal.payload.getClaim("preferred_username").asString(),
            oid = jwtPrincipal.payload.getClaim("oid").asString().let { UUID.fromString(it) },
            navn = jwtPrincipal.payload.getClaim("name").asString(),
        )
    }

    internal fun persister(saksbehandlerDao: SaksbehandlerDao) {
        saksbehandlerDao.opprettSaksbehandler(oid = oid, navn = navn, epost = epostadresse)
    }

    internal fun json() = mapOf(
        "epostaddresse" to epostadresse,
        "oid" to oid,
        "navn" to navn,
    )
}
