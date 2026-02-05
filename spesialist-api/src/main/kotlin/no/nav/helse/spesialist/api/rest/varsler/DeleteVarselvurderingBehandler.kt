package no.nav.helse.spesialist.api.rest.varsler

import io.github.smiley4.ktoropenapi.config.RouteConfig
import io.ktor.http.HttpStatusCode
import no.nav.helse.spesialist.api.rest.ApiErrorCode
import no.nav.helse.spesialist.api.rest.DeleteBehandler
import no.nav.helse.spesialist.api.rest.KallKontekst
import no.nav.helse.spesialist.api.rest.RestResponse
import no.nav.helse.spesialist.api.rest.resources.Varsler
import no.nav.helse.spesialist.api.rest.varsler.DeleteVarselvurderingErrorCode.MANGLER_TILGANG_TIL_PERSON
import no.nav.helse.spesialist.api.rest.varsler.DeleteVarselvurderingErrorCode.VARSEL_HAR_FEIL_STATUS
import no.nav.helse.spesialist.api.rest.varsler.DeleteVarselvurderingErrorCode.VARSEL_IKKE_FUNNET
import no.nav.helse.spesialist.domain.ResultatAvSletting
import no.nav.helse.spesialist.domain.Varsel
import no.nav.helse.spesialist.domain.Varsel.Status.AKTIV
import no.nav.helse.spesialist.domain.Varsel.Status.AVVIKLET
import no.nav.helse.spesialist.domain.Varsel.Status.AVVIST
import no.nav.helse.spesialist.domain.Varsel.Status.GODKJENT
import no.nav.helse.spesialist.domain.Varsel.Status.INAKTIV
import no.nav.helse.spesialist.domain.Varsel.Status.VURDERT
import no.nav.helse.spesialist.domain.VarselId
import no.nav.helse.spesialist.domain.tilgangskontroll.Tilgang

class DeleteVarselvurderingBehandler : DeleteBehandler<Varsler.VarselId.Vurdering, Unit, DeleteVarselvurderingErrorCode> {
    override val påkrevdTilgang = Tilgang.Skriv

    override fun behandle(
        resource: Varsler.VarselId.Vurdering,
        kallKontekst: KallKontekst,
    ): RestResponse<Unit, DeleteVarselvurderingErrorCode> {
        val varsel =
            kallKontekst.transaksjon.varselRepository.finn(VarselId(resource.parent.varselId))
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
    ): RestResponse<Unit, DeleteVarselvurderingErrorCode> =
        when (varsel.status) {
            GODKJENT, INAKTIV, AVVIST, AVVIKLET -> {
                RestResponse.Error(VARSEL_HAR_FEIL_STATUS)
            }

            AKTIV, VURDERT -> {
                val resultat = varsel.slettVurdering()
                if (resultat is ResultatAvSletting.Slettet) {
                    kallKontekst.transaksjon.varselRepository.lagre(varsel)
                }
                RestResponse.NoContent()
            }
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
    VARSEL_HAR_FEIL_STATUS(
        HttpStatusCode.Conflict,
        "Varsel har en status som ikke tillater at vurderingen kan fjernes",
    ),
}
