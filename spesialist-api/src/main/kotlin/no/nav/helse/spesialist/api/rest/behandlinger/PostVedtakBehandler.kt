package no.nav.helse.spesialist.api.rest.behandlinger

import io.github.smiley4.ktoropenapi.config.RouteConfig
import io.ktor.http.HttpStatusCode
import no.nav.helse.bootstrap.EnvironmentToggles
import no.nav.helse.db.BehandlingRepository
import no.nav.helse.db.SessionContext
import no.nav.helse.modell.melding.OppgaveOppdatert
import no.nav.helse.modell.melding.VarselEndret
import no.nav.helse.modell.melding.VedtaksperiodeGodkjentManuelt
import no.nav.helse.modell.oppgave.Oppgave
import no.nav.helse.modell.periodehistorikk.Historikkinnslag
import no.nav.helse.modell.totrinnsvurdering.Totrinnsvurdering
import no.nav.helse.spesialist.api.rest.ApiErrorCode
import no.nav.helse.spesialist.api.rest.ApiVedtakRequest
import no.nav.helse.spesialist.api.rest.KallKontekst
import no.nav.helse.spesialist.api.rest.PostBehandler
import no.nav.helse.spesialist.api.rest.RestResponse
import no.nav.helse.spesialist.api.rest.resources.Behandlinger
import no.nav.helse.spesialist.application.Outbox
import no.nav.helse.spesialist.application.VarselRepository
import no.nav.helse.spesialist.application.VarseldefinisjonRepository
import no.nav.helse.spesialist.domain.Behandling
import no.nav.helse.spesialist.domain.Saksbehandler
import no.nav.helse.spesialist.domain.SaksbehandlerOid
import no.nav.helse.spesialist.domain.SpleisBehandlingId
import no.nav.helse.spesialist.domain.Varsel
import no.nav.helse.spesialist.domain.Vedtak
import no.nav.helse.spesialist.domain.VedtakBegrunnelse
import no.nav.helse.spesialist.domain.VedtaksperiodeId
import no.nav.helse.spesialist.domain.tilgangskontroll.Brukerrolle
import no.nav.helse.spesialist.domain.tilgangskontroll.Tilgang
import java.time.LocalDateTime

