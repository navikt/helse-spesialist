package no.nav.helse.spesialist.application

import no.nav.helse.spesialist.domain.Identitetsnummer
import java.time.LocalDate

data class Folketrygdlovenreferanse(
    val kapittel: Int,
    val paragrafIKapittel: Int,
    val ledd: Int?,
    val bokstav: Char?,
)

open class Forsikring(
    val virkningsdato: LocalDate,
    val opphørsdato: LocalDate?,
    val dekningsgrad: Int,
    val dekningIVentetid: Boolean,
    val navn: String,
    val folketrygdlovenreferanse: Folketrygdlovenreferanse,
)

enum class Ekskluderingsårsak {
    SKJÆRINGSTIDSPUNKT_INNEN_28_DAGER_FØR_VIRKNINGSDATO,
    SKJÆRINGSTIDSPUNKT_MER_ENN_28_DAGER_FØR_VIRKNINGSDATO,
    OPPHØRT_PÅ_SKJÆRINGSTIDSPUNKT,
    ALDRI_BETALT,
}

data class Ekskluderingsbegrunnelse(
    val forklaring: String,
    val folketrygdlovenreferanse: Folketrygdlovenreferanse?,
)

class EkskludertForsikring(
    virkningsdato: LocalDate,
    opphørsdato: LocalDate?,
    dekningsgrad: Int,
    dekningIVentetid: Boolean,
    navn: String,
    folketrygdlovenreferanse: Folketrygdlovenreferanse,
    val ekskluderingsårsak: Ekskluderingsårsak,
    val ekskluderingsbegrunnelse: Ekskluderingsbegrunnelse,
) : Forsikring(
        virkningsdato,
        opphørsdato,
        dekningsgrad,
        dekningIVentetid,
        navn,
        folketrygdlovenreferanse,
    )

data class Forsikringsvurdering(
    val identitetsnummer: Identitetsnummer,
    val harForsikring: Boolean,
    val dekning: Dekning?,
    val ekskluderteForsikringer: List<EkskludertForsikring>,
    val gjeldendeForsikring: Forsikring?,
) {
    data class Dekning(
        val grad: Int,
        val fraDag: Int,
    )
}
