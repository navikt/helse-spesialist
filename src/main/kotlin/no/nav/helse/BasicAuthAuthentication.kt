package no.nav.helse

import io.ktor.application.Application
import io.ktor.auth.UserIdPrincipal
import io.ktor.auth.authentication
import io.ktor.auth.basic

internal fun Application.basicAuthentication(
    adminSecret: String
) {
    authentication {
        basic(name = "admin") {
            this.validate {
                if (it.password == adminSecret) {
                    UserIdPrincipal("admin")
                } else null
            }
        }
    }
}
