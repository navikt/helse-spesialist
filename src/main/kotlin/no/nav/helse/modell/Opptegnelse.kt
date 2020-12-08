package no.nav.helse.modell

import no.nav.helse.modell.abonnement.OpptegnelseType

data class Opptegnelse(
    val sekvensnummer: Int,
    val aktørId: Long,
    val payload: String,
    val type: OpptegnelseType
)
