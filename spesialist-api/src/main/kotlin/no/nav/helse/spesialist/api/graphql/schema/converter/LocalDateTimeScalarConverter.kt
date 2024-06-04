package no.nav.helse.spesialist.api.graphql.schema.converter

import com.expediagroup.graphql.client.converter.ScalarConverter
import java.time.LocalDateTime

/**
 * Brukes ved SERDE av [no.nav.helse.spleis.graphql.hentsnapshot.GraphQLPerson].
 */
class LocalDateTimeScalarConverter : ScalarConverter<LocalDateTime> {
    override fun toJson(value: LocalDateTime) = value.toString()

    override fun toScalar(rawValue: Any): LocalDateTime = LocalDateTime.parse(rawValue as String)
}
