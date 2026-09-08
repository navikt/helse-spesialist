package no.nav.helse.modell.utbetaling

import no.nav.helse.db.SessionContext
import no.nav.helse.mediator.Kommandostarter
import no.nav.helse.mediator.meldinger.Personmelding
import no.nav.helse.modell.kommando.Command
import no.nav.helse.modell.kommando.MacroCommand
import no.nav.helse.spesialist.domain.Identitetsnummer
import no.nav.helse.spesialist.domain.UtbetalingId
import tools.jackson.databind.JsonNode
import java.time.LocalDateTime
import java.util.*

class UtbetalingEndret(
    override val id: UUID,
    private val fødselsnummer: String,
    val organisasjonsnummer: String,
    val utbetalingId: UUID,
    val type: String,
    val gjeldendeStatus: Utbetalingsstatus,
    val opprettet: LocalDateTime,
    val arbeidsgiverbeløp: Int,
    val personbeløp: Int,
    private val json: String,
) : Personmelding {
    constructor(jsonNode: JsonNode) : this(
        id = UUID.fromString(jsonNode["@id"].asString()),
        fødselsnummer = jsonNode["fødselsnummer"].asString(),
        organisasjonsnummer = jsonNode["organisasjonsnummer"].asString(),
        utbetalingId = UUID.fromString(jsonNode["utbetalingId"].asString()),
        type = jsonNode["type"].asString(),
        gjeldendeStatus = Utbetalingsstatus.valueOf(jsonNode["gjeldendeStatus"].asString()),
        opprettet = jsonNode["@opprettet"].asString().let(LocalDateTime::parse),
        arbeidsgiverbeløp = jsonNode["arbeidsgiverOppdrag"]["nettoBeløp"].asInt(),
        personbeløp = jsonNode["personOppdrag"]["nettoBeløp"].asInt(),
        json = jsonNode.toString(),
    )

    override fun behandle(
        kommandostarter: Kommandostarter,
        sessionContext: SessionContext,
    ) {
        val identitetsnummer = Identitetsnummer.fraString(fødselsnummer)
        val utbetalingId = UtbetalingId(utbetalingId)
        if (gjeldendeStatus == Utbetalingsstatus.FORKASTET) {
            val vedtaksperiodeIder = sessionContext.vedtaksperiodeRepository.finnAlleIderForPerson(identitetsnummer)
            val gjeldendeBehandlinger = vedtaksperiodeIder.mapNotNull { sessionContext.behandlingRepository.finnNyesteForVedtaksperiode(it) }
            gjeldendeBehandlinger
                .filter { utbetalingId == it.utbetalingId }
                .onEach {
                    it.forkastUtbetaling()
                    sessionContext.behandlingRepository.lagre(it)
                }
        }
        this.kommandostarter {
            UtbetalingEndretCommand(
                identitetsnummer = identitetsnummer,
                organisasjonsnummer = organisasjonsnummer,
                utbetalingId = utbetalingId,
                utbetalingstype = type,
                gjeldendeStatus = gjeldendeStatus,
                opprettet = opprettet,
                arbeidsgiverbeløp = arbeidsgiverbeløp,
                personbeløp = personbeløp,
                json = toJson(),
            )
        }
    }

    override fun fødselsnummer(): String = fødselsnummer

    override fun toJson(): String = json
}

internal class UtbetalingEndretCommand(
    identitetsnummer: Identitetsnummer,
    organisasjonsnummer: String,
    utbetalingId: UtbetalingId,
    utbetalingstype: String,
    gjeldendeStatus: Utbetalingsstatus,
    opprettet: LocalDateTime,
    arbeidsgiverbeløp: Int,
    personbeløp: Int,
    json: String,
) : MacroCommand() {
    override val commands: List<Command> =
        mutableListOf(
            LagreUtbetalingCommand(
                identitetsnummer = identitetsnummer,
                orgnummer = organisasjonsnummer,
                utbetalingId = utbetalingId,
                type = Utbetalingtype.valueOf(utbetalingstype),
                status = gjeldendeStatus,
                opprettet = opprettet,
                arbeidsgiverbeløp = arbeidsgiverbeløp,
                personbeløp = personbeløp,
                json = json,
            ),
        )
}
