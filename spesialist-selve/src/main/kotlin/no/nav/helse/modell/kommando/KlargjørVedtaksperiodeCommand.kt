package no.nav.helse.modell.kommando

import no.nav.helse.mediator.api.graphql.SnapshotClient
import no.nav.helse.modell.SnapshotDao
import no.nav.helse.modell.VedtakDao
import no.nav.helse.modell.WarningDao
import no.nav.helse.modell.arbeidsgiver.ArbeidsgiverDao
import no.nav.helse.modell.person.PersonDao
import no.nav.helse.modell.utbetaling.UtbetalingDao
import no.nav.helse.modell.vedtaksperiode.Inntektskilde
import no.nav.helse.modell.vedtaksperiode.Periodetype
import java.time.LocalDate
import java.util.*

internal class KlargjørVedtaksperiodeCommand(
    snapshotClient: SnapshotClient,
    fødselsnummer: String,
    organisasjonsnummer: String,
    vedtaksperiodeId: UUID,
    periodeFom: LocalDate,
    periodeTom: LocalDate,
    vedtaksperiodetype: Periodetype,
    inntektskilde: Inntektskilde,
    personDao: PersonDao,
    arbeidsgiverDao: ArbeidsgiverDao,
    snapshotDao: SnapshotDao,
    vedtakDao: VedtakDao,
    warningDao: WarningDao,
    utbetalingId: UUID,
    utbetalingDao: UtbetalingDao
) : MacroCommand() {
    override val commands: List<Command> = listOf(
        OpprettVedtakCommand(
            snapshotClient = snapshotClient,
            fødselsnummer = fødselsnummer,
            orgnummer = organisasjonsnummer,
            vedtaksperiodeId = vedtaksperiodeId,
            periodeFom = periodeFom,
            periodeTom = periodeTom,
            personDao = personDao,
            arbeidsgiverDao = arbeidsgiverDao,
            snapshotDao = snapshotDao,
            vedtakDao = vedtakDao,
            warningDao = warningDao,
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
