package no.nav.helse.mediator.api.graphql

import com.expediagroup.graphql.server.operations.Query
import graphql.GraphqlErrorException
import graphql.execution.DataFetcherResult
import io.ktor.features.*
import no.nav.helse.arbeidsgiver.ArbeidsgiverApiDao
import no.nav.helse.mediator.api.graphql.schema.Person
import no.nav.helse.modell.SnapshotDao
import no.nav.helse.overstyring.OverstyringApiDao
import no.nav.helse.person.PersonApiDao
import no.nav.helse.tildeling.TildelingDao

class PersonQuery(
    private val snapshotDao: SnapshotDao,
    private val personApiDao: PersonApiDao,
    private val tildelingDao: TildelingDao,
    private val arbeidsgiverApiDao: ArbeidsgiverApiDao,
    private val overstyringApiDao: OverstyringApiDao
) : Query {
    fun person(fnr: String): DataFetcherResult<Person?> {
        val person = snapshotDao.hentSnapshotMedMetadata(fnr)?.let { (personinfo, snapshot) ->
            Person(
                snapshot = snapshot,
                personinfo = personinfo,
                personApiDao = personApiDao,
                tildelingDao = tildelingDao,
                arbeidsgiverApiDao = arbeidsgiverApiDao,
                overstyringApiDao = overstyringApiDao
            )
        }

        val error = if (person == null) GraphqlErrorException.newErrorException()
            .cause(NotFoundException())
            .message("Finner ikke snapshot for person med fødselsnummer $fnr")
            .extensions(mapOf("code" to 404, "field" to "person"))
            .build() else null

        return DataFetcherResult.newResult<Person?>()
            .data(person)
            .error(error)
            .build()
    }

}
