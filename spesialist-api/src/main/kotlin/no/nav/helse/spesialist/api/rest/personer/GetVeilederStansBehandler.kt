package no.nav.helse.spesialist.api.rest.personer

import io.ktor.http.HttpStatusCode
import no.nav.helse.spesialist.api.rest.ApiErrorCode
import no.nav.helse.spesialist.api.rest.ApiVeilederStans
import no.nav.helse.spesialist.api.rest.GetBehandler
import no.nav.helse.spesialist.api.rest.KallKontekst
import no.nav.helse.spesialist.api.rest.RestResponse
import no.nav.helse.spesialist.api.rest.Tags
import no.nav.helse.spesialist.api.rest.resources.Personer
import no.nav.helse.spesialist.application.PersonPseudoId
import no.nav.helse.spesialist.domain.VeilederStans
import java.time.ZoneId

class GetVeilederStansBehandler : GetBehandler<Personer.PersonPseudoId.Stans.Veileder, ApiVeilederStans, ApiGetVeilederStansErrorCode> {
    override val tag = Tags.PERSONER

    override fun behandle(
        resource: Personer.PersonPseudoId.Stans.Veileder,
        kallKontekst: KallKontekst,
    ): RestResponse<ApiVeilederStans, ApiGetVeilederStansErrorCode> =
        kallKontekst.medPerson(
            personPseudoId = PersonPseudoId.fraString(resource.parent.parent.pseudoId),
            personPseudoIdIkkeFunnet = { ApiGetVeilederStansErrorCode.PERSON_PSEUDO_ID_IKKE_FUNNET },
            manglerTilgangTilPerson = { ApiGetVeilederStansErrorCode.MANGLER_TILGANG_TIL_PERSON },
        ) { person ->
            val stans =
                kallKontekst.transaksjon.veilederStansRepository.finnAktiv(person.id)
                    ?: return@medPerson RestResponse.Error(
                        ApiGetVeilederStansErrorCode.STANS_IKKE_FUNNET,
                    )
            RestResponse.OK(
                ApiVeilederStans(
                    årsaker =
                        stans.årsaker
                            .map {
                                when (it) {
                                    VeilederStans.StansÅrsak.MEDISINSK_VILKAR -> ApiVeilederStans.ApiStansÅrsak.MEDISINSK_VILKAR
                                    VeilederStans.StansÅrsak.AKTIVITETSKRAV -> ApiVeilederStans.ApiStansÅrsak.AKTIVITETSKRAV
                                    VeilederStans.StansÅrsak.MANGLENDE_MEDVIRKING -> ApiVeilederStans.ApiStansÅrsak.MANGLENDE_MEDVIRKING
                                    VeilederStans.StansÅrsak.BESTRIDELSE_SYKMELDING -> ApiVeilederStans.ApiStansÅrsak.BESTRIDELSE_SYKMELDING
                                }
                            }.toSet(),
                    tidspunkt = stans.opprettet.atZone(ZoneId.of("Europe/Oslo")).toLocalDateTime(),
                ),
            )
        }
}

enum class ApiGetVeilederStansErrorCode(
    override val title: String,
    override val statusCode: HttpStatusCode,
) : ApiErrorCode {
    PERSON_PSEUDO_ID_IKKE_FUNNET("PersonPseudoId har utløpt (eller aldri eksistert)", HttpStatusCode.NotFound),
    MANGLER_TILGANG_TIL_PERSON("Mangler tilgang til person", HttpStatusCode.Forbidden),
    STANS_IKKE_FUNNET("Fant ikke veileder stans", HttpStatusCode.NotFound),
}
