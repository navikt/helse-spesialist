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
import no.nav.helse.spesialist.api.rest.KallKontekst
import no.nav.helse.spesialist.api.rest.PostBehandler
import no.nav.helse.spesialist.api.rest.RestResponse
import no.nav.helse.spesialist.api.rest.behandlinger.ApiPostForkastingErrorCode.BEHANDLING_IKKE_FUNNET
import no.nav.helse.spesialist.api.rest.behandlinger.ApiPostForkastingErrorCode.MANGLER_TILGANG_TIL_PERSON
import no.nav.helse.spesialist.api.rest.behandlinger.ApiPostForkastingErrorCode.OPPGAVE_FEIL_TILSTAND
import no.nav.helse.spesialist.api.rest.behandlinger.ApiPostForkastingErrorCode.OPPGAVE_IKKE_FUNNET
import no.nav.helse.spesialist.api.rest.behandlinger.ApiPostForkastingErrorCode.TOTRINNSVURDERING_SENDT_TIL_BESLUTTER
import no.nav.helse.spesialist.api.rest.resources.Behandlinger
import no.nav.helse.spesialist.application.Outbox
import no.nav.helse.spesialist.domain.Behandling
import no.nav.helse.spesialist.domain.Identitetsnummer
import no.nav.helse.spesialist.domain.Person
import no.nav.helse.spesialist.domain.Saksbehandler
import no.nav.helse.spesialist.domain.SpleisBehandlingId
import no.nav.helse.spesialist.domain.Varsel
import no.nav.helse.spesialist.domain.Varseldefinisjon
import no.nav.helse.spesialist.domain.Vedtaksperiode
import no.nav.helse.spesialist.domain.VedtaksperiodeId
import no.nav.helse.spesialist.domain.tilgangskontroll.Tilgang
import java.time.LocalDateTime

class PostForkastingBehandler : PostBehandler<Behandlinger.BehandlingId.Forkasting, ApiForkastingRequest, Unit, ApiPostForkastingErrorCode> {
    override val påkrevdTilgang = Tilgang.Skriv

    override fun behandle(
        resource: Behandlinger.BehandlingId.Forkasting,
        request: ApiForkastingRequest,
        kallKontekst: KallKontekst,
    ): RestResponse<Unit, ApiPostForkastingErrorCode> =
        kallKontekst.medBehandling(
            spleisBehandlingId = SpleisBehandlingId(resource.parent.behandlingId),
            behandlingIkkeFunnet = { BEHANDLING_IKKE_FUNNET },
            manglerTilgangTilPerson = { MANGLER_TILGANG_TIL_PERSON },
        ) { behandling, vedtaksperiode, person ->
            behandleForBehandling(request, behandling, vedtaksperiode, person, kallKontekst)
        }

    private fun behandleForBehandling(
        request: ApiForkastingRequest,
        behandling: Behandling,
        vedtaksperiode: Vedtaksperiode,
        person: Person,
        kallKontekst: KallKontekst,
    ): RestResponse<Unit, ApiPostForkastingErrorCode> {
        val oppgave =
            kallKontekst.transaksjon.oppgaveRepository.finn(behandling.spleisBehandlingId!!)
                ?: return RestResponse.Error(OPPGAVE_IKKE_FUNNET)

        if (oppgave.tilstand != Oppgave.AvventerSaksbehandler) {
            return RestResponse.Error(OPPGAVE_FEIL_TILSTAND)
        }

        oppgave.avventerSystem(kallKontekst.saksbehandler.ident, kallKontekst.saksbehandler.id.value)
        oppgave.fjernFraPåVent()
        kallKontekst.outbox.leggTil(person.id, OppgaveOppdatert(oppgave), "oppgave avventer system")
        kallKontekst.transaksjon.påVentDao.slettPåVent(oppgave.id)
        kallKontekst.transaksjon.oppgaveRepository.lagre(oppgave)

        val totrinnsvurdering =
            kallKontekst.transaksjon.totrinnsvurderingRepository.finnAktivForPerson(person.id.value)
        if (totrinnsvurdering != null) {
            if (totrinnsvurdering.tilstand == TotrinnsvurderingTilstand.AVVENTER_BESLUTTER) {
                return RestResponse.Error(TOTRINNSVURDERING_SENDT_TIL_BESLUTTER)
            }
            totrinnsvurdering.ferdigstill()
            kallKontekst.transaksjon.totrinnsvurderingRepository.lagre(totrinnsvurdering)
        }

        kallKontekst.transaksjon.reservasjonDao.reserverPerson(kallKontekst.saksbehandler.id.value, person.id.value)

        val varsler = kallKontekst.transaksjon.varselRepository.finnVarslerFor(listOf(behandling.id))
        varsler
            .filter {
                it.kanAvvises()
            }.onEach {
                val gammelStatus = it.status
                it.avvis()
                val varseldefinisjon =
                    kallKontekst.transaksjon.varseldefinisjonRepository.finnGjeldendeFor(it.kode)
                        ?: error("Varsel mangler varseldefinisjon")
                kallKontekst.outbox.leggTilAvvistVarsel(
                    gammelStatus = gammelStatus,
                    varsel = it,
                    vedtaksperiodeId = vedtaksperiode.id,
                    spleisBehandlingId = behandling.spleisBehandlingId!!,
                    identitetsnummer = person.id,
                    varseldefinisjon = varseldefinisjon,
                )
            }.let { avvisteVarsler ->
                kallKontekst.transaksjon.varselRepository.lagre(avvisteVarsler)
            }
        byttTilstandOgPubliserMeldinger(
            identitetsnummer = person.id,
            behandling = behandling,
            oppgave = oppgave,
            totrinnsvurdering = totrinnsvurdering,
            saksbehandler = kallKontekst.saksbehandler,
            outbox = kallKontekst.outbox,
            transaksjon = kallKontekst.transaksjon,
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
        identitetsnummer: Identitetsnummer,
        varseldefinisjon: Varseldefinisjon,
    ) {
        leggTil(
            identitetsnummer = identitetsnummer,
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
        identitetsnummer: Identitetsnummer,
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
            identitetsnummer = identitetsnummer,
            hendelse = behov.medLøsning(),
            årsak = "saksbehandlerforkasting",
        )

        outbox.leggTil(
            identitetsnummer = identitetsnummer,
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
    OPPGAVE_IKKE_FUNNET(HttpStatusCode.BadRequest, "Fant ikke oppgave."),
    MANGLER_TILGANG_TIL_PERSON(HttpStatusCode.Forbidden, "Mangler tilgang til person"),
    OPPGAVE_FEIL_TILSTAND(HttpStatusCode.BadRequest, "Oppgaven er i feil tilstand."),
    TOTRINNSVURDERING_SENDT_TIL_BESLUTTER(
        HttpStatusCode.BadRequest,
        "Totrinnsvurdering er sendt til beslutter, saken må returneres til saksbehandler før den kan kastes ut",
    ),
}
