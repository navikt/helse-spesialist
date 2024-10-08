package no.nav.helse.spesialist.api.graphql.query

import com.expediagroup.graphql.server.operations.Query
import graphql.GraphQLError
import graphql.schema.DataFetchingEnvironment
import no.nav.helse.spesialist.api.egenAnsatt.EgenAnsattApiDao
import no.nav.helse.spesialist.api.graphql.ContextValues.TILGANGER
import no.nav.helse.spesialist.api.graphql.forbiddenError
import no.nav.helse.spesialist.api.person.PersonApiDao
import no.nav.helse.spesialist.api.saksbehandler.manglerTilgang

abstract class AbstractPersonQuery(
    protected val personApiDao: PersonApiDao,
    private val egenAnsattApiDao: EgenAnsattApiDao,
) : Query {
    protected fun isForbidden(
        fnr: String,
        env: DataFetchingEnvironment,
    ): Boolean = manglerTilgang(egenAnsattApiDao, personApiDao, fnr, env.graphQlContext.get(TILGANGER))

    protected fun getForbiddenError(fødselsnummer: String): GraphQLError = forbiddenError(fødselsnummer)
}
