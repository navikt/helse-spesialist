package no.nav.helse.spleis.graphql.scalars

import com.expediagroup.graphql.client.Generated
import no.nav.helse.spesialist.client.spleis.converters.UUIDScalarConverter
import tools.jackson.databind.util.StdConverter
import java.util.UUID

@Generated
public class AnyToUUIDConverter : StdConverter<Any, UUID>() {
    private val converter: UUIDScalarConverter = UUIDScalarConverter()

    override fun convert(`value`: Any): UUID = converter.toScalar(value)
}
