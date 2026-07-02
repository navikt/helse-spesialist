package no.nav.helse.spesialist.client.spillkar

import com.github.navikt.tbd_libs.access_token.AccessTokenProvider

class ClientSpillkarModule(
    configuration: Configuration,
    accessTokenProvider: AccessTokenProvider,
) {
    data class Configuration(
        val apiUrl: String,
        val scope: String,
    )

    val inngangsvilkårHenter =
        SpillkarClientInngangsvilkårHenter(
            configuration = configuration,
            accessTokenProvider = accessTokenProvider,
        )

    val inngangsvilkårInnsender =
        SpillkarClientInngangsvilkårInnsender(
            configuration = configuration,
            accessTokenProvider = accessTokenProvider,
        )
}
