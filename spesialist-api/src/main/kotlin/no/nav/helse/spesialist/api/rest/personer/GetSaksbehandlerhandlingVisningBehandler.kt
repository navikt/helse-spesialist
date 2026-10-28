package no.nav.helse.spesialist.api.rest.personer

import io.ktor.http.*
import no.nav.helse.spesialist.api.rest.*
import no.nav.helse.spesialist.api.rest.andreYtelser.tilHandlingsvisning
import no.nav.helse.spesialist.api.rest.resources.Personer
import no.nav.helse.spesialist.application.PersonPseudoId
import no.nav.helse.spesialist.application.logg.loggInfo
import no.nav.helse.spesialist.domain.Person
import no.nav.helse.spesialist.domain.saksbehandlerhandling.HandlingType
import java.util.UUID

class GetSaksbehandlerhandlingVisningBehandler : GetBehandler<Personer.PersonPseudoId.Saksbehandlerhandlinger.HandlingId.Visning, ApiHandlingsvisning, ApiGetSaksbehandlerhandlingVisningErrorCode> {
    override val tag = Tags.PERSONER

    override fun behandle(
        resource: Personer.PersonPseudoId.Saksbehandlerhandlinger.HandlingId.Visning,
        kallKontekst: KallKontekst,
    ): RestResponse<ApiHandlingsvisning, ApiGetSaksbehandlerhandlingVisningErrorCode> =
        kallKontekst.medPerson(
            personPseudoId = PersonPseudoId.fraUUID(resource.parent.parent.parent.pseudoId),
            personPseudoIdIkkeFunnet = { ApiGetSaksbehandlerhandlingVisningErrorCode.PERSON_PSEUDO_ID_IKKE_FUNNET },
            manglerTilgangTilPerson = { ApiGetSaksbehandlerhandlingVisningErrorCode.MANGLER_TILGANG_TIL_PERSON },
        ) { person ->
            behandleForPerson(resource.parent.handlingId, person, kallKontekst)
        }

    private fun behandleForPerson(
        handlingId: UUID,
        person: Person,
        kallKontekst: KallKontekst,
    ): RestResponse<ApiHandlingsvisning, ApiGetSaksbehandlerhandlingVisningErrorCode> {
        val handling =
            kallKontekst.transaksjon.saksbehandlerhandlingRepository
                .finnAlle(person.id)
                .find { it.handlingId == handlingId }
                ?: return RestResponse.Error(ApiGetSaksbehandlerhandlingVisningErrorCode.HANDLING_IKKE_FUNNET)

        if (handling.handlingType != HandlingType.GRADERTE_ANDRE_YTELSER_ENDRET) {
            return RestResponse.Error(ApiGetSaksbehandlerhandlingVisningErrorCode.VISNING_IKKE_FUNNET)
        }

        val visning =
            kallKontekst.transaksjon.graderteAndreYtelserRepository
                .finnAlleForIdentitetsnummer(person.id)
                .firstNotNullOfOrNull { it.finnEvent(handlingId) }
                ?.tilHandlingsvisning()
                ?: return RestResponse.Error(ApiGetSaksbehandlerhandlingVisningErrorCode.VISNING_IKKE_FUNNET)

        loggInfo("Hentet visning for endring av graderte andre ytelser")

        return RestResponse.OK(visning)
    }
}

enum class ApiGetSaksbehandlerhandlingVisningErrorCode(
    override val title: String,
    override val statusCode: HttpStatusCode,
) : ApiErrorCode {
    PERSON_PSEUDO_ID_IKKE_FUNNET("PersonPseudoId har utløpt (eller aldri eksistert)", HttpStatusCode.NotFound),
    MANGLER_TILGANG_TIL_PERSON("Mangler tilgang til person", HttpStatusCode.Forbidden),
    HANDLING_IKKE_FUNNET("Fant ikke saksbehandlerhandlingen", HttpStatusCode.NotFound),
    VISNING_IKKE_FUNNET("Fant ikke visning for saksbehandlerhandlingen", HttpStatusCode.NotFound),
}
