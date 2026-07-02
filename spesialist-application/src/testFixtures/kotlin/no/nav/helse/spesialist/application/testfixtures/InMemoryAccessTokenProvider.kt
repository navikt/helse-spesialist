package no.nav.helse.spesialist.application.testfixtures

import com.github.navikt.tbd_libs.access_token.AccessTokenProvider

class InMemoryAccessTokenProvider(
    private val token: String = "et-testtoken",
) : AccessTokenProvider {
    override fun machineToken(scope: String) = token

    override fun oboToken(
        accessToken: String,
        scope: String,
    ) = token
}
