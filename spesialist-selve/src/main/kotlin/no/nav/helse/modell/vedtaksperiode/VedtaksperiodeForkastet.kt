package no.nav.helse.modell.vedtaksperiode

import java.util.UUID
import no.nav.helse.mediator.meldinger.Vedtaksperiodemelding
import no.nav.helse.mediator.oppgave.OppgaveMediator
import no.nav.helse.modell.CommandContextDao
import no.nav.helse.modell.SnapshotDao
import no.nav.helse.modell.VedtakDao
import no.nav.helse.modell.kommando.AvbrytCommand
import no.nav.helse.modell.kommando.Command
import no.nav.helse.modell.kommando.MacroCommand
import no.nav.helse.modell.kommando.OppdaterSnapshotCommand
import no.nav.helse.modell.person.PersonDao
import no.nav.helse.spesialist.api.snapshot.SnapshotClient

internal class VedtaksperiodeForkastet(
    override val id: UUID,
    private val vedtaksperiodeId: UUID,
    private val fødselsnummer: String,
    private val json: String
) : Vedtaksperiodemelding {
    override fun fødselsnummer() = fødselsnummer
    override fun vedtaksperiodeId() = vedtaksperiodeId
    override fun toJson() = json
}

internal class VedtaksperiodeForkastetCommand(
    val fødselsnummer: String,
    val vedtaksperiodeId: UUID,
    val id: UUID,
    personDao: PersonDao,
    commandContextDao: CommandContextDao,
    snapshotDao: SnapshotDao,
    vedtakDao: VedtakDao,
    snapshotClient: SnapshotClient,
    oppgaveMediator: OppgaveMediator
): MacroCommand() {
    override val commands: List<Command> = listOf(
        AvbrytCommand(vedtaksperiodeId, commandContextDao, oppgaveMediator),
        OppdaterSnapshotCommand(
            snapshotClient = snapshotClient,
            snapshotDao = snapshotDao,
            vedtaksperiodeId = vedtaksperiodeId,
            fødselsnummer = fødselsnummer,
            personDao = personDao,
        ),
        ForkastVedtaksperiodeCommand(id, vedtaksperiodeId, vedtakDao)
    )
}
