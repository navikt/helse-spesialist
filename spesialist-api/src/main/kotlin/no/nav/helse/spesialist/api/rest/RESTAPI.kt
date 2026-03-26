package no.nav.helse.spesialist.api.rest

import io.github.smiley4.ktoropenapi.openApi
import io.github.smiley4.ktoropenapi.resources.delete
import io.github.smiley4.ktoropenapi.resources.get
import io.github.smiley4.ktoropenapi.resources.patch
import io.github.smiley4.ktoropenapi.resources.post
import io.github.smiley4.ktoropenapi.resources.put
import io.github.smiley4.ktorswaggerui.swaggerUI
import io.ktor.server.auth.authenticate
import io.ktor.server.routing.Route
import io.ktor.server.routing.Routing
import io.ktor.server.routing.route
import no.nav.helse.bootstrap.EnvironmentToggles
import no.nav.helse.db.SessionFactory
import no.nav.helse.spesialist.api.ApiModule
import no.nav.helse.spesialist.api.rest.behandlinger.GetForsikringForPersonBehandler
import no.nav.helse.spesialist.api.rest.behandlinger.PostForkastingBehandler
import no.nav.helse.spesialist.api.rest.behandlinger.PostVedtakBehandler
import no.nav.helse.spesialist.api.rest.dialoger.PatchKommentarBehandler
import no.nav.helse.spesialist.api.rest.dialoger.PostKommentarBehandler
import no.nav.helse.spesialist.api.rest.dokumenter.DokumentMediator
import no.nav.helse.spesialist.api.rest.dokumenter.GetInntektsmeldingBehandler
import no.nav.helse.spesialist.api.rest.dokumenter.GetSoknadBehandler
import no.nav.helse.spesialist.api.rest.notater.GetNotatBehandler
import no.nav.helse.spesialist.api.rest.notater.GetNotatVedtaksperiodeIderForPersonBehandler
import no.nav.helse.spesialist.api.rest.notater.GetNotaterForVedtaksperiodeBehandler
import no.nav.helse.spesialist.api.rest.notater.PatchNotatBehandler
import no.nav.helse.spesialist.api.rest.notater.PostNotatBehandler
import no.nav.helse.spesialist.api.rest.oppgaver.GetAntallOppgaverBehandler
import no.nav.helse.spesialist.api.rest.oppgaver.GetBehandledeOppgaverBehandler
import no.nav.helse.spesialist.api.rest.oppgaver.GetListeOppgaverBehandler
import no.nav.helse.spesialist.api.rest.oppgaver.GetOppgaverBehandler
import no.nav.helse.spesialist.api.rest.oppgaver.påVent.DeletePåVentBehandler
import no.nav.helse.spesialist.api.rest.oppgaver.påVent.PutPåVentBehandler
import no.nav.helse.spesialist.api.rest.personer.GetBehandlendeEnhetForPersonBehandler
import no.nav.helse.spesialist.api.rest.personer.GetInfotrygdperioderForPersonBehandler
import no.nav.helse.spesialist.api.rest.personer.GetKrrRegistrertStatusForPersonBehandler
import no.nav.helse.spesialist.api.rest.personer.GetPersonBehandler
import no.nav.helse.spesialist.api.rest.personer.PatchSaksbehandlerStansBehandler
import no.nav.helse.spesialist.api.rest.personer.PatchVeilederStansBehandler
import no.nav.helse.spesialist.api.rest.personer.PostPersonSokBehandler
import no.nav.helse.spesialist.api.rest.personer.tildeling.PutTildelingBehandler
import no.nav.helse.spesialist.api.rest.saksbehandlere.GetAktiveSaksbehandlereBehandler
import no.nav.helse.spesialist.api.rest.saksbehandlere.GetBrukerBehandler
import no.nav.helse.spesialist.api.rest.tilkomneinntekter.GetTilkomneInntektskilderForPersonBehandler
import no.nav.helse.spesialist.api.rest.tilkomneinntekter.PatchTilkommenInntektBehandler
import no.nav.helse.spesialist.api.rest.tilkomneinntekter.PostTilkomneInntekterBehandler
import no.nav.helse.spesialist.api.rest.varsler.DeleteVarselvurderingBehandler
import no.nav.helse.spesialist.api.rest.varsler.GetVarselBehandler
import no.nav.helse.spesialist.api.rest.varsler.PutVarselvurderingBehandler
import no.nav.helse.spesialist.api.rest.vedtaksperioder.PostVedtaksperiodeAnnullerBehandler
import no.nav.helse.spesialist.api.rest.vurderinger.GetVurderteInngangsvilkårForPersonBehandler
import no.nav.helse.spesialist.api.rest.vurderinger.PostArbeidstidsvurderingBehandler
import no.nav.helse.spesialist.api.rest.vurderinger.PostVurderteInngangsvilkårForPersonBehandler
import no.nav.helse.spesialist.api.sse.sse
import no.nav.helse.spesialist.application.AlleIdenterHenter
import no.nav.helse.spesialist.application.BehandlendeEnhetHenter
import no.nav.helse.spesialist.application.ForsikringHenter
import no.nav.helse.spesialist.application.InfotrygdperiodeHenter
import no.nav.helse.spesialist.application.InngangsvilkårHenter
import no.nav.helse.spesialist.application.InngangsvilkårInnsender
import no.nav.helse.spesialist.application.KrrRegistrertStatusHenter
import no.nav.helse.spesialist.application.OpptegnelseListener
import no.nav.helse.spesialist.application.PersoninfoHenter

