package no.nav.helse.mediator.meldinger

import no.nav.helse.db.SessionContext
import no.nav.helse.mediator.Kommandostarter
import no.nav.helse.modell.person.Person
import java.util.UUID

interface Melding {
    val id: UUID

    fun toJson(): String
}

interface Personmelding : Melding {
    fun behandle(
        person: Person,
        kommandostarter: Kommandostarter,
        sessionContext: SessionContext,
    )

    fun fødselsnummer(): String
}

interface Vedtaksperiodemelding : Personmelding {
    fun vedtaksperiodeId(): UUID
}
