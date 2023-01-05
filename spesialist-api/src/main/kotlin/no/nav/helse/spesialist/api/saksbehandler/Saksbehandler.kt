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

    private fun json() = mapOf(
        "epostaddresse" to epostadresse,
        "oid" to oid,
        "navn" to navn,
        "ident" to ident,
    )

    fun toDto() = SaksbehandlerDto(oid = oid, navn = navn, epost = epostadresse, ident = ident)

    override fun equals(other: Any?): Boolean =
        this === other || (other is Saksbehandler &&
                epostadresse == other.epostadresse &&
                oid == other.oid &&
                navn == other.navn &&
                ident == other.ident)

    override fun hashCode(): Int {
        var result = epostadresse.hashCode()
        result = 31 * result + oid.hashCode()
        result = 31 * result + navn.hashCode()
        result = 31 * result + ident.hashCode()
        result = 31 * result + observere.hashCode()
        return result
    }
}
