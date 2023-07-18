package no.nav.helse.modell.vedtaksperiode

import java.util.UUID
import no.nav.helse.mediator.meldinger.Hendelse
import no.nav.helse.modell.SnapshotDao
import no.nav.helse.modell.kommando.MacroCommand
import no.nav.helse.modell.kommando.SørgForAtSnapshotErOppdatertCommand
import no.nav.helse.spesialist.api.snapshot.SnapshotClient

internal class PåminnetGodkjenningsbehov(
    override val id: UUID,
    private val fødselsnummer: String,
    private val json: String,
    snapshotDao: SnapshotDao,
    snapshotClient: SnapshotClient,
) : Hendelse, MacroCommand() {

    override val commands = listOf(
        SørgForAtSnapshotErOppdatertCommand(
            snapshotDao = snapshotDao,
            snapshotClient = snapshotClient,
            fødselsnummer = fødselsnummer
        ),
    )

    override fun fødselsnummer() = fødselsnummer
    override fun toJson() = json
}
