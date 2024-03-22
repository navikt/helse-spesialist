package no.nav.helse.modell.kommando

import java.time.LocalDate
import java.util.UUID
import no.nav.helse.mediator.GodkjenningMediator
import no.nav.helse.mediator.meldinger.Personmelding
import no.nav.helse.mediator.oppgave.OppgaveMediator
import no.nav.helse.modell.automatisering.Automatisering
import no.nav.helse.modell.automatisering.AutomatiseringForEksisterendeOppgaveCommand
import no.nav.helse.modell.automatisering.SettTidligereAutomatiseringInaktivCommand
import no.nav.helse.modell.gosysoppgaver.OppgaveDataForAutomatisering
import no.nav.helse.modell.sykefraværstilfelle.Sykefraværstilfelle
import no.nav.helse.modell.utbetaling.Utbetaling
import no.nav.helse.modell.vedtaksperiode.Periode
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.asLocalDate

internal class TilbakedateringBehandlet private constructor(
    override val id: UUID,
    private val fødselsnummer: String,
    val syketilfelleStartdato: LocalDate,
    val perioder: List<Periode>,
    private val json: String
) : Personmelding {
    internal constructor(packet: JsonMessage): this(
        id = UUID.fromString(packet["@id"].asText()),
        fødselsnummer = packet["fødselsnummer"].asText(),
        syketilfelleStartdato = packet["syketilfelleStartDato"].asLocalDate(),
        perioder = packet["perioder"].map {
            Periode(it["fom"].asLocalDate(), it["tom"].asLocalDate())
        },
        json = packet.toJson()
    )
    override fun fødselsnummer() = fødselsnummer
    override fun toJson(): String = json
}

internal class TilbakedateringGodkjentCommand(
    fødselsnummer: String,
    sykefraværstilfelle: Sykefraværstilfelle,
    utbetaling: Utbetaling,
    automatisering: Automatisering,
    oppgaveDataForAutomatisering: OppgaveDataForAutomatisering,
    oppgaveMediator: OppgaveMediator,
    godkjenningMediator: GodkjenningMediator,
): MacroCommand() {
    override val commands: List<Command> = listOf(
        ikkesuspenderendeCommand("fjernTilbakedatertEgenskap") {
            oppgaveMediator.fjernTilbakedatert(oppgaveDataForAutomatisering.vedtaksperiodeId)
        },
        SettTidligereAutomatiseringInaktivCommand(
            vedtaksperiodeId = oppgaveDataForAutomatisering.vedtaksperiodeId,
            hendelseId = oppgaveDataForAutomatisering.hendelseId,
            automatisering = automatisering,
        ),
        AutomatiseringForEksisterendeOppgaveCommand(
            fødselsnummer = fødselsnummer,
            vedtaksperiodeId = oppgaveDataForAutomatisering.vedtaksperiodeId,
            hendelseId = oppgaveDataForAutomatisering.hendelseId,
            automatisering = automatisering,
            godkjenningsbehovJson = oppgaveDataForAutomatisering.godkjenningsbehovJson,
            godkjenningMediator = godkjenningMediator,
            oppgaveMediator = oppgaveMediator,
            utbetaling = utbetaling,
            periodetype = oppgaveDataForAutomatisering.periodetype,
            sykefraværstilfelle = sykefraværstilfelle,
        )
    )
}
