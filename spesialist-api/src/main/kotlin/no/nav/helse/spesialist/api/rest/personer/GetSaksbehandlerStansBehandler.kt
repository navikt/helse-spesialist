package no.nav.helse.spesialist.api.rest.personer

import io.ktor.http.HttpStatusCode
import no.nav.helse.spesialist.api.rest.ApiErrorCode
import no.nav.helse.spesialist.api.rest.ApiSaksbehandlerStans
import no.nav.helse.spesialist.api.rest.GetBehandler
import no.nav.helse.spesialist.api.rest.KallKontekst
import no.nav.helse.spesialist.api.rest.RestResponse
import no.nav.helse.spesialist.api.rest.Tags
import no.nav.helse.spesialist.api.rest.resources.Personer
import no.nav.helse.spesialist.application.PersonPseudoId
import java.time.ZoneId

class GetSaksbehandlerStansBehandler : GetBehandler<Personer.PersonPseudoId.Stans.Saksbehandler, ApiSaksbehandlerStans, ApiGetBehandlerdlerStansErrorCode> {
    override val tag = Tags.PERSONER

    override fun behandle(
        resource: Personer.PersonPseudoId.Stans.Saksbehandler,
        kallKontekst: KallKontekst,
    ): RestResponse<ApiSaksbehandlerStans, ApiGetBehandlerdlerStansErrorCode> {
        return kallKontekst.medPerson(
            personPseudoId = PersonPseudoId.fraString(resource.parent.parent.pseudoId),
            personPseudoIdIkkeFunnet = { ApiGetBehandlerdlerStansErrorCode.PERSON_PSEUDO_ID_IKKE_FUNNET },
            manglerTilgangTilPerson = { ApiGetBehandlerdlerStansErrorCode.MANGLER_TILGANG_TIL_PERSON },
        ) { person ->
            val stans =
                kallKontekst.transaksjon.saksbehandlerStansRepository.finnAktiv(person.id)
                    ?: return@medPerson RestResponse.OK(
                        ApiSaksbehandlerStans(
                            erStanset = false,
                            utførtAv = null,
                            begrunnelse = null,
                            opprettetTidspunkt = null,
                        ),
                    )
            RestResponse.OK(
                ApiSaksbehandlerStans(
                    erStanset = true,
                    utførtAv = stans.utførtAv.value,
                    begrunnelse = stans.begrunnelse,
                    opprettetTidspunkt = stans.opprettet.atZone(ZoneId.of("Europe/Oslo")).toLocalDateTime(),
                ),
            )
        }
    }
}

enum class ApiGetBehandlerdlerStansErrorCode(
    override val title: String,
    override val statusCode: HttpStatusCode,
) : ApiErrorCode {
    PERSON_PSEUDO_ID_IKKE_FUNNET("PersonPseudoId har utløpt (eller aldri eksistert)", HttpStatusCode.NotFound),
    MANGLER_TILGANG_TIL_PERSON("Mangler tilgang til person", HttpStatusCode.Forbidden),
}
