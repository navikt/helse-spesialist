package no.nav.helse.spesialist.domain

import no.nav.helse.spesialist.domain.ddd.ValueObject
import java.util.UUID

@JvmInline
value class ForsikringsvurderingId(
    val value: UUID,
) : ValueObject
