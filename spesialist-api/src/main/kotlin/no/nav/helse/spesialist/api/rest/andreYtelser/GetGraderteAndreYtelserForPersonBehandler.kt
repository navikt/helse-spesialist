package no.nav.helse.spesialist.api.rest.andreYtelser

import no.nav.helse.spesialist.api.rest.*
import no.nav.helse.spesialist.api.rest.resources.Personer
import no.nav.helse.spesialist.application.PersonPseudoId
import no.nav.helse.spesialist.application.logg.loggInfo
import no.nav.helse.spesialist.domain.andreytelser.GraderteAndreYtelser
import no.nav.helse.spesialist.domain.andreytelser.GraderteAndreYtelserType

class GetGraderteAndreYtelserForPersonBehandler : GetBehandler<Personer.PersonPseudoId.GraderteAndreYtelser, List<ApiGraderteAndreYtelser>, PersonErrorCode> {
    override val tag = Tags.GRADERTE_ANDRE_YTELSER

    override fun behandle(
        resource: Personer.PersonPseudoId.GraderteAndreYtelser,
        kallKontekst: KallKontekst,
    ): RestResponse<List<ApiGraderteAndreYtelser>, PersonErrorCode> =
        kallKontekst.medPerson(
            personPseudoId = PersonPseudoId.fraString(resource.parent.pseudoId),
        ) { person ->
            val ytelser =
                kallKontekst.transaksjon.graderteAndreYtelserRepository
                    .finnAlleForIdentitetsnummer(person.id)
                    .map { it.tilApiGraderteAndreYtelser() }
                    .sortedBy { graderteAndreYtelser -> graderteAndreYtelser.perioder.minOf { it.fom } }

            loggInfo("Hentet ${ytelser.size} graderte andre ytelser")

            RestResponse.OK(ytelser)
        }

    private fun GraderteAndreYtelser.tilApiGraderteAndreYtelser() =
        ApiGraderteAndreYtelser(
            andreYtelserId = id.value,
            perioder =
                perioder.map { periode ->
                    ApiGraderteAndreYtelserPeriode(
                        fom = periode.periode.fom,
                        tom = periode.periode.tom,
                        grad = periode.grad,
                    )
                },
            andreYtelseType = graderteAndreYtelserType.tilApiGraderteAndreYtelseType(),
            fjernet = fjernet,
        )

    private fun GraderteAndreYtelserType.tilApiGraderteAndreYtelseType() =
        when (this) {
            GraderteAndreYtelserType.FORELDREPENGER -> ApiGraderteAndreYtelseType.FORELDREPENGER
            GraderteAndreYtelserType.SVANGERSKAPSPENGER -> ApiGraderteAndreYtelseType.SVANGERSKAPSPENGER
            GraderteAndreYtelserType.OMSORGSPENGER -> ApiGraderteAndreYtelseType.OMSORGSPENGER
            GraderteAndreYtelserType.PLEIEPENGER -> ApiGraderteAndreYtelseType.PLEIEPENGER
            GraderteAndreYtelserType.OPPLARINGSPENGER -> ApiGraderteAndreYtelseType.OPPLARINGSPENGER
        }
}
