package no.nav.helse

import io.ktor.application.*
import io.ktor.auth.*

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
