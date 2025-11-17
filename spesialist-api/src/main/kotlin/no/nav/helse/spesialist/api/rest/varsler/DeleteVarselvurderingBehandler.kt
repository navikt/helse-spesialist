package no.nav.helse.spesialist.api.rest.varsler

import io.github.smiley4.ktoropenapi.config.RouteConfig
import io.ktor.http.HttpStatusCode
import no.nav.helse.db.SessionContext
import no.nav.helse.spesialist.api.rest.ApiErrorCode
import no.nav.helse.spesialist.api.rest.DeleteBehandler
import no.nav.helse.spesialist.api.rest.RestResponse
import no.nav.helse.spesialist.api.rest.harTilgangTilPerson
import no.nav.helse.spesialist.api.rest.resources.Varsler
import no.nav.helse.spesialist.api.rest.varsler.DeleteVarselvurderingErrorCode.MANGLER_TILGANG_TIL_PERSON
import no.nav.helse.spesialist.api.rest.varsler.DeleteVarselvurderingErrorCode.VARSEL_HAR_FEIL_STATUS
import no.nav.helse.spesialist.api.rest.varsler.DeleteVarselvurderingErrorCode.VARSEL_IKKE_FUNNET
import no.nav.helse.spesialist.application.Outbox
import no.nav.helse.spesialist.domain.Identitetsnummer
import no.nav.helse.spesialist.domain.Saksbehandler
import no.nav.helse.spesialist.domain.Varsel.Status.AKTIV
import no.nav.helse.spesialist.domain.Varsel.Status.AVVIKLET
import no.nav.helse.spesialist.domain.Varsel.Status.AVVIST
import no.nav.helse.spesialist.domain.Varsel.Status.GODKJENT
import no.nav.helse.spesialist.domain.Varsel.Status.INAKTIV
import no.nav.helse.spesialist.domain.VarselId
import no.nav.helse.spesialist.domain.tilgangskontroll.Tilgangsgruppe

class DeleteVarselvurderingBehandler : DeleteBehandler<Varsler.VarselId.Vurdering, Unit, DeleteVarselvurderingErrorCode> {
    override fun behandle(
        resource: Varsler.VarselId.Vurdering,
        saksbehandler: Saksbehandler,
        tilgangsgrupper: Set<Tilgangsgruppe>,
        transaksjon: SessionContext,
        outbox: Outbox,
    ): RestResponse<Unit, DeleteVarselvurderingErrorCode> {
        val varselId = VarselId(resource.parent.varselId)
        val varsel =
            transaksjon.varselRepository.finn(varselId)
                ?: return RestResponse.Error(VARSEL_IKKE_FUNNET)

        val behandling =
            transaksjon.behandlingRepository.finn(varsel.behandlingUnikId)
                ?: error("Fant ikke behandling")

        val vedtaksperiode =
            transaksjon.vedtaksperiodeRepository.finn(behandling.vedtaksperiodeId)
                ?: error("Fant ikke vedtaksperiode")

        val identitetsnummer = Identitetsnummer.fraString(vedtaksperiode.fødselsnummer)

        if (!saksbehandler.harTilgangTilPerson(identitetsnummer, tilgangsgrupper, transaksjon)) {
            return RestResponse.Error(MANGLER_TILGANG_TIL_PERSON)
        }
        if (varsel.status in listOf(GODKJENT, INAKTIV, AVVIST, AVVIKLET)) {
            return RestResponse.Error(VARSEL_HAR_FEIL_STATUS)
        }
        if (varsel.status == AKTIV && varsel.manglerVurdering()) return RestResponse.NoContent()
        varsel.fjernVurdering()
        transaksjon.varselRepository.lagre(varsel)
        return RestResponse.OK(Unit)
    }

    override fun openApi(config: RouteConfig) {
        config.tags("varsler")
    }
}

enum class DeleteVarselvurderingErrorCode(
    override val statusCode: HttpStatusCode,
    override val title: String,
) : ApiErrorCode {
    MANGLER_TILGANG_TIL_PERSON(HttpStatusCode.Forbidden, "Mangler tilgang til person"),
    VARSEL_IKKE_FUNNET(HttpStatusCode.NotFound, "Varsel ikke funnet"),
    VARSEL_HAR_FEIL_STATUS(HttpStatusCode.Conflict, "Varsel har en status som ikke tillater at vurderingen kan fjernes"),
}
