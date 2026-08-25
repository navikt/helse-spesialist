package no.nav.helse.spesialist.api.rest.personer

import no.nav.helse.spesialist.api.rest.ApiVeilederStans
import no.nav.helse.spesialist.api.rest.GetBehandler
import no.nav.helse.spesialist.api.rest.KallKontekst
import no.nav.helse.spesialist.api.rest.PersonErrorCode
import no.nav.helse.spesialist.api.rest.RestResponse
import no.nav.helse.spesialist.api.rest.Tags
import no.nav.helse.spesialist.api.rest.resources.Personer
import no.nav.helse.spesialist.application.PersonPseudoId
import no.nav.helse.spesialist.domain.VeilederStans
import java.time.ZoneId

class GetVeilederStansBehandler : GetBehandler<Personer.PersonPseudoId.Stans.Veileder, ApiVeilederStans, PersonErrorCode> {
    override val tag = Tags.PERSONER

    override fun behandle(
        resource: Personer.PersonPseudoId.Stans.Veileder,
        kallKontekst: KallKontekst,
    ): RestResponse<ApiVeilederStans, PersonErrorCode> =
        kallKontekst.medPerson(
            personPseudoId = PersonPseudoId.fraString(resource.parent.parent.pseudoId),
        ) { person ->
            val stans =
                kallKontekst.transaksjon.veilederStansRepository.finnAktiv(person.id)
                    ?: return@medPerson RestResponse.OK(ApiVeilederStans(erStanset = false, årsaker = emptySet(), tidspunkt = null))
            RestResponse.OK(
                ApiVeilederStans(
                    erStanset = true,
                    årsaker =
                        stans.årsaker
                            .map {
                                when (it) {
                                    VeilederStans.StansÅrsak.MEDISINSK_VILKAR -> ApiVeilederStans.Årsak.MEDISINSK_VILKAR
                                    VeilederStans.StansÅrsak.AKTIVITETSKRAV -> ApiVeilederStans.Årsak.AKTIVITETSKRAV
                                    VeilederStans.StansÅrsak.MANGLENDE_MEDVIRKING -> ApiVeilederStans.Årsak.MANGLENDE_MEDVIRKING
                                    VeilederStans.StansÅrsak.BESTRIDELSE_SYKMELDING -> ApiVeilederStans.Årsak.BESTRIDELSE_SYKMELDING
                                }
                            }.toSet(),
                    tidspunkt = stans.opprettet.atZone(ZoneId.of("Europe/Oslo")).toLocalDateTime(),
                ),
            )
        }
}
