package no.nav.helse.spesialist.application

fun interface OboAccessTokenGenerator {
    fun hentOboAccessToken(
        scope: String,
        userToken: String,
    ): String
}
