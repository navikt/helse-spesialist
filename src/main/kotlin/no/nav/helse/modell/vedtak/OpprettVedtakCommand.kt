package no.nav.helse.modell.vedtak

import kotliquery.Session
import no.nav.helse.measureAsHistogram
import no.nav.helse.modell.arbeidsgiver.findArbeidsgiverByOrgnummer
import no.nav.helse.modell.command.Command
import no.nav.helse.modell.person.findPersonByFødselsnummer
import no.nav.helse.modell.vedtak.snapshot.SpeilSnapshotRestClient
import no.nav.helse.modell.vedtak.snapshot.insertSpeilSnapshot
import java.time.Duration
import java.time.LocalDate
import java.util.*

internal class OpprettVedtakCommand(
    private val speilSnapshotRestClient: SpeilSnapshotRestClient,
    private val fødselsnummer: String,
    private val orgnummer: String,
    private val vedtaksperiodeId: UUID,
    private val periodeFom: LocalDate,
    private val periodeTom: LocalDate,
    eventId: UUID,
    parent: Command
) : Command(
    eventId = eventId,
    parent = parent,
    timeout = Duration.ofHours(1)
) {
    override fun execute(session: Session): Resultat = measureAsHistogram("OpprettVedtakCommand") {
        log.info("Henter snapshot for vedtaksperiode: $vedtaksperiodeId")
        val speilSnapshot = measureAsHistogram("OpprettVedtakCommand_hentSpeilSnapshot") {
            speilSnapshotRestClient.hentSpeilSpapshot(
                fødselsnummer
            )
        }
        val snapshotId = measureAsHistogram("OpprettVedtakCommand_insertSpeilSnapshot") {
            session.insertSpeilSnapshot(
                speilSnapshot
            )
        }
        val personRef = measureAsHistogram("OpprettVedtakCommand_findPersonByFødselsnummer") {
            requireNotNull(session.findPersonByFødselsnummer(fødselsnummer.toLong()))
        }
        val arbeidsgiverRef = measureAsHistogram("OpprettVedtakCommand_findArbeidsgiverByOrgnummer") {
            requireNotNull(session.findArbeidsgiverByOrgnummer(orgnummer.toLong()))
        }
        measureAsHistogram("OpprettVedtakCommand_insertVedtak") {
            session.upsertVedtak(
                vedtaksperiodeId = vedtaksperiodeId,
                fom = periodeFom,
                tom = periodeTom,
                personRef = personRef,
                arbeidsgiverRef = arbeidsgiverRef,
                speilSnapshotRef = snapshotId
            )
        }

        Resultat.Ok.System
    }

}
