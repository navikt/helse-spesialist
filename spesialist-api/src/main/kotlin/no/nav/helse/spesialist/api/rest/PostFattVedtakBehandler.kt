package no.nav.helse.spesialist.api.rest

import io.github.smiley4.ktoropenapi.config.RouteConfig
import io.ktor.http.HttpStatusCode
import no.nav.helse.bootstrap.EnvironmentToggles
import no.nav.helse.db.BehandlingRepository
import no.nav.helse.db.SessionContext
import no.nav.helse.modell.melding.OppgaveOppdatert
import no.nav.helse.modell.melding.VarselEndret
import no.nav.helse.modell.oppgave.Oppgave
import no.nav.helse.modell.totrinnsvurdering.Totrinnsvurdering
import no.nav.helse.spesialist.api.rest.resources.Vedtak
import no.nav.helse.spesialist.api.rest.tilkommeninntekt.bekreftTilgangTilPerson
import no.nav.helse.spesialist.application.Outbox
import no.nav.helse.spesialist.application.VarselRepository
import no.nav.helse.spesialist.application.VarseldefinisjonRepository
import no.nav.helse.spesialist.domain.Behandling
import no.nav.helse.spesialist.domain.Saksbehandler
import no.nav.helse.spesialist.domain.SaksbehandlerOid
import no.nav.helse.spesialist.domain.SpleisBehandlingId
import no.nav.helse.spesialist.domain.Varsel
import no.nav.helse.spesialist.domain.VedtakBegrunnelse
import no.nav.helse.spesialist.domain.VedtaksperiodeId
import no.nav.helse.spesialist.domain.tilgangskontroll.Tilgangsgruppe

