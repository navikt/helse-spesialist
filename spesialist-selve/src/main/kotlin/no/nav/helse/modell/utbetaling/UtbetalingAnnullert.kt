package no.nav.helse.modell.utbetaling

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.db.AnnulleringDao
import no.nav.helse.mediator.Kommandofabrikk
import no.nav.helse.mediator.meldinger.Personmelding
import no.nav.helse.modell.SnapshotDao
import no.nav.helse.modell.kommando.Command
import no.nav.helse.modell.kommando.LagreAnnulleringCommand
import no.nav.helse.modell.kommando.MacroCommand
import no.nav.helse.modell.kommando.OppdaterSnapshotCommand
import no.nav.helse.modell.person.Person
import no.nav.helse.modell.person.PersonDao
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.spesialist.api.snapshot.ISnapshotClient
import java.util.UUID

internal class UtbetalingAnnullert private constructor(
    override val id: UUID,
    private val fødselsnummer: String,
    val utbetalingId: UUID,
    val saksbehandlerEpost: String,
    private val json: String,
) : Personmelding {
    internal constructor(packet: JsonMessage) : this(
        id = UUID.fromString(packet["@id"].asText()),
        fødselsnummer = packet["fødselsnummer"].asText(),
        utbetalingId = UUID.fromString(packet["utbetalingId"].asText()),
        saksbehandlerEpost = packet["epost"].asText(),
        json = packet.toJson(),
    )
    internal constructor(jsonNode: JsonNode) : this(
        id = UUID.fromString(jsonNode["@id"].asText()),
        fødselsnummer = jsonNode["fødselsnummer"].asText(),
        utbetalingId = UUID.fromString(jsonNode["utbetalingId"].asText()),
        saksbehandlerEpost = jsonNode["epost"].asText(),
        json = jsonNode.toString(),
    )

    override fun behandle(
        person: Person,
        kommandofabrikk: Kommandofabrikk,
    ) {
        kommandofabrikk.iverksettUtbetalingAnnulert(this)
    }

    override fun fødselsnummer(): String = fødselsnummer

    override fun toJson(): String = json
}

internal class UtbetalingAnnullertCommand(
    fødselsnummer: String,
    utbetalingId: UUID,
    utbetalingDao: UtbetalingDao,
    personDao: PersonDao,
    snapshotDao: SnapshotDao,
    snapshotClient: ISnapshotClient,
    annulleringDao: AnnulleringDao,
) : MacroCommand() {
    override val commands: List<Command> =
        listOf(
            OppdaterSnapshotCommand(
                snapshotClient = snapshotClient,
                snapshotDao = snapshotDao,
                fødselsnummer = fødselsnummer,
                personDao = personDao,
            ),
            LagreAnnulleringCommand(
                utbetalingDao = utbetalingDao,
                annulleringDao = annulleringDao,
                utbetalingId = utbetalingId,
            ),
        )
}
