package no.nav.helse.spesialist.api.rest.personer.sykefraværstilfeller.sykepengegrunnlag

import io.ktor.http.HttpStatusCode
import no.nav.helse.modell.vilkårsprøving.Lovhjemmel
import no.nav.helse.spesialist.api.rest.ApiErrorCode
import no.nav.helse.spesialist.api.rest.ApiSykepengegrunnlagRequest
import no.nav.helse.spesialist.api.rest.ApiSykepengegrunnlagRequest.ApiSykepengegrunnlagtype.ApiSkjønnsfastsatt
import no.nav.helse.spesialist.api.rest.KallKontekst
import no.nav.helse.spesialist.api.rest.PostBehandler
import no.nav.helse.spesialist.api.rest.RestResponse
import no.nav.helse.spesialist.api.rest.Tags
import no.nav.helse.spesialist.api.rest.resources.Personer
import no.nav.helse.spesialist.application.PersonPseudoId
import no.nav.helse.spesialist.domain.Totrinnsvurdering
import no.nav.helse.spesialist.domain.overstyringer.SkjønnsfastsattArbeidsgiver
import no.nav.helse.spesialist.domain.overstyringer.SkjønnsfastsattArbeidsgiver.Skjønnsfastsettingstype
import no.nav.helse.spesialist.domain.overstyringer.SkjønnsfastsattSykepengegrunnlag

class PostSykepengegrunnlagBehandler : PostBehandler<Personer.PersonPseudoId.Sykefraværstilfeller.Skjæringstidspunkt.Sykepengegrunnlag, ApiSykepengegrunnlagRequest, Unit, ApiPostSykepengegrunnlagErrorCode> {
    override val tag: Tags = Tags.PERSONER

    override fun behandle(
        resource: Personer.PersonPseudoId.Sykefraværstilfeller.Skjæringstidspunkt.Sykepengegrunnlag,
        request: ApiSykepengegrunnlagRequest,
        kallKontekst: KallKontekst,
    ): RestResponse<Unit, ApiPostSykepengegrunnlagErrorCode> =
        kallKontekst.medPerson(
            personPseudoId = PersonPseudoId.fraString(resource.parent.parent.parent.pseudoId),
            personPseudoIdIkkeFunnet = { ApiPostSykepengegrunnlagErrorCode.PERSON_PSEUDO_ID_IKKE_FUNNET },
            manglerTilgangTilPerson = { ApiPostSykepengegrunnlagErrorCode.MANGLER_TILGANG_TIL_PERSON },
        ) { person ->
            val transaksjon = kallKontekst.transaksjon
            val identitetsnummer = person.id
            val saksbehandler = kallKontekst.saksbehandler
            transaksjon.reservasjonDao.reserverPerson(saksbehandlerOid = saksbehandler.id.value, identitetsnummer.value)
            val totrinnsvurdering =
                transaksjon.totrinnsvurderingRepository.finnAktivForPerson(identitetsnummer.value) ?: Totrinnsvurdering.ny(identitetsnummer.value)
            val overstyring =
                when (val sykepengegrunnlagstype = request.sykepengegrunnlagstype) {
                    is ApiSkjønnsfastsatt -> {
                        SkjønnsfastsattSykepengegrunnlag.ny(
                            saksbehandlerOid = saksbehandler.id,
                            fødselsnummer = person.id.value,
                            aktørId = person.aktørId,
                            vedtaksperiodeId = request.intierendeVedtaksperiodeId,
                            skjæringstidspunkt = resource.parent.skjæringstidspunkt,
                            arbeidsgivere =
                                sykepengegrunnlagstype.skjønnsfastsatteInntekter.map {
                                    SkjønnsfastsattArbeidsgiver(
                                        organisasjonsnummer = it.organisasjonsnummer,
                                        årlig = it.årlig,
                                        fraÅrlig = it.fraÅrlig,
                                    )
                                },
                            årsak = request.årsak,
                            type =
                                when (sykepengegrunnlagstype.skjønnsfastsettelsestype) {
                                    ApiSkjønnsfastsatt.ApiSkjønnsfastsettelsestype.OMREGNET_ÅRSINNTEKT -> Skjønnsfastsettingstype.OMREGNET_ÅRSINNTEKT
                                    ApiSkjønnsfastsatt.ApiSkjønnsfastsettelsestype.RAPPORTERT_ÅRSINNTEKT -> Skjønnsfastsettingstype.RAPPORTERT_ÅRSINNTEKT
                                    ApiSkjønnsfastsatt.ApiSkjønnsfastsettelsestype.ANNET -> Skjønnsfastsettingstype.ANNET
                                },
                            begrunnelseMal = request.begrunnelseMal,
                            begrunnelseFritekst = request.begrunnelseFritekst,
                            begrunnelseKonklusjon = request.begrunnelseKonklusjon,
                            lovhjemmel =
                                Lovhjemmel(
                                    sykepengegrunnlagstype.lovverksreferanse.paragraf,
                                    ledd = sykepengegrunnlagstype.lovverksreferanse.ledd,
                                    bokstav = sykepengegrunnlagstype.lovverksreferanse.bokstav,
                                    lovverk = sykepengegrunnlagstype.lovverksreferanse.lovverk,
                                    lovverksversjon = sykepengegrunnlagstype.lovverksreferanse.lovverksversjon,
                                ),
                        )
                    }
                }
            totrinnsvurdering.nyOverstyring(overstyring)
            transaksjon.totrinnsvurderingRepository.lagre(totrinnsvurdering)
            val event =
                overstyring.byggEvent(
                    oid = saksbehandler.id.value,
                    navn = saksbehandler.navn,
                    epost = saksbehandler.epost,
                    ident = saksbehandler.ident.value,
                )
            val subsumsjon = overstyring.byggSubsumsjon(saksbehandler.epost).byggEvent()
            kallKontekst.outbox.leggTil(person.id, event, "skjønnsfastsetting av sykepengegrunnlag")
            kallKontekst.outbox.leggTil(person.id, subsumsjon)

            return@medPerson RestResponse.OK(Unit)
        }
}

enum class ApiPostSykepengegrunnlagErrorCode(
    override val title: String,
    override val statusCode: HttpStatusCode,
) : ApiErrorCode {
    PERSON_PSEUDO_ID_IKKE_FUNNET("PersonPseudoId har utløpt (eller aldri eksistert)", HttpStatusCode.NotFound),
    MANGLER_TILGANG_TIL_PERSON("Mangler tilgang til person", HttpStatusCode.Forbidden),
}
