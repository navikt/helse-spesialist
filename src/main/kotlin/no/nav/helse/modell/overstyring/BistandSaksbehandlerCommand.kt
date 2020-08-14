package no.nav.helse.modell.overstyring

import com.fasterxml.jackson.module.kotlin.readValue
import kotliquery.Session
import no.nav.helse.modell.arbeidsgiver.OppdatertArbeidsgiverCommand
import no.nav.helse.modell.arbeidsgiver.OpprettArbeidsgiverCommand
import no.nav.helse.modell.command.Command
import no.nav.helse.modell.command.MacroCommand
import no.nav.helse.modell.person.OppdaterPersonCommand
import no.nav.helse.modell.person.OpprettPersonCommand
import no.nav.helse.modell.vedtak.OpprettVedtakCommand
import no.nav.helse.modell.vedtak.snapshot.SpeilSnapshotRestClient
import no.nav.helse.objectMapper
import java.time.Duration
import java.time.LocalDate
import java.util.*

internal class BistandSaksbehandlerCommand(
    private val id: UUID,
    override val vedtaksperiodeId: UUID,
    override val fødselsnummer: String,
    private val aktørId: String,
    override val orgnummer: String,
    private val periodeFom: LocalDate,
    private val periodeTom: LocalDate,
    private val speilSnapshotRestClient: SpeilSnapshotRestClient
) : MacroCommand(
    id, Duration.ZERO
) {

    override val oppgaver: Set<Command> = setOf(
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
        )
    )

    override fun execute(session: Session): Resultat {
        return Resultat.Ok.System
    }

    override fun toJson(): String {
        return objectMapper.writeValueAsString(
            BistandSaksbehandlerCommandDTO(
                fødselsnummer = fødselsnummer,
                aktørId = aktørId,
                orgnummer = orgnummer,
                periodeFom = periodeFom,
                periodeTom = periodeTom
            )
        )
    }

    companion object {
        fun restore(
            id: UUID,
            vedtaksperiodeId: UUID,
            data: String,
            speilSnapshotRestClient: SpeilSnapshotRestClient
        ): BistandSaksbehandlerCommand {
            val bistandSaksbehandlerCommandDto = objectMapper.readValue<BistandSaksbehandlerCommandDTO>(data)
            return BistandSaksbehandlerCommand(
                id = id,
                vedtaksperiodeId = vedtaksperiodeId,
                fødselsnummer = bistandSaksbehandlerCommandDto.fødselsnummer,
                aktørId = bistandSaksbehandlerCommandDto.aktørId,
                orgnummer = bistandSaksbehandlerCommandDto.orgnummer,
                periodeFom = bistandSaksbehandlerCommandDto.periodeFom,
                periodeTom = bistandSaksbehandlerCommandDto.periodeTom,
                speilSnapshotRestClient = speilSnapshotRestClient
            )
        }
    }
}

data class BistandSaksbehandlerCommandDTO(
    val fødselsnummer: String,
    val aktørId: String,
    val orgnummer: String,
    val periodeFom: LocalDate,
    val periodeTom: LocalDate
)
