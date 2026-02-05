package no.nav.helse.spesialist.api.rest.personer.vurderinger

import io.github.smiley4.ktoropenapi.config.RouteConfig
import io.ktor.http.HttpStatusCode
import no.nav.helse.modell.melding.SubsumsjonEvent
import no.nav.helse.modell.saksbehandler.handlinger.MinimumSykdomsgrad
import no.nav.helse.modell.saksbehandler.handlinger.MinimumSykdomsgradArbeidsgiver
import no.nav.helse.modell.saksbehandler.handlinger.MinimumSykdomsgradPeriode
import no.nav.helse.modell.totrinnsvurdering.Totrinnsvurdering
import no.nav.helse.modell.vilkårsprøving.Subsumsjon.SporingVurdertMinimumSykdomsgrad
import no.nav.helse.spesialist.api.rest.ApiArbeidstidsvurderingRequest
import no.nav.helse.spesialist.api.rest.ApiErrorCode
import no.nav.helse.spesialist.api.rest.KallKontekst
import no.nav.helse.spesialist.api.rest.PostBehandler
import no.nav.helse.spesialist.api.rest.RestResponse
import no.nav.helse.spesialist.api.rest.resources.Personer
import no.nav.helse.spesialist.application.PersonPseudoId
import no.nav.helse.spesialist.application.logg.teamLogs
import no.nav.helse.spesialist.domain.Person
import no.nav.helse.spesialist.domain.Saksbehandler
import no.nav.helse.spesialist.domain.tilgangskontroll.Tilgang
import java.time.LocalDateTime
import java.util.UUID

class PostArbeidstidsvurderingBehandler : PostBehandler<Personer.PersonPseudoId.Vurderinger.Arbeidstid, ApiArbeidstidsvurderingRequest, Unit, ApiArbeidstidsvurderingErrorCode> {
    override val påkrevdTilgang = Tilgang.Skriv

    override fun behandle(
        resource: Personer.PersonPseudoId.Vurderinger.Arbeidstid,
        request: ApiArbeidstidsvurderingRequest,
        kallKontekst: KallKontekst,
    ): RestResponse<Unit, ApiArbeidstidsvurderingErrorCode> =
        kallKontekst.medPerson(
            personPseudoId = PersonPseudoId.fraString(resource.parent.parent.pseudoId),
            personPseudoIdIkkeFunnet = { ApiArbeidstidsvurderingErrorCode.PERSON_PSEUDO_ID_IKKE_FUNNET },
            manglerTilgangTilPerson = { ApiArbeidstidsvurderingErrorCode.MANGLER_TILGANG_TIL_PERSON },
        ) { person -> behandleForPerson(request, person, kallKontekst) }

    private fun behandleForPerson(
        request: ApiArbeidstidsvurderingRequest,
        person: Person,
        kallKontekst: KallKontekst,
    ): RestResponse<Unit, ApiArbeidstidsvurderingErrorCode> {
        // TODO: Denne bør ikke både ha fødselsnummer i request body, og personPseudoId i URL
        if (request.fødselsnummer != person.id.value) {
            return RestResponse.Error(ApiArbeidstidsvurderingErrorCode.MISMATCH_I_IDENTITETSNUMRE)
        }

        if (request.perioderVurdertOk.isEmpty() && request.perioderVurdertIkkeOk.isEmpty()) {
            return RestResponse.Error(ApiArbeidstidsvurderingErrorCode.MANGLER_VURDERTE_PERIODER)
        }

        val overstyring =
            MinimumSykdomsgrad.ny(
                aktørId = request.aktørId,
                fødselsnummer = request.fødselsnummer,
                saksbehandlerOid = kallKontekst.saksbehandler.id,
                perioderVurdertOk =
                    request.perioderVurdertOk.map {
                        MinimumSykdomsgradPeriode(
                            fom = it.fom,
                            tom = it.tom,
                        )
                    },
                perioderVurdertIkkeOk =
                    request.perioderVurdertIkkeOk.map {
                        MinimumSykdomsgradPeriode(
                            fom = it.fom,
                            tom = it.tom,
                        )
                    },
                begrunnelse = request.begrunnelse,
                arbeidsgivere =
                    request.arbeidsgivere.map {
                        MinimumSykdomsgradArbeidsgiver(
                            organisasjonsnummer = it.organisasjonsnummer,
                            berørtVedtaksperiodeId = it.berørtVedtaksperiodeId,
                        )
                    },
                vedtaksperiodeId = request.initierendeVedtaksperiodeId,
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

        val subsumsjoner =
            overstyring.perioderVurdertOk
                .map { periode ->
                    overstyring.byggSubsumsjonEvent(
                        avslag = false,
                        periode = periode,
                        saksbehandler = kallKontekst.saksbehandler,
                    )
                }.plus(
                    overstyring.perioderVurdertIkkeOk.map { periode ->
                        overstyring.byggSubsumsjonEvent(
                            avslag = true,
                            periode = periode,
                            saksbehandler = kallKontekst.saksbehandler,
                        )
                    },
                )
        subsumsjoner.forEach { subsumsjonEvent ->
            kallKontekst.outbox.leggTil(person.id, subsumsjonEvent)
        }

        kallKontekst.outbox.leggTil(person.id, event, "vurdering av minimum sykdomsgrad")

        return RestResponse.NoContent()
    }

    override fun openApi(config: RouteConfig) {
        with(config) {
            tags = setOf("Vurderinger")
        }
    }

    private fun MinimumSykdomsgrad.byggSubsumsjonEvent(
        avslag: Boolean,
        periode: MinimumSykdomsgradPeriode,
        saksbehandler: Saksbehandler,
    ): SubsumsjonEvent =
        SubsumsjonEvent(
            id = UUID.randomUUID(),
            fødselsnummer = fødselsnummer,
            paragraf = "8-13",
            ledd = "1",
            bokstav = null,
            lovverk = "folketrygdloven",
            lovverksversjon = "2019-01-01",
            utfall = if (!avslag) "VILKAR_OPPFYLT" else "VILKAR_IKKE_OPPFYLT",
            input =
                mapOf(
                    "fom" to periode.fom,
                    "tom" to periode.tom,
                    "initierendeVedtaksperiode" to vedtaksperiodeId,
                ),
            output = emptyMap(),
            sporing =
                SporingVurdertMinimumSykdomsgrad(
                    vedtaksperioder = arbeidsgivere.map { it.berørtVedtaksperiodeId },
                    organisasjonsnummer = arbeidsgivere.map { it.organisasjonsnummer },
                    minimumSykdomsgradId = eksternHendelseId,
                    saksbehandler = listOf(element = saksbehandler.epost),
                ).byggEvent(),
            tidsstempel = LocalDateTime.now(),
            kilde = "spesialist",
        )
}

enum class ApiArbeidstidsvurderingErrorCode(
    override val title: String,
    override val statusCode: HttpStatusCode,
) : ApiErrorCode {
    PERSON_PSEUDO_ID_IKKE_FUNNET("PersonPseudoId har utløpt (eller aldri eksistert)", HttpStatusCode.NotFound),
    MANGLER_TILGANG_TIL_PERSON("Mangler tilgang til person", HttpStatusCode.Forbidden),
    MISMATCH_I_IDENTITETSNUMRE(
        "Person referert til i URL matcher ikke person referert til i request",
        HttpStatusCode.BadRequest,
    ),
    MANGLER_VURDERTE_PERIODER("Mangler vurderte perioder", HttpStatusCode.BadRequest),
}
