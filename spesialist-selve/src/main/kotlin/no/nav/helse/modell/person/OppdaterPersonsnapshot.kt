package no.nav.helse.modell.person

import java.time.LocalDate
import java.util.UUID
import no.nav.helse.mediator.meldinger.Personmelding
import no.nav.helse.modell.SnapshotDao
import no.nav.helse.modell.kommando.Command
import no.nav.helse.modell.kommando.MacroCommand
import no.nav.helse.modell.kommando.OppdaterInfotrygdutbetalingerHardt
import no.nav.helse.modell.kommando.OppdaterSnapshotCommand
import no.nav.helse.modell.kommando.ikkesuspenderendeCommand
import no.nav.helse.spesialist.api.abonnement.OpptegnelseDao
import no.nav.helse.spesialist.api.abonnement.OpptegnelseType
import no.nav.helse.spesialist.api.abonnement.PersonOppdatertPayload
import no.nav.helse.spesialist.api.snapshot.SnapshotClient

internal class OppdaterPersonsnapshot(
    override val id: UUID,
    private val fødselsnummer: String,
    private val json: String,
) : Personmelding {
    override fun fødselsnummer(): String = fødselsnummer
    override fun toJson(): String = json
}

internal class OppdaterPersonsnapshotCommand(
    fødselsnummer: String,
    førsteKjenteDagFinner: () -> LocalDate,
    personDao: PersonDao,
    snapshotDao: SnapshotDao,
    opptegnelseDao: OpptegnelseDao,
    snapshotClient: SnapshotClient
): MacroCommand() {
    override val commands: List<Command> = listOf(
        OppdaterSnapshotCommand(
            snapshotClient = snapshotClient,
            snapshotDao = snapshotDao,
            fødselsnummer = fødselsnummer,
            personDao = personDao,
        ),
        OppdaterInfotrygdutbetalingerHardt(fødselsnummer, personDao, førsteKjenteDagFinner),
        ikkesuspenderendeCommand("opprettOpptegnelse") {
            opptegnelseDao.opprettOpptegnelse(
                fødselsnummer,
                PersonOppdatertPayload,
                OpptegnelseType.PERSONDATA_OPPDATERT
            )
        },
    )

}
