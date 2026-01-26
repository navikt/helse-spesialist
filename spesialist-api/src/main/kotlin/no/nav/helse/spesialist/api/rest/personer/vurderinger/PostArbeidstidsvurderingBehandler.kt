package no.nav.helse.spesialist.api.rest.personer.vurderinger

import io.github.smiley4.ktoropenapi.config.RouteConfig
import io.ktor.http.HttpStatusCode
import no.nav.helse.db.SessionContext
import no.nav.helse.modell.melding.SubsumsjonEvent
import no.nav.helse.modell.saksbehandler.handlinger.MinimumSykdomsgrad
import no.nav.helse.modell.saksbehandler.handlinger.MinimumSykdomsgradArbeidsgiver
import no.nav.helse.modell.saksbehandler.handlinger.MinimumSykdomsgradPeriode
import no.nav.helse.modell.totrinnsvurdering.Totrinnsvurdering
import no.nav.helse.modell.vilkårsprøving.Subsumsjon.SporingVurdertMinimumSykdomsgrad
import no.nav.helse.spesialist.api.rest.ApiArbeidstidsvurderingRequest
import no.nav.helse.spesialist.api.rest.ApiErrorCode
import no.nav.helse.spesialist.api.rest.PostBehandler
import no.nav.helse.spesialist.api.rest.RestResponse
import no.nav.helse.spesialist.api.rest.harTilgangTilPerson
import no.nav.helse.spesialist.api.rest.resources.Personer
import no.nav.helse.spesialist.application.Outbox
import no.nav.helse.spesialist.application.logg.teamLogs
import no.nav.helse.spesialist.domain.Identitetsnummer
import no.nav.helse.spesialist.domain.Saksbehandler
import no.nav.helse.spesialist.domain.tilgangskontroll.Tilgangsgruppe
import java.time.LocalDateTime
import java.util.UUID

class PostArbeidstidsvurderingBehandler : PostBehandler<Personer.PersonPseudoId.Vurderinger.Arbeidstid, ApiArbeidstidsvurderingRequest, Unit, ApiArbeidstidsvurderingErrorCode> {
    override fun behandle(
        resource: Personer.PersonPseudoId.Vurderinger.Arbeidstid,
        request: ApiArbeidstidsvurderingRequest,
        saksbehandler: Saksbehandler,
        tilgangsgrupper: Set<Tilgangsgruppe>,
        transaksjon: SessionContext,
        outbox: Outbox,
    ): RestResponse<Unit, ApiArbeidstidsvurderingErrorCode> {
        val fødselsnummer = request.fødselsnummer

        if (!saksbehandler.harTilgangTilPerson(
                identitetsnummer = Identitetsnummer.fraString(identitetsnummer = fødselsnummer),
                tilgangsgrupper = tilgangsgrupper,
                transaksjon = transaksjon,
            )
        ) {
            return RestResponse.Error(ApiArbeidstidsvurderingErrorCode.MANGLER_TILGANG_TIL_PERSON)
        }

        if (request.perioderVurdertOk.isEmpty() && request.perioderVurdertIkkeOk.isEmpty()) {
            return RestResponse.Error(ApiArbeidstidsvurderingErrorCode.MANGLER_VURDERTE_PERIODER)
        }

        val overstyring =
            MinimumSykdomsgrad.ny(
                aktørId = request.aktørId,
                fødselsnummer = request.fødselsnummer,
                saksbehandlerOid = saksbehandler.id,
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

        teamLogs.info("Reserverer person $fødselsnummer til saksbehandler $saksbehandler")
        transaksjon.reservasjonDao.reserverPerson(saksbehandler.id.value, fødselsnummer)

        val totrinnsvurdering =
            transaksjon.totrinnsvurderingRepository.finnAktivForPerson(fødselsnummer)
                ?: Totrinnsvurdering.ny(fødselsnummer)
        totrinnsvurdering.nyOverstyring(overstyring = overstyring)
        transaksjon.totrinnsvurderingRepository.lagre(totrinnsvurdering)

        val event =
            overstyring.byggEvent(
                oid = saksbehandler.id.value,
                navn = saksbehandler.navn,
                epost = saksbehandler.epost,
                ident = saksbehandler.ident.value,
            )

        val subsumsjoner =
            overstyring.perioderVurdertOk
                .map { periode ->
                    overstyring.byggSubsumsjonEvent(avslag = false, periode = periode, saksbehandler = saksbehandler)
                }.plus(
                    overstyring.perioderVurdertIkkeOk.map { periode ->
                        overstyring.byggSubsumsjonEvent(avslag = true, periode = periode, saksbehandler = saksbehandler)
                    },
                )
        subsumsjoner.forEach { subsumsjonEvent ->
            outbox.leggTil(fødselsnummer, subsumsjonEvent)
        }

        outbox.leggTil(fødselsnummer, event, "vurdering av minimum sykdomsgrad")

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
    MANGLER_TILGANG_TIL_PERSON("Mangler tilgang til person", HttpStatusCode.Forbidden),
    MANGLER_VURDERTE_PERIODER("Mangler vurderte perioder", HttpStatusCode.BadRequest),
}
