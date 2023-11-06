package no.nav.helse.modell.person

import java.util.UUID
import no.nav.helse.mediator.meldinger.Hendelse
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
    snapshotClient: SnapshotClient,
    snapshotDao: SnapshotDao,
    personDao: PersonDao,
    opptegnelseDao: OpptegnelseDao,
) : Hendelse, MacroCommand() {
    override val commands: List<Command> = listOf(
        OppdaterSnapshotCommand(
            snapshotClient = snapshotClient,
            snapshotDao = snapshotDao,
            fødselsnummer = fødselsnummer,
            personDao = personDao,
        ),
        OppdaterInfotrygdutbetalingerHardt(fødselsnummer, personDao),
        ikkesuspenderendeCommand("opprettOpptegnelse") {
            opptegnelseDao.opprettOpptegnelse(
                fødselsnummer,
                PersonOppdatertPayload,
                OpptegnelseType.PERSONDATA_OPPDATERT
            )
        },
    )

    override fun fødselsnummer(): String = fødselsnummer
    override fun vedtaksperiodeId(): UUID? = null
    override fun toJson(): String = json

}
