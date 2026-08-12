package no.nav.helse.spesialist.api.rest.andreYtelser

import io.ktor.http.*
import no.nav.helse.spesialist.api.rest.*
import no.nav.helse.spesialist.api.rest.resources.Personer
import no.nav.helse.spesialist.application.logg.loggInfo
import no.nav.helse.spesialist.domain.Identitetsnummer
import no.nav.helse.spesialist.domain.andreytelser.GraderteAndreYtelser
import no.nav.helse.spesialist.domain.andreytelser.GraderteAndreYtelserType

class GetGraderteAndreYtelserForPersonBehandler : GetBehandler<Personer.PersonPseudoId.GraderteAndreYtelser, List<ApiGraderteAndreYtelser>, ApiGetGraderteAndreYtelserForPersonErrorCode> {
    override val tag = Tags.GRADERTE_ANDRE_YTELSER

    override fun behandle(
        resource: Personer.PersonPseudoId.GraderteAndreYtelser,
        kallKontekst: KallKontekst,
    ): RestResponse<List<ApiGraderteAndreYtelser>, ApiGetGraderteAndreYtelserForPersonErrorCode> =
        kallKontekst.medPerson(
            identitetsnummer = Identitetsnummer.fraString(resource.parent.pseudoId),
            personIkkeFunnet = { ApiGetGraderteAndreYtelserForPersonErrorCode.PERSON_PSEUDO_ID_IKKE_FUNNET },
            manglerTilgangTilPerson = { ApiGetGraderteAndreYtelserForPersonErrorCode.MANGLER_TILGANG_TIL_PERSON },
        ) { person ->
            val ytelser =
                kallKontekst.transaksjon.graderteAndreYtelserRepository
                    .finnAlleForIdentitetsnummer(person.id)
                    .map { it.tilApiGraderteAndreYtelser() }

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

enum class ApiGetGraderteAndreYtelserForPersonErrorCode(
    override val title: String,
    override val statusCode: HttpStatusCode,
) : ApiErrorCode {
    PERSON_PSEUDO_ID_IKKE_FUNNET("Person ikke funnet", HttpStatusCode.BadRequest),
    MANGLER_TILGANG_TIL_PERSON("Mangler tilgang til person", HttpStatusCode.Forbidden),
}
