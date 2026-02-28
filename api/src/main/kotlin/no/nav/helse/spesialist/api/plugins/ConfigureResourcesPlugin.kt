package no.nav.helse.spesialist.api.plugins

import io.ktor.resources.Resources

fun Resources.Configuration.configureResourcesPlugin() {
    serializersModule = customSerializersModule
}
