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
import no.nav.helse.spesialist.api.rest.varsler.GetVarselErrorCode.MANGLER_TILGANG_TIL_PERSON
import no.nav.helse.spesialist.api.rest.varsler.GetVarselErrorCode.VARSEL_IKKE_FUNNET
import no.nav.helse.spesialist.domain.Identitetsnummer
import no.nav.helse.spesialist.domain.Saksbehandler
import no.nav.helse.spesialist.domain.Varsel
import no.nav.helse.spesialist.domain.VarselId
import no.nav.helse.spesialist.domain.tilgangskontroll.Brukerrolle
import no.nav.helse.spesialist.domain.tilgangskontroll.Tilgangsgruppe

class GetVarselBehandler : GetBehandler<Varsler.VarselId, ApiVarsel, GetVarselErrorCode> {
    override fun behandle(
        resource: Varsler.VarselId,
        saksbehandler: Saksbehandler,
        tilgangsgrupper: Set<Tilgangsgruppe>,
        transaksjon: SessionContext,
        brukerroller: Set<Brukerrolle>,
    ): RestResponse<ApiVarsel, GetVarselErrorCode> {
        val varselId = VarselId(resource.varselId)
        val varsel =
            transaksjon.varselRepository.finn(varselId)
                ?: return RestResponse.Error(VARSEL_IKKE_FUNNET)

        val behandling =
            transaksjon.behandlingRepository.finn(varsel.behandlingUnikId)
                ?: error("Fant ikke behandling")

        val vedtaksperiode =
            transaksjon.vedtaksperiodeRepository.finn(behandling.vedtaksperiodeId)
                ?: error("Fant ikke vedtaksperiode")

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
                    ?: error("Fant ikke varseldefinisjon brukt i vurdering av varsel")
            } else {
                transaksjon.varseldefinisjonRepository.finnGjeldendeFor(varsel.kode)
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
                            transaksjon.saksbehandlerRepository.finn(vurdering.saksbehandlerId)
                                ?: error("Finner ikke saksbehandler som vurderte varselet")
                        ApiVarsel.ApiVarselvurdering(
                            ident = saksbehandler.ident.value,
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
    VARSEL_IKKE_FUNNET(HttpStatusCode.NotFound, "Fant ikke varsel"),
}
