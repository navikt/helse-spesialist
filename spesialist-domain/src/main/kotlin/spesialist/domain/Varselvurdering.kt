package no.nav.helse.spesialist.domain

import no.nav.helse.spesialist.domain.ddd.ValueObject
import java.time.LocalDateTime

data class Varselvurdering(
    val saksbehandlerId: SaksbehandlerOid,
    val tidspunkt: LocalDateTime,
    val vurdertDefinisjonId: VarseldefinisjonId,
) : ValueObject
