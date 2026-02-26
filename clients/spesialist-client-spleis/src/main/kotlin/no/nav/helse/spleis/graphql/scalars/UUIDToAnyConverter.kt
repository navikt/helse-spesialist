package no.nav.helse.spleis.graphql.scalars

import com.expediagroup.graphql.client.Generated
import com.fasterxml.jackson.databind.util.StdConverter
import no.nav.helse.spesialist.client.spleis.converters.UUIDScalarConverter
import java.util.UUID

@Generated
public class UUIDToAnyConverter : StdConverter<UUID, Any>() {
    private val converter: UUIDScalarConverter = UUIDScalarConverter()

    override fun convert(`value`: UUID): Any = converter.toJson(value)
}
