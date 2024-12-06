package no.nav.helse.mediator

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import net.logstash.logback.argument.StructuredArguments.kv
import no.nav.helse.bootstrap.Environment
import no.nav.helse.db.AnnulleringDao
import no.nav.helse.db.OpptegnelseDao
import no.nav.helse.db.PeriodehistorikkDao
import no.nav.helse.db.PgDialogDao
import no.nav.helse.db.PgPeriodehistorikkDao
import no.nav.helse.db.ReservasjonDao
import no.nav.helse.db.SaksbehandlerDao
import no.nav.helse.db.VedtakBegrunnelseDao
import no.nav.helse.db.VedtakBegrunnelseFraDatabase
import no.nav.helse.db.VedtakBegrunnelseTypeFraDatabase
import no.nav.helse.mediator.oppgave.OppgaveService
import no.nav.helse.mediator.overstyring.Overstyringlagrer
import no.nav.helse.mediator.overstyring.Saksbehandlingsmelder
import no.nav.helse.mediator.påvent.PåVentRepository
import no.nav.helse.mediator.saksbehandler.SaksbehandlerLagrer
import no.nav.helse.modell.AlleredeAnnullert
import no.nav.helse.modell.FinnerIkkePåVent
import no.nav.helse.modell.ManglerTilgang
import no.nav.helse.modell.Modellfeil
import no.nav.helse.modell.OppgaveAlleredeSendtBeslutter
import no.nav.helse.modell.OppgaveAlleredeSendtIRetur
import no.nav.helse.modell.OppgaveKreverVurderingAvToSaksbehandlere
import no.nav.helse.modell.OppgaveTildeltNoenAndre
import no.nav.helse.modell.oppgave.Egenskap
import no.nav.helse.modell.overstyring.OverstyringDao
import no.nav.helse.modell.periodehistorikk.HistorikkinnslagDto
import no.nav.helse.modell.påvent.PåVentDao
import no.nav.helse.modell.saksbehandler.Saksbehandler
import no.nav.helse.modell.saksbehandler.Saksbehandler.Companion.toDto
import no.nav.helse.modell.saksbehandler.handlinger.Annullering
import no.nav.helse.modell.saksbehandler.handlinger.AnnulleringArsak
import no.nav.helse.modell.saksbehandler.handlinger.Arbeidsforhold
import no.nav.helse.modell.saksbehandler.handlinger.EndrePåVent
import no.nav.helse.modell.saksbehandler.handlinger.FjernPåVent
import no.nav.helse.modell.saksbehandler.handlinger.FjernPåVentUtenHistorikkinnslag
import no.nav.helse.modell.saksbehandler.handlinger.Handling
import no.nav.helse.modell.saksbehandler.handlinger.LeggPåVent
import no.nav.helse.modell.saksbehandler.handlinger.MinimumSykdomsgradArbeidsgiver
import no.nav.helse.modell.saksbehandler.handlinger.MinimumSykdomsgradPeriode
import no.nav.helse.modell.saksbehandler.handlinger.Oppgavehandling
import no.nav.helse.modell.saksbehandler.handlinger.Overstyring
import no.nav.helse.modell.saksbehandler.handlinger.OverstyrtArbeidsforhold
import no.nav.helse.modell.saksbehandler.handlinger.OverstyrtArbeidsgiver
import no.nav.helse.modell.saksbehandler.handlinger.OverstyrtInntektOgRefusjon
import no.nav.helse.modell.saksbehandler.handlinger.OverstyrtTidslinje
import no.nav.helse.modell.saksbehandler.handlinger.OverstyrtTidslinjedag
import no.nav.helse.modell.saksbehandler.handlinger.Personhandling
import no.nav.helse.modell.saksbehandler.handlinger.PåVent
import no.nav.helse.modell.saksbehandler.handlinger.PåVentÅrsak
import no.nav.helse.modell.saksbehandler.handlinger.Refusjonselement
import no.nav.helse.modell.saksbehandler.handlinger.SkjønnsfastsattArbeidsgiver
import no.nav.helse.modell.saksbehandler.handlinger.SkjønnsfastsattSykepengegrunnlag
import no.nav.helse.modell.stoppautomatiskbehandling.StansAutomatiskBehandlingMediator
import no.nav.helse.modell.totrinnsvurdering.TotrinnsvurderingService
import no.nav.helse.modell.vilkårsprøving.Lovhjemmel
import no.nav.helse.spesialist.api.Saksbehandlerhåndterer
import no.nav.helse.spesialist.api.abonnement.AbonnementDao
import no.nav.helse.spesialist.api.bootstrap.Tilgangsgrupper
import no.nav.helse.spesialist.api.feilhåndtering.FinnerIkkeLagtPåVent
import no.nav.helse.spesialist.api.feilhåndtering.IkkeTilgang
import no.nav.helse.spesialist.api.feilhåndtering.ManglerVurderingAvVarsler
import no.nav.helse.spesialist.api.feilhåndtering.OppgaveIkkeTildelt
import no.nav.helse.spesialist.api.graphql.mutation.Avslagshandling
import no.nav.helse.spesialist.api.graphql.mutation.Avslagstype
import no.nav.helse.spesialist.api.graphql.mutation.VedtakMutation.VedtakResultat
import no.nav.helse.spesialist.api.graphql.mutation.VedtakUtfall
import no.nav.helse.spesialist.api.graphql.schema.AnnulleringData
import no.nav.helse.spesialist.api.graphql.schema.ArbeidsforholdOverstyringHandling
import no.nav.helse.spesialist.api.graphql.schema.Avslag
import no.nav.helse.spesialist.api.graphql.schema.InntektOgRefusjonOverstyring
import no.nav.helse.spesialist.api.graphql.schema.MinimumSykdomsgrad
import no.nav.helse.spesialist.api.graphql.schema.Opptegnelse
import no.nav.helse.spesialist.api.graphql.schema.PaVentRequest
import no.nav.helse.spesialist.api.graphql.schema.Skjonnsfastsettelse
import no.nav.helse.spesialist.api.graphql.schema.Skjonnsfastsettelse.SkjonnsfastsettelseArbeidsgiver.SkjonnsfastsettelseType.ANNET
import no.nav.helse.spesialist.api.graphql.schema.Skjonnsfastsettelse.SkjonnsfastsettelseArbeidsgiver.SkjonnsfastsettelseType.OMREGNET_ARSINNTEKT
import no.nav.helse.spesialist.api.graphql.schema.Skjonnsfastsettelse.SkjonnsfastsettelseArbeidsgiver.SkjonnsfastsettelseType.RAPPORTERT_ARSINNTEKT
import no.nav.helse.spesialist.api.graphql.schema.TidslinjeOverstyring
import no.nav.helse.spesialist.api.graphql.schema.VedtakBegrunnelse
import no.nav.helse.spesialist.api.oppgave.OppgaveApiDao
import no.nav.helse.spesialist.api.saksbehandler.SaksbehandlerFraApi
import no.nav.helse.spesialist.api.saksbehandler.handlinger.AvmeldOppgave
import no.nav.helse.spesialist.api.saksbehandler.handlinger.HandlingFraApi
import no.nav.helse.spesialist.api.saksbehandler.handlinger.OpphevStans
import no.nav.helse.spesialist.api.saksbehandler.handlinger.TildelOppgave
import no.nav.helse.spesialist.api.tildeling.TildelingApiDto
import no.nav.helse.spesialist.api.varsel.ApiVarselRepository
import no.nav.helse.spesialist.api.varsel.Varsel
import no.nav.helse.spesialist.api.vedtak.GodkjenningDto
import no.nav.helse.spesialist.api.vedtak.Vedtaksperiode.Companion.avvisVarsler
import no.nav.helse.spesialist.api.vedtak.Vedtaksperiode.Companion.godkjennVarsler
import no.nav.helse.spesialist.api.vedtak.Vedtaksperiode.Companion.harAktiveVarsler
import no.nav.helse.spesialist.api.vedtaksperiode.ApiGenerasjonRepository
import no.nav.helse.tell
import org.slf4j.LoggerFactory
import java.util.UUID
import javax.sql.DataSource

