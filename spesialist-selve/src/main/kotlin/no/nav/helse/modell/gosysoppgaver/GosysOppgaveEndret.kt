package no.nav.helse.modell.gosysoppgaver

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.mediator.GodkjenningMediator
import no.nav.helse.mediator.meldinger.PersonmeldingOld
import no.nav.helse.mediator.oppgave.OppgaveDao
import no.nav.helse.mediator.oppgave.OppgaveService
import no.nav.helse.modell.automatisering.Automatisering
import no.nav.helse.modell.automatisering.AutomatiseringForEksisterendeOppgaveCommand
import no.nav.helse.modell.automatisering.SettTidligereAutomatiseringInaktivCommand
import no.nav.helse.modell.kommando.Command
import no.nav.helse.modell.kommando.MacroCommand
import no.nav.helse.modell.oppgave.SjekkAtOppgaveFortsattErÅpenCommand
import no.nav.helse.modell.sykefraværstilfelle.Sykefraværstilfelle
import no.nav.helse.modell.utbetaling.Utbetaling
import no.nav.helse.modell.vedtaksperiode.GenerasjonRepository
import no.nav.helse.rapids_rivers.JsonMessage
import java.util.UUID

internal class GosysOppgaveEndret private constructor(
    override val id: UUID,
    private val fødselsnummer: String,
    private val json: String,
) : PersonmeldingOld {
    internal constructor(packet: JsonMessage) : this(
        id = UUID.fromString(packet["@id"].asText()),
        fødselsnummer = packet["fødselsnummer"].asText(),
        json = packet.toJson(),
    )

    internal constructor(jsonNode: JsonNode) : this(
        id = UUID.fromString(jsonNode["@id"].asText()),
        fødselsnummer = jsonNode["fødselsnummer"].asText(),
        json = jsonNode.toString(),
    )

    internal lateinit var oppgavedataForAutomatisering: OppgaveDataForAutomatisering
        private set

    internal fun oppgavedataForAutomatisering(oppgavedataForAutomatisering: OppgaveDataForAutomatisering) {
        this.oppgavedataForAutomatisering = oppgavedataForAutomatisering
    }

    override fun fødselsnummer() = fødselsnummer

    override fun toJson(): String = json
}

internal class GosysOppgaveEndretCommand(
    id: UUID,
    fødselsnummer: String,
    aktørId: String,
    utbetaling: Utbetaling,
    sykefraværstilfelle: Sykefraværstilfelle,
    harTildeltOppgave: Boolean,
    oppgavedataForAutomatisering: OppgaveDataForAutomatisering,
    automatisering: Automatisering,
    åpneGosysOppgaverDao: ÅpneGosysOppgaverDao,
    oppgaveDao: OppgaveDao,
    oppgaveService: OppgaveService,
    generasjonRepository: GenerasjonRepository,
    godkjenningMediator: GodkjenningMediator,
    spleisBehandlingId: UUID?,
    organisasjonsnummer: String,
) : MacroCommand() {
    override val commands: List<Command> =
        listOf(
            VurderÅpenGosysoppgave(
                hendelseId = id,
                aktørId = aktørId,
                åpneGosysOppgaverDao = åpneGosysOppgaverDao,
                generasjonRepository = generasjonRepository,
                vedtaksperiodeId = oppgavedataForAutomatisering.vedtaksperiodeId,
                sykefraværstilfelle = sykefraværstilfelle,
                harTildeltOppgave = harTildeltOppgave,
            ),
            SjekkAtOppgaveFortsattErÅpenCommand(fødselsnummer = fødselsnummer, oppgaveDao = oppgaveDao),
            SettTidligereAutomatiseringInaktivCommand(
                vedtaksperiodeId = oppgavedataForAutomatisering.vedtaksperiodeId,
                hendelseId = oppgavedataForAutomatisering.hendelseId,
                automatisering = automatisering,
            ),
            AutomatiseringForEksisterendeOppgaveCommand(
                fødselsnummer = fødselsnummer,
                vedtaksperiodeId = oppgavedataForAutomatisering.vedtaksperiodeId,
                hendelseId = oppgavedataForAutomatisering.hendelseId,
                automatisering = automatisering,
                godkjenningsbehovJson = oppgavedataForAutomatisering.godkjenningsbehovJson,
                godkjenningMediator = godkjenningMediator,
                oppgaveService = oppgaveService,
                utbetaling = utbetaling,
                periodetype = oppgavedataForAutomatisering.periodetype,
                sykefraværstilfelle = sykefraværstilfelle,
                spleisBehandlingId = spleisBehandlingId,
                organisasjonsnummer = organisasjonsnummer,
            ),
        )
}
