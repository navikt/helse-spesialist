package no.nav.helse.spesialist.api.rest.personer

import no.nav.helse.spesialist.api.rest.*
import no.nav.helse.spesialist.api.rest.resources.Personer
import no.nav.helse.spesialist.application.PersonPseudoId
import java.time.ZoneId

class GetSaksbehandlerStansBehandler : GetBehandler<Personer.PersonPseudoId.Stans.Saksbehandler, ApiSaksbehandlerStans, PersonErrorCode> {
    override val tag = Tags.PERSONER

    override fun behandle(
        resource: Personer.PersonPseudoId.Stans.Saksbehandler,
        kallKontekst: KallKontekst,
    ): RestResponse<ApiSaksbehandlerStans, PersonErrorCode> {
        return kallKontekst.medPerson(
            personPseudoId = PersonPseudoId.fraString(resource.parent.parent.pseudoId),
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
