package no.nav.helse.spesialist.api.rest.varsler

import io.github.smiley4.ktoropenapi.config.RouteConfig
import io.ktor.http.HttpStatusCode
import no.nav.helse.spesialist.api.rest.ApiErrorCode
import no.nav.helse.spesialist.api.rest.ApiVarsel
import no.nav.helse.spesialist.api.rest.GetBehandler
import no.nav.helse.spesialist.api.rest.KallKontekst
import no.nav.helse.spesialist.api.rest.RestResponse
import no.nav.helse.spesialist.api.rest.resources.Varsler
import no.nav.helse.spesialist.api.rest.varsler.GetVarselErrorCode.MANGLER_TILGANG_TIL_PERSON
import no.nav.helse.spesialist.api.rest.varsler.GetVarselErrorCode.VARSEL_IKKE_FUNNET
import no.nav.helse.spesialist.application.logg.loggInfo
import no.nav.helse.spesialist.domain.Varsel
import no.nav.helse.spesialist.domain.VarselId
import no.nav.helse.spesialist.domain.tilgangskontroll.Tilgang

class GetVarselBehandler : GetBehandler<Varsler.VarselId, ApiVarsel, GetVarselErrorCode> {
    override val påkrevdTilgang = Tilgang.Les

    override fun behandle(
        resource: Varsler.VarselId,
        kallKontekst: KallKontekst,
    ): RestResponse<ApiVarsel, GetVarselErrorCode> {
        val varsel =
            kallKontekst.transaksjon.varselRepository.finn(VarselId(resource.varselId))
                ?: return RestResponse.Error(VARSEL_IKKE_FUNNET)

        return kallKontekst.medBehandling(
            behandlingUnikId = varsel.behandlingUnikId,
            behandlingIkkeFunnet = { error("Fant ikke behandling") },
            manglerTilgangTilPerson = { MANGLER_TILGANG_TIL_PERSON },
        ) { _, _, _ ->
            behandleForBehandling(varsel, kallKontekst)
        }
    }

    private fun behandleForBehandling(
        varsel: Varsel,
        kallKontekst: KallKontekst,
    ): RestResponse<ApiVarsel, GetVarselErrorCode> {
        val varselvurdering = varsel.vurdering
        val varseldefinisjon =
            if (varselvurdering != null) {
                kallKontekst.transaksjon.varseldefinisjonRepository.finn(varselvurdering.vurdertDefinisjonId)
                    ?: error("Fant ikke varseldefinisjon brukt i vurdering av varsel")
            } else {
                kallKontekst.transaksjon.varseldefinisjonRepository.finnGjeldendeFor(varsel.kode)
                    ?: error("Fant ikke gjeldende varseldefinisjon for aktuell varselkode")
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
                        else -> error("Varselet har en status som ikke er ment å vise til saksbehandler")
                    },
                vurdering =
                    varsel.vurdering?.let { vurdering ->
                        val saksbehandler =
                            kallKontekst.transaksjon.saksbehandlerRepository.finn(vurdering.saksbehandlerId)
                                ?: error("Finner ikke saksbehandler som vurderte varselet")
                        ApiVarsel.ApiVarselvurdering(
                            ident = saksbehandler.ident.value,
                            tidsstempel = vurdering.tidspunkt,
                        )
                    },
            )

        loggInfo("Hentet varsel", "varselId" to varsel.id)

        return RestResponse.OK(apiVarsel)
    }

    override fun openApi(config: RouteConfig) {
        config.tags("varsler")
    }
}

enum class GetVarselErrorCode(
    override val title: String,
    override val statusCode: HttpStatusCode,
) : ApiErrorCode {
    MANGLER_TILGANG_TIL_PERSON("Mangler tilgang til person", HttpStatusCode.Forbidden),
    VARSEL_IKKE_FUNNET("Fant ikke varsel", HttpStatusCode.NotFound),
}