class PostVedtakBehandler(
    private val environmentToggles: EnvironmentToggles,
) : PostBehandler<Behandlinger.BehandlingId.Vedtak, ApiVedtakRequest, Unit, ApiPostVedtakErrorCode> {
    override val påkrevdTilgang = Tilgang.Skriv

    override fun behandle(
        resource: Behandlinger.BehandlingId.Vedtak,
        request: ApiVedtakRequest,
        kallKontekst: KallKontekst,
    ): RestResponse<Unit, ApiPostVedtakErrorCode> =
        kallKontekst.medBehandling(
            spleisBehandlingId = SpleisBehandlingId(resource.parent.behandlingId),
            behandlingIkkeFunnet = ApiPostVedtakErrorCode.BEHANDLING_IKKE_FUNNET,
            vedtaksperiodeIkkeFunnet = ApiPostVedtakErrorCode.VEDTAKSPERIODE_IKKE_FUNNET,
            personIkkeFunnet = ApiPostVedtakErrorCode.PERSON_IKKE_FUNNET,
            manglerTilgangTilPerson = ApiPostVedtakErrorCode.MANGLER_TILGANG_TIL_PERSON,
        ) { behandling, vedtaksperiode, person ->
            try {
                val spleisBehandlingId = behandling.spleisBehandlingId!!

                if (kallKontekst.transaksjon.behandlingRepository.finnNyesteForVedtaksperiode(vedtaksperiode.id) != behandling) {
                    return@medBehandling RestResponse.Error(ApiPostVedtakErrorCode.KAN_IKKE_FATTE_VEDTAK_PÅ_ELDRE_BEHANDLING)
                }

                val oppgave =
                    kallKontekst.transaksjon.oppgaveRepository.finn(spleisBehandlingId)
                        ?: return@medBehandling RestResponse.Error(ApiPostVedtakErrorCode.OPPGAVE_IKKE_FUNNET)
                if (oppgave.tilstand != Oppgave.AvventerSaksbehandler) {
                    return@medBehandling RestResponse.Error(
                        ApiPostVedtakErrorCode.OPPGAVE_FEIL_TILSTAND,
                    )
                }

                val totrinnsvurdering =
                    kallKontekst.transaksjon.totrinnsvurderingRepository.finnAktivForPerson(person.id.value)
                val saksbehandlerSomFattetVedtaket: Saksbehandler
                val beslutter: Saksbehandler?

                if (kallKontekst.transaksjon.vedtakRepository.finn(spleisBehandlingId) != null) {
                    return@medBehandling RestResponse.Error(ApiPostVedtakErrorCode.VEDTAK_ALLEREDE_FATTET)
                }

                val vedtak: Vedtak

                if (totrinnsvurdering != null) {
                    beslutter = kallKontekst.saksbehandler
                    saksbehandlerSomFattetVedtaket =
                        totrinnsvurdering.saksbehandler?.let { kallKontekst.transaksjon.saksbehandlerRepository.finn(it) }
                            ?: return@medBehandling RestResponse.Error(ApiPostVedtakErrorCode.TOTRINNSVURDERING_MANGLER_SAKSBEHANDLER)
                    vedtak =
                        Vedtak.manueltMedTotrinnskontroll(
                            id = spleisBehandlingId,
                            saksbehandlerIdent = saksbehandlerSomFattetVedtaket.ident,
                            beslutterIdent = beslutter.ident,
                        )
                    totrinnsvurdering.godkjenn(beslutter, kallKontekst.brukerroller)
                    val innslag = Historikkinnslag.totrinnsvurderingFerdigbehandletInnslag(beslutter)
                    kallKontekst.transaksjon.periodehistorikkDao.lagreMedOppgaveId(innslag, oppgave.id)
                    kallKontekst.transaksjon.totrinnsvurderingRepository.lagre(totrinnsvurdering)
                } else {
                    beslutter = null
                    saksbehandlerSomFattetVedtaket = kallKontekst.saksbehandler
                    vedtak =
                        Vedtak.manueltUtenTotrinnskontroll(spleisBehandlingId, saksbehandlerSomFattetVedtaket.ident)
                }
                kallKontekst.transaksjon.vedtakRepository.lagre(vedtak)
                kallKontekst.transaksjon.reservasjonDao.reserverPerson(
                    saksbehandlerSomFattetVedtaket.id.value,
                    person.id.value,
                )
                behandling.fattVedtak(
                    transaksjon = kallKontekst.transaksjon,
                    fødselsnummer = person.id.value,
                    saksbehandler = saksbehandlerSomFattetVedtaket,
                    oppgave = oppgave,
                    spleisBehandlingId = spleisBehandlingId,
                    begrunnelse = request.begrunnelse,
                    outbox = kallKontekst.outbox,
                )

                byttTilstandOgPubliserMeldinger(
                    fødselsnummer = person.id.value,
                    behandling = behandling,
                    oppgave = oppgave,
                    totrinnsvurdering = totrinnsvurdering,
                    saksbehandlerSomFattetVedtak = saksbehandlerSomFattetVedtaket,
                    beslutter = beslutter,
                    outbox = kallKontekst.outbox,
                    transaksjon = kallKontekst.transaksjon,
                )
            } catch (e: FattVedtakException) {
                return@medBehandling RestResponse.Error(e.code)
            }

            return@medBehandling RestResponse.NoContent()
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
        if (overlapperMedInfotrygd()) throw FattVedtakException(ApiPostVedtakErrorCode.OVERLAPPER_MED_INFOTRYGD)
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
        val behandlingspakke = behandlingRepository.finnBehandlingspakke(this, fødselsnummer)
        val behandlingUnikIder = behandlingspakke.map { behandling -> behandling.id }
        val varsler = varselRepository.finnVarslerFor(behandlingUnikIder)
        if (varsler.any { it.trengerVurdering() }) throw FattVedtakException(ApiPostVedtakErrorCode.VARSLER_MANGLER_VURDERING)
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
                ?: throw FattVedtakException(ApiPostVedtakErrorCode.VARSEL_MANGLER_VARSELDEFINISJON)
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
        brukerroller: Set<Brukerrolle>,
    ) {
        if (Brukerrolle.Beslutter !in brukerroller && !environmentToggles.kanGodkjenneUtenBesluttertilgang) {
            throw FattVedtakException(ApiPostVedtakErrorCode.SAKSBEHANDLER_MANGLER_BESLUTTERTILGANG)
        }
        if (this.saksbehandler?.value == beslutter.id.value && !environmentToggles.kanBeslutteEgneSaker) {
            throw FattVedtakException(ApiPostVedtakErrorCode.SAKSBEHANDLER_KAN_IKKE_BESLUTTE_EGEN_OPPGAVE)
        }
        settBeslutter(beslutter.id)
        ferdigstill()
    }

    private fun byttTilstandOgPubliserMeldinger(
        fødselsnummer: String,
        behandling: Behandling,
        oppgave: Oppgave,
        totrinnsvurdering: Totrinnsvurdering?,
        saksbehandlerSomFattetVedtak: Saksbehandler,
        beslutter: Saksbehandler?,
        outbox: Outbox,
        transaksjon: SessionContext,
    ) {
        val vedtaksperiodeId = behandling.vedtaksperiodeId
        val saksbehandleroverstyringer =
            totrinnsvurdering
                ?.overstyringer
                ?.filter { it.vedtaksperiodeId == vedtaksperiodeId.value }
                ?.map { it.eksternHendelseId } ?: emptyList()

        val godkjenningsbehov = transaksjon.meldingDao.finnGodkjenningsbehov(oppgave.godkjenningsbehovId)

        val behandlingspakke = transaksjon.behandlingRepository.finnBehandlingspakke(behandling, fødselsnummer)
        behandlingspakke.forEach(Behandling::håndterGodkjentAvSaksbehandler)
        transaksjon.behandlingRepository.lagreAlle(behandlingspakke)

        val behov = godkjenningsbehov.data()
        behov.godkjennManuelt(
            saksbehandlerIdent = saksbehandlerSomFattetVedtak.ident,
            saksbehandlerEpost = saksbehandlerSomFattetVedtak.epost,
            godkjenttidspunkt = LocalDateTime.now(),
            saksbehandleroverstyringer = saksbehandleroverstyringer,
        )

        outbox.leggTil(
            fødselsnummer = fødselsnummer,
            hendelse = behov.medLøsning(),
            årsak = "saksbehandlergodkjenning",
        )

        outbox.leggTil(
            fødselsnummer = fødselsnummer,
            hendelse =
                VedtaksperiodeGodkjentManuelt(
                    vedtaksperiodeId = behov.vedtaksperiodeId,
                    behandlingId = behov.spleisBehandlingId,
                    fødselsnummer = behov.fødselsnummer,
                    yrkesaktivitetstype = behov.yrkesaktivitetstype,
                    periodetype = behov.periodetype.name,
                    saksbehandlerIdent = saksbehandlerSomFattetVedtak.ident,
                    saksbehandlerEpost = saksbehandlerSomFattetVedtak.epost,
                    beslutterIdent = beslutter?.ident,
                    beslutterEpost = beslutter?.epost,
                ),
            årsak = "saksbehandlergodkjenning",
        )
    }

    private fun BehandlingRepository.finnBehandlingspakke(
        behandling: Behandling,
        fødselsnummer: String,
    ): List<Behandling> =
        finnAndreBehandlingerISykefraværstilfelle(
            behandling = behandling,
            fødselsnummer = fødselsnummer,
        ).toList()
            .sortedByDescending { it.tom }
            .filter { it.fom <= behandling.tom }
            .plus(behandling)

    override fun openApi(config: RouteConfig) {
        with(config) {
            tags = listOf("Behandlinger")
        }
    }

    class FattVedtakException(
        val code: ApiPostVedtakErrorCode,
    ) : Exception()
}

