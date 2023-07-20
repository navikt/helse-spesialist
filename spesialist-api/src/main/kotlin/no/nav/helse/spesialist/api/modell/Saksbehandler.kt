package no.nav.helse.spesialist.api.modell

import io.ktor.server.auth.jwt.JWTPrincipal
import java.util.UUID
import no.nav.helse.spesialist.api.modell.saksbehandling.hendelser.OverstyrtTidslinje
import no.nav.helse.spesialist.api.saksbehandler.SaksbehandlerDao
import no.nav.helse.spesialist.api.saksbehandler.SaksbehandlerDto

class Saksbehandler(
    private val epostadresse: String,
    private val oid: UUID,
    private val navn: String,
    private val ident: String
) {
    companion object {
        fun fraOnBehalfOfToken(jwtPrincipal: JWTPrincipal) = Saksbehandler(
            epostadresse = jwtPrincipal.payload.getClaim("preferred_username").asString(),
            oid = jwtPrincipal.payload.getClaim("oid").asString().let { UUID.fromString(it) },
            navn = jwtPrincipal.payload.getClaim("name").asString(),
            ident = jwtPrincipal.payload.getClaim("NAVident").asString(),
        )
    }

    private val observers = mutableListOf<SaksbehandlerObserver>()

    internal fun register(observer: SaksbehandlerObserver) {
        observers.add(observer)
    }

    internal fun ident(): String = ident
    internal fun oid(): UUID = oid

    fun persister(saksbehandlerDao: SaksbehandlerDao) {
        saksbehandlerDao.opprettSaksbehandler(oid = oid, navn = navn, epost = epostadresse, ident = ident)
    }

    internal fun håndter(hendelse: OverstyrtTidslinje) {
        val event = hendelse.byggEvent(oid, navn, epostadresse, ident)
        observers.forEach { it.tidslinjeOverstyrt(event.fødselsnummer, event) }
    }

    fun json() = mapOf(
        "epostaddresse" to epostadresse,
        "oid" to oid,
        "navn" to navn,
        "ident" to ident,
    )

    fun toDto() = SaksbehandlerDto(oid = oid, navn = navn, epost = epostadresse, ident = ident)

    override fun toString(): String = "epostadresse=$epostadresse, oid=$oid"

    override fun equals(other: Any?) = this === other || (
        other is Saksbehandler &&
        epostadresse == other.epostadresse &&
        navn == other.navn &&
        oid == other.oid &&
        ident == other.ident
    )

    override fun hashCode(): Int {
        var result = epostadresse.hashCode()
        result = 31 * result + oid.hashCode()
        result = 31 * result + navn.hashCode()
        result = 31 * result + ident.hashCode()
        return result
    }
}
