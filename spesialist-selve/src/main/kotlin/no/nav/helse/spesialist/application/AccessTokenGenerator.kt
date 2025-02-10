package no.nav.helse.spesialist.application

interface AccessTokenGenerator {
    suspend fun hentAccessToken(scope: String): String
}