class PostFattVedtakBehandler(
    private val environmentToggles: EnvironmentToggles,
) : PostBehandler<Vedtak.Id.Fatt, ApiFattVedtakRequest, Boolean> {
    override fun behandle(
        resource: Vedtak.Id.Fatt,
        request: ApiFattVedtakRequest,
        saksbehandler: Saksbehandler,
        tilgangsgrupper: Set<Tilgangsgruppe>,
        transaksjon: SessionContext,
        outbox: Outbox,
    ): RestResponse<Boolean> {
        val spleisBehandlingId = SpleisBehandlingId(resource.parent.behandlingId)
        val behandling =
            transaksjon.behandlingRepository.finn(spleisBehandlingId)
                ?: throw HttpNotFound("Fant ikke behandling med behandlingId ${resource.parent.behandlingId}")
        val vedtaksperiode =
            transaksjon.vedtaksperiodeRepository.finn(behandling.vedtaksperiodeId)
                ?: throw HttpNotFound("Fant ikke vedtaksperiode med id ${behandling.vedtaksperiodeId.value}")
        val fødselsnummer = vedtaksperiode.fødselsnummer

        bekreftTilgangTilPerson(
            fødselsnummer = fødselsnummer,
            saksbehandler = saksbehandler,
            tilgangsgrupper = tilgangsgrupper,
            transaksjon = transaksjon,
            feilSupplier = ::HttpForbidden,
        )

        val oppgave = transaksjon.oppgaveRepository.finn(spleisBehandlingId) ?: throw HttpBadRequest(OPPGAVE_IKKE_FUNNET)
        if (oppgave.tilstand != Oppgave.AvventerSaksbehandler) throw HttpBadRequest(OPPGAVE_FEIL_TILSTAND)

        val totrinnsvurdering = transaksjon.totrinnsvurderingRepository.finnAktivForPerson(fødselsnummer)
        val oidPersonSkalReserveresTil: SaksbehandlerOid
        if (totrinnsvurdering == null) {
            oidPersonSkalReserveresTil = saksbehandler.id()
            behandling.fattVedtak(transaksjon, fødselsnummer, saksbehandler, oppgave, request.begrunnelse, outbox)
        } else {
            oidPersonSkalReserveresTil = totrinnsvurdering.saksbehandler ?: throw HttpConflict("Behandlende saksbehandler mangler i totrinnsvurdering")
            totrinnsvurdering.godkjenn(saksbehandler, tilgangsgrupper)
            transaksjon.totrinnsvurderingRepository.lagre(totrinnsvurdering)
        }

        transaksjon.reservasjonDao.reserverPerson(oidPersonSkalReserveresTil.value, fødselsnummer)
        return RestResponse(HttpStatusCode.OK, false)
    }

    private fun Behandling.fattVedtak(
        transaksjon: SessionContext,
        fødselsnummer: String,
        saksbehandler: Saksbehandler,
        oppgave: Oppgave,
        begrunnelse: String?,
        outbox: Outbox,
    ) {
        if (overlapperMedInfotrygd()) throw HttpBadRequest(OVERLAPPER_MED_INFOTRYGD)
        val saksbehandlerOid = saksbehandler.id()
        validerOgGodkjennVarsler(
            behandlingRepository = transaksjon.behandlingRepository,
            varselRepository = transaksjon.varselRepository,
            varseldefinisjonRepository = transaksjon.varseldefinisjonRepository,
            fødselsnummer = fødselsnummer,
            saksbehandlerOid = saksbehandlerOid,
            outbox = outbox,
        )
        oppgave.avventerSystem(saksbehandler.ident, saksbehandlerOid.value)
        oppgave.fjernFraPåVent()
        transaksjon.påVentDao.slettPåVent(oppgave.id)
        transaksjon.oppgaveRepository.lagre(oppgave)
        outbox.leggTil(fødselsnummer, OppgaveOppdatert(oppgave), "oppgave avventer system")
        oppdaterVedtakBegrunnelse(transaksjon, begrunnelse, saksbehandlerOid)
    }

    private fun Behandling.oppdaterVedtakBegrunnelse(
        transaksjon: SessionContext,
        begrunnelse: String?,
        saksbehandlerOid: SaksbehandlerOid,
    ) {
        val repo = transaksjon.vedtakBegrunnelseRepository
        val eksisterende = repo.finn(id())
        val nyBegrunnelse = VedtakBegrunnelse.ny(id(), begrunnelse.orEmpty(), utfall(), saksbehandlerOid)
        if (eksisterende == null) {
            repo.lagre(nyBegrunnelse)
        } else if (eksisterende.erForskjelligFra(begrunnelse.orEmpty(), utfall())) {
            eksisterende.invalider()
            repo.lagre(eksisterende)
            repo.lagre(nyBegrunnelse)
        }
    }

    private fun Behandling.validerOgGodkjennVarsler(
        varseldefinisjonRepository: VarseldefinisjonRepository,
        behandlingRepository: BehandlingRepository,
        varselRepository: VarselRepository,
        fødselsnummer: String,
        saksbehandlerOid: SaksbehandlerOid,
        outbox: Outbox,
    ) {
        val behandlingerSomMåSeesUnderEtt =
            behandlingRepository
                .finnAndreBehandlingerISykefraværstilfelle(this, fødselsnummer)
                .filterNot { it.fom > tom }
                .plus(this)

        val behandlingIder = behandlingerSomMåSeesUnderEtt.map { behandling -> behandling.id() }
        val varsler = varselRepository.finnVarsler(behandlingIder)
        if (varsler.any { !it.kanGodkjennes() }) throw HttpBadRequest(VARSLER_MANGLER_VURDERING)
        varsler.forEach {
            it.godkjennOgPubliserEndring(
                vedtaksperiodeId = vedtaksperiodeId,
                spleisBehandlingId = id(),
                saksbehandlerOid = saksbehandlerOid,
                varseldefinisjonRepository = varseldefinisjonRepository,
                outbox = outbox,
                fødselsnummer = fødselsnummer,
            )
        }
        varselRepository.lagre(varsler)
    }

    private fun Varsel.godkjennOgPubliserEndring(
        vedtaksperiodeId: VedtaksperiodeId,
        spleisBehandlingId: SpleisBehandlingId,
        saksbehandlerOid: SaksbehandlerOid,
        varseldefinisjonRepository: VarseldefinisjonRepository,
        outbox: Outbox,
        fødselsnummer: String,
    ) {
        val gammelStatus = status
        godkjenn(saksbehandlerOid)
        val varseldefinisjon =
            varseldefinisjonRepository.finnGjeldendeFor(kode)
                ?: throw HttpBadRequest("Varsel mangler varseldefinisjon")
        outbox.leggTil(
            fødselsnummer = fødselsnummer,
            hendelse =
                VarselEndret(
                    vedtaksperiodeId = vedtaksperiodeId.value,
                    behandlingId = spleisBehandlingId.value,
                    varselId = id().value,
                    varseltittel = varseldefinisjon.tittel,
                    varselkode = kode,
                    forrigeStatus = gammelStatus.name,
                    gjeldendeStatus = status.name,
                ),
            årsak = "varsel godkjent",
        )
    }

    private fun Totrinnsvurdering.godkjenn(
        beslutter: Saksbehandler,
        tilgangsgrupper: Set<Tilgangsgruppe>,
    ) {
        if (Tilgangsgruppe.BESLUTTER !in tilgangsgrupper && !environmentToggles.kanGodkjenneUtenBesluttertilgang) {
            throw HttpForbidden(SAKSBEHANDLER_MANGLER_BESLUTTERTILGANG)
        }
        if (this.saksbehandler?.value == beslutter.id().value && !environmentToggles.kanBeslutteEgneSaker) {
            throw HttpForbidden(SAKSBEHANDLER_KAN_IKKE_BESLUTTE_EGEN_OPPGAVE)
        }
        settBeslutter(beslutter.id())
    }

    override fun openApi(config: RouteConfig) {
        with(config) {
            tags = listOf("Vedtak")
            operationId = operationIdBasertPåKlassenavn()
            response {
                code(HttpStatusCode.OK) {
                    body<Boolean>()
                }
            }
        }
    }

    companion object {
        const val OPPGAVE_IKKE_FUNNET = "Fant ikke oppgave."
        const val OPPGAVE_FEIL_TILSTAND = "Oppgaven er i feil tilstand."
        const val SAKSBEHANDLER_MANGLER_BESLUTTERTILGANG = "Mangler besluttertilgang"
        const val SAKSBEHANDLER_KAN_IKKE_BESLUTTE_EGEN_OPPGAVE = "Kan ikke beslutte egen oppgave"
        const val VARSLER_MANGLER_VURDERING = "Kan ikke godkjenne varsler som ikke er vurdert av en saksbehandler"
        const val OVERLAPPER_MED_INFOTRYGD = "Kan ikke fatte vedtak fordi perioden overlapper med infotrygd"
    }
}
