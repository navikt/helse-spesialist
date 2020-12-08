package no.nav.helse.modell

import java.util.*

data class Abonnement(
    val saksbehandlerId: UUID,
    val aktørId: Long,
    val siste_sekvensnummer: Int?
)
