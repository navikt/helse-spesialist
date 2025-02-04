package no.nav.helse.spesialist.modell

import no.nav.helse.spesialist.modell.ddd.Entity
import java.util.UUID

class Saksbehandler private constructor(
    id: UUID?,
    val navn: String,
    val epost: String,
    val ident: String,
) : Entity<UUID>(id) {
    object Factory {
        fun fraLagring(
            id: UUID,
            navn: String,
            epost: String,
            ident: String,
        ) = Saksbehandler(
            id = id,
            navn = navn,
            epost = epost,
            ident = ident,
        )
    }
}
