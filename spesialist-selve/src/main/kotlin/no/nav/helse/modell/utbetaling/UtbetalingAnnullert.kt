package no.nav.helse.modell.utbetaling

import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.db.SaksbehandlerDao
import no.nav.helse.mediator.meldinger.Personmelding
import no.nav.helse.modell.SnapshotDao
import no.nav.helse.modell.kommando.Command
import no.nav.helse.modell.kommando.LagreAnnulleringCommand
import no.nav.helse.modell.kommando.MacroCommand
import no.nav.helse.modell.kommando.OppdaterSnapshotCommand
import no.nav.helse.modell.person.PersonDao
import no.nav.helse.spesialist.api.snapshot.SnapshotClient

internal class UtbetalingAnnullert(
    override val id: UUID,
    private val fødselsnummer: String,
    val utbetalingId: UUID,
    val annullertTidspunkt: LocalDateTime,
    val saksbehandlerEpost: String,
    private val json: String,
) : Personmelding {
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
