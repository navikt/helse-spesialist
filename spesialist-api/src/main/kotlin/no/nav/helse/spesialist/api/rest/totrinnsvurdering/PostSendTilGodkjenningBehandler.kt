package no.nav.helse.spesialist.api.rest.totrinnsvurdering

import io.ktor.http.HttpStatusCode
import no.nav.helse.db.BehandlingRepository
import no.nav.helse.db.VedtakBegrunnelseFraDatabase
import no.nav.helse.db.VedtakBegrunnelseTypeFraDatabase
import no.nav.helse.modell.Modellfeil
import no.nav.helse.modell.OppgaveAlleredeSendtBeslutter
import no.nav.helse.modell.OppgaveKreverVurderingAvToSaksbehandlere
import no.nav.helse.modell.periodehistorikk.Historikkinnslag
import no.nav.helse.modell.vedtak.Utfall
import no.nav.helse.spesialist.api.rest.ApiErrorCode
import no.nav.helse.spesialist.api.rest.ApiSendTilGodkjenningRequest
import no.nav.helse.spesialist.api.rest.KallKontekst
import no.nav.helse.spesialist.api.rest.PostBehandler
import no.nav.helse.spesialist.api.rest.RestResponse
import no.nav.helse.spesialist.api.rest.Tags
import no.nav.helse.spesialist.api.rest.resources.OppgaverBase
import no.nav.helse.spesialist.domain.Behandling
import no.nav.helse.spesialist.domain.oppgave.OppgaveId

class PostSendTilGodkjenningBehandler : PostBehandler<OppgaverBase.OppgaveId.Totrinnsvurdering.SendTilGodkjenning, ApiSendTilGodkjenningRequest, Unit, ApiPostSendTilGodkjenningErrorCode> {
    override val tag = Tags.OPPGAVER

    override fun behandle(
        resource: OppgaverBase.OppgaveId.Totrinnsvurdering.SendTilGodkjenning,
        request: ApiSendTilGodkjenningRequest,
        kallKontekst: KallKontekst,
    ): RestResponse<Unit, ApiPostSendTilGodkjenningErrorCode> =
        kallKontekst.medOppgave(
            oppgaveId = OppgaveId(resource.parent.parent.oppgaveId),
            oppgaveIkkeFunnet = { ApiPostSendTilGodkjenningErrorCode.OPPGAVE_IKKE_FUNNET },
        ) { oppgave, behandling, _, person ->
            val behandlingspakke =
                kallKontekst.transaksjon.behandlingRepository.finnBehandlingspakke(behandling, person.id.value)
            val harUvurderteVarsler =
                kallKontekst.transaksjon.varselRepository
                    .finnVarslerFor(behandlingspakke.map { it.id })
                    .any { it.trengerVurdering() }
            if (harUvurderteVarsler) {
                return@medOppgave RestResponse.Error(ApiPostSendTilGodkjenningErrorCode.MANGLER_VURDERING_AV_VARSLER)
            }

            val totrinnsvurdering =
                kallKontekst.transaksjon.totrinnsvurderingRepository.finnAktivForPerson(person.id.value)
                    ?: return@medOppgave RestResponse.Error(ApiPostSendTilGodkjenningErrorCode.TOTRINNSVURDERING_IKKE_FUNNET)
            try {
                håndterVedtakBegrunnelse(
                    utfall = behandling.utfall(),
                    begrunnelse = request.begrunnelse,
                    oppgaveId = oppgave.id.value,
                    saksbehandlerOid = kallKontekst.saksbehandler.id.value,
                    kallKontekst = kallKontekst,
                )
                val beslutter =
                    totrinnsvurdering.beslutter
                        ?.let(kallKontekst.transaksjon.saksbehandlerRepository::finn)
                oppgave.sendTilBeslutter(beslutter)
                totrinnsvurdering.sendTilBeslutter(oppgave.id.value, kallKontekst.saksbehandler.id)
            } catch (modellfeil: Modellfeil) {
                return@medOppgave RestResponse.Error(modellfeil.tilErrorCode())
            }
            kallKontekst.transaksjon.oppgaveRepository.lagre(oppgave)
            kallKontekst.transaksjon.totrinnsvurderingRepository.lagre(totrinnsvurdering)

            kallKontekst.transaksjon.påVentRepository.finnFor(oppgave.vedtaksperiodeId)?.let {
                oppgave.fjernFraPåVent()
                kallKontekst.transaksjon.påVentRepository.slett(it.id())
                kallKontekst.transaksjon.oppgaveRepository.lagre(oppgave)
            }

            val innslag = Historikkinnslag.avventerTotrinnsvurdering(kallKontekst.saksbehandler)
            kallKontekst.transaksjon.periodehistorikkDao.lagreMedOppgaveId(innslag, oppgave.id.value)

            RestResponse.NoContent()
        }

