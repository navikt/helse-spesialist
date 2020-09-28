package no.nav.helse.modell

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.module.kotlin.convertValue
import com.fasterxml.jackson.module.kotlin.readValue
import kotliquery.Session
import no.nav.helse.mediator.kafka.MiljøstyrtFeatureToggle
import no.nav.helse.modell.arbeidsgiver.OppdatertArbeidsgiverCommand
import no.nav.helse.modell.arbeidsgiver.OpprettArbeidsgiverCommand
import no.nav.helse.modell.command.MacroCommand
import no.nav.helse.modell.person.OppdaterPersonCommand
import no.nav.helse.modell.person.OpprettPersonCommand
import no.nav.helse.modell.risiko.OldRisikoCommando
import no.nav.helse.modell.risiko.RisikovurderingDao
import no.nav.helse.modell.tildeling.TildelOppgaveCommand
import no.nav.helse.modell.vedtak.OpprettVedtakCommand
import no.nav.helse.modell.vedtak.SaksbehandlerGodkjenningCommand
import no.nav.helse.modell.vedtak.snapshot.SpeilSnapshotRestClient
import no.nav.helse.objectMapper
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.tildeling.ReservasjonDao
import java.time.Duration
import java.time.LocalDate
import java.util.*

internal class Godkjenningsbehov(
    private val id: UUID,
    private val periodeFom: LocalDate,
    private val periodeTom: LocalDate,
    private val aktørId: String,
    override val fødselsnummer: String,
    override val orgnummer: String,
    override val vedtaksperiodeId: UUID,
    private val reservasjonDao: ReservasjonDao,
    private val risikovurderingDao: RisikovurderingDao,
    private val miljøstyrtFeatureToggle: MiljøstyrtFeatureToggle,
    speilSnapshotRestClient: SpeilSnapshotRestClient
) : MacroCommand(
    eventId = id,
    timeout = Duration.ofDays(14)
) {
    override val oppgaver = setOf(
        OpprettPersonCommand(fødselsnummer, aktørId, id, this),
        OppdaterPersonCommand(fødselsnummer, id, this),
        OpprettArbeidsgiverCommand(
            orgnummer,
            id,
            this
        ),
        OppdatertArbeidsgiverCommand(
            orgnummer,
            id,
            this
        ),
        OpprettVedtakCommand(
            speilSnapshotRestClient = speilSnapshotRestClient,
            fødselsnummer = fødselsnummer,
            orgnummer = orgnummer,
            vedtaksperiodeId = vedtaksperiodeId,
            periodeFom = periodeFom,
            periodeTom = periodeTom,
            eventId = id,
            parent = this
        ),
        OldRisikoCommando(id, risikovurderingDao, miljøstyrtFeatureToggle, this),
        TildelOppgaveCommand(fødselsnummer, reservasjonDao, id, this),
        SaksbehandlerGodkjenningCommand(id, this)
    )

    override fun execute(session: Session): Resultat = Resultat.Ok.System

    override fun toJson() =
        JsonMessage.newMessage(
            objectMapper.convertValue(
                SpleisbehovDTO(
                    id = id,
                    fødselsnummer = fødselsnummer,
                    periodeFom = periodeFom,
                    periodeTom = periodeTom,
                    aktørId = aktørId,
                    orgnummer = orgnummer
                )
            )
        ).toJson()

    companion object {
        fun restore(
            id: UUID,
            vedtaksperiodeId: UUID,
            data: String,
            speilSnapshotRestClient: SpeilSnapshotRestClient,
            reservasjonDao: ReservasjonDao,
            risikovurderingDao: RisikovurderingDao,
            miljøstyrtFeatureToggle: MiljøstyrtFeatureToggle
        ): Godkjenningsbehov {
            val spleisbehovDTO = objectMapper.readValue<SpleisbehovDTO>(data)
            return Godkjenningsbehov(
                id = id,
                vedtaksperiodeId = vedtaksperiodeId,
                fødselsnummer = spleisbehovDTO.fødselsnummer,
                periodeFom = spleisbehovDTO.periodeFom,
                periodeTom = spleisbehovDTO.periodeTom,
                aktørId = spleisbehovDTO.aktørId,
                orgnummer = spleisbehovDTO.orgnummer,
                risikovurderingDao = risikovurderingDao,
                reservasjonDao = reservasjonDao,
                speilSnapshotRestClient = speilSnapshotRestClient,
                miljøstyrtFeatureToggle = miljøstyrtFeatureToggle
            )
        }
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class SpleisbehovDTO(
    val id: UUID,
    val fødselsnummer: String,
    val periodeFom: LocalDate,
    val periodeTom: LocalDate,
    val aktørId: String,
    val orgnummer: String
)
