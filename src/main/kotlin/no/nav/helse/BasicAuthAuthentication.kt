package no.nav.helse

import io.ktor.application.Application
import io.ktor.application.install
import io.ktor.auth.Authentication
import io.ktor.auth.UserIdPrincipal
import io.ktor.auth.basic

internal fun Application.basicauthAuthenticaiton(
    adminSecret: String
) {
    install(Authentication) {
        basic(name = "admin") {
            this.validate {
                if (it.password == adminSecret) {
                    UserIdPrincipal("admin")
                } else null
            }
        }
    }
}
