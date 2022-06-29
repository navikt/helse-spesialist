package no.nav.helse.spesialist.api

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper

internal val objectMapper = jacksonObjectMapper()
    .registerModule(JavaTimeModule())
    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
