package no.nav.helse.mediator.meldinger

import java.util.UUID

internal interface Melding {
    val id: UUID
    fun toJson(): String
}

internal interface Personmelding: Melding {
    fun fødselsnummer(): String
}

internal interface Vedtaksperiodemelding: Personmelding {
    fun vedtaksperiodeId(): UUID
}