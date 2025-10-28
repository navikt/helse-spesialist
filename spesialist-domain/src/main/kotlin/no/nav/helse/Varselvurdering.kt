package no.nav.helse

import no.nav.helse.spesialist.domain.SaksbehandlerOid
import no.nav.helse.spesialist.domain.ddd.ValueObject
import java.time.LocalDateTime

class Varselvurdering(
    val saksbehandlerId: SaksbehandlerOid,
    val tidspunkt: LocalDateTime,
) : ValueObject
