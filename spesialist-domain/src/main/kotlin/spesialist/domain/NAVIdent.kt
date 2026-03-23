package no.nav.helse.spesialist.domain

import no.nav.helse.spesialist.domain.ddd.ValueObject

@JvmInline
value class NAVIdent(
    val value: String,
) : ValueObject {
    init {
        check(Regex("^[A-Z]\\d{6}$").matches(value)) { "Ikke gyldig NAV-ident" }
    }
}
