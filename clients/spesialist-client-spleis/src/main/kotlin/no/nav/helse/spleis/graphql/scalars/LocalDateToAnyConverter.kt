package no.nav.helse.spleis.graphql.scalars

import com.expediagroup.graphql.client.Generated
import com.fasterxml.jackson.databind.util.StdConverter
import no.nav.helse.spesialist.client.spleis.converters.LocalDateScalarConverter
import java.time.LocalDate

@Generated
public class LocalDateToAnyConverter : StdConverter<LocalDate, Any>() {
    private val converter: LocalDateScalarConverter = LocalDateScalarConverter()

    override fun convert(`value`: LocalDate): Any = converter.toJson(value)
}
