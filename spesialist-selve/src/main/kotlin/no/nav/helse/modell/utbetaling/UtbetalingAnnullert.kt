package no.nav.helse.modell.utbetaling

import com.fasterxml.jackson.databind.JsonNode
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.db.SaksbehandlerDao
import no.nav.helse.mediator.meldinger.PersonmeldingOld
import no.nav.helse.modell.SnapshotDao
import no.nav.helse.modell.kommando.Command
import no.nav.helse.modell.kommando.LagreAnnulleringCommand
import no.nav.helse.modell.kommando.MacroCommand
import no.nav.helse.modell.kommando.OppdaterSnapshotCommand
import no.nav.helse.modell.person.PersonDao
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.spesialist.api.snapshot.SnapshotClient

internal class UtbetalingAnnullert private constructor(
    override val id: UUID,
    private val fødselsnummer: String,
    val utbetalingId: UUID,
    val annullertTidspunkt: LocalDateTime,
    val saksbehandlerEpost: String,
    private val json: String,
) : PersonmeldingOld {
    internal constructor(packet: JsonMessage): this(
        id = UUID.fromString(packet["@id"].asText()),
        fødselsnummer = packet["fødselsnummer"].asText(),
        utbetalingId = UUID.fromString(packet["utbetalingId"].asText()),
        annullertTidspunkt = LocalDateTime.parse(packet["tidspunkt"].asText()),
        saksbehandlerEpost = packet["epost"].asText(),
        json = packet.toJson()
    )
    internal constructor(jsonNode: JsonNode): this(
        id = UUID.fromString(jsonNode["@id"].asText()),
        fødselsnummer = jsonNode["fødselsnummer"].asText(),
        utbetalingId = UUID.fromString(jsonNode["utbetalingId"].asText()),
        annullertTidspunkt = LocalDateTime.parse(jsonNode["tidspunkt"].asText()),
        saksbehandlerEpost = jsonNode["epost"].asText(),
        json = jsonNode.toString()
    )

    override fun fødselsnummer(): String = fødselsnummer
    override fun toJson(): String = json
}

internal class UtbetalingAnnullertCommand(
    fødselsnummer: String,
    utbetalingId: UUID,
    saksbehandlerEpost: String,
    annullertTidspunkt: LocalDateTime,
    utbetalingDao: UtbetalingDao,
    personDao: PersonDao,
    snapshotDao: SnapshotDao,
    snapshotClient: SnapshotClient,
    saksbehandlerDao: SaksbehandlerDao
): MacroCommand() {
    override val commands: List<Command> = listOf(
        OppdaterSnapshotCommand(
            snapshotClient = snapshotClient,
            snapshotDao = snapshotDao,
            fødselsnummer = fødselsnummer,
            personDao = personDao,
        ),
        LagreAnnulleringCommand(
            utbetalingDao = utbetalingDao,
            saksbehandlerDao = saksbehandlerDao,
            annullertTidspunkt = annullertTidspunkt,
            saksbehandlerEpost = saksbehandlerEpost,
            utbetalingId = utbetalingId
        )
    )
}
