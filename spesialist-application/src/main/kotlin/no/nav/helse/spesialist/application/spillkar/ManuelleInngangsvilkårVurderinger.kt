package no.nav.helse.spesialist.application.spillkar

import java.time.Instant
import java.time.LocalDate

data class ManuelleInngangsvilkårVurderinger(
    val personidentifikator: String,
    val skjæringstidspunkt: LocalDate,
    val versjon: Int,
    val vurderinger: List<ManuellInngangsvilkårVurdering>,
)

data class ManuellInngangsvilkårVurdering(
    val vilkårskode: String,
    val vurderingskode: String,
    val tidspunkt: Instant,
    val begrunnelse: String,
)
