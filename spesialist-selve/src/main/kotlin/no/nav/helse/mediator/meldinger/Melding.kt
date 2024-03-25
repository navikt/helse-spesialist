package no.nav.helse.mediator.meldinger

import java.util.UUID
import no.nav.helse.mediator.Kommandofabrikk
import no.nav.helse.modell.person.Person

internal interface Melding {
    val id: UUID
    fun toJson(): String
}
@Deprecated("Dette skal erstattes av ny løype for å behandle meldinger")
internal interface PersonmeldingOld: Melding {
    fun fødselsnummer(): String
    fun behandle(person: Person, kommandofabrikk: Kommandofabrikk) {}
}

internal interface VedtaksperiodemeldingOld: PersonmeldingOld {
    fun vedtaksperiodeId(): UUID
}