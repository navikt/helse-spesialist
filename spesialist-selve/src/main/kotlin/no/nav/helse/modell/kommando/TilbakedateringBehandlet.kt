package no.nav.helse.modell.kommando

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.mediator.GodkjenningMediator
import no.nav.helse.mediator.Kommandostarter
import no.nav.helse.mediator.meldinger.Personmelding
import no.nav.helse.mediator.oppgave.OppgaveService
import no.nav.helse.modell.automatisering.Automatisering
import no.nav.helse.modell.automatisering.AutomatiseringForEksisterendeOppgaveCommand
import no.nav.helse.modell.automatisering.SettTidligereAutomatiseringInaktivCommand
import no.nav.helse.modell.gosysoppgaver.OppgaveDataForAutomatisering
import no.nav.helse.modell.person.Person
import no.nav.helse.modell.sykefraværstilfelle.Sykefraværstilfelle
import no.nav.helse.modell.utbetaling.Utbetaling
import no.nav.helse.modell.vedtaksperiode.GodkjenningsbehovData
import no.nav.helse.modell.vedtaksperiode.Periode
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.asLocalDate
import java.util.UUID

internal class TilbakedateringBehandlet private constructor(
    override val id: UUID,
    private val fødselsnummer: String,
    val perioder: List<Periode>,
    private val json: String,
) : Personmelding {
    internal constructor(packet: JsonMessage) : this(
        id = UUID.fromString(packet["@id"].asText()),
        fødselsnummer = packet["fødselsnummer"].asText(),
        perioder =
            packet["perioder"].map {
                Periode(it["fom"].asLocalDate(), it["tom"].asLocalDate())
            },
        json = packet.toJson(),
    )
    internal constructor(jsonNode: JsonNode) : this(
        id = UUID.fromString(jsonNode["@id"].asText()),
        fødselsnummer = jsonNode["fødselsnummer"].asText(),
        perioder =
            jsonNode["perioder"].map {
                Periode(it["fom"].asLocalDate(), it["tom"].asLocalDate())
            },
        json = jsonNode.toString(),
    )

    override fun behandle(
        person: Person,
        kommandostarter: Kommandostarter,
    ) {
        person.behandleTilbakedateringBehandlet(perioder)
        kommandostarter {
            val oppgaveDataForAutomatisering = finnOppgavedata(fødselsnummer) ?: return@kommandostarter null
            tilbakedateringGodkjent(this@TilbakedateringBehandlet, person, oppgaveDataForAutomatisering)
        }
    }

    override fun fødselsnummer() = fødselsnummer

    override fun toJson(): String = json
}

internal class TilbakedateringGodkjentCommand(
    sykefraværstilfelle: Sykefraværstilfelle,
    utbetaling: Utbetaling,
    automatisering: Automatisering,
    oppgaveDataForAutomatisering: OppgaveDataForAutomatisering,
    oppgaveService: OppgaveService,
    godkjenningMediator: GodkjenningMediator,
    søknadsperioder: List<Periode>,
    godkjenningsbehov: GodkjenningsbehovData,
) : MacroCommand() {
    override val commands: List<Command> =
        listOf(
            VurderOmSøknadsperiodenOverlapperMedOppgave(oppgaveDataForAutomatisering, søknadsperioder),
            ikkesuspenderendeCommand("fjernTilbakedatertEgenskap") {
                oppgaveService.fjernTilbakedatert(godkjenningsbehov.vedtaksperiodeId)
            },
            SettTidligereAutomatiseringInaktivCommand(
                vedtaksperiodeId = godkjenningsbehov.vedtaksperiodeId,
                hendelseId = godkjenningsbehov.id,
                automatisering = automatisering,
            ),
            AutomatiseringForEksisterendeOppgaveCommand(
                automatisering = automatisering,
                godkjenningMediator = godkjenningMediator,
                oppgaveService = oppgaveService,
                utbetaling = utbetaling,
                sykefraværstilfelle = sykefraværstilfelle,
                godkjenningsbehov = godkjenningsbehov,
            ),
        )
}
