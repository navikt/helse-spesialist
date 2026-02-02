package no.nav.helse.spesialist.api.rest.vedtaksperioder.notater

import io.github.smiley4.ktoropenapi.config.RouteConfig
import io.ktor.http.HttpStatusCode
import no.nav.helse.spesialist.api.rest.ApiErrorCode
import no.nav.helse.spesialist.api.rest.ApiKommentar
import no.nav.helse.spesialist.api.rest.ApiNotat
import no.nav.helse.spesialist.api.rest.ApiNotatType
import no.nav.helse.spesialist.api.rest.GetBehandler
import no.nav.helse.spesialist.api.rest.KallKontekst
import no.nav.helse.spesialist.api.rest.RestResponse
import no.nav.helse.spesialist.api.rest.resources.Vedtaksperioder
import no.nav.helse.spesialist.api.rest.vedtaksperioder.notater.GetNotatErrorCode.MANGLER_TILGANG_TIL_PERSON
import no.nav.helse.spesialist.api.rest.vedtaksperioder.notater.GetNotatErrorCode.NOTAT_IKKE_FUNNET
import no.nav.helse.spesialist.api.rest.vedtaksperioder.notater.GetNotatErrorCode.PERSON_IKKE_FUNNET
import no.nav.helse.spesialist.domain.Dialog
import no.nav.helse.spesialist.domain.Identitetsnummer
import no.nav.helse.spesialist.domain.Kommentar
import no.nav.helse.spesialist.domain.Notat
import no.nav.helse.spesialist.domain.NotatId
import no.nav.helse.spesialist.domain.NotatType
import no.nav.helse.spesialist.domain.Saksbehandler
import no.nav.helse.spesialist.domain.VedtaksperiodeId
import java.time.LocalDateTime

class GetNotatBehandler : GetBehandler<Vedtaksperioder.VedtaksperiodeId.Notater.NotatId, ApiNotat, GetNotatErrorCode> {
    override fun behandle(
        resource: Vedtaksperioder.VedtaksperiodeId.Notater.NotatId,
        kallKontekst: KallKontekst,
    ): RestResponse<ApiNotat, GetNotatErrorCode> {
        val notatId = NotatId(resource.notatId)
        val notat =
            kallKontekst.transaksjon.notatRepository.finn(notatId) ?: return RestResponse.Error(NOTAT_IKKE_FUNNET)

        val vedtaksperiode =
            kallKontekst.transaksjon.vedtaksperiodeRepository.finn(VedtaksperiodeId(notat.vedtaksperiodeId))
                ?: error("Fant ikke vedtaksperiode")

        val identitetsnummer = Identitetsnummer.fraString(vedtaksperiode.fødselsnummer)

        return kallKontekst.medPerson(
            identitetsnummer = identitetsnummer,
            personIkkeFunnet = PERSON_IKKE_FUNNET,
            manglerTilgangTilPerson = MANGLER_TILGANG_TIL_PERSON,
        ) { _ ->
            val dialog =
                kallKontekst.transaksjon.dialogRepository.finn(notat.dialogRef)
                    ?: error("Kunne ikke finne dialog med id ${notat.dialogRef}")

            RestResponse.OK(notat.tilApiNotat(kallKontekst.saksbehandler, dialog))
        }
    }

    override fun openApi(config: RouteConfig) {
        config.tags("Notater")
    }
}

private fun Notat.tilApiNotat(
    saksbehandler: Saksbehandler,
    dialog: Dialog,
) = ApiNotat(
    id = id().value,
    // TODO: Dette vil bli et problem på et tidspunkt!
    dialogRef = dialogRef.value.toInt(),
    tekst = tekst,
    opprettet = opprettetTidspunkt.roundToMicroseconds(),
    saksbehandlerOid = saksbehandlerOid.value,
    saksbehandlerNavn = saksbehandler.navn,
    saksbehandlerEpost = saksbehandler.epost,
    saksbehandlerIdent = saksbehandler.ident.value,
    vedtaksperiodeId = vedtaksperiodeId,
    feilregistrert = feilregistrert,
    feilregistrert_tidspunkt = feilregistrertTidspunkt?.roundToMicroseconds(),
    type = type.tilApiNotatType(),
    kommentarer = dialog.kommentarer.map { it.tilApiKommentar() },
)

private fun NotatType.tilApiNotatType() =
    when (this) {
        NotatType.Generelt -> ApiNotatType.Generelt
        NotatType.OpphevStans -> ApiNotatType.OpphevStans
    }

private fun Kommentar.tilApiKommentar() =
    ApiKommentar(
        id = id().value,
        tekst = tekst,
        opprettet = opprettetTidspunkt.roundToMicroseconds(),
        saksbehandlerident = saksbehandlerident.value,
        feilregistrert_tidspunkt = feilregistrertTidspunkt?.roundToMicroseconds(),
    )

enum class GetNotatErrorCode(
    override val statusCode: HttpStatusCode,
    override val title: String,
) : ApiErrorCode {
    PERSON_IKKE_FUNNET(HttpStatusCode.InternalServerError, "Person ikke funnet"),
    MANGLER_TILGANG_TIL_PERSON(HttpStatusCode.Forbidden, "Mangler tilgang til person"),
    NOTAT_IKKE_FUNNET(HttpStatusCode.NotFound, "Fant ikke notat"),
}

private fun LocalDateTime.roundToMicroseconds(): LocalDateTime = withNano(nano.roundHalfUp(1000))

private fun Int.roundHalfUp(scale: Int): Int = this - this % scale + if (this % scale >= scale / 2) scale else 0
