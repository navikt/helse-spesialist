package no.nav.helse.spesialist.api.rest.behandlinger

import io.github.smiley4.ktoropenapi.config.RouteConfig
import io.ktor.http.HttpStatusCode
import no.nav.helse.db.SessionContext
import no.nav.helse.modell.melding.OppgaveOppdatert
import no.nav.helse.modell.melding.VarselEndret
import no.nav.helse.modell.melding.VedtaksperiodeAvvistManuelt
import no.nav.helse.modell.oppgave.Oppgave
import no.nav.helse.modell.totrinnsvurdering.Totrinnsvurdering
import no.nav.helse.modell.totrinnsvurdering.TotrinnsvurderingTilstand
import no.nav.helse.spesialist.api.rest.ApiErrorCode
import no.nav.helse.spesialist.api.rest.ApiForkastingRequest
import no.nav.helse.spesialist.api.rest.PostBehandler
import no.nav.helse.spesialist.api.rest.RestResponse
import no.nav.helse.spesialist.api.rest.behandlinger.ApiPostForkastingErrorCode.BEHANDLING_IKKE_FUNNET
import no.nav.helse.spesialist.api.rest.behandlinger.ApiPostForkastingErrorCode.MANGLER_TILGANG_TIL_PERSON
import no.nav.helse.spesialist.api.rest.behandlinger.ApiPostForkastingErrorCode.OPPGAVE_FEIL_TILSTAND
import no.nav.helse.spesialist.api.rest.behandlinger.ApiPostForkastingErrorCode.OPPGAVE_IKKE_FUNNET
import no.nav.helse.spesialist.api.rest.behandlinger.ApiPostForkastingErrorCode.TOTRINNSVURDERING_SENDT_TIL_BESLUTTER
import no.nav.helse.spesialist.api.rest.behandlinger.ApiPostForkastingErrorCode.VEDTAKSPERIODE_IKKE_FUNNET
import no.nav.helse.spesialist.api.rest.harTilgangTilPerson
import no.nav.helse.spesialist.api.rest.resources.Behandlinger
import no.nav.helse.spesialist.application.Outbox
import no.nav.helse.spesialist.domain.Behandling
import no.nav.helse.spesialist.domain.Identitetsnummer
import no.nav.helse.spesialist.domain.Saksbehandler
import no.nav.helse.spesialist.domain.SpleisBehandlingId
import no.nav.helse.spesialist.domain.Varsel
import no.nav.helse.spesialist.domain.Varseldefinisjon
import no.nav.helse.spesialist.domain.VedtaksperiodeId
import no.nav.helse.spesialist.domain.tilgangskontroll.Tilgangsgruppe
import java.time.LocalDateTime

class PostForkastingBehandler : PostBehandler<Behandlinger.BehandlingId.Forkasting, ApiForkastingRequest, Unit, ApiPostForkastingErrorCode> {
    override fun behandle(
        resource: Behandlinger.BehandlingId.Forkasting,
        request: ApiForkastingRequest,
        saksbehandler: Saksbehandler,
        tilgangsgrupper: Set<Tilgangsgruppe>,
        transaksjon: SessionContext,
        outbox: Outbox,
    ): RestResponse<Unit, ApiPostForkastingErrorCode> {
        val spleisBehandlingId = SpleisBehandlingId(resource.parent.behandlingId)
        val behandling =
            transaksjon.behandlingRepository.finn(spleisBehandlingId)
                ?: return RestResponse.Error(BEHANDLING_IKKE_FUNNET)
        val vedtaksperiode =
            transaksjon.vedtaksperiodeRepository.finn(behandling.vedtaksperiodeId)
                ?: return RestResponse.Error(VEDTAKSPERIODE_IKKE_FUNNET)

        val fødselsnummer = vedtaksperiode.fødselsnummer

        if (!saksbehandler.harTilgangTilPerson(
                identitetsnummer = Identitetsnummer.fraString(identitetsnummer = fødselsnummer),
                tilgangsgrupper = tilgangsgrupper,
                transaksjon = transaksjon,
            )
        ) {
            return RestResponse.Error(MANGLER_TILGANG_TIL_PERSON)
        }
        val oppgave =
            transaksjon.oppgaveRepository.finn(spleisBehandlingId)
                ?: return RestResponse.Error(OPPGAVE_IKKE_FUNNET)
        if (oppgave.tilstand != Oppgave.AvventerSaksbehandler) return RestResponse.Error(OPPGAVE_FEIL_TILSTAND)

        oppgave.avventerSystem(saksbehandler.ident, saksbehandler.id.value)
        oppgave.fjernFraPåVent()
        outbox.leggTil(fødselsnummer, OppgaveOppdatert(oppgave), "oppgave avventer system")
        transaksjon.påVentDao.slettPåVent(oppgave.id)
        transaksjon.oppgaveRepository.lagre(oppgave)

        val totrinnsvurdering = transaksjon.totrinnsvurderingRepository.finnAktivForPerson(fødselsnummer)
        if (totrinnsvurdering != null) {
            if (totrinnsvurdering.tilstand == TotrinnsvurderingTilstand.AVVENTER_BESLUTTER) {
                return RestResponse.Error(TOTRINNSVURDERING_SENDT_TIL_BESLUTTER)
            }
            totrinnsvurdering.ferdigstill()
            transaksjon.totrinnsvurderingRepository.lagre(totrinnsvurdering)
        }

        transaksjon.reservasjonDao.reserverPerson(saksbehandler.id.value, fødselsnummer)

        val varsler = transaksjon.varselRepository.finnVarslerFor(listOf(behandling.id))
        varsler
            .filter {
                it.kanAvvises()
            }.onEach {
                val gammelStatus = it.status
                it.avvis()
                val varseldefinisjon =
                    transaksjon.varseldefinisjonRepository.finnGjeldendeFor(it.kode)
                        ?: error("Varsel mangler varseldefinisjon")
                outbox.leggTilAvvistVarsel(gammelStatus, it, vedtaksperiode.id, spleisBehandlingId, fødselsnummer, varseldefinisjon)
            }.let { avvisteVarsler ->
                transaksjon.varselRepository.lagre(avvisteVarsler)
            }
        byttTilstandOgPubliserMeldinger(
            fødselsnummer = fødselsnummer,
            behandling = behandling,
            oppgave = oppgave,
            totrinnsvurdering = totrinnsvurdering,
            saksbehandler = saksbehandler,
            outbox = outbox,
            transaksjon = transaksjon,
            årsak = request.årsak,
            begrunnelser = request.begrunnelser,
            kommentar = request.kommentar,
        )
        return RestResponse.NoContent()
    }