enum class ApiPostVedtakErrorCode(
    override val title: String,
    override val statusCode: HttpStatusCode,
) : ApiErrorCode {
    PERSON_IKKE_FUNNET("Person ikke funnet", HttpStatusCode.InternalServerError),
    MANGLER_TILGANG_TIL_PERSON("Mangler tilgang til person", HttpStatusCode.Forbidden),
    OPPGAVE_IKKE_FUNNET("Fant ikke oppgave.", HttpStatusCode.BadRequest),
    OPPGAVE_FEIL_TILSTAND("Oppgaven er i feil tilstand.", HttpStatusCode.BadRequest),
    SAKSBEHANDLER_MANGLER_BESLUTTERTILGANG("Mangler besluttertilgang", HttpStatusCode.Forbidden),
    SAKSBEHANDLER_KAN_IKKE_BESLUTTE_EGEN_OPPGAVE("Kan ikke beslutte egen oppgave", HttpStatusCode.Forbidden),
    VARSLER_MANGLER_VURDERING(
        "Kan ikke godkjenne varsler som ikke er vurdert av en saksbehandler",
        HttpStatusCode.BadRequest,
    ),
    OVERLAPPER_MED_INFOTRYGD(
        "Kan ikke fatte vedtak fordi perioden overlapper med infotrygd",
        HttpStatusCode.BadRequest,
    ),
    BEHANDLING_IKKE_FUNNET("Fant ikke behandling", HttpStatusCode.NotFound),
    KAN_IKKE_FATTE_VEDTAK_PÅ_ELDRE_BEHANDLING(
        "Kan ikke fatte vedtak på en eldre behandling av vedtaksperioden",
        HttpStatusCode.BadRequest,
    ),
    VEDTAKSPERIODE_IKKE_FUNNET("Fant ikke vedtaksperiode", HttpStatusCode.NotFound),
    TOTRINNSVURDERING_MANGLER_SAKSBEHANDLER(
        "Behandlende saksbehandler mangler i totrinnsvurdering",
        HttpStatusCode.Conflict,
    ),
    VARSEL_MANGLER_VARSELDEFINISJON("Varsel mangler varseldefinisjon", HttpStatusCode.BadRequest),
    VEDTAK_ALLEREDE_FATTET("Vedtaket er allerede fattet", HttpStatusCode.Conflict),
}
