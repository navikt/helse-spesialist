package no.nav.helse.modell.stoppautomatiskbehandling

import no.nav.helse.db.SessionContext
import no.nav.helse.mediator.Kommandostarter
import no.nav.helse.mediator.meldinger.Personmelding
import no.nav.helse.modell.person.LegacyPerson
import java.time.LocalDateTime
import java.util.UUID

class VeilederStansMelding(
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
        person: LegacyPerson,
        kommandostarter: Kommandostarter,
        sessionContext: SessionContext,
    ) {
        kommandostarter { veilederStansBehandler(this@VeilederStansMelding) }
    }

    override fun fødselsnummer(): String = fødselsnummer

    override fun toJson(): String = json
}
