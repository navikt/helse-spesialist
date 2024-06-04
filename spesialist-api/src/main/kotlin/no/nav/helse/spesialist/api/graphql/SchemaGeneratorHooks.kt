package no.nav.helse.spesialist.api.graphql

import com.expediagroup.graphql.generator.hooks.SchemaGeneratorHooks
import graphql.GraphQLContext
import graphql.execution.CoercedVariables
import graphql.language.Value
import graphql.schema.Coercing
import graphql.schema.GraphQLScalarType
import graphql.schema.GraphQLType
import java.time.LocalDateTime
import java.util.Locale
import java.util.UUID
import kotlin.reflect.KClass
import kotlin.reflect.KType

/**
 * Støttekode for å kunne bruke UUID som type, i stedet for å måtte ha den som String.
 */
internal val schemaGeneratorHooks =
    object : SchemaGeneratorHooks {
        override fun willGenerateGraphQLType(type: KType): GraphQLType? =
            when (type.classifier as? KClass<*>) {
                UUID::class -> graphQLUUID
                LocalDateTime::class -> graphQLLocalDateTime
                else -> null
            }
    }

private val graphQLLocalDateTime: GraphQLScalarType =
    GraphQLScalarType.newScalar()
        .name(LocalDateTime::class.simpleName)
        .description(LocalDateTime::class.toString())
        .coercing(LocalDateTimeCoercing)
        .build()

private val graphQLUUID: GraphQLScalarType =
    GraphQLScalarType.newScalar()
        .name(UUID::class.simpleName)
        .description(UUID::class.toString())
        .coercing(UuidCoercing)
        .build()

private object UuidCoercing : Coercing<UUID, String> {
    override fun serialize(
        dataFetcherResult: Any,
        graphQLContext: GraphQLContext,
        locale: Locale,
    ) = dataFetcherResult.toString()

    override fun parseValue(
        input: Any,
        graphQLContext: GraphQLContext,
        locale: Locale,
    ): UUID = UUID.fromString(serialize(input, graphQLContext, locale))

    override fun parseLiteral(
        input: Value<*>,
        variables: CoercedVariables,
        graphQLContext: GraphQLContext,
        locale: Locale,
    ): UUID = UUID.fromString(serialize(input, graphQLContext, locale))
}

private object LocalDateTimeCoercing : Coercing<LocalDateTime, String> {
    override fun serialize(
        dataFetcherResult: Any,
        graphQLContext: GraphQLContext,
        locale: Locale,
    ) = dataFetcherResult.toString()

    override fun parseValue(
        input: Any,
        graphQLContext: GraphQLContext,
        locale: Locale,
    ): LocalDateTime = LocalDateTime.parse(serialize(input, graphQLContext, locale))

    override fun parseLiteral(
        input: Value<*>,
        variables: CoercedVariables,
        graphQLContext: GraphQLContext,
        locale: Locale,
    ): LocalDateTime = LocalDateTime.parse(serialize(input, graphQLContext, locale))
}
