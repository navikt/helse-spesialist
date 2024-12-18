package no.nav.helse.modell.gosysoppgaver

import com.fasterxml.jackson.databind.JsonNode
import kotliquery.TransactionalSession
import no.nav.helse.db.AutomatiseringRepository
import no.nav.helse.db.OppgaveDao
import no.nav.helse.db.ÅpneGosysOppgaverRepository
import no.nav.helse.mediator.GodkjenningMediator
import no.nav.helse.mediator.Kommandostarter
import no.nav.helse.mediator.meldinger.Personmelding
import no.nav.helse.mediator.oppgave.OppgaveService
import no.nav.helse.modell.automatisering.Automatisering
import no.nav.helse.modell.automatisering.SettTidligereAutomatiseringInaktivCommand
import no.nav.helse.modell.automatisering.VurderAutomatiskInnvilgelse
import no.nav.helse.modell.kommando.Command
import no.nav.helse.modell.kommando.MacroCommand
import no.nav.helse.modell.oppgave.SjekkAtOppgaveFortsattErÅpenCommand
import no.nav.helse.modell.person.Person
import no.nav.helse.modell.person.Sykefraværstilfelle
import no.nav.helse.modell.utbetaling.Utbetaling
import no.nav.helse.modell.vedtaksperiode.GodkjenningsbehovData
import java.util.UUID

internal class GosysOppgaveEndret(
    override val id: UUID,
    private val fødselsnummer: String,
    private val json: String,
) : Personmelding {
    internal constructor(jsonNode: JsonNode) : this(
        id = UUID.fromString(jsonNode["@id"].asText()),
        fødselsnummer = jsonNode["fødselsnummer"].asText(),
        json = jsonNode.toString(),
    )

    override fun behandle(
        person: Person,
        kommandostarter: Kommandostarter,
        transactionalSession: TransactionalSession,
    ) {
        kommandostarter {
            val oppgaveDataForAutomatisering =
                finnOppgavedata(fødselsnummer, transactionalSession) ?: return@kommandostarter null
            gosysOppgaveEndret(person, oppgaveDataForAutomatisering, transactionalSession)
        }
    }

    override fun fødselsnummer() = fødselsnummer

    override fun toJson(): String = json
}

internal class GosysOppgaveEndretCommand(
    utbetaling: Utbetaling,
    sykefraværstilfelle: Sykefraværstilfelle,
    harTildeltOppgave: Boolean,
    oppgavedataForAutomatisering: OppgaveDataForAutomatisering,
    automatisering: Automatisering,
    åpneGosysOppgaverRepository: ÅpneGosysOppgaverRepository,
    oppgaveDao: OppgaveDao,
    oppgaveService: OppgaveService,
    godkjenningMediator: GodkjenningMediator,
    godkjenningsbehov: GodkjenningsbehovData,
    automatiseringRepository: AutomatiseringRepository,
) : MacroCommand() {
    override val commands: List<Command> =
        listOf(
            VurderÅpenGosysoppgave(
                åpneGosysOppgaverRepository = åpneGosysOppgaverRepository,
                vedtaksperiodeId = oppgavedataForAutomatisering.vedtaksperiodeId,
                sykefraværstilfelle = sykefraværstilfelle,
                harTildeltOppgave = harTildeltOppgave,
                oppgaveService = oppgaveService,
            ),
            SjekkAtOppgaveFortsattErÅpenCommand(
                fødselsnummer = godkjenningsbehov.fødselsnummer,
                oppgaveDao = oppgaveDao,
            ),
            SettTidligereAutomatiseringInaktivCommand(
                vedtaksperiodeId = oppgavedataForAutomatisering.vedtaksperiodeId,
                hendelseId = oppgavedataForAutomatisering.hendelseId,
                automatisering = automatisering,
            ),
            VurderAutomatiskInnvilgelse(
                automatisering = automatisering,
                godkjenningMediator = godkjenningMediator,
                oppgaveService = oppgaveService,
                utbetaling = utbetaling,
                sykefraværstilfelle = sykefraværstilfelle,
                godkjenningsbehov = godkjenningsbehov,
                automatiseringRepository = automatiseringRepository,
            ),
        )
}