fun Routing.restRoutes(
    restAdapter: RestAdapter,
    configuration: ApiModule.Configuration,
    dokumentMediator: DokumentMediator,
    environmentToggles: EnvironmentToggles,
    krrRegistrertStatusHenter: KrrRegistrertStatusHenter,
    behandlendeEnhetHenter: BehandlendeEnhetHenter,
    forsikringHenter: ForsikringHenter,
    infotrygdperiodeHenter: InfotrygdperiodeHenter,
    inngangsvilkårHenter: InngangsvilkårHenter,
    inngangsvilkårInnsender: InngangsvilkårInnsender,
    alleIdenterHenter: AlleIdenterHenter,
    personinfoHenter: PersoninfoHenter,
    sessionFactory: SessionFactory,
    opptegnelseListener: OpptegnelseListener,
) {
    route("/api") {
        if (configuration.eksponerOpenApi) {
            route("/openapi.json") {
                openApi()
            }
            route("swagger") {
                swaggerUI("../openapi.json")
            }
        }
        authenticate("oidc") {
            sse(sessionFactory, opptegnelseListener)
            get(GetAktiveSaksbehandlereBehandler(), restAdapter)
            get(GetBrukerBehandler(), restAdapter)

            get(GetOppgaverBehandler(), restAdapter)
            get(GetAntallOppgaverBehandler(), restAdapter)
            get(GetBehandledeOppgaverBehandler(), restAdapter)
            get(GetListeOppgaverBehandler(), restAdapter)

            put(PutPåVentBehandler(), restAdapter)
            delete(DeletePåVentBehandler(), restAdapter)

            put(PutTildelingBehandler(), restAdapter)

            patch(PatchSaksbehandlerStansBehandler(), restAdapter)
            patch(PatchVeilederStansBehandler(), restAdapter)

            get(GetSoknadBehandler(dokumentMediator = dokumentMediator), restAdapter)
            get(GetInntektsmeldingBehandler(dokumentMediator = dokumentMediator), restAdapter)

            get(GetTilkomneInntektskilderForPersonBehandler(), restAdapter)
            get(GetVurderteInngangsvilkårForPersonBehandler(inngangsvilkårHenter, alleIdenterHenter), restAdapter)
            post(PostVurderteInngangsvilkårForPersonBehandler(inngangsvilkårInnsender), restAdapter)
            post(PostTilkomneInntekterBehandler(), restAdapter)
            patch(PatchTilkommenInntektBehandler(), restAdapter)

            post(PostVedtakBehandler(environmentToggles), restAdapter)
            post(PostForkastingBehandler(), restAdapter)

            post(PostVedtaksperiodeAnnullerBehandler(), restAdapter)

            post(PostArbeidstidsvurderingBehandler(), restAdapter)

            get(GetVarselBehandler(), restAdapter)
            put(PutVarselvurderingBehandler(), restAdapter)
            delete(DeleteVarselvurderingBehandler(), restAdapter)

            post(PostKommentarBehandler(), restAdapter)
            patch(PatchKommentarBehandler(), restAdapter)

            get(GetNotatBehandler(), restAdapter)
            get(GetNotaterForVedtaksperiodeBehandler(), restAdapter)
            post(PostNotatBehandler(), restAdapter)
            patch(PatchNotatBehandler(), restAdapter)

            post(PostPersonSokBehandler(), restAdapter)

            get(GetKrrRegistrertStatusForPersonBehandler(krrRegistrertStatusHenter), restAdapter)

            get(GetBehandlendeEnhetForPersonBehandler(behandlendeEnhetHenter), restAdapter)

            get(GetPersonBehandler(personinfoHenter, alleIdenterHenter), restAdapter)

            get(GetNotatVedtaksperiodeIderForPersonBehandler(), restAdapter)

            get(GetInfotrygdperioderForPersonBehandler(infotrygdperiodeHenter), restAdapter)

            get(GetForsikringForPersonBehandler(forsikringHenter), restAdapter)
        }
    }
}

