package no.nav.helse.mediator.api.graphql

import com.expediagroup.graphql.server.operations.Query
import graphql.GraphQLError
import graphql.GraphqlErrorException
import graphql.schema.DataFetchingEnvironment
import no.nav.helse.modell.egenansatt.EgenAnsattDao
import no.nav.helse.person.Adressebeskyttelse
import no.nav.helse.person.PersonApiDao

abstract class AbstractPersonQuery(
    protected val personApiDao: PersonApiDao,
    protected val egenAnsattDao: EgenAnsattDao,
) : Query {

    protected fun isForbidden(fnr: String, env: DataFetchingEnvironment): Boolean {
        val kanSeSkjermede = env.graphQlContext.get<Boolean>("kanSeSkjermedePersoner")
        val erSkjermet = egenAnsattDao.erEgenAnsatt(fnr) ?: return true
        if (erSkjermet && !kanSeSkjermede) return true

        val kanSeKode7 = env.graphQlContext.get<Boolean>("kanSeKode7")
        val erFortrolig = personApiDao.personHarAdressebeskyttelse(fnr, Adressebeskyttelse.Fortrolig)
        val erUgradert = personApiDao.personHarAdressebeskyttelse(fnr, Adressebeskyttelse.Ugradert)
        return (!kanSeKode7 && erFortrolig) || (!erFortrolig && !erUgradert)
    }

    protected fun getForbiddenError(fnr: String): GraphQLError = GraphqlErrorException.newErrorException()
        .message("Har ikke tilgang til person med fødselsnummer $fnr")
        .extensions(mapOf("code" to 403, "field" to "person"))
        .build()

    protected fun getNotFoundError(fnr: String? = null): GraphQLError = GraphqlErrorException.newErrorException()
        .message("Finner ikke data for person med fødselsnummer $fnr")
        .extensions(mapOf("code" to 404, "field" to "person"))
        .build()

}
