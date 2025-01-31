package no.nav.helse.spesialist.api.snapshot

import no.nav.helse.db.api.SnapshotApiDao
import no.nav.helse.spesialist.api.graphql.schema.Adressebeskyttelse
import no.nav.helse.spesialist.api.graphql.schema.Kjonn
import no.nav.helse.spesialist.api.graphql.schema.Personinfo
import no.nav.helse.spleis.graphql.hentsnapshot.GraphQLPerson
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class SnapshotService(private val snapshotDao: SnapshotApiDao, private val snapshotClient: ISnapshotClient) {
    private val sikkerLogg: Logger = LoggerFactory.getLogger("tjenestekall")

    fun hentSnapshot(fødselsnummer: String): Pair<Personinfo, GraphQLPerson>? {
        // TODO: trenger ikke lagre snapshot fra spleis-api lenger, kan legge på metadata fra Spesialist
        // on the fly og returnere komplett snapshot (Pair<Personinfo, GraphQLPerson>) her
        hentOgLagre(fødselsnummer)
        sikkerLogg.info("Henter snapshot for person med fødselsnummer=$fødselsnummer")
        val snapshot =
            try {
                snapshotDao.hentSnapshotMedMetadata(fødselsnummer)
            } catch (e: Exception) {
                sikkerLogg.warn("feil under henting av snapshot fra databasen, prøver en gang til", e)
                hentOgLagre(fødselsnummer)
                snapshotDao.hentSnapshotMedMetadata(fødselsnummer)
            }
        return snapshot?.let { (personinfo, graphqlPerson) ->
            Personinfo(
                fornavn = personinfo.fornavn,
                mellomnavn = personinfo.mellomnavn,
                etternavn = personinfo.etternavn,
                fodselsdato = personinfo.fodselsdato,
                kjonn =
                    personinfo.kjonn.let {
                        when (it) {
                            SnapshotApiDao.Personinfo.Kjonn.Kvinne -> Kjonn.Kvinne
                            SnapshotApiDao.Personinfo.Kjonn.Mann -> Kjonn.Mann
                            SnapshotApiDao.Personinfo.Kjonn.Ukjent -> Kjonn.Ukjent
                        }
                    },
                adressebeskyttelse =
                    personinfo.adressebeskyttelse.let {
                        when (it) {
                            SnapshotApiDao.Personinfo.Adressebeskyttelse.Ugradert -> Adressebeskyttelse.Ugradert
                            SnapshotApiDao.Personinfo.Adressebeskyttelse.Fortrolig -> Adressebeskyttelse.Fortrolig
                            SnapshotApiDao.Personinfo.Adressebeskyttelse.StrengtFortrolig -> Adressebeskyttelse.StrengtFortrolig
                            SnapshotApiDao.Personinfo.Adressebeskyttelse.StrengtFortroligUtland -> Adressebeskyttelse.StrengtFortroligUtland
                            SnapshotApiDao.Personinfo.Adressebeskyttelse.Ukjent -> Adressebeskyttelse.Ukjent
                        }
                    },
            ) to graphqlPerson
        }
    }

    private fun hentOgLagre(fødselsnummer: String) {
        snapshotClient.hentSnapshot(fødselsnummer).data?.person?.let {
            snapshotDao.lagre(fødselsnummer, it)
        }
    }
}