@Suppress("unused")
private inline fun <reified RESOURCE : Any, reified RESPONSE : Any, reified ERROR : ApiErrorCode> Route.delete(
    behandler: DeleteBehandler<RESOURCE, RESPONSE, ERROR>,
    adapter: RestAdapter,
) {
    delete<RESOURCE>({ behandler.openApiUtenRequestBody<RESPONSE, ERROR>(this) }) { resource ->
        adapter.behandle(
            resource,
            call,
            behandler,
        )
    }
}

private inline fun <reified RESOURCE : Any, reified RESPONSE : Any, reified ERROR : ApiErrorCode> Route.get(
    behandler: GetBehandler<RESOURCE, RESPONSE, ERROR>,
    adapter: RestAdapter,
) {
    get<RESOURCE>({ behandler.openApiUtenRequestBody<RESPONSE, ERROR>(this) }) { resource ->
        adapter.behandle(
            resource,
            call,
            behandler,
        )
    }
}

@Suppress("unused")
private inline fun <reified RESOURCE : Any, reified REQUEST : Any, reified RESPONSE : Any, reified ERROR : ApiErrorCode> Route.patch(
    behandler: PatchBehandler<RESOURCE, REQUEST, RESPONSE, ERROR>,
    adapter: RestAdapter,
) {
    patch<RESOURCE>({ behandler.openApiMedRequestBody<REQUEST, RESPONSE, ERROR>(this) }) { resource ->
        adapter.behandle(
            resource,
            call,
            behandler,
        )
    }
}

private inline fun <reified RESOURCE : Any, reified REQUEST : Any, reified RESPONSE : Any, reified ERROR : ApiErrorCode> Route.post(
    behandler: PostBehandler<RESOURCE, REQUEST, RESPONSE, ERROR>,
    adapter: RestAdapter,
) {
    post<RESOURCE>({ behandler.openApiMedRequestBody<REQUEST, RESPONSE, ERROR>(this) }) { resource ->
        adapter.behandle(
            resource,
            call,
            behandler,
        )
    }
}

private inline fun <reified RESOURCE : Any, reified REQUEST : Any, reified RESPONSE : Any, reified ERROR : ApiErrorCode> Route.put(
    behandler: PutBehandler<RESOURCE, REQUEST, RESPONSE, ERROR>,
    adapter: RestAdapter,
) {
    put<RESOURCE>({ behandler.openApiMedRequestBody<REQUEST, RESPONSE, ERROR>(this) }) { resource ->
        adapter.behandle(
            resource,
            call,
            behandler,
        )
    }
}
