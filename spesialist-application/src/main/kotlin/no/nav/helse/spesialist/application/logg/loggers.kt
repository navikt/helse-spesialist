package no.nav.helse.spesialist.application.logg

import org.slf4j.Logger
import org.slf4j.LoggerFactory

val sikkerlogg: Logger = LoggerFactory.getLogger("tjenestekall")
inline val <reified T> T.logg: Logger
    get() = LoggerFactory.getLogger(T::class.java)
