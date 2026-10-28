package no.nav.helse.spesialist.api.rest.personer

import io.ktor.http.*
import no.nav.helse.spesialist.api.rest.*
import no.nav.helse.spesialist.api.rest.resources.Personer
import no.nav.helse.spesialist.application.PersonPseudoId
import no.nav.helse.spesialist.application.logg.loggInfo
import no.nav.helse.spesialist.domain.TotrinnsvurderingId
import no.nav.helse.spesialist.domain.saksbehandlerhandling.HandlingType
import no.nav.helse.spesialist.domain.saksbehandlerhandling.Saksbehandlerhandling
import java.time.ZoneId

class GetSaksbehandlerhandlingerForPersonBehandler : GetBehandler<Personer.PersonPseudoId.Saksbehandlerhandlinger, List<ApiSaksbehandlerhandling>, ApiGetSaksbehandlerhandlingerForPersonErrorCode> {
    override val tag = Tags.PERSONER

    override fun behandle(
        resource: Personer.PersonPseudoId.Saksbehandlerhandlinger,
        kallKontekst: KallKontekst,
    ): RestResponse<List<ApiSaksbehandlerhandling>, ApiGetSaksbehandlerhandlingerForPersonErrorCode> =
        kallKontekst.medPerson(
            personPseudoId = PersonPseudoId.fraUUID(resource.parent.pseudoId),
            personPseudoIdIkkeFunnet = { ApiGetSaksbehandlerhandlingerForPersonErrorCode.PERSON_PSEUDO_ID_IKKE_FUNNET },
            manglerTilgangTilPerson = { ApiGetSaksbehandlerhandlingerForPersonErrorCode.MANGLER_TILGANG_TIL_PERSON },
        ) { person ->
            val aktivTotrinnsvurderingId =
                kallKontekst.transaksjon.totrinnsvurderingRepository
                    .finnAktivForPersonOrNull(person.id.value)
                    ?.id()

            val handlinger =
                kallKontekst.transaksjon.saksbehandlerhandlingRepository
                    .finnAlle(person.id)
                    .sortedByDescending { it.tidspunkt }
                    .map { it.tilApiSaksbehandlerhandling(aktivTotrinnsvurderingId) }

            loggInfo("Hentet ${handlinger.size} saksbehandlerhandlinger")

            RestResponse.OK(handlinger)
        }
}

enum class ApiGetSaksbehandlerhandlingerForPersonErrorCode(
    override val title: String,
    override val statusCode: HttpStatusCode,
) : ApiErrorCode {
    PERSON_PSEUDO_ID_IKKE_FUNNET("PersonPseudoId har utløpt (eller aldri eksistert)", HttpStatusCode.NotFound),
    MANGLER_TILGANG_TIL_PERSON("Mangler tilgang til person", HttpStatusCode.Forbidden),
}

private fun Saksbehandlerhandling.tilApiSaksbehandlerhandling(aktivTotrinnsvurderingId: TotrinnsvurderingId?) =
    ApiSaksbehandlerhandling(
        handlingId = handlingId,
        handlingType = handlingType.tilApiHandlingType(),
        saksbehandlerIdent = saksbehandlerIdent,
        tidspunkt = tidspunkt.atZone(ZoneId.of("Europe/Oslo")).toLocalDateTime(),
        skjæringstidspunkt = skjæringstidspunkt,
        behandlingId = behandlingId?.value,
        erDelAvAktivTotrinnsvurdering = totrinnsvurderingId != null && totrinnsvurderingId == aktivTotrinnsvurderingId,
    )

private fun HandlingType.tilApiHandlingType() =
    when (this) {
        HandlingType.MANUELT_VURDERT_OPPTJENING -> ApiHandlingType.MANUELT_VURDERT_OPPTJENING
        HandlingType.NY_AUTOMATISK_OPPTJENINGSVURDERING -> ApiHandlingType.NY_AUTOMATISK_OPPTJENINGSVURDERING
        HandlingType.GRADERTE_ANDRE_YTELSER_ENDRET -> ApiHandlingType.GRADERTE_ANDRE_YTELSER_ENDRET
        HandlingType.MANUELT_VURDERT_MEDLEMSKAP -> ApiHandlingType.MANUELT_VURDERT_MEDLEMSKAP
    }
