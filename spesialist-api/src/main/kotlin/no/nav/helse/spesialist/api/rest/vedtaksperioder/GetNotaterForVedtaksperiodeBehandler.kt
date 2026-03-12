package no.nav.helse.spesialist.api.rest.vedtaksperioder

import io.ktor.http.HttpStatusCode
import no.nav.helse.spesialist.api.rest.ApiErrorCode
import no.nav.helse.spesialist.api.rest.ApiNotat
import no.nav.helse.spesialist.api.rest.GetBehandler
import no.nav.helse.spesialist.api.rest.KallKontekst
import no.nav.helse.spesialist.api.rest.RestResponse
import no.nav.helse.spesialist.api.rest.Tags
import no.nav.helse.spesialist.api.rest.mapping.tilApiNotat
import no.nav.helse.spesialist.api.rest.resources.Vedtaksperioder
import no.nav.helse.spesialist.application.logg.loggInfo
import no.nav.helse.spesialist.domain.Vedtaksperiode
import no.nav.helse.spesialist.domain.VedtaksperiodeId

class GetNotaterForVedtaksperiodeBehandler : GetBehandler<Vedtaksperioder.VedtaksperiodeId.Notater, List<ApiNotat>, GetNotaterForVedtaksperiodeErrorCode> {
    override val tag = Tags.NOTATER

    override fun behandle(
        resource: Vedtaksperioder.VedtaksperiodeId.Notater,
        kallKontekst: KallKontekst,
    ): RestResponse<List<ApiNotat>, GetNotaterForVedtaksperiodeErrorCode> =
        kallKontekst.medVedtaksperiode(
            vedtaksperiodeId = VedtaksperiodeId(resource.parent.vedtaksperiodeId),
            vedtaksperiodeIkkeFunnet = { GetNotaterForVedtaksperiodeErrorCode.VEDTAKSPERIODE_IKKE_FUNNET },
            manglerTilgangTilPerson = { GetNotaterForVedtaksperiodeErrorCode.MANGLER_TILGANG_TIL_PERSON },
        ) { vedtaksperiode, _ ->
            behandleForVedtaksperiode(vedtaksperiode, kallKontekst)
        }

    private fun behandleForVedtaksperiode(
        vedtaksperiode: Vedtaksperiode,
        kallKontekst: KallKontekst,
    ): RestResponse<List<ApiNotat>, GetNotaterForVedtaksperiodeErrorCode> {
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

        loggInfo("Hentet ${apiNotater.size} notater for vedtaksperioden")

        return RestResponse.OK(apiNotater)
    }
}

enum class GetNotaterForVedtaksperiodeErrorCode(
    override val statusCode: HttpStatusCode,
    override val title: String,
) : ApiErrorCode {
    MANGLER_TILGANG_TIL_PERSON(HttpStatusCode.Forbidden, "Mangler tilgang til person"),
    VEDTAKSPERIODE_IKKE_FUNNET(HttpStatusCode.NotFound, "Vedtaksperiode ikke funnet"),
}
