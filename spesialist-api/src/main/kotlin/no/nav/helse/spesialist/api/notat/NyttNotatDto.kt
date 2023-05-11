package no.nav.helse.spesialist.api.notat

import no.nav.helse.spesialist.api.graphql.schema.NotatType

data class NyttNotatDto(
    val tekst: String,
    val type: NotatType
)