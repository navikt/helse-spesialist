package no.nav.helse.spesialist.api.graphql

import com.expediagroup.graphql.generator.hooks.SchemaGeneratorHooks
import graphql.GraphQLContext
import graphql.execution.CoercedVariables
import graphql.language.StringValue
import graphql.language.Value
import graphql.schema.Coercing
import graphql.schema.GraphQLScalarType
import graphql.schema.GraphQLType
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Year
import java.time.YearMonth
import java.util.Locale
import java.util.UUID
import kotlin.reflect.KClass
import kotlin.reflect.KType

/**
 * Støttekode for å kunne bruke UUID som type, i stedet for å måtte ha den som String.
 */
val schemaGeneratorHooks =
    object : SchemaGeneratorHooks {
        override fun willGenerateGraphQLType(type: KType): GraphQLType? =
            when (type.classifier as? KClass<*>) {
                UUID::class -> graphQLUUID
                BigDecimal::class -> graphQLBigDecimal
                LocalDateTime::class -> graphQLLocalDateTime
                LocalDate::class -> graphQLLocalDate
                YearMonth::class -> graphQLYearMonth
                Year::class -> graphQLYear
                else -> null
            }
    }

private val graphQLLocalDateTime: GraphQLScalarType =
    GraphQLScalarType
        .newScalar()
        .name(LocalDateTime::class.simpleName)
        .description(LocalDateTime::class.toString())
        .coercing(LocalDateTimeCoercing)
        .build()

private val graphQLLocalDate: GraphQLScalarType =
    GraphQLScalarType
        .newScalar()
        .name(LocalDate::class.simpleName)
        .description(LocalDate::class.toString())
        .coercing(LocalDateCoercing)
        .build()

private val graphQLYearMonth: GraphQLScalarType =
    GraphQLScalarType
        .newScalar()
        .name(YearMonth::class.simpleName)
        .description(YearMonth::class.toString())
        .coercing(YearMonthCoercing)
        .build()

private val graphQLYear: GraphQLScalarType =
    GraphQLScalarType
        .newScalar()
        .name(Year::class.simpleName)
        .description(Year::class.toString())
        .coercing(YearCoercing)
        .build()

private val graphQLUUID: GraphQLScalarType =
    GraphQLScalarType
        .newScalar()
        .name(UUID::class.simpleName)
        .description(UUID::class.toString())
        .coercing(UuidCoercing)
        .build()

private val graphQLBigDecimal: GraphQLScalarType =
    GraphQLScalarType
        .newScalar()
        .name(BigDecimal::class.simpleName)
        .description(BigDecimal::class.toString())
        .coercing(BigDecimalCoercing)
        .build()

private object UuidCoercing : Coercing<UUID, String> {
    override fun serialize(
        dataFetcherResult: Any,
        graphQLContext: GraphQLContext,
        locale: Locale,
    ): String =
        when (dataFetcherResult) {
            is StringValue -> dataFetcherResult.value
            else -> dataFetcherResult.toString()
        }

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

private object BigDecimalCoercing : Coercing<BigDecimal, String> {
    override fun serialize(
        dataFetcherResult: Any,
        graphQLContext: GraphQLContext,
        locale: Locale,
    ): String =
        when (dataFetcherResult) {
            is StringValue -> dataFetcherResult.value
            else -> dataFetcherResult.toString()
        }

    override fun parseValue(
        input: Any,
        graphQLContext: GraphQLContext,
        locale: Locale,
    ): BigDecimal = BigDecimal(serialize(input, graphQLContext, locale))

    override fun parseLiteral(
        input: Value<*>,
        variables: CoercedVariables,
        graphQLContext: GraphQLContext,
        locale: Locale,
    ): BigDecimal = BigDecimal(serialize(input, graphQLContext, locale))
}

private object LocalDateTimeCoercing : Coercing<LocalDateTime, String> {
    override fun serialize(
        dataFetcherResult: Any,
        graphQLContext: GraphQLContext,
        locale: Locale,
    ): String =
        when (dataFetcherResult) {
            is StringValue -> dataFetcherResult.value
            else -> dataFetcherResult.toString()
        }

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

private object LocalDateCoercing : Coercing<LocalDate, String> {
    override fun serialize(
        dataFetcherResult: Any,
        graphQLContext: GraphQLContext,
        locale: Locale,
    ): String =
        when (dataFetcherResult) {
            is StringValue -> dataFetcherResult.value
            else -> dataFetcherResult.toString()
        }

    override fun parseValue(
        input: Any,
        graphQLContext: GraphQLContext,
        locale: Locale,
    ): LocalDate = LocalDate.parse(serialize(input, graphQLContext, locale))

    override fun parseLiteral(
        input: Value<*>,
        variables: CoercedVariables,
        graphQLContext: GraphQLContext,
        locale: Locale,
    ): LocalDate = LocalDate.parse(serialize(input, graphQLContext, locale))
}

private object YearMonthCoercing : Coercing<YearMonth, String> {
    override fun serialize(
        dataFetcherResult: Any,
        graphQLContext: GraphQLContext,
        locale: Locale,
    ): String =
        when (dataFetcherResult) {
            is StringValue -> dataFetcherResult.value
            else -> dataFetcherResult.toString()
        }

    override fun parseValue(
        input: Any,
        graphQLContext: GraphQLContext,
        locale: Locale,
    ): YearMonth = YearMonth.parse(serialize(input, graphQLContext, locale))

    override fun parseLiteral(
        input: Value<*>,
        variables: CoercedVariables,
        graphQLContext: GraphQLContext,
        locale: Locale,
    ): YearMonth = YearMonth.parse(serialize(input, graphQLContext, locale))
}

private object YearCoercing : Coercing<Year, String> {
    override fun serialize(
        dataFetcherResult: Any,
        graphQLContext: GraphQLContext,
        locale: Locale,
    ): String =
        when (dataFetcherResult) {
            is StringValue -> dataFetcherResult.value
            else -> dataFetcherResult.toString()
        }

    override fun parseValue(
        input: Any,
        graphQLContext: GraphQLContext,
        locale: Locale,
    ): Year = Year.parse(serialize(input, graphQLContext, locale))

    override fun parseLiteral(
        input: Value<*>,
        variables: CoercedVariables,
        graphQLContext: GraphQLContext,
        locale: Locale,
    ): Year = Year.parse(serialize(input, graphQLContext, locale))
}
