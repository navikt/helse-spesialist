package no.nav.helse.spleis.graphql.scalars

import com.expediagroup.graphql.client.Generated
import no.nav.helse.spesialist.client.spleis.converters.LocalDateTimeScalarConverter
import tools.jackson.databind.util.StdConverter
import java.time.LocalDateTime

@Generated
public class LocalDateTimeToAnyConverter : StdConverter<LocalDateTime, Any>() {
    private val converter: LocalDateTimeScalarConverter = LocalDateTimeScalarConverter()

    override fun convert(`value`: LocalDateTime): Any = converter.toJson(value)
}
