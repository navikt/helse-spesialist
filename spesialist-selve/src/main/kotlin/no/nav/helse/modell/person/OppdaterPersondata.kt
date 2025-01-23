package no.nav.helse.modell.person

import com.fasterxml.jackson.databind.JsonNode
import kotliquery.TransactionalSession
import no.nav.helse.db.OpptegnelseRepository
import no.nav.helse.db.PersonDao
import no.nav.helse.mediator.Kommandostarter
import no.nav.helse.mediator.meldinger.Personmelding
import no.nav.helse.modell.kommando.Command
import no.nav.helse.modell.kommando.MacroCommand
import no.nav.helse.modell.kommando.OppdaterInfotrygdutbetalingerHardt
import no.nav.helse.modell.kommando.ikkesuspenderendeCommand
import no.nav.helse.spesialist.api.abonnement.OpptegnelseType
import no.nav.helse.spesialist.api.abonnement.PersonOppdatertPayload
import java.time.LocalDate
import java.util.UUID

class OppdaterPersondata(
    override val id: UUID,
    private val fødselsnummer: String,
    private val json: String,
) : Personmelding {
    constructor(jsonNode: JsonNode) : this(
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
    personDao: PersonDao,
    opptegnelseRepository: OpptegnelseRepository,
) : MacroCommand() {
    override val commands: List<Command> =
        listOf(
            OppdaterInfotrygdutbetalingerHardt(fødselsnummer, personDao, førsteKjenteDagFinner),
            ikkesuspenderendeCommand("opprettOpptegnelse") {
                opptegnelseRepository.opprettOpptegnelse(
                    fødselsnummer,
                    PersonOppdatertPayload,
                    OpptegnelseType.PERSONDATA_OPPDATERT,
                )
            },
        )
}
