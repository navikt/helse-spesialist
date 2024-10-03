package no.nav.helse.mediator.meldinger

import kotliquery.TransactionalSession
import no.nav.helse.mediator.Kommandostarter
import no.nav.helse.modell.person.Person
import java.util.UUID
import javax.naming.OperationNotSupportedException

internal interface Melding {
    val id: UUID

    fun toJson(): String
}

internal interface Personmelding : Melding {
    fun skalKjøresTransaksjonelt(): Boolean = false

    fun behandle(
        person: Person,
        kommandostarter: Kommandostarter,
    )

    fun transaksjonellBehandle(
        person: Person,
        kommandostarter: Kommandostarter,
        transactionalSession: TransactionalSession,
    ): Unit = throw OperationNotSupportedException()

    fun fødselsnummer(): String
}

internal interface Vedtaksperiodemelding : Personmelding {
    fun vedtaksperiodeId(): UUID
}
