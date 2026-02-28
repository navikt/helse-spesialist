package no.nav.helse.spesialist.application

import java.util.UUID

@JvmInline
value class PersonPseudoId private constructor(
    val value: UUID,
) {
    companion object {
        fun ny(): PersonPseudoId = PersonPseudoId(UUID.randomUUID())

        fun fraString(value: String): PersonPseudoId = PersonPseudoId(UUID.fromString(value))
    }
}
