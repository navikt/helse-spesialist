package no.nav.helse.modell.gosysoppgaver

import com.fasterxml.jackson.databind.JsonNode
import java.util.UUID
import no.nav.helse.mediator.GodkjenningMediator
import no.nav.helse.mediator.meldinger.Personmelding
import no.nav.helse.mediator.oppgave.OppgaveDao
import no.nav.helse.mediator.oppgave.OppgaveMediator
import no.nav.helse.modell.automatisering.Automatisering
import no.nav.helse.modell.automatisering.AutomatiseringForEksisterendeOppgaveCommand
import no.nav.helse.modell.automatisering.SettTidligereAutomatiseringInaktivCommand
import no.nav.helse.modell.kommando.Command
import no.nav.helse.modell.kommando.MacroCommand
import no.nav.helse.modell.oppgave.SjekkAtOppgaveFortsattErÅpenCommand
import no.nav.helse.modell.sykefraværstilfelle.Sykefraværstilfelle
import no.nav.helse.modell.utbetaling.Utbetaling
import no.nav.helse.rapids_rivers.JsonMessage

internal class GosysOppgaveEndret private constructor(
    override val id: UUID,
    private val fødselsnummer: String,
    private val json: String,
) : Personmelding {
    internal constructor(packet: JsonMessage): this(
        id = UUID.fromString(packet["@id"].asText()),
        fødselsnummer = packet["fødselsnummer"].asText(),
        json = packet.toJson()
    )

    internal constructor(jsonNode: JsonNode): this(
        id = UUID.fromString(jsonNode["@id"].asText()),
        fødselsnummer = jsonNode["fødselsnummer"].asText(),
        json = jsonNode.toString()
    )

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
    oppgaveMediator: OppgaveMediator,
    godkjenningMediator: GodkjenningMediator,
): MacroCommand() {
    override val commands: List<Command> = listOf(
        VurderÅpenGosysoppgave(
            hendelseId = id,
            aktørId = aktørId,
            åpneGosysOppgaverDao = åpneGosysOppgaverDao,
            oppgaveMediator = oppgaveMediator,
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
            oppgaveMediator = oppgaveMediator,
            utbetaling = utbetaling,
            periodetype = oppgavedataForAutomatisering.periodetype,
            sykefraværstilfelle = sykefraværstilfelle,
        )
    )

}
