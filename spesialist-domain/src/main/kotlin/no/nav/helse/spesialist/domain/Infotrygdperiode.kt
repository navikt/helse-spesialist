package no.nav.helse.spesialist.domain

import no.nav.helse.spesialist.domain.ddd.ValueObject
import java.math.BigDecimal
import java.time.LocalDate

data class Infotrygdperiode(
    val personidentifikator: Identitetsnummer,
    val organisasjonsnummer: String?,
    val fom: LocalDate,
    val tom: LocalDate,
    val grad: Int,
    val dagsats: BigDecimal,
    val type: String,
    val tags: Set<String>,
) : ValueObject
