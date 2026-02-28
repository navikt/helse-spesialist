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
import no.nav.helse.modell.oppgave.Oppgave
import no.nav.helse.modell.person.LegacyPerson
import no.nav.helse.modell.person.Sykefraværstilfelle
import no.nav.helse.modell.utbetaling.Utbetaling
import no.nav.helse.modell.vedtaksperiode.GodkjenningsbehovData
import no.nav.helse.spesialist.application.VedtakRepository
import no.nav.helse.spesialist.application.logg.loggInfo
import no.nav.helse.spesialist.domain.Identitetsnummer
import no.nav.helse.spesialist.domain.Periode
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
        person: LegacyPerson,
        kommandostarter: Kommandostarter,
        sessionContext: SessionContext,
    ) {
        val identitetsnummer = Identitetsnummer.fraString(person.fødselsnummer)
        person.behandleTilbakedateringBehandlet(perioder)
        kommandostarter {
            val oppgave =
                sessionContext.oppgaveRepository.finnAktivForPerson(identitetsnummer)
            if (oppgave == null) {
                loggInfo("Ingen aktiv oppgave for personen, avslutter behandling av meldingen")
                return@kommandostarter null
            }
            tilbakedateringGodkjent(this@TilbakedateringBehandlet, person, oppgave, sessionContext)
        }
    }

    override fun fødselsnummer() = fødselsnummer

    override fun toJson(): String = json
}

internal class TilbakedateringGodkjentCommand(
    sykefraværstilfelle: Sykefraværstilfelle,
    utbetaling: Utbetaling,
    automatisering: Automatisering,
    oppgave: Oppgave,
    oppgaveService: OppgaveService,
    godkjenningMediator: GodkjenningMediator,
    søknadsperioder: List<Periode>,
    godkjenningsbehov: GodkjenningsbehovData,
    automatiseringDao: AutomatiseringDao,
    vedtakRepository: VedtakRepository,
    sessionContext: SessionContext,
) : MacroCommand() {
    override val commands: List<Command> =
        listOf(
            VurderOmSøknadsperiodenOverlapperMedOppgave(sessionContext, oppgave, søknadsperioder),
            ikkesuspenderendeCommand("fjernTilbakedatertEgenskap") {
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
                sykefraværstilfelle = sykefraværstilfelle,
                godkjenningsbehov = godkjenningsbehov,
                automatiseringDao = automatiseringDao,
                vedtakRepository = vedtakRepository,
            ),
        )
}
