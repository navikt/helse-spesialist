package no.nav.helse.db.api

import no.nav.helse.spesialist.application.logg.logg
import java.time.LocalDate
import java.util.UUID

data class VedtaksperiodeDbDto(
    val vedtaksperiodeId: UUID,
    val fom: LocalDate,
    val tom: LocalDate,
    val skjæringstidspunkt: LocalDate,
    val tags: Set<String>,
    val varsler: Set<VarselDbDto>,
) {
    fun vedtaksperiodeId() = this.vedtaksperiodeId

    fun tidligereEnnOgSammenhengende(other: VedtaksperiodeDbDto): Boolean = this.fom <= other.tom && this.skjæringstidspunkt == other.skjæringstidspunkt

    // I stedet for å logge her, kunne man ha bygget opp et feilresponsobjekt og returnert det i stedet for en boolean,
    // og så logge på høyere nivå og med mer info, for eksempel organisasjonsnummer (og kanskje sende med detaljer i
    // responsen på GraphQL-kallet?).
    private fun harAktiveVarsler(): Boolean {
        val aktiveVarsler = varsler.filter { it.erAktiv() }
        val harAktiveVarsler = aktiveVarsler.isNotEmpty()
        if (harAktiveVarsler) {
            val koder = aktiveVarsler.map { it.kode }
            logg.info("Vedtaksperiode med fom=$fom, tom=$tom har aktive varsler, med kode(r): $koder")
        }
        return harAktiveVarsler
    }

    companion object {
        fun Set<VedtaksperiodeDbDto>.harAktiveVarsler(): Boolean = any { it.harAktiveVarsler() }
    }
}
