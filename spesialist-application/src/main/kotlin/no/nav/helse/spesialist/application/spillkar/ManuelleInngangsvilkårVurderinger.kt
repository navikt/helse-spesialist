package no.nav.helse.spesialist.application.spillkar

import java.time.LocalDate
import java.time.LocalDateTime

data class ManuelleInngangsvilkårVurderinger(
    val personidentifikator: String,
    val skjæringstidspunkt: LocalDate,
    val versjon: Int,
    val vurderinger: List<ManuellInngangsvilkårVurdering>,
)

data class ManuellInngangsvilkårVurdering(
    val vilkårskode: String,
    val vurderingskode: String,
    val tidspunkt: LocalDateTime,
    val begrunnelse: String,
)
