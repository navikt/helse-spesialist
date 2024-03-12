package no.nav.helse.mediator.meldinger

import java.util.UUID
import no.nav.helse.mediator.Kommandofabrikk
import no.nav.helse.modell.person.Person

internal interface Melding {
    val id: UUID
    fun toJson(): String
}

internal interface Personmelding: Melding {
    fun fødselsnummer(): String
    fun behandle(person: Person, kommandofabrikk: Kommandofabrikk) {}
}

internal interface Vedtaksperiodemelding: Personmelding {
    fun vedtaksperiodeId(): UUID
}