package no.nav.helse.modell.kommando

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.helse.mediator.Toggle
import no.nav.helse.mediator.api.graphql.SpeilSnapshotGraphQLClient
import no.nav.helse.mediator.graphql.hentsnapshot.GraphQLPerson
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
        return oppdaterRestApiSnapshot(vedtakRef) && (if (Toggle.GraphQLApi.enabled) oppdaterGraphQLApiSnapshot(
            vedtakRef
        ) else true)
    }

    private fun oppdaterRestApiSnapshot(vedtakRef: Long): Boolean {
        log.info("Henter oppdatert snapshot for vedtaksperiode: $vedtaksperiodeId")
        val snapshot = speilSnapshotRestClient.hentSpeilSnapshot(fødselsnummer)
        val snapshotId = speilSnapshotDao.lagre(fødselsnummer, snapshot)
        oppdaterWarnings(snapshot)

        log.info("Oppdaterer vedtak for vedtaksperiode: $vedtaksperiodeId")
        vedtakDao.oppdater(
            vedtakRef = vedtakRef,
            fom = periodeFom,
            tom = periodeTom,
            speilSnapshotRef = snapshotId
        )
        return true
    }

    private fun oppdaterGraphQLApiSnapshot(vedtakRef: Long): Boolean {
        log.info("Henter oppdatert graphql-snapshot for vedtaksperiode: $vedtaksperiodeId")
        return speilSnapshotGraphQLClient.hentSnapshot(fødselsnummer).data?.person?.let {
            val id = snapshotDao.lagre(fødselsnummer, it)
            oppdaterWarnings(it)
            log.info("Oppdaterer vedtak for vedtaksperiode: $vedtaksperiodeId")
            vedtakDao.oppdaterGraphQLSnapshot(
                vedtakRef = vedtakRef,
                snapshotRef = id
            )
            true
        } ?: false
    }

    private fun opprett(): Boolean {
        log.info("Henter snapshot for vedtaksperiode: $vedtaksperiodeId")
        val restApiSnapshot = speilSnapshotRestClient.hentSpeilSnapshot(fødselsnummer)
        val restApiSnapshotId = speilSnapshotDao.lagre(fødselsnummer, restApiSnapshot)
        val graphQLApiSnapshot = speilSnapshotGraphQLClient.hentSnapshot(fødselsnummer)
        val graphQLApiSnapshotId = if (Toggle.GraphQLApi.enabled) graphQLApiSnapshot.data?.person?.let {
            snapshotDao.lagre(fødselsnummer, it)
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
            snapshotRef = graphQLApiSnapshotId
        )
        oppdaterWarnings(restApiSnapshot)
        if (Toggle.GraphQLApi.enabled) {
            graphQLApiSnapshot.data?.person?.let {
                oppdaterWarnings(it)
            }
        }
        return true
    }

    private fun oppdaterWarnings(snapshot: String) {
        warningDao.oppdaterSpleisWarnings(
            vedtaksperiodeId, Warning.warnings(
                vedtaksperiodeId, objectMapper.readValue(snapshot)
            )
        )
    }

    private fun oppdaterWarnings(person: GraphQLPerson) {
        warningDao.oppdaterSpleisWarnings(
            vedtaksperiodeId, Warning.graphQLWarnings(
                vedtaksperiodeId, person
            )
        )
    }

}
