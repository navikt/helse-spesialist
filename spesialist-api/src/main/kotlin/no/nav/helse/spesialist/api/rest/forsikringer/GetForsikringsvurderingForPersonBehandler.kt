package no.nav.helse.spesialist.api.rest.forsikringer

import io.ktor.http.HttpStatusCode
import no.nav.helse.spesialist.api.rest.ApiErrorCode
import no.nav.helse.spesialist.api.rest.ApiForsikring
import no.nav.helse.spesialist.api.rest.ForsikringInnhold
import no.nav.helse.spesialist.api.rest.GetBehandler
import no.nav.helse.spesialist.api.rest.KallKontekst
import no.nav.helse.spesialist.api.rest.RestResponse
import no.nav.helse.spesialist.api.rest.Tags
import no.nav.helse.spesialist.api.rest.resources.Personer
import no.nav.helse.spesialist.application.Forsikringsvurdering
import no.nav.helse.spesialist.application.ForsikringsvurderingHenter
import no.nav.helse.spesialist.application.PersonPseudoId
import no.nav.helse.spesialist.domain.ForsikringsvurderingId
import no.nav.helse.spesialist.domain.Person
import java.util.UUID

class GetForsikringsvurderingForPersonBehandler(
    private val forsikringsvurderingHenter: ForsikringsvurderingHenter,
) : GetBehandler<Personer.PersonPseudoId.Forsikringsvurderinger.ForsikringvurderingId, ApiForsikring, ApiGetForsikringsvurderingForPersonErrorCode> {
    override val tag = Tags.FORSIKRINGER

    override fun behandle(
        resource: Personer.PersonPseudoId.Forsikringsvurderinger.ForsikringvurderingId,
        kallKontekst: KallKontekst,
    ): RestResponse<ApiForsikring, ApiGetForsikringsvurderingForPersonErrorCode> =
        kallKontekst.medPerson(
            personPseudoId = PersonPseudoId.fraString(resource.parent.parent.pseudoId),
            personPseudoIdIkkeFunnet = { ApiGetForsikringsvurderingForPersonErrorCode.PERSON_PSEUDO_ID_IKKE_FUNNET },
            manglerTilgangTilPerson = { ApiGetForsikringsvurderingForPersonErrorCode.MANGLER_TILGANG_TIL_PERSON },
        ) { person ->
            behandleForPerson(person, resource.forsikringvurderingId)
        }

    private fun behandleForPerson(
        person: Person,
        forsikringsvurderingId: UUID,
    ): RestResponse<ApiForsikring, ApiGetForsikringsvurderingForPersonErrorCode> {
        val forsikringsvurdering =
            runCatching {
                forsikringsvurderingHenter.hent(ForsikringsvurderingId(forsikringsvurderingId))
            }.getOrElse {
                return RestResponse.Error(ApiGetForsikringsvurderingForPersonErrorCode.FEIL_VED_VIDERE_KALL)
            }

        if (forsikringsvurdering == null || forsikringsvurdering.identitetsnummer != person.id) {
            return RestResponse.Error(ApiGetForsikringsvurderingForPersonErrorCode.FORSIKRINGSVURDERING_IKKE_FUNNET)
        }

        return RestResponse.OK(forsikringsvurdering.tilApiForsikring())
    }
}

private fun Forsikringsvurdering.tilApiForsikring(): ApiForsikring =
    ApiForsikring(
        eksisterer = harForsikring,
        forsikringInnhold =
            dekning?.let {
                ForsikringInnhold(
                    dekningsgrad = it.grad,
                    gjelderFraDag = it.fraDag,
                )
            },
    )

enum class ApiGetForsikringsvurderingForPersonErrorCode(
    override val title: String,
    override val statusCode: HttpStatusCode,
) : ApiErrorCode {
    PERSON_PSEUDO_ID_IKKE_FUNNET("PersonPseudoId har utløpt (eller aldri eksistert)", HttpStatusCode.NotFound),
    MANGLER_TILGANG_TIL_PERSON("Mangler tilgang til person", HttpStatusCode.Forbidden),
    FORSIKRINGSVURDERING_IKKE_FUNNET("Forsikringsvurderingen ble ikke funnet", HttpStatusCode.NotFound),
    FEIL_VED_VIDERE_KALL("Klarte ikke kommunisere med bakomforliggende system", HttpStatusCode.InternalServerError),
}
