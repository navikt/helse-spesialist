package no.nav.helse.spesialist.application

import no.nav.helse.spesialist.application.spillkar.`SamlingAvVurderteInngangsvilkår`
import java.time.LocalDate

fun interface InngangsvilkårHenter {
    fun hentInngangsvilkår(
        personidentifikatorer: List<String>,
        skjæringstidspunkt: LocalDate,
    ): List<SamlingAvVurderteInngangsvilkår>
}
