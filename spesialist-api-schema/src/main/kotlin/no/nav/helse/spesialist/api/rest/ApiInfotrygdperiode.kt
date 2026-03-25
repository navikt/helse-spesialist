package no.nav.helse.spesialist.api.rest

import java.math.BigDecimal
import java.time.LocalDate

data class ApiInfotrygdperiode(
    val fom: LocalDate,
    val tom: LocalDate,
    val grad: Int,
    val dagsats: BigDecimal,
    val typetekst: String,
    val organisasjonsnummer: String?,
)
