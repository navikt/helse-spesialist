package no.nav.helse.modell.kommando

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.db.AutomatiseringDao
import no.nav.helse.db.SessionContext
import no.nav.helse.mediator.GodkjenningMediator
import no.nav.helse.mediator.Kommandostarter
import no.nav.helse.mediator.meldinger.Personmelding
import no.nav.helse.mediator.oppgave.OppgaveService
import no.nav.helse.modell.automatisering.Automatisering
import no.nav.helse.modell.automatisering.SettTidligereAutomatiseringInaktivCommand
import no.nav.helse.modell.automatisering.VurderAutomatiskInnvilgelse
import no.nav.helse.modell.gosysoppgaver.OppgaveDataForAutomatisering
import no.nav.helse.modell.person.Person
import no.nav.helse.modell.person.Sykefraværstilfelle
import no.nav.helse.modell.person.vedtaksperiode.Periode
import no.nav.helse.modell.utbetaling.Utbetaling
import no.nav.helse.modell.vedtaksperiode.GodkjenningsbehovData
import java.time.LocalDate
import java.util.UUID

class TilbakedateringBehandlet(
    override val id: UUID,
    private val fødselsnummer: String,
    val perioder: List<Periode>,
    private val json: String,
) : Personmelding {
    constructor(jsonNode: JsonNode) : this(
        id = UUID.fromString(jsonNode["@id"].asText()),
        fødselsnummer = jsonNode["fødselsnummer"].asText(),
        perioder =
            jsonNode["perioder"].map {
                Periode(
                    fom = it["fom"].asText().let(LocalDate::parse),
                    tom = it["tom"].asText().let(LocalDate::parse),
                )
            },
        json = jsonNode.toString(),
    )

    override fun behandle(
        person: Person,
        kommandostarter: Kommandostarter,
        sessionContext: SessionContext,
    ) {
        person.behandleTilbakedateringBehandlet(perioder)
        kommandostarter {
            val oppgaveDataForAutomatisering =
                finnOppgavedata(fødselsnummer, sessionContext) ?: return@kommandostarter null
            tilbakedateringGodkjent(this@TilbakedateringBehandlet, person, oppgaveDataForAutomatisering, sessionContext)
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
    automatiseringDao: AutomatiseringDao,
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
            VurderAutomatiskInnvilgelse(
                automatisering = automatisering,
                godkjenningMediator = godkjenningMediator,
                oppgaveService = oppgaveService,
                utbetaling = utbetaling,
                sykefraværstilfelle = sykefraværstilfelle,
                godkjenningsbehov = godkjenningsbehov,
                automatiseringDao = automatiseringDao,
            ),
        )
}
