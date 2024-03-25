package no.nav.helse.mediator.meldinger

import java.util.UUID
import no.nav.helse.mediator.Kommandofabrikk
import no.nav.helse.modell.person.Person

internal interface Melding {
    val id: UUID
    fun toJson(): String
}

internal interface Personmelding: PersonmeldingOld {
    fun behandle(person: Person, kommandofabrikk: Kommandofabrikk)
}
@Deprecated("Dette skal erstattes av ny løype for å behandle meldinger")
internal interface PersonmeldingOld: Melding {
    fun fødselsnummer(): String
}

internal interface Vedtaksperiodemelding: Personmelding {
    fun vedtaksperiodeId(): UUID
}
internal interface VedtaksperiodemeldingOld: PersonmeldingOld {
    fun vedtaksperiodeId(): UUID
}