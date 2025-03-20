package no.nav.helse.spesialist.client.krr

import no.nav.helse.spesialist.application.AccessTokenGenerator
import no.nav.helse.spesialist.application.Reservasjonshenter
import no.nav.helse.spesialist.application.logg.logg

class ClientKrrModule(
    configuration: Configuration,
    accessTokenGenerator: AccessTokenGenerator,
) {
    data class Configuration(
        val client: Client?,
    ) {
        data class Client(
            val apiUrl: String,
            val scope: String,
        )
    }

    val reservasjonshenter = (
        configuration.client?.let {
            KRRClientReservasjonshenter(
                configuration = it,
                accessTokenGenerator = accessTokenGenerator,
            )
        }
            ?: Reservasjonshenter { null }.also { logg.info("Bruker nulloperasjonsversjon av reservasjonshenter") }
    )
}
