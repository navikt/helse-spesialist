package no.nav.helse.spleis.graphql.scalars

import com.expediagroup.graphql.client.Generated
import com.fasterxml.jackson.databind.util.StdConverter
import no.nav.helse.spesialist.client.spleis.converters.LocalDateTimeScalarConverter
import java.time.LocalDateTime

@Generated
public class AnyToLocalDateTimeConverter : StdConverter<Any, LocalDateTime>() {
    private val converter: LocalDateTimeScalarConverter = LocalDateTimeScalarConverter()

    override fun convert(`value`: Any): LocalDateTime = converter.toScalar(value)
}
