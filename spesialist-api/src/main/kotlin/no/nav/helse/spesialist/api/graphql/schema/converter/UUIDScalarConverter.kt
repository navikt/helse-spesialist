package no.nav.helse.spesialist.api.graphql.schema.converter

import com.expediagroup.graphql.client.converter.ScalarConverter
import java.util.UUID

/**
 * Brukes ved SERDE av [no.nav.helse.spleis.graphql.hentsnapshot.GraphQLPerson].
 */
class UUIDScalarConverter : ScalarConverter<UUID> {
    override fun toJson(value: UUID) = value.toString()

    override fun toScalar(rawValue: Any): UUID = UUID.fromString(rawValue as String)
}
