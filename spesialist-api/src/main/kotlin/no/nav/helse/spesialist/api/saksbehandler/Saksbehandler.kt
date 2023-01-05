package no.nav.helse.spesialist.api.saksbehandler

import io.ktor.server.auth.jwt.JWTPrincipal
import java.util.UUID

class Saksbehandler(
    private val epostadresse: String,
    private val oid: UUID,
    private val navn: String,
    private val ident: String
) {
    private val observere: MutableList<SaksbehandlerObserver> = mutableListOf()
    companion object {
        fun fraOnBehalfOfToken(jwtPrincipal: JWTPrincipal) = Saksbehandler(
            epostadresse = jwtPrincipal.payload.getClaim("preferred_username").asString(),
            oid = jwtPrincipal.payload.getClaim("oid").asString().let { UUID.fromString(it) },
            navn = jwtPrincipal.payload.getClaim("name").asString(),
            ident = jwtPrincipal.payload.getClaim("NAVident").asString(),
        )
    }

    internal fun register(observer: SaksbehandlerObserver) {
        observere.add(observer)
    }

    internal fun annuller(
        aktørId: String,
        fødselsnummer: String,
        organisasjonsnummer: String,
        fagsystemId: String,
        begrunnelser: List<String>,
        kommentar: String?
    ) {
        observere.notify {
            annulleringEvent(aktørId, fødselsnummer, organisasjonsnummer, json(), fagsystemId, begrunnelser, kommentar)
        }
    }

    private fun List<SaksbehandlerObserver>.notify(hendelse: SaksbehandlerObserver.() -> Unit) {
        forEach { it.hendelse() }
    }

    fun json() = mapOf(
        "epostaddresse" to epostadresse,
        "oid" to oid,
        "navn" to navn,
        "ident" to ident,
    )

    fun toDto() = SaksbehandlerDto(oid = oid, navn = navn, epost = epostadresse, ident = ident)
}
