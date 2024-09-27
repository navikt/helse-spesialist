package no.nav.helse.modell.person

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.db.PersonRepository
import no.nav.helse.mediator.Kommandostarter
import no.nav.helse.mediator.meldinger.Personmelding
import no.nav.helse.modell.SnapshotDao
import no.nav.helse.modell.kommando.Command
import no.nav.helse.modell.kommando.MacroCommand
import no.nav.helse.modell.kommando.OppdaterInfotrygdutbetalingerHardt
import no.nav.helse.modell.kommando.OppdaterSnapshotCommand
import no.nav.helse.modell.kommando.ikkesuspenderendeCommand
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.spesialist.api.abonnement.OpptegnelseDao
import no.nav.helse.spesialist.api.abonnement.OpptegnelseType
import no.nav.helse.spesialist.api.abonnement.PersonOppdatertPayload
import no.nav.helse.spesialist.api.snapshot.ISnapshotClient
import java.time.LocalDate
import java.util.UUID

internal class OppdaterPersonsnapshot private constructor(
    override val id: UUID,
    private val fødselsnummer: String,
    private val json: String,
) : Personmelding {
    internal constructor(packet: JsonMessage) : this(
        id = UUID.fromString(packet["@id"].asText()),
        fødselsnummer = packet["fødselsnummer"].asText(),
        json = packet.toJson(),
    )

    internal constructor(jsonNode: JsonNode) : this(
        id = UUID.fromString(jsonNode["@id"].asText()),
        fødselsnummer = jsonNode["fødselsnummer"].asText(),
        json = jsonNode.toString(),
    )

    override fun behandle(
        person: Person,
        kommandostarter: Kommandostarter,
    ) {
        kommandostarter { oppdaterPersonsnapshot(this@OppdaterPersonsnapshot) }
    }

    override fun fødselsnummer(): String = fødselsnummer

    override fun toJson(): String = json
}

internal class OppdaterPersonsnapshotCommand(
    fødselsnummer: String,
    førsteKjenteDagFinner: () -> LocalDate,
    personRepository: PersonRepository,
    snapshotDao: SnapshotDao,
    opptegnelseDao: OpptegnelseDao,
    snapshotClient: ISnapshotClient,
) : MacroCommand() {
    override val commands: List<Command> =
        listOf(
            OppdaterSnapshotCommand(
                snapshotClient = snapshotClient,
                snapshotDao = snapshotDao,
                fødselsnummer = fødselsnummer,
                personRepository = personRepository,
            ),
            OppdaterInfotrygdutbetalingerHardt(fødselsnummer, personRepository, førsteKjenteDagFinner),
            ikkesuspenderendeCommand("opprettOpptegnelse") {
                opptegnelseDao.opprettOpptegnelse(
                    fødselsnummer,
                    PersonOppdatertPayload,
                    OpptegnelseType.PERSONDATA_OPPDATERT,
                )
            },
        )
}
