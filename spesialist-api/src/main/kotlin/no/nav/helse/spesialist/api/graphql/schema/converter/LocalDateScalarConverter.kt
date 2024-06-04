package no.nav.helse.spesialist.api.graphql.schema.converter

import com.expediagroup.graphql.client.converter.ScalarConverter
import java.time.LocalDate

/**
 * Brukes ved SERDE av [no.nav.helse.spleis.graphql.hentsnapshot.GraphQLPerson].
 */
class LocalDateScalarConverter : ScalarConverter<LocalDate> {
    override fun toJson(value: LocalDate) = value.toString()

    override fun toScalar(rawValue: Any): LocalDate = LocalDate.parse(rawValue as String)
}
