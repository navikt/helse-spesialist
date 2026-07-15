package no.nav.helse.spleis.graphql.scalars

import com.expediagroup.graphql.client.Generated
import no.nav.helse.spesialist.client.spleis.converters.YearMonthScalarConverter
import tools.jackson.databind.util.StdConverter
import java.time.YearMonth

@Generated
public class YearMonthToAnyConverter : StdConverter<YearMonth, Any>() {
    private val converter: YearMonthScalarConverter = YearMonthScalarConverter()

    override fun convert(`value`: YearMonth): Any = converter.toJson(value)
}
