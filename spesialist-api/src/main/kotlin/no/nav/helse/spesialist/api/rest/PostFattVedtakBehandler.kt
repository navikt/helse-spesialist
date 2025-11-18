package no.nav.helse.spesialist.api.rest

import io.github.smiley4.ktoropenapi.config.RouteConfig
import io.ktor.http.HttpStatusCode
import net.logstash.logback.argument.StructuredArguments
import no.nav.helse.bootstrap.EnvironmentToggles
import no.nav.helse.db.BehandlingRepository
import no.nav.helse.db.SessionContext
import no.nav.helse.modell.melding.OppgaveOppdatert
import no.nav.helse.modell.melding.Saksbehandlerløsning
import no.nav.helse.modell.melding.VarselEndret
import no.nav.helse.modell.oppgave.Oppgave
import no.nav.helse.modell.periodehistorikk.Historikkinnslag
import no.nav.helse.modell.totrinnsvurdering.Totrinnsvurdering
import no.nav.helse.spesialist.api.rest.resources.Vedtak
import no.nav.helse.spesialist.application.Outbox
import no.nav.helse.spesialist.application.VarselRepository
import no.nav.helse.spesialist.application.VarseldefinisjonRepository
import no.nav.helse.spesialist.application.logg.logg
import no.nav.helse.spesialist.domain.Behandling
import no.nav.helse.spesialist.domain.Identitetsnummer
import no.nav.helse.spesialist.domain.Saksbehandler
import no.nav.helse.spesialist.domain.SaksbehandlerOid
import no.nav.helse.spesialist.domain.SpleisBehandlingId
import no.nav.helse.spesialist.domain.Varsel
import no.nav.helse.spesialist.domain.VedtakBegrunnelse
import no.nav.helse.spesialist.domain.VedtaksperiodeId
import no.nav.helse.spesialist.domain.tilgangskontroll.Tilgangsgruppe
import java.time.LocalDateTime

