package no.nav.helse.mediator.api.graphql

import com.expediagroup.graphql.generator.execution.GraphQLContext
import com.expediagroup.graphql.server.execution.GraphQLContextFactory
import io.ktor.request.*
import java.util.*

data class AuthorizedContext(val id: UUID = UUID.randomUUID()) : GraphQLContext

class ContextFactory : GraphQLContextFactory<AuthorizedContext, ApplicationRequest> {

    override suspend fun generateContextMap(request: ApplicationRequest): Map<*, Any>? {
        return super.generateContextMap(request)
    }

    override suspend fun generateContext(request: ApplicationRequest): AuthorizedContext {
        return AuthorizedContext()
    }

}
