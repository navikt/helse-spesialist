package no.nav.helse.spesialist.api.rest

import io.github.smiley4.ktoropenapi.config.RouteConfig
import io.ktor.http.HttpStatusCode
import no.nav.helse.db.SessionContext
import no.nav.helse.modell.melding.SubsumsjonEvent
import no.nav.helse.modell.saksbehandler.handlinger.MinimumSykdomsgrad
import no.nav.helse.modell.saksbehandler.handlinger.MinimumSykdomsgradArbeidsgiver
import no.nav.helse.modell.saksbehandler.handlinger.MinimumSykdomsgradPeriode
import no.nav.helse.modell.totrinnsvurdering.Totrinnsvurdering
import no.nav.helse.modell.vilkårsprøving.Subsumsjon
import no.nav.helse.spesialist.api.rest.tilkommeninntekt.harTilgangTilPerson
import no.nav.helse.spesialist.application.Outbox
import no.nav.helse.spesialist.application.logg.sikkerlogg
import no.nav.helse.spesialist.domain.Saksbehandler
import no.nav.helse.spesialist.domain.tilgangskontroll.Tilgangsgruppe
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.spesialist.api.rest.resources.MinimumSykdomsgrad as MinimumSykdomsgradResource

class PostMinimumSykdomsgradBehandler(
    private val versjonAvKode: String,
) : PostBehandler<MinimumSykdomsgradResource, ApiMinimumSykdomsgradRequest, Unit, ApiPostMinimumSykdomsgradErrorCode> {
    override fun behandle(
        resource: MinimumSykdomsgradResource,
        request: ApiMinimumSykdomsgradRequest,
        saksbehandler: Saksbehandler,
        tilgangsgrupper: Set<Tilgangsgruppe>,
        transaksjon: SessionContext,
        outbox: Outbox,
    ): RestResponse<Unit, ApiPostMinimumSykdomsgradErrorCode> {
        val fødselsnummer = request.fødselsnummer

        if (!harTilgangTilPerson(
                fødselsnummer = fødselsnummer,
                saksbehandler = saksbehandler,
                tilgangsgrupper = tilgangsgrupper,
                transaksjon = transaksjon,
            )
        ) {
            return RestResponse.Error(ApiPostMinimumSykdomsgradErrorCode.MANGLER_TILGANG_TIL_PERSON)
        }

        if (request.perioderVurdertOk.isEmpty() && request.perioderVurdertIkkeOk.isEmpty()) {
            return RestResponse.Error(ApiPostMinimumSykdomsgradErrorCode.MANGLER_VURDERTE_PERIODER)
        }

        val overstyring =
            MinimumSykdomsgrad.ny(
                aktørId = request.aktørId,
                fødselsnummer = request.fødselsnummer,
                saksbehandlerOid = saksbehandler.id(),
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

        sikkerlogg.info("Reserverer person $fødselsnummer til saksbehandler $saksbehandler")
        transaksjon.reservasjonDao.reserverPerson(saksbehandler.id().value, fødselsnummer)

        val totrinnsvurdering =
            transaksjon.totrinnsvurderingRepository.finnAktivForPerson(fødselsnummer)
                ?: Totrinnsvurdering.ny(fødselsnummer)
        totrinnsvurdering.nyOverstyring(overstyring = overstyring)
        transaksjon.totrinnsvurderingRepository.lagre(totrinnsvurdering)

        val event =
            overstyring.byggEvent(
                oid = saksbehandler.id().value,
                navn = saksbehandler.navn,
                epost = saksbehandler.epost,
                ident = saksbehandler.ident,
            )

        val subsumsjoner =
            overstyring.perioderVurdertOk
                .map { periode ->
                    byggSubsumsjonEvent(
                        avslag = false,
                        periode = periode,
                        fødselsnummer = overstyring.fødselsnummer,
                        initierendeVedtaksperiodeId = overstyring.vedtaksperiodeId,
                        arbeidsgivere = overstyring.arbeidsgivere,
                        overstyringId = overstyring.eksternHendelseId,
                        saksbehandlerEpost = saksbehandler.epost,
                    )
                }.plus(
                    overstyring.perioderVurdertIkkeOk.map { periode ->
                        byggSubsumsjonEvent(
                            avslag = true,
                            periode = periode,
                            fødselsnummer = overstyring.fødselsnummer,
                            initierendeVedtaksperiodeId = overstyring.vedtaksperiodeId,
                            arbeidsgivere = overstyring.arbeidsgivere,
                            overstyringId = overstyring.eksternHendelseId,
                            saksbehandlerEpost = saksbehandler.epost,
                        )
                    },
                )
        subsumsjoner.forEach { subsumsjonEvent ->
            outbox.leggTil(fødselsnummer, subsumsjonEvent, versjonAvKode)
        }

        outbox.leggTil(fødselsnummer, event, "vurdering av minimum sykdomsgrad")

        return RestResponse.NoContent()
    }

    override fun openApi(config: RouteConfig) {
        with(config) {
            tags = setOf("Overstyringer")
        }
    }

    private fun byggSubsumsjonEvent(
        avslag: Boolean,
        periode: MinimumSykdomsgradPeriode,
        fødselsnummer: String,
        initierendeVedtaksperiodeId: UUID,
        arbeidsgivere: List<MinimumSykdomsgradArbeidsgiver>,
        overstyringId: UUID,
        saksbehandlerEpost: String,
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
                    "initierendeVedtaksperiode" to initierendeVedtaksperiodeId,
                ),
            output = emptyMap(),
            sporing =
                Subsumsjon
                    .SporingVurdertMinimumSykdomsgrad(
                        vedtaksperioder = arbeidsgivere.map { it.berørtVedtaksperiodeId },
                        organisasjonsnummer = arbeidsgivere.map { it.organisasjonsnummer },
                        minimumSykdomsgradId = overstyringId,
                        saksbehandler = listOf(saksbehandlerEpost),
                    ).byggEvent(),
            tidsstempel = LocalDateTime.now(),
            kilde = "spesialist",
        )
}

enum class ApiPostMinimumSykdomsgradErrorCode(
    override val title: String,
    override val statusCode: HttpStatusCode,
) : ApiErrorCode {
    MANGLER_TILGANG_TIL_PERSON("Mangler tilgang til person", HttpStatusCode.Forbidden),
    MANGLER_VURDERTE_PERIODER("Mangler vurderte perioder", HttpStatusCode.BadRequest),
}
