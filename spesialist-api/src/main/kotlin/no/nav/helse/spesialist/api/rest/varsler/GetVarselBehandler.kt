package no.nav.helse.spesialist.api.rest.varsler

import io.github.smiley4.ktoropenapi.config.RouteConfig
import io.ktor.http.HttpStatusCode
import no.nav.helse.db.SessionContext
import no.nav.helse.spesialist.api.rest.ApiErrorCode
import no.nav.helse.spesialist.api.rest.ApiVarsel
import no.nav.helse.spesialist.api.rest.GetBehandler
import no.nav.helse.spesialist.api.rest.RestResponse
import no.nav.helse.spesialist.api.rest.harTilgangTilPerson
import no.nav.helse.spesialist.api.rest.resources.Varsler
import no.nav.helse.spesialist.api.rest.varsler.GetVarselErrorCode.BEHANDLING_IKKE_FUNNET
import no.nav.helse.spesialist.api.rest.varsler.GetVarselErrorCode.MANGLER_TILGANG_TIL_PERSON
import no.nav.helse.spesialist.api.rest.varsler.GetVarselErrorCode.SAKSBEHANDLER_MANGLER
import no.nav.helse.spesialist.api.rest.varsler.GetVarselErrorCode.UGYLDIG_VARSEL_STATUS
import no.nav.helse.spesialist.api.rest.varsler.GetVarselErrorCode.VARSELDEFINISJON_MANGLER_FOR_KODE
import no.nav.helse.spesialist.api.rest.varsler.GetVarselErrorCode.VARSELDEFINISJON_MANGLER_FOR_VURDERING
import no.nav.helse.spesialist.api.rest.varsler.GetVarselErrorCode.VARSEL_IKKE_FUNNET
import no.nav.helse.spesialist.api.rest.varsler.GetVarselErrorCode.VEDTAKSPERIODE_IKKE_FUNNET
import no.nav.helse.spesialist.domain.Identitetsnummer
import no.nav.helse.spesialist.domain.Saksbehandler
import no.nav.helse.spesialist.domain.Varsel
import no.nav.helse.spesialist.domain.VarselId
import no.nav.helse.spesialist.domain.tilgangskontroll.Tilgangsgruppe

class GetVarselBehandler : GetBehandler<Varsler.VarselId, ApiVarsel, GetVarselErrorCode> {
    override fun behandle(
        resource: Varsler.VarselId,
        saksbehandler: Saksbehandler,
        tilgangsgrupper: Set<Tilgangsgruppe>,
        transaksjon: SessionContext,
    ): RestResponse<ApiVarsel, GetVarselErrorCode> {
        val varselId = VarselId(resource.varselId)
        val varsel = transaksjon.varselRepository.finn(varselId) ?: return RestResponse.Error(VARSEL_IKKE_FUNNET)
        val behandling =
            transaksjon.behandlingRepository.finn(varsel.behandlingUnikId)
                ?: return RestResponse.Error(BEHANDLING_IKKE_FUNNET)

        val vedtaksperiode =
            transaksjon.vedtaksperiodeRepository.finn(behandling.vedtaksperiodeId)
                ?: return RestResponse.Error(VEDTAKSPERIODE_IKKE_FUNNET)

        if (!saksbehandler.harTilgangTilPerson(
                identitetsnummer = Identitetsnummer.fraString(vedtaksperiode.fødselsnummer),
                tilgangsgrupper = tilgangsgrupper,
                transaksjon = transaksjon,
            )
        ) {
            return RestResponse.Error(MANGLER_TILGANG_TIL_PERSON)
        }

        val varselvurdering = varsel.vurdering
        val varseldefinisjon =
            if (varselvurdering != null) {
                transaksjon.varseldefinisjonRepository.finn(varselvurdering.vurdertDefinisjonId)
                    ?: return RestResponse.Error(VARSELDEFINISJON_MANGLER_FOR_VURDERING, detail = "Varsel-id: ${varsel.id.value}")
            } else {
                transaksjon.varseldefinisjonRepository.finnGjeldendeFor(varsel.kode)
                    ?: return RestResponse.Error(VARSELDEFINISJON_MANGLER_FOR_KODE, detail = "Varselkode: ${varsel.kode}")
            }

        val apiVarsel =
            ApiVarsel(
                id = varsel.id.value,
                definisjonId = varseldefinisjon.id.value,
                opprettet = varsel.opprettetTidspunkt,
                tittel = varseldefinisjon.tittel,
                forklaring = varseldefinisjon.forklaring,
                handling = varseldefinisjon.handling,
                status =
                    when (varsel.status) {
                        Varsel.Status.AKTIV -> ApiVarsel.ApiVarselstatus.AKTIV
                        Varsel.Status.VURDERT -> ApiVarsel.ApiVarselstatus.VURDERT
                        Varsel.Status.GODKJENT -> ApiVarsel.ApiVarselstatus.GODKJENT
                        else -> return RestResponse.Error(UGYLDIG_VARSEL_STATUS)
                    },
                vurdering =
                    varsel.vurdering?.let { vurdering ->
                        val saksbehandler =
                            transaksjon.saksbehandlerRepository.finn(vurdering.saksbehandlerId)
                                ?: return RestResponse.Error(SAKSBEHANDLER_MANGLER)
                        ApiVarsel.ApiVarselvurdering(
                            ident = saksbehandler.ident,
                            tidsstempel = vurdering.tidspunkt,
                        )
                    },
            )
        return RestResponse.OK(apiVarsel)
    }

    override fun openApi(config: RouteConfig) {
        config.tags("varsler")
    }
}

enum class GetVarselErrorCode(
    override val statusCode: HttpStatusCode,
    override val title: String,
) : ApiErrorCode {
    MANGLER_TILGANG_TIL_PERSON(HttpStatusCode.Forbidden, "Mangler tilgang til person"),
    BEHANDLING_IKKE_FUNNET(HttpStatusCode.InternalServerError, "Fant ikke behandling"),
    VEDTAKSPERIODE_IKKE_FUNNET(HttpStatusCode.InternalServerError, "Fant ikke vedtaksperiode"),
    VARSEL_IKKE_FUNNET(HttpStatusCode.NotFound, "Fant ikke varsel"),
    VARSELDEFINISJON_MANGLER_FOR_KODE(HttpStatusCode.InternalServerError, "Fant ikke varseldefinisjon for varselkode"),
    VARSELDEFINISJON_MANGLER_FOR_VURDERING(HttpStatusCode.InternalServerError, "Fant ikke varseldefinisjon for varselvurdering"),
    SAKSBEHANDLER_MANGLER(HttpStatusCode.InternalServerError, "Fant ikke saksbehandler som vurderte varsel"),
    UGYLDIG_VARSEL_STATUS(HttpStatusCode.InternalServerError, "Varselet har en status som impliserer at varselet ikke skal vises"),
}
