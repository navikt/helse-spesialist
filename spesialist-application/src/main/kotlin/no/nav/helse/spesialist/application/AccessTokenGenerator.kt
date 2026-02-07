package no.nav.helse.spesialist.application

fun interface AccessTokenGenerator {
    fun hentAccessToken(scope: String): String
}
