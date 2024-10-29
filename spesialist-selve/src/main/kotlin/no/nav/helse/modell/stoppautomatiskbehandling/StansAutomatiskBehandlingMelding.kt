package no.nav.helse.modell.stoppautomatiskbehandling

import kotliquery.TransactionalSession
import no.nav.helse.mediator.Kommandostarter
import no.nav.helse.mediator.meldinger.Personmelding
import no.nav.helse.modell.person.Person
import java.time.LocalDateTime
import java.util.UUID

internal class StansAutomatiskBehandlingMelding(
    override val id: UUID,
    private val fødselsnummer: String,
    val kilde: String,
    val status: String,
    val årsaker: Set<StoppknappÅrsak>,
    val opprettet: LocalDateTime,
    val originalMelding: String,
    private val json: String,
) : Personmelding {
    override fun behandle(
        person: Person,
        kommandostarter: Kommandostarter,
        transactionalSession: TransactionalSession,
    ) {
        kommandostarter { stansAutomatiskBehandling(this@StansAutomatiskBehandlingMelding, transactionalSession) }
    }

    override fun fødselsnummer(): String = fødselsnummer

    override fun toJson(): String = json
}
