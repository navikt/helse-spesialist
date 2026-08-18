package no.nav.helse.modell.stoppautomatiskbehandling

import no.nav.helse.db.SessionContext
import no.nav.helse.mediator.Kommandostarter
import no.nav.helse.mediator.meldinger.Personmelding
import no.nav.helse.modell.kommando.ikkesuspenderendeCommand
import no.nav.helse.modell.person.LegacyPerson
import no.nav.helse.spesialist.application.Outbox
import java.time.LocalDateTime
import java.util.*

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
    override fun behandleMedLegacyPerson(
        person: LegacyPerson,
        kommandostarter: Kommandostarter,
        sessionContext: SessionContext,
    ) {
        kommandostarter {
            ikkesuspenderendeCommand { sessionContext: SessionContext, _: Outbox ->
                VeilederStansMediator.Factory.veilederStansMediator(sessionContext).håndter(this@VeilederStansMelding)
            }
        }
    }

    override fun fødselsnummer(): String = fødselsnummer

    override fun toJson(): String = json
}
