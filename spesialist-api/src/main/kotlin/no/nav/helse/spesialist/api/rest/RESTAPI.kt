package no.nav.helse.spesialist.api.rest

import io.github.smiley4.ktoropenapi.openApi
import io.github.smiley4.ktoropenapi.resources.*
import io.github.smiley4.ktorswaggerui.swaggerUI
import io.ktor.server.auth.*
import io.ktor.server.routing.*
import no.nav.helse.bootstrap.EnvironmentToggles
import no.nav.helse.db.SessionFactory
import no.nav.helse.mediator.BehandlingsstatistikkService
import no.nav.helse.spesialist.api.ApiModule
import no.nav.helse.spesialist.api.rest.andreYtelser.*
import no.nav.helse.spesialist.api.rest.behandlinger.PostForkastingBehandler
import no.nav.helse.spesialist.api.rest.behandlinger.PostVedtakBehandler
import no.nav.helse.spesialist.api.rest.behandlingsstatistikk.GetBehandlingsstatistikkBehandler
import no.nav.helse.spesialist.api.rest.dialoger.PatchKommentarBehandler
import no.nav.helse.spesialist.api.rest.dialoger.PostKommentarBehandler
import no.nav.helse.spesialist.api.rest.dokumenter.DokumentMediator
import no.nav.helse.spesialist.api.rest.dokumenter.GetInntektsmeldingBehandler
import no.nav.helse.spesialist.api.rest.dokumenter.GetSoknadBehandler
import no.nav.helse.spesialist.api.rest.forsikringer.GetForsikringsvurderingForPersonBehandler
import no.nav.helse.spesialist.api.rest.notater.*
import no.nav.helse.spesialist.api.rest.oppgaver.GetAntallOppgaverBehandler
import no.nav.helse.spesialist.api.rest.oppgaver.GetBehandledeOppgaverBehandler
import no.nav.helse.spesialist.api.rest.oppgaver.GetOppgaverBehandler
import no.nav.helse.spesialist.api.rest.oppgaver.påVent.DeletePåVentBehandler
import no.nav.helse.spesialist.api.rest.oppgaver.påVent.PutPåVentBehandler
import no.nav.helse.spesialist.api.rest.personer.*
import no.nav.helse.spesialist.api.rest.personer.sykefraværstilfeller.sykepengegrunnlag.PostSykepengegrunnlagBehandler
import no.nav.helse.spesialist.api.rest.personer.tildeling.DeleteTildelingBehandler
import no.nav.helse.spesialist.api.rest.personer.tildeling.PutTildelingBehandler
import no.nav.helse.spesialist.api.rest.saksbehandlere.GetAktiveSaksbehandlereBehandler
import no.nav.helse.spesialist.api.rest.saksbehandlere.GetBrukerBehandler
import no.nav.helse.spesialist.api.rest.tilkomneinntekter.GetTilkomneInntektskilderForPersonBehandler
import no.nav.helse.spesialist.api.rest.tilkomneinntekter.PatchTilkommenInntektBehandler
import no.nav.helse.spesialist.api.rest.tilkomneinntekter.PostTilkomneInntekterBehandler
import no.nav.helse.spesialist.api.rest.totrinnsvurdering.PostSendIReturBehandler
import no.nav.helse.spesialist.api.rest.totrinnsvurdering.PostSendTilGodkjenningBehandler
import no.nav.helse.spesialist.api.rest.varsler.DeleteVarselvurderingBehandler
import no.nav.helse.spesialist.api.rest.varsler.GetVarselBehandler
import no.nav.helse.spesialist.api.rest.varsler.PutVarselvurderingBehandler
import no.nav.helse.spesialist.api.rest.vedtaksperioder.PostAnmodOmForkastingBehandler
import no.nav.helse.spesialist.api.rest.vedtaksperioder.PostVedtaksperiodeAnnullerBehandler
import no.nav.helse.spesialist.api.rest.vurderinger.PostArbeidstidsvurderingBehandler
import no.nav.helse.spesialist.api.sse.sse
import no.nav.helse.spesialist.application.*

