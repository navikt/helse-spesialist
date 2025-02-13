package no.nav.helse.spesialist.api.snapshot

import no.nav.helse.db.api.PersoninfoDao
import no.nav.helse.spesialist.api.graphql.schema.ApiAdressebeskyttelse
import no.nav.helse.spesialist.api.graphql.schema.ApiKjonn
import no.nav.helse.spesialist.api.graphql.schema.ApiPersoninfo
import no.nav.helse.spesialist.application.Snapshothenter
import no.nav.helse.spesialist.application.snapshot.SnapshotPerson
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class SnapshotService(private val personinfoDao: PersoninfoDao, private val snapshothenter: Snapshothenter) {
    private val sikkerLogg: Logger = LoggerFactory.getLogger("tjenestekall")

    fun hentSnapshot(fødselsnummer: String): Pair<ApiPersoninfo, SnapshotPerson>? {
        sikkerLogg.info("Henter snapshot for person med fødselsnummer=$fødselsnummer")
        val graphqlPerson =
            snapshothenter.hentPerson(fødselsnummer)
                ?: return null.also { sikkerLogg.warn("Fikk ikke personsnapshot fra Spleis") }
        val personinfo =
            personinfoDao.hentPersoninfo(fødselsnummer)
                ?: error("Fant ikke personinfo i databasen")

        return ApiPersoninfo(
            fornavn = personinfo.fornavn,
            mellomnavn = personinfo.mellomnavn,
            etternavn = personinfo.etternavn,
            fodselsdato = personinfo.fodselsdato,
            kjonn =
                when (personinfo.kjonn) {
                    PersoninfoDao.Personinfo.Kjonn.Kvinne -> ApiKjonn.Kvinne
                    PersoninfoDao.Personinfo.Kjonn.Mann -> ApiKjonn.Mann
                    PersoninfoDao.Personinfo.Kjonn.Ukjent -> ApiKjonn.Ukjent
                },
            adressebeskyttelse =
                when (personinfo.adressebeskyttelse) {
                    PersoninfoDao.Personinfo.Adressebeskyttelse.Ugradert -> ApiAdressebeskyttelse.Ugradert
                    PersoninfoDao.Personinfo.Adressebeskyttelse.Fortrolig -> ApiAdressebeskyttelse.Fortrolig
                    PersoninfoDao.Personinfo.Adressebeskyttelse.StrengtFortrolig -> ApiAdressebeskyttelse.StrengtFortrolig
                    PersoninfoDao.Personinfo.Adressebeskyttelse.StrengtFortroligUtland -> ApiAdressebeskyttelse.StrengtFortroligUtland
                    PersoninfoDao.Personinfo.Adressebeskyttelse.Ukjent -> ApiAdressebeskyttelse.Ukjent
                },
        ) to graphqlPerson
    }
}