    override fun openApi(config: RouteConfig) {
        config.tags("Behandlinger")
    }

    private fun Outbox.leggTilAvvistVarsel(
        gammelStatus: Varsel.Status,
        varsel: Varsel,
        vedtaksperiodeId: VedtaksperiodeId,
        spleisBehandlingId: SpleisBehandlingId,
        fødselsnummer: String,
        varseldefinisjon: Varseldefinisjon,
    ) {
        this.leggTil(
            fødselsnummer = fødselsnummer,
            hendelse =
                VarselEndret(
                    vedtaksperiodeId = vedtaksperiodeId.value,
                    behandlingIdForBehandlingSomBleGodkjent = spleisBehandlingId.value,
                    varselId = varsel.id.value,
                    varseltittel = varseldefinisjon.tittel,
                    varselkode = varsel.kode,
                    forrigeStatus = gammelStatus.name,
                    gjeldendeStatus = varsel.status.name,
                ),
            årsak = "varsel avvist",
        )
    }

    private fun byttTilstandOgPubliserMeldinger(
        fødselsnummer: String,
        behandling: Behandling,
        oppgave: Oppgave,
        totrinnsvurdering: Totrinnsvurdering?,
        saksbehandler: Saksbehandler,
        outbox: Outbox,
        transaksjon: SessionContext,
        årsak: String,
        begrunnelser: List<String>,
        kommentar: String?,
    ) {
        val vedtaksperiodeId = behandling.vedtaksperiodeId
        val saksbehandleroverstyringer =
            totrinnsvurdering
                ?.overstyringer
                ?.filter { it.vedtaksperiodeId == vedtaksperiodeId.value }
                ?.map { it.eksternHendelseId } ?: emptyList()

        val godkjenningsbehov = transaksjon.meldingDao.finnGodkjenningsbehov(oppgave.godkjenningsbehovId)

        val behov = godkjenningsbehov.data()
        behov.avvisManuelt(
            saksbehandlerIdent = saksbehandler.ident,
            saksbehandlerEpost = saksbehandler.epost,
            avvisttidspunkt = LocalDateTime.now(),
            saksbehandleroverstyringer = saksbehandleroverstyringer,
            årsak = årsak,
            begrunnelser = begrunnelser,
            kommentar = kommentar,
        )

        outbox.leggTil(
            fødselsnummer = fødselsnummer,
            hendelse = behov.medLøsning(),
            årsak = "saksbehandlerforkasting",
        )

        outbox.leggTil(
            fødselsnummer = fødselsnummer,
            hendelse =
                VedtaksperiodeAvvistManuelt(
                    vedtaksperiodeId = behov.vedtaksperiodeId,
                    behandlingId = behov.spleisBehandlingId,
                    fødselsnummer = behov.fødselsnummer,
                    yrkesaktivitetstype = behov.yrkesaktivitetstype,
                    periodetype = behov.periodetype.name,
                    saksbehandlerIdent = saksbehandler.ident,
                    saksbehandlerEpost = saksbehandler.epost,
                    årsak = årsak,
                    begrunnelser = begrunnelser,
                    kommentar = kommentar,
                ),
            årsak = "saksbehandlerforkasting",
        )
    }
}

enum class ApiPostForkastingErrorCode(
    override val statusCode: HttpStatusCode,
    override val title: String,
) : ApiErrorCode {
    BEHANDLING_IKKE_FUNNET(HttpStatusCode.NotFound, "Fant ikke behandling"),
    VEDTAKSPERIODE_IKKE_FUNNET(HttpStatusCode.NotFound, "Fant ikke vedtaksperiode"),
    OPPGAVE_IKKE_FUNNET(HttpStatusCode.BadRequest, "Fant ikke oppgave."),
    MANGLER_TILGANG_TIL_PERSON(HttpStatusCode.Forbidden, "Mangler tilgang til person"),
    OPPGAVE_FEIL_TILSTAND(HttpStatusCode.BadRequest, "Oppgaven er i feil tilstand."),
    TOTRINNSVURDERING_SENDT_TIL_BESLUTTER(HttpStatusCode.BadRequest, "Totrinnsvurdering er sendt til beslutter, saken må returneres til saksbehandler før den kan kastes ut"),
}
