package no.nav.helse.spesialist.api.rest.vedtaksperioder.overstyring

import io.ktor.http.HttpStatusCode
import no.nav.helse.modell.vilkårsprøving.Lovhjemmel
import no.nav.helse.spesialist.api.rest.ApiErrorCode
import no.nav.helse.spesialist.api.rest.ApiOverstyrArbeidsforholdRequest
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
import no.nav.helse.spesialist.domain.overstyringer.Arbeidsforhold
import no.nav.helse.spesialist.domain.overstyringer.OverstyrtArbeidsforhold

class PostOverstyrArbeidsforholdBehandler : PostBehandler<Vedtaksperioder.VedtaksperiodeId.Overstyringer.Arbeidsforhold, ApiOverstyrArbeidsforholdRequest, Unit, ApiOverstyrArbeidsforholdErrorCode> {
    override val tag = Tags.OVERSTYRINGER

    override fun behandle(
        resource: Vedtaksperioder.VedtaksperiodeId.Overstyringer.Arbeidsforhold,
        request: ApiOverstyrArbeidsforholdRequest,
        kallKontekst: KallKontekst,
    ): RestResponse<Unit, ApiOverstyrArbeidsforholdErrorCode> =
        kallKontekst.medVedtaksperiode(
            vedtaksperiodeId = VedtaksperiodeId(resource.parent.parent.vedtaksperiodeId),
            vedtaksperiodeIkkeFunnet = { ApiOverstyrArbeidsforholdErrorCode.VEDTAKSPERIODE_IKKE_FUNNET },
            manglerTilgangTilPerson = { ApiOverstyrArbeidsforholdErrorCode.MANGLER_TILGANG_TIL_PERSON },
        ) { vedtaksperiode, person ->
            behandleForVedtaksperiode(request, vedtaksperiode, person, kallKontekst)
        }

    private fun behandleForVedtaksperiode(
        request: ApiOverstyrArbeidsforholdRequest,
        vedtaksperiode: Vedtaksperiode,
        person: Person,
        kallKontekst: KallKontekst,
    ): RestResponse<Unit, ApiOverstyrArbeidsforholdErrorCode> {
        val overstyring =
            OverstyrtArbeidsforhold.ny(
                saksbehandlerOid = kallKontekst.saksbehandler.id,
                fødselsnummer = person.id.value,
                aktørId = person.aktørId,
                vedtaksperiodeId = vedtaksperiode.id.value,
                skjæringstidspunkt = request.skjæringstidspunkt,
                overstyrteArbeidsforhold =
                    request.overstyrteArbeidsforhold.map { arbeidsforhold ->
                        Arbeidsforhold(
                            organisasjonsnummer = arbeidsforhold.organisasjonsnummer,
                            deaktivert = arbeidsforhold.deaktivert,
                            begrunnelse = arbeidsforhold.begrunnelse,
                            forklaring = arbeidsforhold.forklaring,
                            lovhjemmel =
                                arbeidsforhold.lovhjemmel?.let {
                                    Lovhjemmel(it.paragraf, it.ledd, it.bokstav, it.lovverk, it.lovverksversjon)
                                },
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
        kallKontekst.outbox.leggTil(person.id, event, "overstyring av arbeidsforhold")

        loggInfo("Overstyrte arbeidsforhold for vedtaksperiode")

        return RestResponse.NoContent()
    }
}

enum class ApiOverstyrArbeidsforholdErrorCode(
    override val title: String,
    override val statusCode: HttpStatusCode,
) : ApiErrorCode {
    VEDTAKSPERIODE_IKKE_FUNNET("Fant ikke vedtaksperiode", HttpStatusCode.NotFound),
    MANGLER_TILGANG_TIL_PERSON("Mangler tilgang til person", HttpStatusCode.Forbidden),
}
