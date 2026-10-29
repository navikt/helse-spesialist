package no.nav.helse.spesialist.api.rest.personer

import io.ktor.http.HttpStatusCode
import no.nav.helse.spesialist.api.rest.ApiErrorCode
import no.nav.helse.spesialist.api.rest.ApiOverstyringerForPerson
import no.nav.helse.spesialist.api.rest.GetBehandler
import no.nav.helse.spesialist.api.rest.KallKontekst
import no.nav.helse.spesialist.api.rest.RestResponse
import no.nav.helse.spesialist.api.rest.Tags
import no.nav.helse.spesialist.api.rest.resources.Personer
import no.nav.helse.spesialist.application.PersonPseudoId

class GetOverstyringerForPersonBehandler : GetBehandler<Personer.PersonPseudoId.Overstyringer, ApiOverstyringerForPerson, ApiGetOverstyringerForPersonErrorCode> {
    override val tag = Tags.PERSONER

    override fun behandle(
        resource: Personer.PersonPseudoId.Overstyringer,
        kallKontekst: KallKontekst,
    ): RestResponse<ApiOverstyringerForPerson, ApiGetOverstyringerForPersonErrorCode> =
        kallKontekst.medPerson(
            personPseudoId = PersonPseudoId.fraUUID(resource.parent.pseudoId),
            personPseudoIdIkkeFunnet = { ApiGetOverstyringerForPersonErrorCode.PERSON_PSEUDO_ID_IKKE_FUNNET },
            manglerTilgangTilPerson = { ApiGetOverstyringerForPersonErrorCode.MANGLER_TILGANG_TIL_PERSON },
        ) { person ->
            RestResponse.OK(
                ApiOverstyringerForPerson(
                    overstyringer =
                        kallKontekst.transaksjon.overstyringDao
                            .finnAlleForPerson(person.id.value)
                            .map { it.tilApiOverstyring() },
                ),
            )
        }
}

enum class ApiGetOverstyringerForPersonErrorCode(
    override val title: String,
    override val statusCode: HttpStatusCode,
) : ApiErrorCode {
    PERSON_PSEUDO_ID_IKKE_FUNNET("PersonPseudoId har utløpt (eller aldri eksistert)", HttpStatusCode.NotFound),
    MANGLER_TILGANG_TIL_PERSON("Mangler tilgang til person", HttpStatusCode.Forbidden),
}
