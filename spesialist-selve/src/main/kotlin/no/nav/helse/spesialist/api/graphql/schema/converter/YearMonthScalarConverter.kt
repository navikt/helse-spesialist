package no.nav.helse.spesialist.api.graphql.schema.converter

import com.expediagroup.graphql.client.converter.ScalarConverter
import java.time.YearMonth

/**
 * Brukes ved SERDE av [no.nav.helse.spleis.graphql.hentsnapshot.GraphQLPerson].
 */
class YearMonthScalarConverter : ScalarConverter<YearMonth> {
    override fun toJson(value: YearMonth) = value.toString()

    override fun toScalar(rawValue: Any): YearMonth = YearMonth.parse(rawValue as String)
}
