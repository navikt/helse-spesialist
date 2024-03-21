package no.nav.helse.modell.kommando

import java.util.UUID
import no.nav.helse.modell.SnapshotDao
import no.nav.helse.modell.VedtakDao
import no.nav.helse.modell.person.PersonDao
import no.nav.helse.modell.utbetaling.UtbetalingDao
import no.nav.helse.modell.vedtaksperiode.Inntektskilde
import no.nav.helse.modell.vedtaksperiode.Periodetype
import no.nav.helse.spesialist.api.snapshot.SnapshotClient

internal class KlargjørVedtaksperiodeCommand(
    snapshotClient: SnapshotClient,
    fødselsnummer: String,
    vedtaksperiodeId: UUID,
    vedtaksperiodetype: Periodetype,
    inntektskilde: Inntektskilde,
    personDao: PersonDao,
    snapshotDao: SnapshotDao,
    vedtakDao: VedtakDao,
    utbetalingId: UUID,
    utbetalingDao: UtbetalingDao
) : MacroCommand() {
    override val commands: List<Command> = listOf(
        OppdaterSnapshotCommand(
            snapshotClient = snapshotClient,
            snapshotDao = snapshotDao,
            fødselsnummer = fødselsnummer,
            personDao = personDao
        ),
        PersisterVedtaksperiodetypeCommand(
            vedtaksperiodeId = vedtaksperiodeId,
            vedtaksperiodetype = vedtaksperiodetype,
            inntektskilde = inntektskilde,
            vedtakDao = vedtakDao
        ),
        OpprettKoblingTilUtbetalingCommand(
            vedtaksperiodeId = vedtaksperiodeId,
            utbetalingId = utbetalingId,
            utbetalingDao = utbetalingDao
        )
    )
}
