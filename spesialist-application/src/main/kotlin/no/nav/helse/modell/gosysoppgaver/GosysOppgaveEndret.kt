package no.nav.helse.modell.gosysoppgaver

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.db.AutomatiseringDao
import no.nav.helse.db.OppgaveDao
import no.nav.helse.db.SessionContext
import no.nav.helse.db.ÅpneGosysOppgaverDao
import no.nav.helse.mediator.GodkjenningMediator
import no.nav.helse.mediator.Kommandostarter
import no.nav.helse.mediator.meldinger.Personmelding
import no.nav.helse.mediator.oppgave.OppgaveService
import no.nav.helse.modell.automatisering.Automatisering
import no.nav.helse.modell.automatisering.SettTidligereAutomatiseringInaktivCommand
import no.nav.helse.modell.automatisering.VurderAutomatiskInnvilgelse
import no.nav.helse.modell.kommando.Command
import no.nav.helse.modell.kommando.MacroCommand
import no.nav.helse.modell.oppgave.Oppgave
import no.nav.helse.modell.oppgave.SjekkAtOppgaveFortsattErÅpenCommand
import no.nav.helse.modell.person.LegacyPerson
import no.nav.helse.modell.person.Sykefraværstilfelle
import no.nav.helse.modell.utbetaling.Utbetaling
import no.nav.helse.modell.vedtaksperiode.GodkjenningsbehovData
import no.nav.helse.spesialist.application.VedtakRepository
import no.nav.helse.spesialist.domain.Identitetsnummer
import java.util.UUID

class GosysOppgaveEndret(
    override val id: UUID,
    private val fødselsnummer: String,
    private val json: String,
) : Personmelding {
    constructor(jsonNode: JsonNode) : this(
        id = UUID.fromString(jsonNode["@id"].asText()),
        fødselsnummer = jsonNode["fødselsnummer"].asText(),
        json = jsonNode.toString(),
    )

    override fun behandle(
        person: LegacyPerson,
        kommandostarter: Kommandostarter,
        sessionContext: SessionContext,
    ) {
        val identitetsnummer = Identitetsnummer.fraString(person.fødselsnummer)
        kommandostarter {
            val oppgave = sessionContext.oppgaveRepository.finnAktivForPerson(identitetsnummer) ?: return@kommandostarter null
            gosysOppgaveEndret(person, oppgave, sessionContext)
        }
    }

    override fun fødselsnummer() = fødselsnummer

    override fun toJson(): String = json
}

internal class GosysOppgaveEndretCommand(
    utbetaling: Utbetaling,
    sykefraværstilfelle: Sykefraværstilfelle,
    harTildeltOppgave: Boolean,
    oppgave: Oppgave,
    automatisering: Automatisering,
    åpneGosysOppgaverDao: ÅpneGosysOppgaverDao,
    oppgaveDao: OppgaveDao,
    oppgaveService: OppgaveService,
    godkjenningMediator: GodkjenningMediator,
    godkjenningsbehov: GodkjenningsbehovData,
    automatiseringDao: AutomatiseringDao,
    vedtakRepository: VedtakRepository,
) : MacroCommand() {
    override val commands: List<Command> =
        listOf(
            VurderÅpenGosysoppgave(
                åpneGosysOppgaverDao = åpneGosysOppgaverDao,
                vedtaksperiodeId = oppgave.vedtaksperiodeId,
                sykefraværstilfelle = sykefraværstilfelle,
                harTildeltOppgave = harTildeltOppgave,
                oppgaveService = oppgaveService,
            ),
            SjekkAtOppgaveFortsattErÅpenCommand(
                fødselsnummer = godkjenningsbehov.fødselsnummer,
                oppgaveDao = oppgaveDao,
            ),
            SettTidligereAutomatiseringInaktivCommand(
                vedtaksperiodeId = oppgave.vedtaksperiodeId,
                hendelseId = oppgave.godkjenningsbehovId,
                automatisering = automatisering,
            ),
            VurderAutomatiskInnvilgelse(
                automatisering = automatisering,
                godkjenningMediator = godkjenningMediator,
                oppgaveService = oppgaveService,
                utbetaling = utbetaling,
                sykefraværstilfelle = sykefraværstilfelle,
                godkjenningsbehov = godkjenningsbehov,
                automatiseringDao = automatiseringDao,
                vedtakRepository = vedtakRepository,
            ),
        )
}
