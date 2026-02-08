package no.nav.helse.spesialist.domain

import no.nav.helse.spesialist.domain.ddd.ValueObject
import java.time.Instant

data class EgenAnsattStatus(
    val erEgenAnsatt: Boolean,
    val oppdatertTidspunkt: Instant,
) : ValueObject
