package no.nav.helse.spesialist.api.rest.andreYtelser

import no.nav.helse.spesialist.api.rest.ApiEndringsgruppe
import no.nav.helse.spesialist.api.rest.ApiEndringsrad
import no.nav.helse.spesialist.api.rest.ApiHandlingsvisning
import no.nav.helse.spesialist.api.rest.ApiVisningsfelt
import no.nav.helse.spesialist.domain.andreytelser.Grad
import no.nav.helse.spesialist.domain.andreytelser.GraderteAndreYtelserEndretDag
import no.nav.helse.spesialist.domain.andreytelser.GraderteAndreYtelserEvent
import no.nav.helse.spesialist.domain.andreytelser.GraderteAndreYtelserType
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val datoformat = DateTimeFormatter.ofPattern("dd.MM.yyyy")

internal fun GraderteAndreYtelserEvent.tilHandlingsvisning() =
    ApiHandlingsvisning(
        felter =
            listOfNotNull(
                notatTilBeslutter.takeIf { it.isNotBlank() }?.let {
                    ApiVisningsfelt.Tekst(tittel = "Begrunnelse", tekst = it)
                },
                ApiVisningsfelt.Endringer(
                    tittel = "Dager endret",
                    grupper = grupperSammenhengendeLikeDager(dager).map { it.tilEndringsgruppe() },
                ),
            ),
    )

private class Daggruppe(
    val fom: LocalDate,
    val tom: LocalDate,
    val endring: GraderteAndreYtelserEndretDag,
)

private fun grupperSammenhengendeLikeDager(dager: List<GraderteAndreYtelserEndretDag>): List<Daggruppe> =
    dager.sortedBy { it.dato }.fold(emptyList()) { grupper, dag ->
        val forrige = grupper.lastOrNull()
        if (forrige != null && forrige.tom.plusDays(1) == dag.dato && forrige.endring == dag.copy(dato = forrige.endring.dato)) {
            grupper.dropLast(1) + Daggruppe(forrige.fom, dag.dato, forrige.endring)
        } else {
            grupper + Daggruppe(dag.dato, dag.dato, dag)
        }
    }

private fun Daggruppe.tilEndringsgruppe(): ApiEndringsgruppe {
    val datotekst = if (fom == tom) fom.format(datoformat) else "${fom.format(datoformat)} - ${tom.format(datoformat)}"
    val fraYtelser = endring.fraAndreYtelserGrader
    val ytelsestyper = (endring.andreYtelserGrader.keys + fraYtelser?.keys.orEmpty()).sorted()
    return ApiEndringsgruppe(
        rader =
            listOf(ApiEndringsrad(navn = "Dato", fra = null, til = datotekst)) +
                ytelsestyper.map { type ->
                    endringsrad(type.visningsnavn(), fraYtelser?.let { it[type] ?: 0 }, endring.andreYtelserGrader[type] ?: 0)
                } +
                endringsrad("Sykdomsgrad", endring.fraSykdomsgrad, endring.sykdomsgrad) +
                endringsrad("Arbeidsgrad", endring.fraArbeidsgrad, endring.arbeidsgrad),
    )
}

private fun endringsrad(
    navn: String,
    fra: Grad?,
    til: Grad,
) = ApiEndringsrad(
    navn = navn,
    fra = fra?.takeIf { it != til }?.let(::prosent),
    til = prosent(til),
)

private fun prosent(grad: Grad) = "$grad %"

private fun GraderteAndreYtelserType.visningsnavn() =
    when (this) {
        GraderteAndreYtelserType.FORELDREPENGER -> "Foreldrepenger"
        GraderteAndreYtelserType.SVANGERSKAPSPENGER -> "Svangerskapspenger"
        GraderteAndreYtelserType.OMSORGSPENGER -> "Omsorgspenger"
        GraderteAndreYtelserType.PLEIEPENGER -> "Pleiepenger"
        GraderteAndreYtelserType.OPPLARINGSPENGER -> "Opplæringspenger"
    }
