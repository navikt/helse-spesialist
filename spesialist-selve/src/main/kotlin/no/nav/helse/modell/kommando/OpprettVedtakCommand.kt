package no.nav.helse.modell.kommando

import java.time.LocalDate
import java.util.UUID
import no.nav.helse.modell.SnapshotDao
import no.nav.helse.modell.VedtakDao
import no.nav.helse.modell.WarningDao
import no.nav.helse.modell.arbeidsgiver.ArbeidsgiverDao
import no.nav.helse.modell.person.PersonDao
import no.nav.helse.modell.vedtak.Warning
import no.nav.helse.spesialist.api.graphql.hentsnapshot.GraphQLPerson
import no.nav.helse.spesialist.api.snapshot.SnapshotClient
import org.slf4j.LoggerFactory

internal class OpprettVedtakCommand(
    private val snapshotClient: SnapshotClient,
    private val fødselsnummer: String,
    private val orgnummer: String,
    private val vedtaksperiodeId: UUID,
    private val periodeFom: LocalDate,
    private val periodeTom: LocalDate,
    private val personDao: PersonDao,
    private val arbeidsgiverDao: ArbeidsgiverDao,
    private val snapshotDao: SnapshotDao,
    private val vedtakDao: VedtakDao,
    private val warningDao: WarningDao
) : Command {
    private companion object {
        private val log = LoggerFactory.getLogger(OpprettVedtakCommand::class.java)
    }

    override fun execute(context: CommandContext): Boolean {
        val vedtakRef = vedtakDao.finnVedtakId(vedtaksperiodeId) ?: return opprett()
        return oppdaterSnapshot(vedtakRef)
    }

    private fun oppdaterSnapshot(vedtakRef: Long): Boolean {
        log.info("Henter oppdatert graphql-snapshot for vedtaksperiode: $vedtaksperiodeId")
        return snapshotClient.hentSnapshot(fødselsnummer).data?.person?.let {
            val id = snapshotDao.lagre(fødselsnummer, it)
            oppdaterWarnings(it)
            log.info("Oppdaterer vedtak for vedtaksperiode: $vedtaksperiodeId")
            vedtakDao.oppdaterSnaphot(
                vedtakRef = vedtakRef,
                fom = periodeFom,
                tom = periodeTom,
                snapshotRef = id
            )
            true
        } ?: false
    }

    private fun opprett(): Boolean {
        log.info("Henter snapshot for vedtaksperiode: $vedtaksperiodeId")
        val snapshot = snapshotClient.hentSnapshot(fødselsnummer)
        val snapshotId = snapshot.data?.person?.let {
            snapshotDao.lagre(fødselsnummer, it)
        }
        val personRef = requireNotNull(personDao.findPersonByFødselsnummer(fødselsnummer))
        val arbeidsgiverRef = requireNotNull(arbeidsgiverDao.findArbeidsgiverByOrgnummer(orgnummer))
        log.info("Oppretter vedtak for vedtaksperiode: $vedtaksperiodeId for person=$personRef, arbeidsgiver=$arbeidsgiverRef")
        vedtakDao.opprett(
            vedtaksperiodeId = vedtaksperiodeId,
            fom = periodeFom,
            tom = periodeTom,
            personRef = personRef,
            arbeidsgiverRef = arbeidsgiverRef,
            snapshotRef = snapshotId
        )
        snapshot.data?.person?.let {
            oppdaterWarnings(it)
        }
        return true
    }

    private fun oppdaterWarnings(person: GraphQLPerson) {
        warningDao.oppdaterSpleisWarnings(
            vedtaksperiodeId, Warning.graphQLWarnings(
                vedtaksperiodeId, person
            )
        )
    }

}
