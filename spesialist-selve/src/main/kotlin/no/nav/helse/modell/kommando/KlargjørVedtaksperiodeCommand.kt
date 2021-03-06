package no.nav.helse.modell.kommando

import no.nav.helse.modell.SnapshotDao
import no.nav.helse.modell.VedtakDao
import no.nav.helse.modell.WarningDao
import no.nav.helse.modell.arbeidsgiver.ArbeidsgiverDao
import no.nav.helse.modell.person.PersonDao
import no.nav.helse.modell.vedtak.snapshot.SpeilSnapshotRestClient
import no.nav.helse.modell.vedtaksperiode.Inntektskilde
import no.nav.helse.modell.vedtaksperiode.Periodetype
import java.time.LocalDate
import java.util.*

internal class KlargjørVedtaksperiodeCommand(
    speilSnapshotRestClient: SpeilSnapshotRestClient,
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
    warningDao: WarningDao
) : MacroCommand() {
    override val commands: List<Command> = listOf(
        OpprettVedtakCommand(
            speilSnapshotRestClient,
            fødselsnummer,
            organisasjonsnummer,
            vedtaksperiodeId,
            periodeFom,
            periodeTom,
            personDao,
            arbeidsgiverDao,
            snapshotDao,
            vedtakDao,
            warningDao,
        ),
        PersisterVedtaksperiodetypeCommand(vedtaksperiodeId, vedtaksperiodetype, inntektskilde, vedtakDao)
    )
}