internal class SaksbehandlerMediator(
    dataSource: DataSource,
    private val versjonAvKode: String,
    private val rapidsConnection: RapidsConnection,
    private val oppgaveService: OppgaveService,
    private val tilgangsgrupper: Tilgangsgrupper,
    private val stansAutomatiskBehandlingMediator: StansAutomatiskBehandlingMediator,
    private val totrinnsvurderingService: TotrinnsvurderingService,
) : Saksbehandlerhåndterer {
    private val saksbehandlerDao = SaksbehandlerDao(dataSource)
    private val generasjonRepository = ApiGenerasjonRepository(dataSource)
    private val varselRepository = ApiVarselRepository(dataSource)
    private val oppgaveApiDao = OppgaveApiDao(dataSource)
    private val opptegnelseDao = OpptegnelseDao(dataSource)
    private val abonnementDao = AbonnementDao(dataSource)
    private val reservasjonDao = ReservasjonDao(dataSource)
    private val overstyringDao = OverstyringDao(dataSource)
    private val påVentDao = PåVentDao(dataSource)
    private val periodehistorikkDao: PeriodehistorikkDao = PgPeriodehistorikkDao(dataSource)
    private val vedtakBegrunnelseDao = VedtakBegrunnelseDao(dataSource)
    private val annulleringDao = AnnulleringDao(dataSource)
    private val dialogDao = PgDialogDao(dataSource)
    private val env = Environment()

    override fun håndter(
        handlingFraApi: HandlingFraApi,
        saksbehandlerFraApi: SaksbehandlerFraApi,
    ) {
        val saksbehandler = saksbehandlerFraApi.tilSaksbehandler()
        val modellhandling = handlingFraApi.tilModellversjon()
        SaksbehandlerLagrer(saksbehandlerDao).lagre(saksbehandler)
        tell(modellhandling)
        saksbehandler.register(Saksbehandlingsmelder(rapidsConnection))
        saksbehandler.register(Subsumsjonsmelder(versjonAvKode, rapidsConnection))
        val handlingId = UUID.randomUUID()

        withMDC(
            mapOf(
                "saksbehandlerOid" to saksbehandler.oid().toString(),
                "handlingId" to handlingId.toString(),
            ),
        ) {
            sikkerlogg.info("Utfører handling ${modellhandling.loggnavn()} på vegne av saksbehandler $saksbehandler")
            when (modellhandling) {
                is Overstyring -> håndter(modellhandling, saksbehandler)
                is Oppgavehandling -> håndter(modellhandling, saksbehandler)
                is PåVent -> error("dette burde ikke skje")
                is Personhandling -> håndter(modellhandling, saksbehandler)
                is Annullering -> håndter(modellhandling, saksbehandler)
                else -> modellhandling.utførAv(saksbehandler)
            }
            sikkerlogg.info("Handling ${modellhandling.loggnavn()} utført")
        }
    }

    override fun vedtak(
        saksbehandlerFraApi: SaksbehandlerFraApi,
        oppgavereferanse: Long,
        godkjent: Boolean,
        avslag: no.nav.helse.spesialist.api.graphql.mutation.Avslag?,
    ): VedtakResultat {
        val saksbehandler = saksbehandlerFraApi.tilSaksbehandler()
        return vedtak(oppgavereferanse, saksbehandler, godkjent).also {
            håndterAvslag(
                avslag = avslag,
                oppgaveId = oppgavereferanse,
                saksbehandlerOid = saksbehandler.oid(),
            )
        }
    }

    override fun vedtak(
        saksbehandlerFraApi: SaksbehandlerFraApi,
        oppgavereferanse: Long,
        godkjent: Boolean,
        utfall: VedtakUtfall,
        begrunnelse: String?,
    ): VedtakResultat {
        val saksbehandler = saksbehandlerFraApi.tilSaksbehandler()
        return vedtak(oppgavereferanse, saksbehandler, godkjent).also {
            håndterVedtakBegrunnelse(
                utfall = utfall,
                begrunnelse = begrunnelse,
                oppgaveId = oppgavereferanse,
                saksbehandlerOid = saksbehandler.oid(),
            )
        }
    }

    override fun infotrygdVedtak(
        saksbehandlerFraApi: SaksbehandlerFraApi,
        oppgavereferanse: Long,
        godkjent: Boolean,
    ): VedtakResultat =
        vedtak(
            oppgavereferanse,
            saksbehandlerFraApi.tilSaksbehandler(),
            godkjent,
        )

    private fun vedtak(
        oppgavereferanse: Long,
        saksbehandler: Saksbehandler,
        godkjent: Boolean,
    ): VedtakResultat {
        val erÅpenOppgave = oppgaveService.venterPåSaksbehandler(oppgavereferanse)
        val spleisBehandlingId = oppgaveService.spleisBehandlingId(oppgavereferanse)
        val fødselsnummer = oppgaveApiDao.finnFødselsnummer(oppgavereferanse)
        if (!erÅpenOppgave) return VedtakResultat.Feil.IkkeÅpenOppgave()

        if (totrinnsvurderingService.erBeslutterOppgave(oppgavereferanse)) {
            if (!saksbehandler.harTilgangTil(listOf(Egenskap.BESLUTTER)) && !env.erDev) {
                return VedtakResultat.Feil.BeslutterFeil.TrengerBeslutterRolle()
            }
            if (totrinnsvurderingService.erEgenOppgave(oppgavereferanse, saksbehandler.oid()) && !env.erDev) {
                return VedtakResultat.Feil.BeslutterFeil.KanIkkeBeslutteEgenOppgave()
            }
            totrinnsvurderingService.settBeslutter(oppgavereferanse, saksbehandler.oid())
        }

        if (godkjent) {
            val perioderTilBehandling = generasjonRepository.perioderTilBehandling(oppgavereferanse)
            if (perioderTilBehandling.harAktiveVarsler()) return VedtakResultat.Feil.HarAktiveVarsler(oppgavereferanse)

            perioderTilBehandling.godkjennVarsler(
                fødselsnummer = fødselsnummer,
                behandlingId = spleisBehandlingId,
                ident = saksbehandler.ident(),
                godkjenner = this::vurderVarsel,
            )
        } else {
            val periodeTilGodkjenning = generasjonRepository.periodeTilGodkjenning(oppgavereferanse)
            periodeTilGodkjenning.avvisVarsler(
                fødselsnummer = fødselsnummer,
                behandlingId = spleisBehandlingId,
                ident = saksbehandler.ident(),
                godkjenner = this::vurderVarsel,
            )
        }

        påVentDao.slettPåVent(oppgavereferanse)
        return VedtakResultat.Ok(spleisBehandlingId)
    }

    override fun påVent(
        handling: PaVentRequest,
        saksbehandlerFraApi: SaksbehandlerFraApi,
    ) {
        val saksbehandler = saksbehandlerFraApi.tilSaksbehandler()
        val modellhandling = handling.tilModellversjon()
        SaksbehandlerLagrer(saksbehandlerDao).lagre(saksbehandler)
        tell(modellhandling)
        val handlingId = UUID.randomUUID()

        withMDC(
            mapOf(
                "saksbehandlerOid" to saksbehandler.oid().toString(),
                "handlingId" to handlingId.toString(),
            ),
        ) {
            sikkerlogg.info("Utfører handling ${modellhandling.loggnavn()} på vegne av saksbehandler $saksbehandler")
            when (modellhandling) {
                is LeggPåVent -> leggPåVent(modellhandling, saksbehandler)
                is FjernPåVent -> fjernFraPåVent(modellhandling, saksbehandler)
                is FjernPåVentUtenHistorikkinnslag ->
                    fjernFraPåVentUtenHistorikkinnslag(
                        modellhandling,
                    )

                is EndrePåVent -> endrePåVent(modellhandling, saksbehandler)
            }
            sikkerlogg.info(
                "Handling ${modellhandling.loggnavn()} utført på oppgave ${modellhandling.oppgaveId} på vegne av saksbehandler $saksbehandler",
            )
        }
    }

    private fun håndter(
        handling: Annullering,
        saksbehandler: Saksbehandler,
    ) {
        try {
            if (annulleringDao.finnAnnullering(handling.toDto()) != null) throw AlleredeAnnullert(handling)
            annulleringDao.lagreAnnullering(handling.toDto(), saksbehandler)
            handling.utførAv(saksbehandler)
        } catch (e: Modellfeil) {
            throw e.tilApiversjon()
        }
    }

    private fun håndter(
        handling: Oppgavehandling,
        saksbehandler: Saksbehandler,
    ) {
        try {
            oppgaveService.håndter(handling, saksbehandler)
        } catch (e: Modellfeil) {
            throw e.tilApiversjon()
        }
    }

    private fun håndter(
        handling: Personhandling,
        saksbehandler: Saksbehandler,
    ) = try {
        stansAutomatiskBehandlingMediator.håndter(handling, saksbehandler)
        handling.utførAv(saksbehandler)
    } catch (e: Modellfeil) {
        throw e.tilApiversjon()
    }

    private fun leggPåVent(
        handling: LeggPåVent,
        saksbehandler: Saksbehandler,
    ) {
        try {
            val dialogRef = dialogDao.lagre()
            val innslag =
                HistorikkinnslagDto.lagtPåVentInnslag(
                    notattekst = handling.notatTekst,
                    saksbehandler = saksbehandler.toDto(),
                    årsaker = handling.årsaker,
                    frist = handling.frist,
                    dialogRef = dialogRef,
                )
            periodehistorikkDao.lagreMedOppgaveId(innslag, handling.oppgaveId)
            oppgaveService.leggPåVent(handling, saksbehandler)
            PåVentRepository(påVentDao).leggPåVent(saksbehandler.oid(), handling, dialogRef)
            saksbehandler.register(Saksbehandlingsmelder(rapidsConnection))
            handling.utførAv(saksbehandler)
        } catch (e: Modellfeil) {
            throw e.tilApiversjon()
        }
    }

    private fun endrePåVent(
        handling: EndrePåVent,
        saksbehandler: Saksbehandler,
    ) {
        if (!påVentDao.erPåVent(handling.oppgaveId)) throw FinnerIkkeLagtPåVent(handling.oppgaveId)

        val dialogRef = dialogDao.lagre()
        val innslag =
            HistorikkinnslagDto.endrePåVentInnslag(
                notattekst = handling.notatTekst,
                saksbehandler = saksbehandler.toDto(),
                årsaker = handling.årsaker,
                frist = handling.frist,
                dialogRef = dialogRef,
            )
        periodehistorikkDao.lagreMedOppgaveId(innslag, handling.oppgaveId)
        oppgaveService.endrePåVent(handling, saksbehandler)
        PåVentRepository(påVentDao).endrePåVent(saksbehandler.oid(), handling, dialogRef)

        saksbehandler.register(Saksbehandlingsmelder(rapidsConnection))
        handling.utførAv(saksbehandler)
    }

    private fun fjernFraPåVent(
        handling: FjernPåVent,
        saksbehandler: Saksbehandler,
    ) {
        if (!påVentDao.erPåVent(handling.oppgaveId)) {
            sikkerlogg.info("Oppgave ${handling.oppgaveId} er ikke på vent")
            return
        }
        try {
            val innslag = HistorikkinnslagDto.fjernetFraPåVentInnslag(saksbehandler.toDto())
            periodehistorikkDao.lagreMedOppgaveId(innslag, handling.oppgaveId)
            oppgaveService.fjernFraPåVent(handling.oppgaveId)
            PåVentRepository(påVentDao).fjernFraPåVent(handling.oppgaveId)
        } catch (e: Modellfeil) {
            throw e.tilApiversjon()
        }
    }

    private fun fjernFraPåVentUtenHistorikkinnslag(handling: FjernPåVentUtenHistorikkinnslag) {
        if (!påVentDao.erPåVent(handling.oppgaveId)) {
            sikkerlogg.info("Oppgave ${handling.oppgaveId} er ikke på vent")
            return
        }
        try {
            oppgaveService.fjernFraPåVent(handling.oppgaveId)
            PåVentRepository(påVentDao).fjernFraPåVent(handling.oppgaveId)
        } catch (e: Modellfeil) {
            throw e.tilApiversjon()
        }
    }

    private fun håndter(
        handling: Overstyring,
        saksbehandler: Saksbehandler,
    ) {
        val fødselsnummer = handling.gjelderFødselsnummer()
        oppgaveService.håndter(handling)
        reservasjonDao.reserverPerson(saksbehandler.oid(), fødselsnummer)
        sikkerlogg.info("Reserverer person $fødselsnummer til saksbehandler $saksbehandler")
        Overstyringlagrer(overstyringDao).apply {
            this.lagre(handling, saksbehandler.oid())
        }
        handling.utførAv(saksbehandler)
    }

    override fun opprettAbonnement(
        saksbehandlerFraApi: SaksbehandlerFraApi,
        personidentifikator: String,
    ) {
        val saksbehandler = saksbehandlerFraApi.tilSaksbehandler()
        SaksbehandlerLagrer(saksbehandlerDao).lagre(saksbehandler)
        abonnementDao.opprettAbonnement(saksbehandler.oid(), personidentifikator)
    }

    override fun hentAbonnerteOpptegnelser(
        saksbehandlerFraApi: SaksbehandlerFraApi,
        sisteSekvensId: Int,
    ): List<Opptegnelse> {
        val saksbehandler = saksbehandlerFraApi.tilSaksbehandler()
        SaksbehandlerLagrer(saksbehandlerDao).lagre(saksbehandler)
        abonnementDao.registrerSistekvensnummer(saksbehandler.oid(), sisteSekvensId)
        return opptegnelseDao.finnOpptegnelser(saksbehandler.oid())
    }

    override fun hentAbonnerteOpptegnelser(saksbehandlerFraApi: SaksbehandlerFraApi): List<Opptegnelse> {
        val saksbehandler = saksbehandlerFraApi.tilSaksbehandler()
        SaksbehandlerLagrer(saksbehandlerDao).lagre(saksbehandler)
        return opptegnelseDao.finnOpptegnelser(saksbehandler.oid())
    }

    override fun hentAvslag(
        vedtaksperiodeId: UUID,
        utbetalingId: UUID,
    ): Set<Avslag> =
        vedtakBegrunnelseDao.finnAlleVedtakBegrunnelser(vedtaksperiodeId, utbetalingId)
            .mapNotNull { vedtakBegrunnelse ->
                when (vedtakBegrunnelse.type) {
                    VedtakBegrunnelseTypeFraDatabase.AVSLAG -> Avslagstype.AVSLAG
                    VedtakBegrunnelseTypeFraDatabase.DELVIS_INNVILGELSE -> Avslagstype.DELVIS_AVSLAG
                    VedtakBegrunnelseTypeFraDatabase.INNVILGELSE -> null
                }?.let { type ->
                    Avslag(
                        type = type,
                        begrunnelse = vedtakBegrunnelse.begrunnelse,
                        opprettet = vedtakBegrunnelse.opprettet,
                        saksbehandlerIdent = vedtakBegrunnelse.saksbehandlerIdent,
                        invalidert = vedtakBegrunnelse.invalidert,
                    )
                }
            }.toSet()

    override fun hentVedtakBegrunnelser(
        vedtaksperiodeId: UUID,
        utbetalingId: UUID,
    ): List<VedtakBegrunnelse> =
        vedtakBegrunnelseDao.finnAlleVedtakBegrunnelser(vedtaksperiodeId, utbetalingId)
            .map { vedtakBegrunnelse ->
                VedtakBegrunnelse(
                    utfall =
                        when (vedtakBegrunnelse.type) {
                            VedtakBegrunnelseTypeFraDatabase.AVSLAG -> VedtakUtfall.AVSLAG
                            VedtakBegrunnelseTypeFraDatabase.DELVIS_INNVILGELSE -> VedtakUtfall.DELVIS_INNVILGELSE
                            VedtakBegrunnelseTypeFraDatabase.INNVILGELSE -> VedtakUtfall.INNVILGELSE
                        },
                    begrunnelse = vedtakBegrunnelse.begrunnelse,
                    opprettet = vedtakBegrunnelse.opprettet,
                    saksbehandlerIdent = vedtakBegrunnelse.saksbehandlerIdent,
                )
            }

    override fun håndterAvslag(
        oppgaveId: Long,
        saksbehandlerFraApi: SaksbehandlerFraApi,
        avslag: no.nav.helse.spesialist.api.graphql.mutation.Avslag,
    ) {
        håndterAvslag(
            avslag = avslag,
            oppgaveId = oppgaveId,
            saksbehandlerOid = saksbehandlerFraApi.oid,
        )
    }

    override fun håndterVedtakBegrunnelse(
        oppgaveId: Long,
        saksbehandlerFraApi: SaksbehandlerFraApi,
        utfall: VedtakUtfall,
        begrunnelse: String?,
    ) {
        håndterVedtakBegrunnelse(
            utfall = utfall,
            begrunnelse = begrunnelse,
            oppgaveId = oppgaveId,
            saksbehandlerOid = saksbehandlerFraApi.oid,
        )
    }

    private fun håndterAvslag(
        avslag: no.nav.helse.spesialist.api.graphql.mutation.Avslag?,
        oppgaveId: Long,
        saksbehandlerOid: UUID,
    ) {
        if (avslag != null) {
            if (avslag.handling == Avslagshandling.INVALIDER) {
                vedtakBegrunnelseDao.invaliderVedtakBegrunnelse(oppgaveId = oppgaveId)
            } else {
                vedtakBegrunnelseDao.lagreVedtakBegrunnelse(
                    oppgaveId = oppgaveId,
                    type = avslag.data!!.type.toVedtakBegrunnelseTypeFraDatabase(),
                    begrunnelse = avslag.data!!.begrunnelse,
                    saksbehandlerOid = saksbehandlerOid,
                )
            }
        }
    }

    private fun håndterVedtakBegrunnelse(
        utfall: VedtakUtfall,
        begrunnelse: String?,
        oppgaveId: Long,
        saksbehandlerOid: UUID,
    ) {
        val oppdatertBegrunnelse =
            VedtakBegrunnelseFraDatabase(
                type = utfall.toDatabaseType(),
                tekst = begrunnelse.orEmpty(),
            )
        val eksisterendeBegrunnelse = vedtakBegrunnelseDao.finnVedtakBegrunnelse(oppgaveId = oppgaveId)
        val erEndret = eksisterendeBegrunnelse != oppdatertBegrunnelse
        val erNy = eksisterendeBegrunnelse == null
        if (!erNy && erEndret) {
            vedtakBegrunnelseDao.invaliderVedtakBegrunnelse(oppgaveId = oppgaveId)
        }
        if (erNy || erEndret) {
            vedtakBegrunnelseDao.lagreVedtakBegrunnelse(
                oppgaveId = oppgaveId,
                vedtakBegrunnelse = oppdatertBegrunnelse,
                saksbehandlerOid = saksbehandlerOid,
            )
        }
    }

    override fun hentAnnullering(
        arbeidsgiverFagsystemId: String,
        personFagsystemId: String,
    ): no.nav.helse.spesialist.api.graphql.schema.Annullering? =
        annulleringDao.finnAnnullering(
            arbeidsgiverFagsystemId = arbeidsgiverFagsystemId,
            personFagsystemId = personFagsystemId,
        )

    override fun håndter(
        godkjenning: GodkjenningDto,
        behandlingId: UUID,
        saksbehandlerFraApi: SaksbehandlerFraApi,
    ) {
        val saksbehandler = saksbehandlerFraApi.tilSaksbehandler()
        val fødselsnummer = oppgaveApiDao.finnFødselsnummer(godkjenning.oppgavereferanse)

        if (godkjenning.godkjent) {
            val perioderTilBehandling = generasjonRepository.perioderTilBehandling(godkjenning.oppgavereferanse)
            if (perioderTilBehandling.harAktiveVarsler()) {
                throw ManglerVurderingAvVarsler(godkjenning.oppgavereferanse)
            }
            perioderTilBehandling.godkjennVarsler(
                fødselsnummer,
                behandlingId,
                saksbehandler.ident(),
                this::vurderVarsel,
            )
        } else {
            val periodeTilGodkjenning = generasjonRepository.periodeTilGodkjenning(godkjenning.oppgavereferanse)
            periodeTilGodkjenning.avvisVarsler(fødselsnummer, behandlingId, saksbehandler.ident(), this::vurderVarsel)
        }

        påVentDao.slettPåVent(godkjenning.oppgavereferanse)
        håndterAvslag(
            avslag = godkjenning.avslag,
            oppgaveId = godkjenning.oppgavereferanse,
            saksbehandlerOid = saksbehandler.oid(),
        )
    }

    override fun håndterTotrinnsvurdering(oppgavereferanse: Long) {
        val perioderTilBehandling = generasjonRepository.perioderTilBehandling(oppgavereferanse)
        if (perioderTilBehandling.harAktiveVarsler()) {
            throw ManglerVurderingAvVarsler(oppgavereferanse)
        }
    }

    private fun vurderVarsel(
        fødselsnummer: String,
        behandlingId: UUID,
        vedtaksperiodeId: UUID,
        varselId: UUID,
        varseltittel: String,
        varselkode: String,
        forrigeStatus: Varsel.Varselstatus,
        gjeldendeStatus: Varsel.Varselstatus,
        saksbehandlerIdent: String,
    ) {
        varselRepository.vurderVarselFor(varselId, gjeldendeStatus, saksbehandlerIdent)
        val message =
            JsonMessage.newMessage(
                "varsel_endret",
                mapOf(
                    "fødselsnummer" to fødselsnummer,
                    "vedtaksperiode_id" to vedtaksperiodeId,
                    "behandling_id" to behandlingId,
                    "varsel_id" to varselId,
                    "varseltittel" to varseltittel,
                    "varselkode" to varselkode,
                    "forrige_status" to forrigeStatus.name,
                    "gjeldende_status" to gjeldendeStatus.name,
                ),
            )
        sikkerlogg.info(
            "Publiserer varsel_endret for varsel med {}, {}, {}",
            kv("varselId", varselId),
            kv("varselkode", varselkode),
            kv("status", gjeldendeStatus),
        )
        rapidsConnection.publish(fødselsnummer, message.toJson())
    }

    internal companion object {
        private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")

        internal fun Modellfeil.tilApiversjon(): no.nav.helse.spesialist.api.feilhåndtering.Modellfeil =
            when (this) {
                is no.nav.helse.modell.OppgaveIkkeTildelt -> OppgaveIkkeTildelt(oppgaveId)
                is OppgaveTildeltNoenAndre -> {
                    val (epost, oid, navn) = this.saksbehandler.toDto()
                    no.nav.helse.spesialist.api.feilhåndtering.OppgaveTildeltNoenAndre(
                        TildelingApiDto(navn, epost, oid),
                    )
                }

                is OppgaveAlleredeSendtBeslutter ->
                    no.nav.helse.spesialist.api.feilhåndtering.OppgaveAlleredeSendtBeslutter(
                        oppgaveId,
                    )

                is OppgaveAlleredeSendtIRetur ->
                    no.nav.helse.spesialist.api.feilhåndtering.OppgaveAlleredeSendtIRetur(
                        oppgaveId,
                    )

                is OppgaveKreverVurderingAvToSaksbehandlere ->
                    no.nav.helse.spesialist.api.feilhåndtering.OppgaveKreverVurderingAvToSaksbehandlere(
                        oppgaveId,
                    )

                is ManglerTilgang -> IkkeTilgang(oid, oppgaveId)

                is AlleredeAnnullert ->
                    no.nav.helse.spesialist.api.feilhåndtering
                        .AlleredeAnnullert(handling.toDto().vedtaksperiodeId)

                is FinnerIkkePåVent -> FinnerIkkeLagtPåVent(oppgaveId)
            }
    }

    private fun SaksbehandlerFraApi.tilSaksbehandler() =
        Saksbehandler(epost, oid, navn, ident, TilgangskontrollørForApi(this.grupper, tilgangsgrupper))

    private fun HandlingFraApi.tilModellversjon(): Handling =
        when (this) {
            is ArbeidsforholdOverstyringHandling -> this.tilModellversjon()
            is InntektOgRefusjonOverstyring -> this.tilModellversjon()
            is TidslinjeOverstyring -> this.tilModellversjon()
            is Skjonnsfastsettelse -> this.tilModellversjon()
            is MinimumSykdomsgrad -> this.tilModellversjon()
            is AnnulleringData -> this.tilModellversjon()
            is TildelOppgave -> this.tilModellversjon()
            is AvmeldOppgave -> this.tilModellversjon()
            is OpphevStans -> this.tilModellversjon()
            else -> throw IllegalStateException("Støtter ikke handling ${this::class.simpleName}")
        }

    private fun PaVentRequest.tilModellversjon(): PåVent =
        when (this) {
            is PaVentRequest.LeggPaVent -> this.tilModellversjon()
            is PaVentRequest.FjernPaVent -> this.tilModellversjon()
            is PaVentRequest.FjernPaVentUtenHistorikkinnslag -> this.tilModellversjon()
            is PaVentRequest.EndrePaVent -> this.tilModellversjon()
        }

    private fun ArbeidsforholdOverstyringHandling.tilModellversjon(): OverstyrtArbeidsforhold =
        OverstyrtArbeidsforhold(
            fødselsnummer = fodselsnummer,
            aktørId = aktorId,
            skjæringstidspunkt = skjaringstidspunkt,
            vedtaksperiodeId = vedtaksperiodeId,
            overstyrteArbeidsforhold =
                overstyrteArbeidsforhold.map { overstyrtArbeidsforhold ->
                    Arbeidsforhold(
                        organisasjonsnummer = overstyrtArbeidsforhold.orgnummer,
                        deaktivert = overstyrtArbeidsforhold.deaktivert,
                        begrunnelse = overstyrtArbeidsforhold.begrunnelse,
                        forklaring = overstyrtArbeidsforhold.forklaring,
                        lovhjemmel =
                            overstyrtArbeidsforhold.lovhjemmel?.let {
                                Lovhjemmel(it.paragraf, it.ledd, it.bokstav, it.lovverk, it.lovverksversjon)
                            },
                    )
                },
        )

    private fun Skjonnsfastsettelse.tilModellversjon(): SkjønnsfastsattSykepengegrunnlag =
        SkjønnsfastsattSykepengegrunnlag(
            aktørId = aktorId,
            fødselsnummer = fodselsnummer,
            skjæringstidspunkt = skjaringstidspunkt,
            vedtaksperiodeId = vedtaksperiodeId,
            arbeidsgivere =
                arbeidsgivere.map { ag ->
                    SkjønnsfastsattArbeidsgiver(
                        organisasjonsnummer = ag.organisasjonsnummer,
                        årlig = ag.arlig,
                        fraÅrlig = ag.fraArlig,
                        årsak = ag.arsak,
                        type =
                            when (ag.type) {
                                OMREGNET_ARSINNTEKT -> SkjønnsfastsattArbeidsgiver.Skjønnsfastsettingstype.OMREGNET_ÅRSINNTEKT
                                RAPPORTERT_ARSINNTEKT -> SkjønnsfastsattArbeidsgiver.Skjønnsfastsettingstype.RAPPORTERT_ÅRSINNTEKT
                                ANNET -> SkjønnsfastsattArbeidsgiver.Skjønnsfastsettingstype.ANNET
                            },
                        begrunnelseMal = ag.begrunnelseMal,
                        begrunnelseFritekst = ag.begrunnelseFritekst,
                        begrunnelseKonklusjon = ag.begrunnelseKonklusjon,
                        lovhjemmel =
                            ag.lovhjemmel?.let {
                                Lovhjemmel(it.paragraf, it.ledd, it.bokstav, it.lovverk, it.lovverksversjon)
                            },
                        initierendeVedtaksperiodeId = ag.initierendeVedtaksperiodeId,
                    )
                },
        )

    private fun MinimumSykdomsgrad.tilModellversjon(): no.nav.helse.modell.saksbehandler.handlinger.MinimumSykdomsgrad =
        no.nav.helse.modell.saksbehandler.handlinger.MinimumSykdomsgrad(
            aktørId = aktorId,
            fødselsnummer = fodselsnummer,
            perioderVurdertOk =
                perioderVurdertOk.map {
                    MinimumSykdomsgradPeriode(
                        fom = it.fom,
                        tom = it.tom,
                    )
                },
            perioderVurdertIkkeOk =
                perioderVurdertIkkeOk.map {
                    MinimumSykdomsgradPeriode(
                        fom = it.fom,
                        tom = it.tom,
                    )
                },
            begrunnelse = begrunnelse,
            arbeidsgivere =
                arbeidsgivere.map {
                    MinimumSykdomsgradArbeidsgiver(
                        organisasjonsnummer = it.organisasjonsnummer,
                        berørtVedtaksperiodeId = it.berortVedtaksperiodeId,
                    )
                },
            initierendeVedtaksperiodeId = initierendeVedtaksperiodeId,
        )

    private fun InntektOgRefusjonOverstyring.tilModellversjon(): OverstyrtInntektOgRefusjon =
        OverstyrtInntektOgRefusjon(
            aktørId = aktorId,
            fødselsnummer = fodselsnummer,
            skjæringstidspunkt = skjaringstidspunkt,
            vedtaksperiodeId = vedtaksperiodeId,
            arbeidsgivere =
                arbeidsgivere.map { overstyrArbeidsgiver ->
                    OverstyrtArbeidsgiver(
                        overstyrArbeidsgiver.organisasjonsnummer,
                        overstyrArbeidsgiver.manedligInntekt,
                        overstyrArbeidsgiver.fraManedligInntekt,
                        overstyrArbeidsgiver.refusjonsopplysninger?.map {
                            Refusjonselement(it.fom, it.tom, it.belop)
                        },
                        overstyrArbeidsgiver.fraRefusjonsopplysninger?.map {
                            Refusjonselement(it.fom, it.tom, it.belop)
                        },
                        begrunnelse = overstyrArbeidsgiver.begrunnelse,
                        forklaring = overstyrArbeidsgiver.forklaring,
                        lovhjemmel =
                            overstyrArbeidsgiver.lovhjemmel?.let {
                                Lovhjemmel(it.paragraf, it.ledd, it.bokstav, it.lovverk, it.lovverksversjon)
                            },
                        fom = overstyrArbeidsgiver.fom,
                        tom = overstyrArbeidsgiver.tom,
                    )
                },
        )

    private fun TidslinjeOverstyring.tilModellversjon(): OverstyrtTidslinje =
        OverstyrtTidslinje(
            vedtaksperiodeId = vedtaksperiodeId,
            aktørId = aktorId,
            fødselsnummer = fodselsnummer,
            organisasjonsnummer = organisasjonsnummer,
            dager =
                dager.map {
                    OverstyrtTidslinjedag(
                        dato = it.dato,
                        type = it.type,
                        fraType = it.fraType,
                        grad = it.grad,
                        fraGrad = it.fraGrad,
                        lovhjemmel =
                            it.lovhjemmel?.let { lovhjemmel ->
                                Lovhjemmel(
                                    paragraf = lovhjemmel.paragraf,
                                    ledd = lovhjemmel.ledd,
                                    bokstav = lovhjemmel.bokstav,
                                    lovverk = lovhjemmel.lovverk,
                                    lovverksversjon = lovhjemmel.lovverksversjon,
                                )
                            },
                    )
                },
            begrunnelse = begrunnelse,
        )

    private fun AnnulleringData.tilModellversjon(): Annullering =
        Annullering(
            aktørId = this.aktorId,
            fødselsnummer = this.fodselsnummer,
            organisasjonsnummer = this.organisasjonsnummer,
            vedtaksperiodeId = this.vedtaksperiodeId,
            utbetalingId = this.utbetalingId,
            arbeidsgiverFagsystemId = this.arbeidsgiverFagsystemId,
            personFagsystemId = this.personFagsystemId,
            begrunnelser = this.begrunnelser,
            arsaker = this.arsaker.map { arsak -> AnnulleringArsak(key = arsak._key, arsak = arsak.arsak) },
            kommentar = this.kommentar,
        )

    private fun PaVentRequest.LeggPaVent.tilModellversjon(): LeggPåVent =
        LeggPåVent(
            fødselsnummer = oppgaveService.fødselsnummer(oppgaveId),
            oppgaveId = oppgaveId,
            behandlingId = oppgaveService.spleisBehandlingId(oppgaveId),
            frist = frist,
            skalTildeles = skalTildeles,
            notatTekst = notatTekst,
            årsaker = årsaker.map { årsak -> PåVentÅrsak(key = årsak._key, årsak = årsak.arsak) },
        )

    private fun PaVentRequest.EndrePaVent.tilModellversjon(): EndrePåVent {
        return EndrePåVent(
            fødselsnummer = oppgaveService.fødselsnummer(oppgaveId),
            oppgaveId = oppgaveId,
            behandlingId = oppgaveService.spleisBehandlingId(oppgaveId),
            frist = frist,
            skalTildeles = skalTildeles,
            notatTekst = notatTekst,
            årsaker = årsaker.map { årsak -> PåVentÅrsak(key = årsak._key, årsak = årsak.arsak) },
        )
    }

    private fun PaVentRequest.FjernPaVent.tilModellversjon(): FjernPåVent = FjernPåVent(oppgaveId)

    private fun PaVentRequest.FjernPaVentUtenHistorikkinnslag.tilModellversjon(): FjernPåVentUtenHistorikkinnslag =
        FjernPåVentUtenHistorikkinnslag(oppgaveId)

    private fun TildelOppgave.tilModellversjon(): no.nav.helse.modell.saksbehandler.handlinger.TildelOppgave =
        no.nav.helse.modell.saksbehandler.handlinger
            .TildelOppgave(this.oppgaveId)

    private fun AvmeldOppgave.tilModellversjon(): no.nav.helse.modell.saksbehandler.handlinger.AvmeldOppgave =
        no.nav.helse.modell.saksbehandler.handlinger
            .AvmeldOppgave(this.oppgaveId)

    private fun OpphevStans.tilModellversjon(): no.nav.helse.modell.saksbehandler.handlinger.OpphevStans =
        no.nav.helse.modell.saksbehandler.handlinger
            .OpphevStans(this.fødselsnummer, this.begrunnelse)

    private fun Avslagstype.toVedtakBegrunnelseTypeFraDatabase() =
        when (this) {
            Avslagstype.AVSLAG -> VedtakBegrunnelseTypeFraDatabase.AVSLAG
            Avslagstype.DELVIS_AVSLAG -> VedtakBegrunnelseTypeFraDatabase.DELVIS_INNVILGELSE
        }

    private fun VedtakUtfall.toDatabaseType() =
        when (this) {
            VedtakUtfall.AVSLAG -> VedtakBegrunnelseTypeFraDatabase.AVSLAG
            VedtakUtfall.DELVIS_INNVILGELSE -> VedtakBegrunnelseTypeFraDatabase.DELVIS_INNVILGELSE
            VedtakUtfall.INNVILGELSE -> VedtakBegrunnelseTypeFraDatabase.INNVILGELSE
        }
}
