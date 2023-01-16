package no.nav.helse.spesialist.api.snapshot

import java.util.UUID
import no.nav.helse.spesialist.api.graphql.hentsnapshot.GraphQLBeregnetPeriode
import no.nav.helse.spesialist.api.graphql.hentsnapshot.GraphQLPerson
import no.nav.helse.spesialist.api.graphql.hentsnapshot.GraphQLUtbetaling
import no.nav.helse.spesialist.api.graphql.schema.Personinfo
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class SnapshotMediator(private val snapshotDao: SnapshotApiDao, private val snapshotClient: SnapshotClient) {
    private val sikkerLogg: Logger = LoggerFactory.getLogger("tjenestekall")

    private fun oppdaterSnapshot(fødselsnummer: String) {
        if (snapshotDao.utdatert(fødselsnummer)) {
            sikkerLogg.debug("snapshot for $fødselsnummer er utdatert, henter nytt")
            snapshotClient.hentSnapshot(fødselsnummer).data?.person?.let {
                snapshotDao.lagre(fødselsnummer, it)
            }
        }
    }

    fun hentSnapshot(fødselsnummer: String): Pair<Personinfo, GraphQLPerson>? {
        oppdaterSnapshot(fødselsnummer)
        val snapshot = try {
            snapshotDao.hentSnapshotMedMetadata(fødselsnummer)
        } catch (e: Exception) {
            snapshotClient.hentSnapshot(fødselsnummer).data?.person?.let {
                snapshotDao.lagre(fødselsnummer, it)
            }
            snapshotDao.hentSnapshotMedMetadata(fødselsnummer)
        }
        return snapshot
    }

    fun finnUtbetaling(fødselsnummer: String, utbetalingId: UUID): GraphQLUtbetaling? {
        return hentSnapshot(fødselsnummer)?.second?.let {
            it.arbeidsgivere.firstNotNullOfOrNull {  arbeidsgiver ->
                arbeidsgiver.generasjoner.firstOrNull()?.perioder?.filterIsInstance<GraphQLBeregnetPeriode>()
                    ?.find { periode ->
                        UUID.fromString(periode.utbetaling.id) == utbetalingId
                    }?.utbetaling
            }
        }
    }

}
