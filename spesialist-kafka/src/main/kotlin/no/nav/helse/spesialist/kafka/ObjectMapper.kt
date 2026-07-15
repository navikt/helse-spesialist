package no.nav.helse.spesialist.kafka

import tools.jackson.databind.ObjectMapper
import tools.jackson.databind.introspect.DefaultAccessorNamingStrategy
import tools.jackson.module.kotlin.jacksonMapperBuilder

val objectMapper: ObjectMapper =
    jacksonMapperBuilder()
        .accessorNaming(DefaultAccessorNamingStrategy.Provider().withFirstCharAcceptance(true, true))
        .build()
