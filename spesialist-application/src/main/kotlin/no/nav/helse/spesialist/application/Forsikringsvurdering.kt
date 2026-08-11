package no.nav.helse.spesialist.application

import no.nav.helse.spesialist.domain.Identitetsnummer
import java.time.LocalDate

open class Forsikring(
    val virkningsdato: LocalDate,
    val opphørsdato: LocalDate?,
    val dekningsgrad: Int,
    val dekningIVentetid: Boolean,
)

enum class Ekskluderingsårsak {
    SKJÆRINGSTIDSPUNKT_INNEN_28_DAGER_FØR_VIRKNINGSDATO,
    SKJÆRINGSTIDSPUNKT_MER_ENN_28_DAGER_FØR_VIRKNINGSDATO,
    OPPHØRT_PÅ_SKJÆRINGSTIDSPUNKT,
    ALDRI_BETALT,
}

class EkskludertForsikring(
    virkningsdato: LocalDate,
    opphørsdato: LocalDate?,
    dekningsgrad: Int,
    dekningIVentetid: Boolean,
    val ekskluderingsårsak: Ekskluderingsårsak,
) : Forsikring(
        virkningsdato,
        opphørsdato,
        dekningsgrad,
        dekningIVentetid,
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
