package no.nav.helse.spesialist.client.spillkar

import no.nav.helse.spesialist.application.AccessTokenGenerator

class ClientSpillkarModule(
    configuration: Configuration,
    accessTokenGenerator: AccessTokenGenerator,
) {
    data class Configuration(
        val apiUrl: String,
        val scope: String,
    )

    val inngangsvilkårHenter =
        SpillkarClientInngangsvilkårHenter(
            configuration = configuration,
            accessTokenGenerator = accessTokenGenerator,
        )

    val inngangsvilkårInnsender =
        SpillkarClientInngangsvilkårInnsender(
            configuration = configuration,
            accessTokenGenerator = accessTokenGenerator,
        )
}
