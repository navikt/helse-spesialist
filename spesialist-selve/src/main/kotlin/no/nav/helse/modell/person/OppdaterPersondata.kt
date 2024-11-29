package no.nav.helse.modell.person

import com.fasterxml.jackson.databind.JsonNode
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import kotliquery.TransactionalSession
import no.nav.helse.db.OpptegnelseRepository
import no.nav.helse.db.PersonRepository
import no.nav.helse.mediator.Kommandostarter
import no.nav.helse.mediator.asUUID
import no.nav.helse.mediator.meldinger.Personmelding
import no.nav.helse.modell.kommando.Command
import no.nav.helse.modell.kommando.MacroCommand
import no.nav.helse.modell.kommando.OppdaterInfotrygdutbetalingerHardt
import no.nav.helse.modell.kommando.ikkesuspenderendeCommand
import no.nav.helse.spesialist.api.abonnement.OpptegnelseType
import no.nav.helse.spesialist.api.abonnement.PersonOppdatertPayload
import java.time.LocalDate
import java.util.UUID

internal class OppdaterPersondata private constructor(
    override val id: UUID,
    private val fødselsnummer: String,
    private val json: String,
) : Personmelding {
    internal constructor(packet: JsonMessage) : this(
        id = packet["@id"].asUUID(),
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
        transactionalSession: TransactionalSession,
    ) {
        kommandostarter { oppdaterPersondata(this@OppdaterPersondata, transactionalSession) }
    }

    override fun fødselsnummer(): String = fødselsnummer

    override fun toJson(): String = json
}

internal class OppdaterPersondataCommand(
    fødselsnummer: String,
    førsteKjenteDagFinner: () -> LocalDate,
    personRepository: PersonRepository,
    opptegnelseRepository: OpptegnelseRepository,
) : MacroCommand() {
    override val commands: List<Command> =
        listOf(
            OppdaterInfotrygdutbetalingerHardt(fødselsnummer, personRepository, førsteKjenteDagFinner),
            ikkesuspenderendeCommand("opprettOpptegnelse") {
                opptegnelseRepository.opprettOpptegnelse(
                    fødselsnummer,
                    PersonOppdatertPayload,
                    OpptegnelseType.PERSONDATA_OPPDATERT,
                )
            },
        )
}
