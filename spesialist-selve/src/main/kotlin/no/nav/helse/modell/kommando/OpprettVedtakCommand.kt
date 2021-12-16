package no.nav.helse.modell.kommando

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.helse.mediator.Toggle
import no.nav.helse.mediator.api.graphql.SpeilSnapshotGraphQLClient
import no.nav.helse.modell.SnapshotDao
import no.nav.helse.modell.SpeilSnapshotDao
import no.nav.helse.modell.VedtakDao
import no.nav.helse.modell.WarningDao
import no.nav.helse.modell.arbeidsgiver.ArbeidsgiverDao
import no.nav.helse.modell.person.PersonDao
import no.nav.helse.modell.vedtak.Warning
import no.nav.helse.modell.vedtak.snapshot.SpeilSnapshotRestClient
import no.nav.helse.objectMapper
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.util.*

internal class OpprettVedtakCommand(
    private val speilSnapshotRestClient: SpeilSnapshotRestClient,
    private val speilSnapshotGraphQLClient: SpeilSnapshotGraphQLClient,
    private val fødselsnummer: String,
    private val orgnummer: String,
    private val vedtaksperiodeId: UUID,
    private val periodeFom: LocalDate,
    private val periodeTom: LocalDate,
    private val personDao: PersonDao,
    private val arbeidsgiverDao: ArbeidsgiverDao,
    private val speilSnapshotDao: SpeilSnapshotDao,
    private val snapshotDao: SnapshotDao,
    private val vedtakDao: VedtakDao,
    private val warningDao: WarningDao
) : Command {
    private companion object {
        private val log = LoggerFactory.getLogger(OpprettVedtakCommand::class.java)
    }

    override fun execute(context: CommandContext): Boolean {
        val vedtakRef = vedtakDao.finnVedtakId(vedtaksperiodeId) ?: return opprett()
        return oppdater(vedtakRef)
    }

    private fun oppdater(vedtakRef: Long): Boolean {
        log.info("Henter oppdatert snapshot for vedtaksperiode: $vedtaksperiodeId")
        return (if (Toggle.GraphQLApi.enabled) oppdaterGraphQLSnapshot(vedtakRef) else true)
            && oppdaterRestApiSnapshot(vedtakRef)
    }

    private fun oppdaterRestApiSnapshot(vedtakRef: Long): Boolean {
        val snapshot = speilSnapshotRestClient.hentSpeilSnapshot(fødselsnummer)
        val snapshotId = speilSnapshotDao.lagre(fødselsnummer, snapshot)
        oppdaterWarnings(snapshot)

        log.info("Oppdaterer REST-api-snapshot for vedtak for vedtaksperiode: $vedtaksperiodeId")
        vedtakDao.oppdater(
            vedtakRef = vedtakRef,
            fom = periodeFom,
            tom = periodeTom,
            speilSnapshotRef = snapshotId
        )
        return true
    }

    private fun oppdaterGraphQLSnapshot(vedtakRef: Long): Boolean {
        return speilSnapshotGraphQLClient.hentSnapshot(fødselsnummer).data?.person?.let { person ->
            val snapshotId = snapshotDao.lagre(fødselsnummer, person)
            log.info("Oppdaterer GraphQL-snapshot for vedtak for vedtaksperiode: $vedtaksperiodeId")
            vedtakDao.oppdaterGraphQLSnapshot(vedtakRef = vedtakRef, snapshotRef = snapshotId)
            true
        } ?: false
    }

    private fun opprett(): Boolean {
        log.info("Henter snapshot for vedtaksperiode: $vedtaksperiodeId")
        val snapshot = speilSnapshotRestClient.hentSpeilSnapshot(fødselsnummer)
        val restApiSnapshotId = speilSnapshotDao.lagre(fødselsnummer, snapshot)
        val graphQLSnapshotId =
            if (Toggle.GraphQLApi.enabled) speilSnapshotGraphQLClient.hentSnapshot(fødselsnummer).data?.person?.let { person ->
                snapshotDao.lagre(fødselsnummer = fødselsnummer, snapshot = person)
            } else null
        val personRef = requireNotNull(personDao.findPersonByFødselsnummer(fødselsnummer))
        val arbeidsgiverRef = requireNotNull(arbeidsgiverDao.findArbeidsgiverByOrgnummer(orgnummer))
        log.info("Oppretter vedtak for vedtaksperiode: $vedtaksperiodeId for person=$personRef, arbeidsgiver=$arbeidsgiverRef")
        vedtakDao.opprett(
            vedtaksperiodeId = vedtaksperiodeId,
            fom = periodeFom,
            tom = periodeTom,
            personRef = personRef,
            arbeidsgiverRef = arbeidsgiverRef,
            speilSnapshotRef = restApiSnapshotId,
            graphQLSnapshotRef = graphQLSnapshotId
        )
        oppdaterWarnings(snapshot)
        return true
    }

    private fun oppdaterWarnings(snapshot: String) {
        warningDao.oppdaterSpleisWarnings(
            vedtaksperiodeId, Warning.warnings(
                vedtaksperiodeId, objectMapper.readValue(snapshot)
            )
        )
    }

}