    private fun håndterVedtakBegrunnelse(
        utfall: Utfall,
        begrunnelse: String?,
        oppgaveId: Long,
        saksbehandlerOid: java.util.UUID,
        kallKontekst: KallKontekst,
    ) {
        val oppdatertBegrunnelse =
            VedtakBegrunnelseFraDatabase(
                type = utfall.tilDatabaseType(),
                tekst = begrunnelse.orEmpty(),
            )
        val eksisterendeBegrunnelse = kallKontekst.transaksjon.vedtakBegrunnelseDao.finnVedtakBegrunnelse(oppgaveId = oppgaveId)
        val erEndret = eksisterendeBegrunnelse != oppdatertBegrunnelse
        val erNy = eksisterendeBegrunnelse == null
        if (!erNy && erEndret) {
            kallKontekst.transaksjon.vedtakBegrunnelseDao.invaliderVedtakBegrunnelse(oppgaveId = oppgaveId)
        }
        if (erNy || erEndret) {
            kallKontekst.transaksjon.vedtakBegrunnelseDao.lagreVedtakBegrunnelse(
                oppgaveId = oppgaveId,
                vedtakBegrunnelse = oppdatertBegrunnelse,
                saksbehandlerOid = saksbehandlerOid,
            )
        }
    }

    private fun Utfall.tilDatabaseType() =
        when (this) {
            Utfall.AVSLAG -> VedtakBegrunnelseTypeFraDatabase.AVSLAG
            Utfall.DELVIS_INNVILGELSE -> VedtakBegrunnelseTypeFraDatabase.DELVIS_INNVILGELSE
            Utfall.INNVILGELSE -> VedtakBegrunnelseTypeFraDatabase.INNVILGELSE
        }

    private fun Modellfeil.tilErrorCode(): ApiPostSendTilGodkjenningErrorCode =
        when (this) {
            is OppgaveAlleredeSendtBeslutter -> ApiPostSendTilGodkjenningErrorCode.OPPGAVE_ALLEREDE_SENDT_TIL_BESLUTTER
            is OppgaveKreverVurderingAvToSaksbehandlere -> ApiPostSendTilGodkjenningErrorCode.KREVER_TOTRINNSVURDERING_AV_ANNEN
            else -> ApiPostSendTilGodkjenningErrorCode.UVENTET_MODELLFEIL
        }

    /**
     * Alle behandlinger som hører til samme sykefraværstilfelle (dvs. samme skjæringstidspunkt) som [behandling],
     * inkludert [behandling] selv. Brukes for å sikre at et uvurdert varsel på en tidligere behandling i samme
     * tilfelle blokkerer innsending til godkjenning, på samme måte som ved fatting av vedtak (se
     * PostVedtakBehandler).
     */
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
}

enum class ApiPostSendTilGodkjenningErrorCode(
    override val title: String,
    override val statusCode: HttpStatusCode,
) : ApiErrorCode {
    OPPGAVE_IKKE_FUNNET("Oppgave ikke funnet", HttpStatusCode.NotFound),
    MANGLER_VURDERING_AV_VARSLER("Det finnes aktive varsler som mangler vurdering", HttpStatusCode.Conflict),
    TOTRINNSVURDERING_IKKE_FUNNET("Aktiv totrinnsvurdering mangler for oppgaven", HttpStatusCode.Conflict),
    OPPGAVE_ALLEREDE_SENDT_TIL_BESLUTTER("Oppgaven er allerede sendt til beslutter", HttpStatusCode.Conflict),
    KREVER_TOTRINNSVURDERING_AV_ANNEN("Oppgaven krever totrinnsvurdering av annen saksbehandler", HttpStatusCode.Conflict),
    UVENTET_MODELLFEIL("Kunne ikke sende oppgaven til godkjenning", HttpStatusCode.BadRequest),
}
