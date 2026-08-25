package no.nav.helse.spesialist.api.rest.vedtaksperioder.overstyring

import io.ktor.http.HttpStatusCode
import no.nav.helse.modell.vilkårsprøving.Lovhjemmel
import no.nav.helse.spesialist.api.rest.ApiErrorCode
import no.nav.helse.spesialist.api.rest.ApiOverstyrInntektOgRefusjonRequest
import no.nav.helse.spesialist.api.rest.KallKontekst
import no.nav.helse.spesialist.api.rest.PostBehandler
import no.nav.helse.spesialist.api.rest.RestResponse
import no.nav.helse.spesialist.api.rest.Tags
import no.nav.helse.spesialist.api.rest.resources.Vedtaksperioder
import no.nav.helse.spesialist.application.logg.loggInfo
import no.nav.helse.spesialist.application.logg.teamLogs
import no.nav.helse.spesialist.domain.Person
import no.nav.helse.spesialist.domain.Totrinnsvurdering
import no.nav.helse.spesialist.domain.Vedtaksperiode
import no.nav.helse.spesialist.domain.VedtaksperiodeId
import no.nav.helse.spesialist.domain.overstyringer.OverstyrtArbeidsgiver
import no.nav.helse.spesialist.domain.overstyringer.OverstyrtInntektOgRefusjon
import no.nav.helse.spesialist.domain.overstyringer.Refusjonselement

class PostOverstyrInntektOgRefusjonBehandler : PostBehandler<Vedtaksperioder.VedtaksperiodeId.Overstyringer.InntektOgRefusjon, ApiOverstyrInntektOgRefusjonRequest, Unit, ApiOverstyrInntektOgRefusjonErrorCode> {
    override val tag = Tags.OVERSTYRINGER

    override fun behandle(
        resource: Vedtaksperioder.VedtaksperiodeId.Overstyringer.InntektOgRefusjon,
        request: ApiOverstyrInntektOgRefusjonRequest,
        kallKontekst: KallKontekst,
    ): RestResponse<Unit, ApiOverstyrInntektOgRefusjonErrorCode> =
        kallKontekst.medVedtaksperiode(
            vedtaksperiodeId = VedtaksperiodeId(resource.parent.parent.vedtaksperiodeId),
            vedtaksperiodeIkkeFunnet = { ApiOverstyrInntektOgRefusjonErrorCode.VEDTAKSPERIODE_IKKE_FUNNET },
            manglerTilgangTilPerson = { ApiOverstyrInntektOgRefusjonErrorCode.MANGLER_TILGANG_TIL_PERSON },
        ) { vedtaksperiode, person ->
            behandleForVedtaksperiode(request, vedtaksperiode, person, kallKontekst)
        }

    private fun behandleForVedtaksperiode(
        request: ApiOverstyrInntektOgRefusjonRequest,
        vedtaksperiode: Vedtaksperiode,
        person: Person,
        kallKontekst: KallKontekst,
    ): RestResponse<Unit, ApiOverstyrInntektOgRefusjonErrorCode> {
        val overstyring =
            OverstyrtInntektOgRefusjon.ny(
                saksbehandlerOid = kallKontekst.saksbehandler.id,
                fødselsnummer = person.id.value,
                aktørId = person.aktørId,
                vedtaksperiodeId = vedtaksperiode.id.value,
                skjæringstidspunkt = request.skjæringstidspunkt,
                arbeidsgivere =
                    request.arbeidsgivere.map { arbeidsgiver ->
                        OverstyrtArbeidsgiver(
                            organisasjonsnummer = arbeidsgiver.organisasjonsnummer,
                            månedligInntekt = arbeidsgiver.månedligInntekt,
                            fraMånedligInntekt = arbeidsgiver.fraMånedligInntekt,
                            refusjonsopplysninger =
                                arbeidsgiver.refusjonsopplysninger?.map {
                                    Refusjonselement(it.fom, it.tom, it.beløp)
                                },
                            fraRefusjonsopplysninger =
                                arbeidsgiver.fraRefusjonsopplysninger?.map {
                                    Refusjonselement(it.fom, it.tom, it.beløp)
                                },
                            begrunnelse = arbeidsgiver.begrunnelse,
                            forklaring = arbeidsgiver.forklaring,
                            lovhjemmel =
                                arbeidsgiver.lovhjemmel?.let {
                                    Lovhjemmel(it.paragraf, it.ledd, it.bokstav, it.lovverk, it.lovverksversjon)
                                },
                            fom = arbeidsgiver.fom,
                            tom = arbeidsgiver.tom,
                        )
                    },
            )

        teamLogs.info("Reserverer person ${person.id.value} til saksbehandler ${kallKontekst.saksbehandler}")
        kallKontekst.transaksjon.reservasjonDao.reserverPerson(kallKontekst.saksbehandler.id.value, person.id.value)

        val totrinnsvurdering =
            kallKontekst.transaksjon.totrinnsvurderingRepository.finnAktivForPerson(person.id.value)
                ?: Totrinnsvurdering.ny(person.id.value)
        totrinnsvurdering.nyOverstyring(overstyring = overstyring)
        kallKontekst.transaksjon.totrinnsvurderingRepository.lagre(totrinnsvurdering)

        val event =
            overstyring.byggEvent(
                oid = kallKontekst.saksbehandler.id.value,
                navn = kallKontekst.saksbehandler.navn,
                epost = kallKontekst.saksbehandler.epost,
                ident = kallKontekst.saksbehandler.ident.value,
            )
        kallKontekst.outbox.leggTil(person.id, event, "overstyring av inntekt og refusjon")

        loggInfo("Overstyrte inntekt og refusjon for vedtaksperiode")

        return RestResponse.NoContent()
    }
}

enum class ApiOverstyrInntektOgRefusjonErrorCode(
    override val title: String,
    override val statusCode: HttpStatusCode,
) : ApiErrorCode {
    VEDTAKSPERIODE_IKKE_FUNNET("Fant ikke vedtaksperiode", HttpStatusCode.NotFound),
    MANGLER_TILGANG_TIL_PERSON("Mangler tilgang til person", HttpStatusCode.Forbidden),
}
