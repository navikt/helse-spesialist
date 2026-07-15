package no.nav.helse.spleis.graphql.scalars

import com.expediagroup.graphql.client.Generated
import no.nav.helse.spesialist.client.spleis.converters.LocalDateScalarConverter
import tools.jackson.databind.util.StdConverter
import java.time.LocalDate

@Generated
public class AnyToLocalDateConverter : StdConverter<Any, LocalDate>() {
    private val converter: LocalDateScalarConverter = LocalDateScalarConverter()

    override fun convert(`value`: Any): LocalDate = converter.toScalar(value)
}