fun Routing.restRoutes(
    restAdapter: RestAdapter,
    configuration: ApiModule.Configuration,
    dokumentMediator: DokumentMediator,
    environmentToggles: EnvironmentToggles,
    krrRegistrertStatusHenter: KrrRegistrertStatusHenter,
    behandlendeEnhetHenter: BehandlendeEnhetHenter,
    forsikringsvurderingHenter: ForsikringsvurderingHenter,
    infotrygdperiodeHenter: InfotrygdperiodeHenter,
    alleIdenterHenter: AlleIdenterHenter,
    personinfoHenter: PersoninfoHenter,
    sessionFactory: SessionFactory,
    opptegnelseListener: OpptegnelseListener,
    personPseudoIdProvider: PersonPseudoIdProvider,
    behandlingsstatistikkService: BehandlingsstatistikkService,
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
            sse(sessionFactory, opptegnelseListener, personPseudoIdProvider)
            get(GetAktiveSaksbehandlereBehandler(), restAdapter)
            get(GetBrukerBehandler(), restAdapter)

            get(GetOppgaverBehandler(), restAdapter)
            get(GetAntallOppgaverBehandler(), restAdapter)
            get(GetBehandledeOppgaverBehandler(), restAdapter)

            get(GetBehandlingsstatistikkBehandler(behandlingsstatistikkService), restAdapter)

            put(PutPåVentBehandler(), restAdapter)
            delete(DeletePåVentBehandler(), restAdapter)
            post(PostSendTilGodkjenningBehandler(), restAdapter)
            post(PostSendIReturBehandler(), restAdapter)

            put(PutTildelingBehandler(), restAdapter)
            delete(DeleteTildelingBehandler(), restAdapter)

            get(GetSaksbehandlerStansBehandler(), restAdapter)
            patch(PatchSaksbehandlerStansBehandler(), restAdapter)

            get(GetVeilederStansBehandler(), restAdapter)
            patch(PatchVeilederStansBehandler(), restAdapter)

            get(GetSoknadBehandler(dokumentMediator = dokumentMediator), restAdapter)
            get(GetInntektsmeldingBehandler(dokumentMediator = dokumentMediator), restAdapter)

            get(GetTilkomneInntektskilderForPersonBehandler(), restAdapter)
            post(PostTilkomneInntekterBehandler(), restAdapter)
            patch(PatchTilkommenInntektBehandler(), restAdapter)

            get(GetGraderteAndreYtelserForPersonBehandler(), restAdapter)
            post(PostGraderteAndreYtelserBehandler(), restAdapter)
            patch(PatchEndreGraderteAndreYtelserBehandler(), restAdapter)
            post(PostFjernGraderteAndreYtelserBehandler(), restAdapter)
            post(PostGjenopprettGraderteAndreYtelserBehandler(), restAdapter)

            post(PostSykepengegrunnlagBehandler(), restAdapter)

            post(PostVedtakBehandler(environmentToggles), restAdapter)
            post(PostForkastingBehandler(), restAdapter)

            post(PostVedtaksperiodeAnnullerBehandler(), restAdapter)
            post(PostAnmodOmForkastingBehandler(), restAdapter)

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

            post(PostPersonSokBehandler(alleIdenterHenter, personinfoHenter), restAdapter)

            get(GetKrrRegistrertStatusForPersonBehandler(krrRegistrertStatusHenter), restAdapter)

            get(GetBehandlendeEnhetForPersonBehandler(behandlendeEnhetHenter), restAdapter)

            get(GetPersonBehandler(personinfoHenter, alleIdenterHenter), restAdapter)

            get(GetNotatVedtaksperiodeIderForPersonBehandler(), restAdapter)

            get(GetInfotrygdperioderForPersonBehandler(infotrygdperiodeHenter), restAdapter)

            get(GetForsikringsvurderingForPersonBehandler(forsikringsvurderingHenter), restAdapter)
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
