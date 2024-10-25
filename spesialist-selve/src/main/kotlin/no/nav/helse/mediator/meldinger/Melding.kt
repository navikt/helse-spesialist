package no.nav.helse.mediator.meldinger

import kotliquery.TransactionalSession
import no.nav.helse.mediator.Kommandostarter
import no.nav.helse.modell.person.Person
import java.util.UUID

internal interface Melding {
    val id: UUID

    fun toJson(): String
}

internal interface Personmelding : Melding {
    fun behandle(
        person: Person,
        kommandostarter: Kommandostarter,
        transactionalSession: TransactionalSession,
    )

    fun fødselsnummer(): String
}

internal interface Vedtaksperiodemelding : Personmelding {
    fun vedtaksperiodeId(): UUID
}
