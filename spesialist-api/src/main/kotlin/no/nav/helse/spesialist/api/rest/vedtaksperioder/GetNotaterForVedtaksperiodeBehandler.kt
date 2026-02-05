package no.nav.helse.spesialist.api.rest.vedtaksperioder

import io.github.smiley4.ktoropenapi.config.RouteConfig
import io.ktor.http.HttpStatusCode
import no.nav.helse.spesialist.api.rest.ApiErrorCode
import no.nav.helse.spesialist.api.rest.ApiNotat
import no.nav.helse.spesialist.api.rest.GetBehandler
import no.nav.helse.spesialist.api.rest.KallKontekst
import no.nav.helse.spesialist.api.rest.RestResponse
import no.nav.helse.spesialist.api.rest.harTilgangTilPerson
import no.nav.helse.spesialist.api.rest.mapping.tilApiNotat
import no.nav.helse.spesialist.api.rest.resources.Vedtaksperioder
import no.nav.helse.spesialist.domain.Identitetsnummer
import no.nav.helse.spesialist.domain.VedtaksperiodeId
import no.nav.helse.spesialist.domain.tilgangskontroll.Tilgang

class GetNotaterForVedtaksperiodeBehandler : GetBehandler<Vedtaksperioder.VedtaksperiodeId.Notater, List<ApiNotat>, GetNotaterForVedtaksperiodeErrorCode> {
    override fun behandle(
        resource: Vedtaksperioder.VedtaksperiodeId.Notater,
        kallKontekst: KallKontekst,
    ): RestResponse<List<ApiNotat>, GetNotaterForVedtaksperiodeErrorCode> {
        val vedtaksperiode =
            kallKontekst.transaksjon.vedtaksperiodeRepository.finn(VedtaksperiodeId(resource.parent.vedtaksperiodeId))
                ?: return RestResponse.Error(GetNotaterForVedtaksperiodeErrorCode.VEDTAKSPERIODE_IKKE_FUNNET)

        if (!kallKontekst.saksbehandler.harTilgangTilPerson(
                identitetsnummer = Identitetsnummer.fraString(identitetsnummer = vedtaksperiode.fødselsnummer),
                brukerroller = kallKontekst.brukerroller,
                transaksjon = kallKontekst.transaksjon,
            )
        ) {
            return RestResponse.Error(GetNotaterForVedtaksperiodeErrorCode.MANGLER_TILGANG_TIL_PERSON)
        }

        val notater = kallKontekst.transaksjon.notatRepository.finnAlleForVedtaksperiode(vedtaksperiode.id.value)
        val saksbehandlere =
            kallKontekst.transaksjon.saksbehandlerRepository.finnAlle(notater.map { it.saksbehandlerOid }.toSet())
        val dialoger = kallKontekst.transaksjon.dialogRepository.finnAlle(notater.map { it.dialogRef }.toSet())

        val apiNotater =
            notater.map { notat ->
                val saksbehandler =
                    saksbehandlere.find { it.id == notat.saksbehandlerOid }
                        ?: error("Kan ikke finne saksbehandler for notat ${notat.id().value}")
                val dialog =
                    dialoger.find { it.id() == notat.dialogRef }
                        ?: error("Kan ikke finne dialog for notat ${notat.dialogRef}")
                notat.tilApiNotat(
                    saksbehandler = saksbehandler,
                    dialog = dialog,
                )
            }

        return RestResponse.OK(apiNotater)
    }

    override fun openApi(config: RouteConfig) {
        config.tags("Notater")
    }

    override val påkrevdTilgang: Tilgang
        get() = Tilgang.Les
}

enum class GetNotaterForVedtaksperiodeErrorCode(
    override val statusCode: HttpStatusCode,
    override val title: String,
) : ApiErrorCode {
    MANGLER_TILGANG_TIL_PERSON(HttpStatusCode.Forbidden, "Mangler tilgang til person"),
    VEDTAKSPERIODE_IKKE_FUNNET(HttpStatusCode.NotFound, "Vedtaksperiode ikke funnet"),
}
