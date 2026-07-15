package no.nav.helse.spleis.graphql.scalars

import com.expediagroup.graphql.client.Generated
import no.nav.helse.spesialist.client.spleis.converters.LocalDateScalarConverter
import tools.jackson.databind.util.StdConverter
import java.time.LocalDate

@Generated
public class LocalDateToAnyConverter : StdConverter<LocalDate, Any>() {
    private val converter: LocalDateScalarConverter = LocalDateScalarConverter()

    override fun convert(`value`: LocalDate): Any = converter.toJson(value)
}
