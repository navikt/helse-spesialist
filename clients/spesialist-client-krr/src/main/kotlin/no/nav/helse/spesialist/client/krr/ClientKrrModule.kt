package no.nav.helse.spesialist.client.krr

import no.nav.helse.spesialist.application.AccessTokenGenerator
import no.nav.helse.spesialist.application.Cache
import no.nav.helse.spesialist.application.KrrRegistrertStatusHenter
import no.nav.helse.spesialist.application.logg.logg

class ClientKrrModule(
    configuration: Configuration,
    accessTokenGenerator: AccessTokenGenerator,
    cache: Cache,
) {
    data class Configuration(
        val client: Client?,
    ) {
        data class Client(
            val apiUrl: String,
            val scope: String,
        )
    }

    val krrRegistrertStatusHenter = (
        configuration.client?.let {
            KRRClientKrrRegistrertStatusHenter(
                configuration = it,
                accessTokenGenerator = accessTokenGenerator,
                cache = cache,
            )
        }
            ?: KrrRegistrertStatusHenter { KrrRegistrertStatusHenter.KrrRegistrertStatus.IKKE_REGISTRERT_I_KRR }
                .also { logg.info("Bruker nulloperasjonsversjon av reservasjonshenter") }
    )
}
