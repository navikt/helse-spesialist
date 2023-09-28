package no.nav.helse.modell.utbetaling

import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.db.SaksbehandlerDao
import no.nav.helse.mediator.meldinger.Hendelse
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
    utbetalingId: UUID,
    annullertTidspunkt: LocalDateTime,
    saksbehandlerEpost: String,
    private val json: String,
    utbetalingDao: UtbetalingDao,
    saksbehandlerDao: SaksbehandlerDao,
    snapshotClient: SnapshotClient,
    snapshotDao: SnapshotDao,
    personDao: PersonDao,
) : Hendelse, MacroCommand() {
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

    override fun fødselsnummer(): String = fødselsnummer
    override fun vedtaksperiodeId(): UUID? = null
    override fun toJson(): String = json

}
