package no.nav.helse.spesialist.api.rest.personer.sykefraværstilfeller.arbeidsforhold

import io.ktor.http.HttpStatusCode
import no.nav.helse.modell.vilkårsprøving.Lovhjemmel
import no.nav.helse.spesialist.api.rest.ApiArbeidsforholdoverstyringRequest
import no.nav.helse.spesialist.api.rest.ApiErrorCode
import no.nav.helse.spesialist.api.rest.KallKontekst
import no.nav.helse.spesialist.api.rest.PostBehandler
import no.nav.helse.spesialist.api.rest.RestResponse
import no.nav.helse.spesialist.api.rest.Tags
import no.nav.helse.spesialist.api.rest.resources.Personer
import no.nav.helse.spesialist.application.PersonPseudoId
import no.nav.helse.spesialist.application.logg.loggInfo
import no.nav.helse.spesialist.domain.Person
import no.nav.helse.spesialist.domain.Totrinnsvurdering
import no.nav.helse.spesialist.domain.overstyringer.Arbeidsforhold
import no.nav.helse.spesialist.domain.overstyringer.OverstyrtArbeidsforhold
import no.nav.helse.tell

class PostArbeidsforholdoverstyringBehandler : PostBehandler<Personer.PersonPseudoId.Sykefraværstilfeller.Skjæringstidspunkt.Arbeidsforhold, ApiArbeidsforholdoverstyringRequest, Unit, ApiPostArbeidsforholdoverstyringErrorCode> {
    override val tag: Tags = Tags.PERSONER

    override fun behandle(
        resource: Personer.PersonPseudoId.Sykefraværstilfeller.Skjæringstidspunkt.Arbeidsforhold,
        request: ApiArbeidsforholdoverstyringRequest,
        kallKontekst: KallKontekst,
    ): RestResponse<Unit, ApiPostArbeidsforholdoverstyringErrorCode> =
        kallKontekst.medPerson(
            personPseudoId = PersonPseudoId.fraString(resource.parent.parent.parent.pseudoId),
            personPseudoIdIkkeFunnet = { ApiPostArbeidsforholdoverstyringErrorCode.PERSON_PSEUDO_ID_IKKE_FUNNET },
            manglerTilgangTilPerson = { ApiPostArbeidsforholdoverstyringErrorCode.MANGLER_TILGANG_TIL_PERSON },
        ) { person -> behandleForPerson(resource, request, person, kallKontekst) }

    private fun behandleForPerson(
        resource: Personer.PersonPseudoId.Sykefraværstilfeller.Skjæringstidspunkt.Arbeidsforhold,
        request: ApiArbeidsforholdoverstyringRequest,
        person: Person,
        kallKontekst: KallKontekst,
    ): RestResponse<Unit, ApiPostArbeidsforholdoverstyringErrorCode> {
        if (request.overstyrteArbeidsforhold.isEmpty()) {
            return RestResponse.Error(ApiPostArbeidsforholdoverstyringErrorCode.TOM_LISTE_MED_ARBEIDSFORHOLD)
        }

        val transaksjon = kallKontekst.transaksjon
        val saksbehandler = kallKontekst.saksbehandler
        val identitetsnummer = person.id

        val overstyring =
            OverstyrtArbeidsforhold.ny(
                saksbehandlerOid = saksbehandler.id,
                fødselsnummer = identitetsnummer.value,
                aktørId = person.aktørId,
                vedtaksperiodeId = request.initierendeVedtaksperiodeId,
                skjæringstidspunkt = resource.parent.skjæringstidspunkt,
                overstyrteArbeidsforhold =
                    request.overstyrteArbeidsforhold.map { arbeidsforhold ->
                        Arbeidsforhold(
                            organisasjonsnummer = arbeidsforhold.organisasjonsnummer,
                            deaktivert = arbeidsforhold.deaktivert,
                            begrunnelse = arbeidsforhold.begrunnelse,
                            forklaring = arbeidsforhold.forklaring,
                            lovhjemmel =
                                arbeidsforhold.lovverksreferanse?.let {
                                    Lovhjemmel(
                                        paragraf = it.paragraf,
                                        ledd = it.ledd,
                                        bokstav = it.bokstav,
                                        lovverk = it.lovverk,
                                        lovverksversjon = it.lovverksversjon,
                                    )
                                },
                        )
                    },
            )

        transaksjon.reservasjonDao.reserverPerson(saksbehandlerOid = saksbehandler.id.value, identitetsnummer.value)

        val totrinnsvurdering =
            transaksjon.totrinnsvurderingRepository.finnAktivForPerson(identitetsnummer.value)
                ?: Totrinnsvurdering.ny(identitetsnummer.value)
        totrinnsvurdering.nyOverstyring(overstyring)
        transaksjon.totrinnsvurderingRepository.lagre(totrinnsvurdering)

        val event =
            overstyring.byggEvent(
                oid = saksbehandler.id.value,
                navn = saksbehandler.navn,
                epost = saksbehandler.epost,
                ident = saksbehandler.ident.value,
            )
        kallKontekst.outbox.leggTil(identitetsnummer, event, "overstyring av arbeidsforhold")

        tell(overstyring)

        loggInfo("Utførte overstyring av arbeidsforhold for person")

        return RestResponse.NoContent()
    }
}

enum class ApiPostArbeidsforholdoverstyringErrorCode(
    override val title: String,
    override val statusCode: HttpStatusCode,
) : ApiErrorCode {
    PERSON_PSEUDO_ID_IKKE_FUNNET("PersonPseudoId har utløpt (eller aldri eksistert)", HttpStatusCode.NotFound),
    MANGLER_TILGANG_TIL_PERSON("Mangler tilgang til person", HttpStatusCode.Forbidden),
    TOM_LISTE_MED_ARBEIDSFORHOLD("Mangler arbeidsforhold å overstyre", HttpStatusCode.BadRequest),
}
