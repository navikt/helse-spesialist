package no.nav.helse.mediator.api.graphql

import com.expediagroup.graphql.server.operations.Query
import graphql.GraphQLError
import graphql.GraphqlErrorException
import graphql.execution.DataFetcherResult
import graphql.schema.DataFetchingEnvironment
import io.ktor.features.*
import no.nav.helse.arbeidsgiver.ArbeidsgiverApiDao
import no.nav.helse.mediator.api.graphql.schema.Person
import no.nav.helse.modell.SnapshotDao
import no.nav.helse.overstyring.OverstyringApiDao
import no.nav.helse.person.Adressebeskyttelse
import no.nav.helse.person.PersonApiDao
import no.nav.helse.risikovurdering.RisikovurderingApiDao
import no.nav.helse.tildeling.TildelingDao
import no.nav.helse.vedtaksperiode.VarselDao

class PersonQuery(
    private val snapshotDao: SnapshotDao,
    private val personApiDao: PersonApiDao,
    private val tildelingDao: TildelingDao,
    private val arbeidsgiverApiDao: ArbeidsgiverApiDao,
    private val overstyringApiDao: OverstyringApiDao,
    private val risikovurderingApiDao: RisikovurderingApiDao,
    private val varselDao: VarselDao,
    private val snapshotGraphQLClient: SpeilSnapshotGraphQLClient
) : Query {

    fun person(fnr: String, env: DataFetchingEnvironment): DataFetcherResult<Person?> {
        if (isForbidden(fnr, env)) {
            return DataFetcherResult.newResult<Person?>().error(getForbiddenError(fnr)).build()
        }

        if (snapshotDao.utdatert(fnr)) {
            snapshotGraphQLClient.hentSnapshot(fnr).data?.person?.let {
                snapshotDao.lagre(fnr, it)
            }
        }

        val person = snapshotDao.hentSnapshotMedMetadata(fnr)?.let { (personinfo, snapshot) ->
            Person(
                snapshot = snapshot,
                personinfo = personinfo,
                personApiDao = personApiDao,
                tildelingDao = tildelingDao,
                arbeidsgiverApiDao = arbeidsgiverApiDao,
                overstyringApiDao = overstyringApiDao,
                risikovurderingApiDao = risikovurderingApiDao,
                varselDao = varselDao
            )
        }

        return if (person == null) {
            DataFetcherResult.newResult<Person?>().error(getNotFoundError(fnr)).build()
        } else {
            DataFetcherResult.newResult<Person?>().data(person).build()
        }
    }

    private fun isForbidden(fnr: String, env: DataFetchingEnvironment): Boolean {
        val kanSeKode7 = env.graphQlContext.get<Boolean>("kanSeKode7")
        val erFortrolig = personApiDao.personHarAdressebeskyttelse(fnr, Adressebeskyttelse.Fortrolig)
        val erUgradert = personApiDao.personHarAdressebeskyttelse(fnr, Adressebeskyttelse.Ugradert)
        return (!kanSeKode7 && erFortrolig) || (!erFortrolig && !erUgradert)
    }

    private fun getNotFoundError(fnr: String): GraphQLError = GraphqlErrorException.newErrorException()
        .cause(NotFoundException())
        .message("Finner ikke snapshot for person med fødselsnummer $fnr")
        .extensions(mapOf("code" to 404, "field" to "person"))
        .build()

    private fun getForbiddenError(fnr: String): GraphQLError = GraphqlErrorException.newErrorException()
        .cause(NotFoundException())
        .message("Har ikke tilgang til person med fødselsnummer $fnr")
        .extensions(mapOf("code" to 403, "field" to "person"))
        .build()

}
