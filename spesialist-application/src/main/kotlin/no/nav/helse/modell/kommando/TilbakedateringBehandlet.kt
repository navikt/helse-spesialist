package no.nav.helse.modell.kommando

import no.nav.helse.db.SessionContext
import no.nav.helse.mediator.GodkjenningMediator
import no.nav.helse.mediator.Kommandostarter
import no.nav.helse.mediator.meldinger.Personmelding
import no.nav.helse.mediator.oppgave.OppgaveService
import no.nav.helse.modell.automatisering.Automatisering
import no.nav.helse.modell.automatisering.SettTidligereAutomatiseringInaktivCommand
import no.nav.helse.modell.automatisering.VurderAutomatiskInnvilgelse
import no.nav.helse.modell.utbetaling.Utbetaling
import no.nav.helse.modell.vedtaksperiode.GodkjenningsbehovData
import no.nav.helse.spesialist.application.Outbox
import no.nav.helse.spesialist.application.logg.loggInfo
import no.nav.helse.spesialist.domain.Identitetsnummer
import no.nav.helse.spesialist.domain.Periode
import no.nav.helse.spesialist.domain.SpleisBehandlingId
import no.nav.helse.spesialist.domain.oppgave.Oppgave
import tools.jackson.databind.JsonNode
import java.time.LocalDate
import java.util.*

class TilbakedateringBehandlet(
    override val id: UUID,
    private val fødselsnummer: String,
    val perioder: List<Periode>,
    private val json: String,
) : Personmelding {
    constructor(jsonNode: JsonNode) : this(
        id = UUID.fromString(jsonNode["@id"].asString()),
        fødselsnummer = jsonNode["fødselsnummer"].asString(),
        perioder =
            jsonNode["perioder"].toList().map {
                Periode(
                    fom = it["fom"].asString().let(LocalDate::parse),
                    tom = it["tom"].asString().let(LocalDate::parse),
                )
            },
        json = jsonNode.toString(),
    )

    override fun behandle(
        kommandostarter: Kommandostarter,
        sessionContext: SessionContext,
    ) {
        val identitetsnummer = Identitetsnummer.fraString(fødselsnummer)
        val vedtaksperioder =
            sessionContext.vedtaksperiodeRepository
                .finnAlleForPerson(identitetsnummer)
                .filterNot { it.forkastet }

        val gjeldendeBehandlingerSomOverlapper =
            vedtaksperioder
                .mapNotNull {
                    sessionContext.behandlingRepository.finnNyesteForVedtaksperiode(it.id)
                }.filter { it.overlapperMed(perioder) }

        val tilbakedateringsvarslerForGjeldendeBehandlinger =
            sessionContext.varselRepository
                .finnAktiveVarslerFor(gjeldendeBehandlingerSomOverlapper)
                .filter { it.kode == "RV_SØ_3" }

        tilbakedateringsvarslerForGjeldendeBehandlinger.forEach {
            it.deaktiver()
            sessionContext.varselRepository.lagre(it)
        }

        kommandostarter {
            val oppgave =
                sessionContext.oppgaveRepository.finnAktivForPerson(identitetsnummer)
            if (oppgave == null) {
                loggInfo("Ingen aktiv oppgave for personen, avslutter behandling av meldingen")
                return@kommandostarter null
            }
            tilbakedateringGodkjent(this@TilbakedateringBehandlet, oppgave, sessionContext)
        }
    }

    override fun fødselsnummer() = fødselsnummer

    override fun toJson(): String = json
}

internal class TilbakedateringGodkjentCommand(
    utbetaling: Utbetaling,
    automatisering: Automatisering,
    oppgave: Oppgave,
    oppgaveService: OppgaveService,
    godkjenningMediator: GodkjenningMediator,
    søknadsperioder: List<Periode>,
    godkjenningsbehov: GodkjenningsbehovData,
) : MacroCommand() {
    override val commands: List<Command> =
        listOf(
            VurderOmSøknadsperiodenOverlapperMedOppgave(oppgave, søknadsperioder),
            ikkesuspenderendeCommand("fjernTilbakedatertEgenskap") { sessionContext: SessionContext, _: Outbox ->
                oppgave.fjernTilbakedatert()
                sessionContext.oppgaveRepository.lagre(oppgave)
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
                godkjenningsbehov = godkjenningsbehov,
                spleisBehandlingId = SpleisBehandlingId(godkjenningsbehov.spleisBehandlingId),
            ),
        )
}