class PostFattVedtakBehandler(
    private val environmentToggles: EnvironmentToggles,
) : PostBehandler<Vedtak.Id.Fatt, ApiFattVedtakRequest, Unit, ApiPostFattVedtakErrorCode> {
    override fun behandle(
        resource: Vedtak.Id.Fatt,
        request: ApiFattVedtakRequest,
        saksbehandler: Saksbehandler,
        tilgangsgrupper: Set<Tilgangsgruppe>,
        transaksjon: SessionContext,
        outbox: Outbox,
    ): RestResponse<Unit, ApiPostFattVedtakErrorCode> {
        try {
            val spleisBehandlingId = SpleisBehandlingId(resource.parent.behandlingId)
            val behandling =
                transaksjon.behandlingRepository.finn(spleisBehandlingId)
                    ?: return RestResponse.Error(ApiPostFattVedtakErrorCode.BEHANDLING_IKKE_FUNNET)
            val vedtaksperiode =
                transaksjon.vedtaksperiodeRepository.finn(behandling.vedtaksperiodeId)
                    ?: return RestResponse.Error(ApiPostFattVedtakErrorCode.VEDTAKSPERIODE_IKKE_FUNNET)
            val fødselsnummer = vedtaksperiode.fødselsnummer

            if (!saksbehandler.harTilgangTilPerson(
                    identitetsnummer = Identitetsnummer.fraString(identitetsnummer = fødselsnummer),
                    tilgangsgrupper = tilgangsgrupper,
                    transaksjon = transaksjon,
                )
            ) {
                return RestResponse.Error(ApiPostFattVedtakErrorCode.MANGLER_TILGANG_TIL_PERSON)
            }

            val oppgave =
                transaksjon.oppgaveRepository.finn(spleisBehandlingId)
                    ?: return RestResponse.Error(ApiPostFattVedtakErrorCode.OPPGAVE_IKKE_FUNNET)
            if (oppgave.tilstand != Oppgave.AvventerSaksbehandler) return RestResponse.Error(ApiPostFattVedtakErrorCode.OPPGAVE_FEIL_TILSTAND)

            val totrinnsvurdering = transaksjon.totrinnsvurderingRepository.finnAktivForPerson(fødselsnummer)
            var saksbehandlerSomFattetVedtak = saksbehandler
            var beslutter: Saksbehandler? = null

            if (totrinnsvurdering != null) {
                beslutter = saksbehandler
                totrinnsvurdering.godkjenn(beslutter, tilgangsgrupper)
                saksbehandlerSomFattetVedtak =
                    totrinnsvurdering.saksbehandler?.let { transaksjon.saksbehandlerRepository.finn(it) }
                        ?: return RestResponse.Error(ApiPostFattVedtakErrorCode.TOTRINNSVURDERING_MANGLER_SAKSBEHANDLER)
                val innslag = Historikkinnslag.totrinnsvurderingFerdigbehandletInnslag(beslutter)
                transaksjon.periodehistorikkDao.lagreMedOppgaveId(innslag, oppgave.id)
                transaksjon.totrinnsvurderingRepository.lagre(totrinnsvurdering)
            }
            transaksjon.reservasjonDao.reserverPerson(saksbehandlerSomFattetVedtak.id.value, fødselsnummer)
            behandling.fattVedtak(
                transaksjon = transaksjon,
                fødselsnummer = fødselsnummer,
                saksbehandler = saksbehandlerSomFattetVedtak,
                oppgave = oppgave,
                spleisBehandlingId = spleisBehandlingId,
                begrunnelse = request.begrunnelse,
                outbox = outbox,
            )

            sendSaksbehandlerLøsning(
                fødselsnummer = fødselsnummer,
                behandling = behandling,
                oppgave = oppgave,
                totrinnsvurdering = totrinnsvurdering,
                saksbehandlerSomFattetVedtak = saksbehandlerSomFattetVedtak,
                beslutter = beslutter,
                outbox = outbox,
            )
        } catch (e: FattVedtakException) {
            return RestResponse.Error(e.code)
        }

        return RestResponse.NoContent()
    }

    private fun Behandling.fattVedtak(
        transaksjon: SessionContext,
        fødselsnummer: String,
        saksbehandler: Saksbehandler,
        oppgave: Oppgave,
        spleisBehandlingId: SpleisBehandlingId,
        begrunnelse: String?,
        outbox: Outbox,
    ) {
        if (overlapperMedInfotrygd()) throw FattVedtakException(ApiPostFattVedtakErrorCode.OVERLAPPER_MED_INFOTRYGD)
        val saksbehandlerOid = saksbehandler.id
        validerOgGodkjennVarsler(
            behandlingRepository = transaksjon.behandlingRepository,
            varselRepository = transaksjon.varselRepository,
            varseldefinisjonRepository = transaksjon.varseldefinisjonRepository,
            fødselsnummer = fødselsnummer,
            spleisBehandlingId = spleisBehandlingId,
            outbox = outbox,
        )
        oppgave.avventerSystem(saksbehandler.ident, saksbehandlerOid.value)
        oppgave.fjernFraPåVent()
        transaksjon.påVentDao.slettPåVent(oppgave.id)
        transaksjon.oppgaveRepository.lagre(oppgave)
        outbox.leggTil(fødselsnummer, OppgaveOppdatert(oppgave), "oppgave avventer system")
        oppdaterVedtakBegrunnelse(transaksjon, begrunnelse, saksbehandlerOid, spleisBehandlingId)
    }

    private fun Behandling.oppdaterVedtakBegrunnelse(
        transaksjon: SessionContext,
        begrunnelse: String?,
        saksbehandlerOid: SaksbehandlerOid,
        spleisBehandlingId: SpleisBehandlingId,
    ) {
        val repo = transaksjon.vedtakBegrunnelseRepository
        val eksisterende = repo.finn(spleisBehandlingId)
        val nyBegrunnelse = VedtakBegrunnelse.ny(spleisBehandlingId, begrunnelse.orEmpty(), utfall(), saksbehandlerOid)
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
        spleisBehandlingId: SpleisBehandlingId,
        outbox: Outbox,
    ) {
        val behandlingerSomMåSeesUnderEtt =
            behandlingRepository
                .finnAndreBehandlingerISykefraværstilfelle(this, fødselsnummer)
                .filterNot { it.fom > tom }
                .plus(this)

        val behandlingUnikIder = behandlingerSomMåSeesUnderEtt.map { behandling -> behandling.id }
        val varsler = varselRepository.finnVarslerFor(behandlingUnikIder)
        if (varsler.any { it.trengerVurdering() }) throw FattVedtakException(ApiPostFattVedtakErrorCode.VARSLER_MANGLER_VURDERING)
        varsler
            .filter { it.kanGodkjennes() }
            .forEach {
                it.godkjennOgPubliserEndring(
                    vedtaksperiodeId = vedtaksperiodeId,
                    spleisBehandlingId = spleisBehandlingId,
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
        varseldefinisjonRepository: VarseldefinisjonRepository,
        outbox: Outbox,
        fødselsnummer: String,
    ) {
        val gammelStatus = status
        godkjenn()
        val varseldefinisjon =
            varseldefinisjonRepository.finnGjeldendeFor(kode)
                ?: throw FattVedtakException(ApiPostFattVedtakErrorCode.VARSEL_MANGLER_VARSELDEFINISJON)
        outbox.leggTil(
            fødselsnummer = fødselsnummer,
            hendelse =
                VarselEndret(
                    vedtaksperiodeId = vedtaksperiodeId.value,
                    behandlingIdForBehandlingSomBleGodkjent = spleisBehandlingId.value,
                    varselId = id.value,
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
            throw FattVedtakException(ApiPostFattVedtakErrorCode.SAKSBEHANDLER_MANGLER_BESLUTTERTILGANG)
        }
        if (this.saksbehandler?.value == beslutter.id.value && !environmentToggles.kanBeslutteEgneSaker) {
            throw FattVedtakException(ApiPostFattVedtakErrorCode.SAKSBEHANDLER_KAN_IKKE_BESLUTTE_EGEN_OPPGAVE)
        }
        settBeslutter(beslutter.id)
        ferdigstill()
    }

    private fun sendSaksbehandlerLøsning(
        fødselsnummer: String,
        behandling: Behandling,
        oppgave: Oppgave,
        totrinnsvurdering: Totrinnsvurdering?,
        saksbehandlerSomFattetVedtak: Saksbehandler,
        beslutter: Saksbehandler?,
        outbox: Outbox,
    ) {
        val saksbehandlerløsning =
            Saksbehandlerløsning(
                godkjenningsbehovId = oppgave.godkjenningsbehovId,
                oppgaveId = oppgave.id,
                godkjent = true,
                saksbehandlerIdent = saksbehandlerSomFattetVedtak.ident,
                saksbehandlerOid = saksbehandlerSomFattetVedtak.id.value,
                saksbehandlerEpost = saksbehandlerSomFattetVedtak.epost,
                godkjenttidspunkt = LocalDateTime.now(),
                saksbehandleroverstyringer =
                    totrinnsvurdering
                        ?.overstyringer
                        ?.filter {
                            it.vedtaksperiodeId == behandling.vedtaksperiodeId.value
                        }?.map { it.eksternHendelseId } ?: emptyList(),
                saksbehandler = saksbehandlerSomFattetVedtak,
                årsak = null,
                begrunnelser = null,
                kommentar = null,
                beslutter = beslutter,
            )
        logg.info(
            "Publiserer saksbehandler-løsning for {}, {}",
            StructuredArguments.keyValue("oppgaveId", oppgave.id),
            StructuredArguments.keyValue("hendelseId", oppgave.godkjenningsbehovId),
        )
        outbox.leggTil(fødselsnummer, saksbehandlerløsning, "saksbehandlergodkjenning")
    }

    override fun openApi(config: RouteConfig) {
        with(config) {
            tags = listOf("Vedtak")
        }
    }

    class FattVedtakException(
        val code: ApiPostFattVedtakErrorCode,
    ) : Exception()
}

enum class ApiPostFattVedtakErrorCode(
    override val title: String,
    override val statusCode: HttpStatusCode,
) : ApiErrorCode {
    MANGLER_TILGANG_TIL_PERSON("Mangler tilgang til person", HttpStatusCode.Forbidden),
    OPPGAVE_IKKE_FUNNET("Fant ikke oppgave.", HttpStatusCode.BadRequest),
    OPPGAVE_FEIL_TILSTAND("Oppgaven er i feil tilstand.", HttpStatusCode.BadRequest),
    SAKSBEHANDLER_MANGLER_BESLUTTERTILGANG("Mangler besluttertilgang", HttpStatusCode.Forbidden),
    SAKSBEHANDLER_KAN_IKKE_BESLUTTE_EGEN_OPPGAVE("Kan ikke beslutte egen oppgave", HttpStatusCode.Forbidden),
    VARSLER_MANGLER_VURDERING("Kan ikke godkjenne varsler som ikke er vurdert av en saksbehandler", HttpStatusCode.BadRequest),
    OVERLAPPER_MED_INFOTRYGD("Kan ikke fatte vedtak fordi perioden overlapper med infotrygd", HttpStatusCode.BadRequest),
    BEHANDLING_IKKE_FUNNET("Fant ikke behandling", HttpStatusCode.NotFound),
    VEDTAKSPERIODE_IKKE_FUNNET("Fant ikke vedtaksperiode", HttpStatusCode.NotFound),
    TOTRINNSVURDERING_MANGLER_SAKSBEHANDLER("Behandlende saksbehandler mangler i totrinnsvurdering", HttpStatusCode.Conflict),
    VARSEL_MANGLER_VARSELDEFINISJON("Varsel mangler varseldefinisjon", HttpStatusCode.BadRequest),
}
