package no.nav.helse.spesialist.api.rest.varsler

import io.github.smiley4.ktoropenapi.config.RouteConfig
import io.ktor.http.HttpStatusCode
import no.nav.helse.db.SessionContext
import no.nav.helse.spesialist.api.rest.ApiErrorCode
import no.nav.helse.spesialist.api.rest.ApiVarselvurdering
import no.nav.helse.spesialist.api.rest.PutBehandler
import no.nav.helse.spesialist.api.rest.RestResponse
import no.nav.helse.spesialist.api.rest.resources.Varsler
import no.nav.helse.spesialist.api.rest.tilkommeninntekt.harTilgangTilPerson
import no.nav.helse.spesialist.api.rest.varsler.PutVarselvurderingErrorCode.MANGLER_TILGANG_TIL_PERSON
import no.nav.helse.spesialist.api.rest.varsler.PutVarselvurderingErrorCode.VARSEL_IKKE_FUNNET
import no.nav.helse.spesialist.api.rest.varsler.PutVarselvurderingErrorCode.VARSEL_KAN_IKKE_VURDERES
import no.nav.helse.spesialist.api.rest.varsler.PutVarselvurderingErrorCode.VARSEL_VURDERT_AV_ANNEN_SAKSBEHANDLER
import no.nav.helse.spesialist.api.rest.varsler.PutVarselvurderingErrorCode.VARSEL_VURDERT_MED_ANNEN_DEFINISJON
import no.nav.helse.spesialist.application.Outbox
import no.nav.helse.spesialist.domain.Saksbehandler
import no.nav.helse.spesialist.domain.Varsel
import no.nav.helse.spesialist.domain.VarselId
import no.nav.helse.spesialist.domain.VarseldefinisjonId
import no.nav.helse.spesialist.domain.tilgangskontroll.Tilgangsgruppe

class PutVarselvurderingBehandler : PutBehandler<Varsler.VarselId.Vurdering, ApiVarselvurdering, Unit, PutVarselvurderingErrorCode> {
    override fun behandle(
        resource: Varsler.VarselId.Vurdering,
        request: ApiVarselvurdering,
        saksbehandler: Saksbehandler,
        tilgangsgrupper: Set<Tilgangsgruppe>,
        transaksjon: SessionContext,
        outbox: Outbox,
    ): RestResponse<Unit, PutVarselvurderingErrorCode> {
        val varselId = VarselId(resource.parent.varselId)
        val varseldefinisjonId = VarseldefinisjonId(request.definisjonId)
        val varsel =
            transaksjon.varselRepository.finn(varselId)
                ?: return RestResponse.Error(VARSEL_IKKE_FUNNET)

        val behandling =
            transaksjon.behandlingRepository.finn(varsel.behandlingUnikId)
                ?: error("Fant ikke behandling")

        val vedtaksperiode =
            transaksjon.vedtaksperiodeRepository.finn(behandling.vedtaksperiodeId)
                ?: error("Fant ikke vedtaksperiode")

        if (!harTilgangTilPerson(vedtaksperiode.fødselsnummer, saksbehandler, tilgangsgrupper, transaksjon)) {
            return RestResponse.Error(MANGLER_TILGANG_TIL_PERSON)
        }

        if (varsel.status == Varsel.Status.VURDERT) {
            val eksisterendeVurdering =
                varsel.vurdering
                    ?: error("Fant ikke varselvurdering for varsel som er vurdert")
            if (eksisterendeVurdering.saksbehandlerId != saksbehandler.id) {
                return RestResponse.Error(VARSEL_VURDERT_AV_ANNEN_SAKSBEHANDLER)
            }
            if (eksisterendeVurdering.vurdertDefinisjonId != varseldefinisjonId) {
                return RestResponse.Error(VARSEL_VURDERT_MED_ANNEN_DEFINISJON)
            }
            return RestResponse.NoContent()
        }

        if (!varsel.kanVurderes()) {
            return RestResponse.Error(VARSEL_KAN_IKKE_VURDERES)
        }

        varsel.vurder(saksbehandler.id, varseldefinisjonId)
        transaksjon.varselRepository.lagre(varsel)
        return RestResponse.OK(Unit)
    }

    override fun openApi(config: RouteConfig) {
        config.tags("varsler")
    }
}

enum class PutVarselvurderingErrorCode(
    override val statusCode: HttpStatusCode,
    override val title: String,
) : ApiErrorCode {
    MANGLER_TILGANG_TIL_PERSON(HttpStatusCode.Forbidden, "Mangler tilgang til person"),
    VARSEL_IKKE_FUNNET(HttpStatusCode.NotFound, "Varsel ikke funnet"),
    VARSEL_VURDERT_AV_ANNEN_SAKSBEHANDLER(HttpStatusCode.Conflict, "Varsel har blitt vurdert av en annen saksbehandler"),
    VARSEL_VURDERT_MED_ANNEN_DEFINISJON(HttpStatusCode.Conflict, "Varsel har blitt vurdert basert på en annen definisjon"),
    VARSEL_KAN_IKKE_VURDERES(HttpStatusCode.Conflict, "Varsel har en status som ikke tillater at det kan vurderes"),
}
