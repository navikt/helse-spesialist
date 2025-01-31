package no.nav.helse.db.api

import no.nav.helse.spleis.graphql.hentsnapshot.GraphQLPerson
import java.time.LocalDate

interface SnapshotApiDao {
    fun hentSnapshotMedMetadata(fødselsnummer: String): Pair<Personinfo, GraphQLPerson>?

    fun lagre(
        fødselsnummer: String,
        snapshot: GraphQLPerson,
    ): Int

    data class Personinfo(
        val fornavn: String,
        val mellomnavn: String?,
        val etternavn: String,
        val fodselsdato: LocalDate,
        val kjonn: Kjonn,
        val adressebeskyttelse: Adressebeskyttelse,
    ) {
        enum class Kjonn {
            Kvinne,
            Mann,
            Ukjent,
        }

        enum class Adressebeskyttelse {
            Ugradert,
            Fortrolig,
            StrengtFortrolig,
            StrengtFortroligUtland,
            Ukjent,
        }
    }
}
