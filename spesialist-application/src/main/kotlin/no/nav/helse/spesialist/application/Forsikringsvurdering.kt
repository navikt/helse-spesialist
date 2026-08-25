package no.nav.helse.spesialist.application

import no.nav.helse.spesialist.domain.Identitetsnummer
import java.time.Instant
import java.time.LocalDate

data class Folketrygdlovenreferanse(
    val kapittel: Int,
    val paragrafIKapittel: Int,
    val ledd: Int?,
    val bokstav: Char?,
)

data class KollektivForsikring(
    val navn: String,
    val dekningFolketrygdlovenreferanse: Folketrygdlovenreferanse,
    val kollektivFolketrygdlovenreferanse: Folketrygdlovenreferanse,
)

data class IndividuellForsikring(
    val navn: String,
    val dekningFolketrygdlovenreferanse: Folketrygdlovenreferanse,
    val virkningsdato: LocalDate,
    val opphørsdato: LocalDate?,
    val konklusjon: Konklusjon,
    val lagtTilGrunn: Boolean,
) {
    data class Konklusjon(
        val forklaring: String,
        val folketrygdlovenreferanse: Folketrygdlovenreferanse?,
    )
}

data class Forsikringsvurdering(
    val identitetsnummer: Identitetsnummer,
    val samletDekning: Dekning?,
    val kollektivForsikring: KollektivForsikring?,
    val individuelleForsikringer: List<IndividuellForsikring>,
    val vurdertTidspunkt: Instant,
) {
    data class Dekning(
        val grad: Int,
        val fraDag: Int,
    )
}
