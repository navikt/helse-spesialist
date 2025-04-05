package no.nav.helse.mediator

import net.logstash.logback.argument.StructuredArguments
import no.nav.helse.MeldingPubliserer
import no.nav.helse.bootstrap.EnvironmentToggles
import no.nav.helse.db.AnnulleringRepository
import no.nav.helse.db.Daos
import no.nav.helse.db.OpptegnelseDao
import no.nav.helse.db.SessionContext
import no.nav.helse.db.SessionFactory
import no.nav.helse.db.VedtakBegrunnelseFraDatabase
import no.nav.helse.db.VedtakBegrunnelseTypeFraDatabase
import no.nav.helse.db.api.VarselDbDto
import no.nav.helse.db.api.VedtaksperiodeDbDto.Companion.avvisVarsler
import no.nav.helse.db.api.VedtaksperiodeDbDto.Companion.godkjennVarsler
import no.nav.helse.db.api.VedtaksperiodeDbDto.Companion.harAktiveVarsler
import no.nav.helse.mediator.oppgave.ApiOppgaveService
import no.nav.helse.mediator.oppgave.OppgaveService
import no.nav.helse.mediator.overstyring.Saksbehandlingsmelder
import no.nav.helse.mediator.påvent.PåVentRepository
import no.nav.helse.modell.AlleredeAnnullert
import no.nav.helse.modell.FinnerIkkePåVent
import no.nav.helse.modell.ManglerTilgang
import no.nav.helse.modell.Modellfeil
import no.nav.helse.modell.OppgaveAlleredeSendtBeslutter
import no.nav.helse.modell.OppgaveAlleredeSendtIRetur
import no.nav.helse.modell.OppgaveKreverVurderingAvToSaksbehandlere
import no.nav.helse.modell.OppgaveTildeltNoenAndre
import no.nav.helse.modell.melding.VarselEndret
import no.nav.helse.modell.oppgave.Egenskap
import no.nav.helse.modell.periodehistorikk.Historikkinnslag
import no.nav.helse.modell.saksbehandler.SaksbehandlerDto
import no.nav.helse.modell.saksbehandler.Tilgangskontroll
import no.nav.helse.modell.saksbehandler.handlinger.Annullering
import no.nav.helse.modell.saksbehandler.handlinger.AnnulleringArsak
import no.nav.helse.modell.saksbehandler.handlinger.Arbeidsforhold
import no.nav.helse.modell.saksbehandler.handlinger.EndrePåVent
import no.nav.helse.modell.saksbehandler.handlinger.FjernPåVent
import no.nav.helse.modell.saksbehandler.handlinger.FjernPåVentUtenHistorikkinnslag
import no.nav.helse.modell.saksbehandler.handlinger.Handling
import no.nav.helse.modell.saksbehandler.handlinger.LeggPåVent
import no.nav.helse.modell.saksbehandler.handlinger.MinimumSykdomsgrad
import no.nav.helse.modell.saksbehandler.handlinger.MinimumSykdomsgradArbeidsgiver
import no.nav.helse.modell.saksbehandler.handlinger.MinimumSykdomsgradPeriode
import no.nav.helse.modell.saksbehandler.handlinger.Oppgavehandling
import no.nav.helse.modell.saksbehandler.handlinger.OpphevStans
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
import no.nav.helse.modell.stoppautomatiskbehandling.StansAutomatiskBehandlinghåndtererImpl
import no.nav.helse.modell.totrinnsvurdering.Totrinnsvurdering
import no.nav.helse.modell.totrinnsvurdering.TotrinnsvurderingTilstand.AVVENTER_BESLUTTER
import no.nav.helse.modell.vedtak.Utfall
import no.nav.helse.modell.vilkårsprøving.Lovhjemmel
import no.nav.helse.spesialist.api.SendIReturResult
import no.nav.helse.spesialist.api.SendTilGodkjenningResult
import no.nav.helse.spesialist.api.bootstrap.Tilgangsgrupper
import no.nav.helse.spesialist.api.feilhåndtering.FinnerIkkeLagtPåVent
import no.nav.helse.spesialist.api.feilhåndtering.IkkeTilgang
import no.nav.helse.spesialist.api.feilhåndtering.ManglerVurderingAvVarsler
import no.nav.helse.spesialist.api.feilhåndtering.OppgaveIkkeTildelt
import no.nav.helse.spesialist.api.graphql.mutation.VedtakMutationHandler.VedtakResultat
import no.nav.helse.spesialist.api.graphql.schema.ApiAnnulleringData
import no.nav.helse.spesialist.api.graphql.schema.ApiArbeidsforholdOverstyringHandling
import no.nav.helse.spesialist.api.graphql.schema.ApiInntektOgRefusjonOverstyring
import no.nav.helse.spesialist.api.graphql.schema.ApiMinimumSykdomsgrad
import no.nav.helse.spesialist.api.graphql.schema.ApiOpptegnelse
import no.nav.helse.spesialist.api.graphql.schema.ApiOpptegnelsetype
import no.nav.helse.spesialist.api.graphql.schema.ApiPaVentRequest
import no.nav.helse.spesialist.api.graphql.schema.ApiSkjonnsfastsettelse
import no.nav.helse.spesialist.api.graphql.schema.ApiSkjonnsfastsettelse.ApiSkjonnsfastsettelseArbeidsgiver.ApiSkjonnsfastsettelseType.ANNET
import no.nav.helse.spesialist.api.graphql.schema.ApiSkjonnsfastsettelse.ApiSkjonnsfastsettelseArbeidsgiver.ApiSkjonnsfastsettelseType.OMREGNET_ARSINNTEKT
import no.nav.helse.spesialist.api.graphql.schema.ApiSkjonnsfastsettelse.ApiSkjonnsfastsettelseArbeidsgiver.ApiSkjonnsfastsettelseType.RAPPORTERT_ARSINNTEKT
import no.nav.helse.spesialist.api.graphql.schema.ApiTidslinjeOverstyring
import no.nav.helse.spesialist.api.saksbehandler.SaksbehandlerFraApi
import no.nav.helse.spesialist.api.saksbehandler.handlinger.ApiOpphevStans
import no.nav.helse.spesialist.api.saksbehandler.handlinger.AvmeldOppgave
import no.nav.helse.spesialist.api.saksbehandler.handlinger.HandlingFraApi
import no.nav.helse.spesialist.api.saksbehandler.handlinger.TildelOppgave
import no.nav.helse.spesialist.api.tildeling.TildelingApiDto
import no.nav.helse.spesialist.application.TotrinnsvurderingRepository
import no.nav.helse.spesialist.application.logg.sikkerlogg
import no.nav.helse.spesialist.domain.Saksbehandler
import no.nav.helse.spesialist.domain.SaksbehandlerOid
import no.nav.helse.spesialist.domain.SpleisBehandlingId
import no.nav.helse.spesialist.domain.legacy.LegacySaksbehandler
import no.nav.helse.spesialist.domain.legacy.LegacySaksbehandler.Companion.gjenopprett
import no.nav.helse.spesialist.domain.legacy.LegacySaksbehandler.Companion.toDto
import no.nav.helse.tell
import org.slf4j.LoggerFactory
import java.util.UUID
import no.nav.helse.spesialist.api.feilhåndtering.Modellfeil as ApiModellfeil

class SaksbehandlerMediator(
    daos: Daos,
    private val versjonAvKode: String,
    private val meldingPubliserer: MeldingPubliserer,
    private val oppgaveService: OppgaveService,
    private val apiOppgaveService: ApiOppgaveService,
    private val tilgangsgrupper: Tilgangsgrupper,
    private val stansAutomatiskBehandlinghåndterer: StansAutomatiskBehandlinghåndtererImpl,
    private val annulleringRepository: AnnulleringRepository,
    private val environmentToggles: EnvironmentToggles,
    private val sessionFactory: SessionFactory,
    private val tilgangskontroll: Tilgangskontroll,
) {
    private val behandlingRepository = daos.behandlingApiRepository
    private val varselRepository = daos.varselApiRepository
    private val oppgaveApiDao = daos.oppgaveApiDao
    private val opptegnelseRepository = daos.opptegnelseDao
    private val abonnementDao = daos.abonnementApiDao
    private val påVentDao = daos.påVentDao
    private val periodehistorikkDao = daos.periodehistorikkDao
    private val vedtakBegrunnelseDao = daos.vedtakBegrunnelseDao
    private val dialogDao = daos.dialogDao

    fun håndter(
        handlingFraApi: HandlingFraApi,
        saksbehandlerFraApi: SaksbehandlerFraApi,
    ) {
        val saksbehandler = saksbehandlerFraApi.tilSaksbehandler()
        val modellhandling = handlingFraApi.tilModellversjon(saksbehandler.id())
        sessionFactory.transactionalSessionScope { it.saksbehandlerRepository.lagre(saksbehandler) }
        tell(modellhandling)
        val legacySaksbehandler = saksbehandler.tilLegacySaksbehandler(saksbehandlerFraApi.grupper)
        legacySaksbehandler.register(Saksbehandlingsmelder(meldingPubliserer))
        legacySaksbehandler.register(Subsumsjonsmelder(versjonAvKode, meldingPubliserer))
        val handlingId = UUID.randomUUID()

        withMDC(
            mapOf(
                "saksbehandlerOid" to saksbehandler.id().value.toString(),
                "handlingId" to handlingId.toString(),
            ),
        ) {
            sikkerlogg.info("Utfører handling ${modellhandling.loggnavn()} på vegne av saksbehandler $saksbehandler")
            when (modellhandling) {
                is Overstyring ->
                    overstyringUnitOfWork(
                        overstyring = modellhandling,
                        saksbehandler = saksbehandlerFraApi.tilSaksbehandler(),
                        sessionFactory = sessionFactory,
                    ) {
                        modellhandling.utførAv(legacySaksbehandler)
                    }

                is Oppgavehandling -> håndter(modellhandling, legacySaksbehandler)
                is PåVent -> error("dette burde ikke skje")
                is OpphevStans -> håndter(modellhandling, legacySaksbehandler)
                is Personhandling -> håndter(modellhandling, legacySaksbehandler)
                is Annullering -> håndter(modellhandling, legacySaksbehandler)
                else -> modellhandling.utførAv(legacySaksbehandler)
            }
            sikkerlogg.info("Handling ${modellhandling.loggnavn()} utført")
        }
    }

    fun vedtak(
        saksbehandlerFraApi: SaksbehandlerFraApi,
        oppgavereferanse: Long,
        begrunnelse: String?,
    ): VedtakResultat =
        sessionFactory.transactionalSessionScope { sessionContext ->
            val legacySaksbehandler =
                saksbehandlerFraApi.tilSaksbehandler().tilLegacySaksbehandler(saksbehandlerFraApi.grupper)
            val spleisBehandlingId = apiOppgaveService.spleisBehandlingId(oppgavereferanse)
            val fødselsnummer = oppgaveApiDao.finnFødselsnummer(oppgavereferanse)
            if (!apiOppgaveService.venterPåSaksbehandler(oppgavereferanse)) {
                VedtakResultat.Feil.IkkeÅpenOppgave()
            } else {
                håndterTotrinnsvurderingBeslutning(
                    fødselsnummer = fødselsnummer,
                    legacySaksbehandler = legacySaksbehandler,
                    totrinnsvurderingRepository = sessionContext.totrinnsvurderingRepository,
                ) ?: håndterGodkjenning(oppgavereferanse, fødselsnummer, spleisBehandlingId, legacySaksbehandler).also {
                    håndterVedtakBegrunnelse(
                        utfall = hentUtfallFraBehandling(spleisBehandlingId, sessionContext),
                        begrunnelse = begrunnelse,
                        oppgaveId = oppgavereferanse,
                        saksbehandlerOid = legacySaksbehandler.oid(),
                    )
                }
            }
        }

    private fun hentUtfallFraBehandling(
        spleisBehandlingId: UUID,
        sessionContext: SessionContext,
    ): Utfall {
        val behandling =
            sessionContext.behandlingRepository.finn(SpleisBehandlingId(spleisBehandlingId))
                ?: error("Fant ikke behandling for SpleisBehandlingId $spleisBehandlingId")
        return behandling.utfall()
    }

    fun infotrygdVedtak(
        saksbehandlerFraApi: SaksbehandlerFraApi,
        oppgavereferanse: Long,
    ): VedtakResultat =
        sessionFactory.transactionalSessionScope { sessionContext ->
            val legacySaksbehandler =
                saksbehandlerFraApi.tilSaksbehandler().tilLegacySaksbehandler(saksbehandlerFraApi.grupper)
            val spleisBehandlingId = apiOppgaveService.spleisBehandlingId(oppgavereferanse)
            val fødselsnummer = oppgaveApiDao.finnFødselsnummer(oppgavereferanse)
            if (!apiOppgaveService.venterPåSaksbehandler(oppgavereferanse)) {
                VedtakResultat.Feil.IkkeÅpenOppgave()
            } else {
                håndterTotrinnsvurderingBeslutning(
                    fødselsnummer = fødselsnummer,
                    legacySaksbehandler = legacySaksbehandler,
                    totrinnsvurderingRepository = sessionContext.totrinnsvurderingRepository,
                ) ?: håndterAvvisning(oppgavereferanse, fødselsnummer, spleisBehandlingId, legacySaksbehandler)
            }
        }

    private fun håndterAvvisning(
        oppgavereferanse: Long,
        fødselsnummer: String,
        spleisBehandlingId: UUID,
        legacySaksbehandler: LegacySaksbehandler,
    ): VedtakResultat.Ok {
        val periodeTilGodkjenning = behandlingRepository.periodeTilGodkjenning(oppgavereferanse)
        periodeTilGodkjenning.avvisVarsler(
            fødselsnummer = fødselsnummer,
            behandlingId = spleisBehandlingId,
            ident = legacySaksbehandler.ident(),
            godkjenner = this::vurderVarsel,
        )

        påVentDao.slettPåVent(oppgavereferanse)
        return VedtakResultat.Ok(spleisBehandlingId)
    }

    private fun håndterGodkjenning(
        oppgavereferanse: Long,
        fødselsnummer: String,
        spleisBehandlingId: UUID,
        legacySaksbehandler: LegacySaksbehandler,
    ): VedtakResultat {
        val perioderTilBehandling = behandlingRepository.perioderTilBehandling(oppgavereferanse)
        return when {
            perioderTilBehandling.harAktiveVarsler() -> {
                VedtakResultat.Feil.HarAktiveVarsler(
                    oppgavereferanse,
                )
            }
            else -> {
                perioderTilBehandling.godkjennVarsler(
                    fødselsnummer = fødselsnummer,
                    behandlingId = spleisBehandlingId,
                    ident = legacySaksbehandler.ident(),
                    godkjenner = this::vurderVarsel,
                )

                påVentDao.slettPåVent(oppgavereferanse)
                VedtakResultat.Ok(spleisBehandlingId)
            }
        }
    }

    private fun håndterTotrinnsvurderingBeslutning(
        fødselsnummer: String,
        legacySaksbehandler: LegacySaksbehandler,
        totrinnsvurderingRepository: TotrinnsvurderingRepository,
    ): VedtakResultat.Feil.BeslutterFeil? {
        val totrinnsvurdering = totrinnsvurderingRepository.finn(fødselsnummer)
        val feil =
            if (totrinnsvurdering?.tilstand == AVVENTER_BESLUTTER) {
                if (!legacySaksbehandler.harTilgangTil(listOf(Egenskap.BESLUTTER)) && !environmentToggles.kanGodkjenneUtenBesluttertilgang) {
                    VedtakResultat.Feil.BeslutterFeil.TrengerBeslutterRolle()
                } else if (totrinnsvurdering.saksbehandler?.value == legacySaksbehandler.oid && !environmentToggles.kanBeslutteEgneSaker) {
                    VedtakResultat.Feil.BeslutterFeil.KanIkkeBeslutteEgenOppgave()
                } else {
                    totrinnsvurdering.settBeslutter(SaksbehandlerOid(legacySaksbehandler.oid))
                    totrinnsvurderingRepository.lagre(totrinnsvurdering)
                    null
                }
            } else {
                null
            }
        return feil
    }

    fun påVent(
        handling: ApiPaVentRequest,
        saksbehandlerFraApi: SaksbehandlerFraApi,
    ) {
        val saksbehandler = saksbehandlerFraApi.tilSaksbehandler()
        val modellhandling = handling.tilModellversjon()
        sessionFactory.transactionalSessionScope { it.saksbehandlerRepository.lagre(saksbehandler) }
        tell(modellhandling)
        val handlingId = UUID.randomUUID()

        withMDC(
            mapOf(
                "saksbehandlerOid" to saksbehandler.id().value.toString(),
                "handlingId" to handlingId.toString(),
            ),
        ) {
            sikkerlogg.info("Utfører handling ${modellhandling.loggnavn()} på vegne av saksbehandler $saksbehandler")
            val legacySaksbehandler = saksbehandler.tilLegacySaksbehandler(saksbehandlerFraApi.grupper)
            when (modellhandling) {
                is LeggPåVent -> leggPåVent(modellhandling, legacySaksbehandler)
                is FjernPåVent -> fjernFraPåVent(modellhandling, legacySaksbehandler)
                is FjernPåVentUtenHistorikkinnslag ->
                    fjernFraPåVentUtenHistorikkinnslag(
                        modellhandling,
                    )

                is EndrePåVent -> endrePåVent(modellhandling, legacySaksbehandler)
            }
            sikkerlogg.info(
                "Handling ${modellhandling.loggnavn()} utført på oppgave ${modellhandling.oppgaveId} på vegne av saksbehandler $saksbehandler",
            )
        }
    }

    private fun håndter(
        handling: Annullering,
        legacySaksbehandler: LegacySaksbehandler,
    ) {
        try {
            if (annulleringRepository.finnAnnullering(handling.toDto()) != null) throw AlleredeAnnullert(handling)
            annulleringRepository.lagreAnnullering(handling.toDto(), legacySaksbehandler)
            handling.utførAv(legacySaksbehandler)
        } catch (e: Modellfeil) {
            throw e.tilApiversjon()
        }
    }

    private fun håndter(
        handling: Oppgavehandling,
        legacySaksbehandler: LegacySaksbehandler,
    ) {
        try {
            oppgaveService.avbrytOppgave(handling, legacySaksbehandler)
        } catch (e: Modellfeil) {
            throw e.tilApiversjon()
        }
    }

    private fun håndter(
        handling: OpphevStans,
        legacySaksbehandler: LegacySaksbehandler,
    ) = try {
        stansAutomatiskBehandlinghåndterer.håndter(handling, legacySaksbehandler)
        handling.utførAv(legacySaksbehandler)
    } catch (e: Modellfeil) {
        throw e.tilApiversjon()
    }

    private fun håndter(
        handling: Personhandling,
        legacySaksbehandler: LegacySaksbehandler,
    ) = try {
        handling.utførAv(legacySaksbehandler)
    } catch (e: Modellfeil) {
        throw e.tilApiversjon()
    }

    private fun leggPåVent(
        handling: LeggPåVent,
        legacySaksbehandler: LegacySaksbehandler,
    ) {
        try {
            val dialogRef = dialogDao.lagre()
            val innslag =
                Historikkinnslag.lagtPåVentInnslag(
                    notattekst = handling.notatTekst,
                    saksbehandler = legacySaksbehandler.toDto(),
                    årsaker = handling.årsaker,
                    frist = handling.frist,
                    dialogRef = dialogRef,
                )
            periodehistorikkDao.lagreMedOppgaveId(innslag, handling.oppgaveId)
            oppgaveService.leggPåVent(handling, legacySaksbehandler)
            PåVentRepository(påVentDao).leggPåVent(legacySaksbehandler.oid(), handling, dialogRef)
            legacySaksbehandler.register(Saksbehandlingsmelder(meldingPubliserer))
            handling.utførAv(legacySaksbehandler)
        } catch (e: Modellfeil) {
            throw e.tilApiversjon()
        }
    }

    private fun endrePåVent(
        handling: EndrePåVent,
        legacySaksbehandler: LegacySaksbehandler,
    ) {
        if (!påVentDao.erPåVent(handling.oppgaveId)) throw FinnerIkkeLagtPåVent(handling.oppgaveId)

        val dialogRef = dialogDao.lagre()
        val innslag =
            Historikkinnslag.endrePåVentInnslag(
                notattekst = handling.notatTekst,
                saksbehandler = legacySaksbehandler.toDto(),
                årsaker = handling.årsaker,
                frist = handling.frist,
                dialogRef = dialogRef,
            )
        periodehistorikkDao.lagreMedOppgaveId(innslag, handling.oppgaveId)
        oppgaveService.endrePåVent(handling, legacySaksbehandler)
        PåVentRepository(påVentDao).endrePåVent(legacySaksbehandler.oid(), handling, dialogRef)

        legacySaksbehandler.register(Saksbehandlingsmelder(meldingPubliserer))
        handling.utførAv(legacySaksbehandler)
    }

    private fun fjernFraPåVent(
        handling: FjernPåVent,
        legacySaksbehandler: LegacySaksbehandler,
    ) {
        if (!påVentDao.erPåVent(handling.oppgaveId)) {
            sikkerlogg.info("Oppgave ${handling.oppgaveId} er ikke på vent")
            return
        }
        try {
            val innslag = Historikkinnslag.fjernetFraPåVentInnslag(legacySaksbehandler.toDto())
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

    fun opprettAbonnement(
        saksbehandlerFraApi: SaksbehandlerFraApi,
        personidentifikator: String,
    ) {
        val saksbehandler = saksbehandlerFraApi.tilSaksbehandler()
        sessionFactory.transactionalSessionScope { it.saksbehandlerRepository.lagre(saksbehandler) }
        abonnementDao.opprettAbonnement(saksbehandler.id().value, personidentifikator)
    }

    fun hentAbonnerteOpptegnelser(
        saksbehandlerFraApi: SaksbehandlerFraApi,
        sisteSekvensId: Int,
    ): List<ApiOpptegnelse> {
        val saksbehandler = saksbehandlerFraApi.tilSaksbehandler()
        sessionFactory.transactionalSessionScope { it.saksbehandlerRepository.lagre(saksbehandler) }
        abonnementDao.registrerSistekvensnummer(saksbehandler.id().value, sisteSekvensId)
        return opptegnelseRepository.finnOpptegnelser(saksbehandler.id().value).toApiOpptegnelser()
    }

    fun hentAbonnerteOpptegnelser(saksbehandlerFraApi: SaksbehandlerFraApi): List<ApiOpptegnelse> =
        sessionFactory.transactionalSessionScope { session ->
            val saksbehandler = saksbehandlerFraApi.tilSaksbehandler()
            session.saksbehandlerRepository.lagre(saksbehandler)
            session.opptegnelseDao.finnOpptegnelser(saksbehandler.id().value)
        }.toApiOpptegnelser()

    private fun List<OpptegnelseDao.Opptegnelse>.toApiOpptegnelser() =
        map { opptegnelse ->
            ApiOpptegnelse(
                aktorId = opptegnelse.aktorId,
                sekvensnummer = opptegnelse.sekvensnummer,
                type =
                    opptegnelse.type.let { type ->
                        when (type) {
                            OpptegnelseDao.Opptegnelse.Type.UTBETALING_ANNULLERING_FEILET -> ApiOpptegnelsetype.UTBETALING_ANNULLERING_FEILET
                            OpptegnelseDao.Opptegnelse.Type.UTBETALING_ANNULLERING_OK -> ApiOpptegnelsetype.UTBETALING_ANNULLERING_OK
                            OpptegnelseDao.Opptegnelse.Type.FERDIGBEHANDLET_GODKJENNINGSBEHOV -> ApiOpptegnelsetype.FERDIGBEHANDLET_GODKJENNINGSBEHOV
                            OpptegnelseDao.Opptegnelse.Type.NY_SAKSBEHANDLEROPPGAVE -> ApiOpptegnelsetype.NY_SAKSBEHANDLEROPPGAVE
                            OpptegnelseDao.Opptegnelse.Type.REVURDERING_AVVIST -> ApiOpptegnelsetype.REVURDERING_AVVIST
                            OpptegnelseDao.Opptegnelse.Type.REVURDERING_FERDIGBEHANDLET -> ApiOpptegnelsetype.REVURDERING_FERDIGBEHANDLET
                            OpptegnelseDao.Opptegnelse.Type.PERSONDATA_OPPDATERT -> ApiOpptegnelsetype.PERSONDATA_OPPDATERT
                            OpptegnelseDao.Opptegnelse.Type.PERSON_KLAR_TIL_BEHANDLING -> ApiOpptegnelsetype.PERSON_KLAR_TIL_BEHANDLING
                        }
                    },
                payload = opptegnelse.payload,
            )
        }

    private fun håndterVedtakBegrunnelse(
        utfall: Utfall,
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

    fun hentAnnullering(
        arbeidsgiverFagsystemId: String,
        personFagsystemId: String,
    ): no.nav.helse.modell.Annullering? =
        annulleringRepository.finnAnnullering(
            arbeidsgiverFagsystemId = arbeidsgiverFagsystemId,
            personFagsystemId = personFagsystemId,
        )

    fun sendIRetur(
        oppgavereferanse: Long,
        besluttendeSaksbehandler: SaksbehandlerFraApi,
        notatTekst: String,
    ): SendIReturResult {
        sikkerlogg.info(
            "Oppgave med {} sendes i retur av beslutter med {}",
            StructuredArguments.kv("oppgaveId", oppgavereferanse),
            StructuredArguments.kv("oid", besluttendeSaksbehandler.oid),
        )

        try {
            sessionFactory.transactionalSessionScope { session ->
                val fødselsnummer = oppgaveService.finnFødselsnummer(oppgavereferanse)
                val totrinnsvurdering = session.totrinnsvurderingRepository.finn(fødselsnummer)
                checkNotNull(totrinnsvurdering) {
                    "Forventer at det eksisterer en aktiv totrinnsvurdering når oppgave sendes i retur"
                }
                val opprinneligSaksbehandler =
                    checkNotNull(
                        totrinnsvurdering.saksbehandler?.let(session.saksbehandlerRepository::finn)
                            ?.let(this::tilLegacySaksbehandler),
                    ) {
                        "Opprinnelig saksbehandler kan ikke være null ved retur av beslutteroppgave"
                    }

                apiOppgaveService.sendIRetur(oppgavereferanse, opprinneligSaksbehandler)
                totrinnsvurdering.sendIRetur(oppgavereferanse, SaksbehandlerOid(besluttendeSaksbehandler.oid))
                session.totrinnsvurderingRepository.lagre(totrinnsvurdering)
            }
        } catch (modellfeil: Modellfeil) {
            return SendIReturResult.Feil.KunneIkkeSendeIRetur(modellfeil.tilApiversjon())
        }

        try {
            påVent(
                ApiPaVentRequest.ApiFjernPaVentUtenHistorikkinnslag(oppgavereferanse),
                besluttendeSaksbehandler,
            )
        } catch (modellfeil: ApiModellfeil) {
            return SendIReturResult.Feil.KunneIkkeLeggePåVent(modellfeil)
        }

        try {
            val dialogRef = dialogDao.lagre()
            val innslag =
                Historikkinnslag.totrinnsvurderingRetur(
                    notattekst = notatTekst,
                    saksbehandler = besluttendeSaksbehandler.toDto(),
                    dialogRef = dialogRef,
                )
            periodehistorikkDao.lagreMedOppgaveId(innslag, oppgavereferanse)
        } catch (e: Exception) {
            return SendIReturResult.Feil.KunneIkkeOppretteHistorikkinnslag(e)
        }

        log.info("OppgaveId $oppgavereferanse sendt i retur")

        return SendIReturResult.Ok
    }

    fun håndterTotrinnsvurdering(
        oppgavereferanse: Long,
        saksbehandlerFraApi: SaksbehandlerFraApi,
        begrunnelse: String?,
    ): SendTilGodkjenningResult =
        sessionFactory.transactionalSessionScope { sessionContext ->
            try {
                val perioderTilBehandling = behandlingRepository.perioderTilBehandling(oppgavereferanse)
                if (perioderTilBehandling.harAktiveVarsler()) {
                    return@transactionalSessionScope SendTilGodkjenningResult.Feil.ManglerVurderingAvVarsler(
                        ManglerVurderingAvVarsler(
                            oppgavereferanse,
                        ),
                    )
                }
            } catch (e: Exception) {
                return@transactionalSessionScope SendTilGodkjenningResult.Feil.KunneIkkeFinnePerioderTilBehandling(e)
            }

            val spleisBehandlingId = apiOppgaveService.spleisBehandlingId(oppgavereferanse)
            try {
                håndterVedtakBegrunnelse(
                    utfall = hentUtfallFraBehandling(spleisBehandlingId, sessionContext),
                    begrunnelse = begrunnelse,
                    oppgaveId = oppgavereferanse,
                    saksbehandlerOid = saksbehandlerFraApi.oid,
                )
            } catch (e: Exception) {
                return@transactionalSessionScope SendTilGodkjenningResult.Feil.KunneIkkeHåndtereBegrunnelse(e)
            }

            try {
                sessionFactory.transactionalSessionScope { session ->
                    val fødselsnummer = oppgaveService.finnFødselsnummer(oppgavereferanse)
                    val totrinnsvurdering = session.totrinnsvurderingRepository.finn(fødselsnummer)
                    checkNotNull(totrinnsvurdering) {
                        "Forventer at det eksisterer en aktiv totrinnsvurdering når oppgave sendes til beslutter"
                    }

                    val beslutter =
                        totrinnsvurdering.beslutter?.let(session.saksbehandlerRepository::finn)
                            ?.let(this::tilLegacySaksbehandler)
                    apiOppgaveService.sendTilBeslutter(oppgavereferanse, beslutter)
                    totrinnsvurdering.sendTilBeslutter(oppgavereferanse, SaksbehandlerOid(saksbehandlerFraApi.oid))
                    session.totrinnsvurderingRepository.lagre(totrinnsvurdering)
                }
            } catch (modellfeil: Modellfeil) {
                return@transactionalSessionScope SendTilGodkjenningResult.Feil.KunneIkkeSendeTilBeslutter(modellfeil.tilApiversjon())
            } catch (e: Exception) {
                return@transactionalSessionScope SendTilGodkjenningResult.Feil.UventetFeilVedSendigTilBeslutter(e)
            }

            try {
                påVent(ApiPaVentRequest.ApiFjernPaVentUtenHistorikkinnslag(oppgavereferanse), saksbehandlerFraApi)
            } catch (modellfeil: no.nav.helse.spesialist.api.feilhåndtering.Modellfeil) {
                return@transactionalSessionScope SendTilGodkjenningResult.Feil.KunneIkkeFjerneFraPåVent(modellfeil)
            } catch (e: Exception) {
                return@transactionalSessionScope SendTilGodkjenningResult.Feil.UventetFeilVedFjernFraPåVent(e)
            }

            sikkerlogg.info(
                "Oppgave med {} sendes til godkjenning av saksbehandler med {}",
                StructuredArguments.kv("oppgaveId", oppgavereferanse),
                StructuredArguments.kv("oid", saksbehandlerFraApi.oid),
            )

            try {
                val innslag = Historikkinnslag.avventerTotrinnsvurdering(saksbehandlerFraApi.toDto())
                periodehistorikkDao.lagreMedOppgaveId(innslag, oppgavereferanse)
            } catch (e: Exception) {
                return@transactionalSessionScope SendTilGodkjenningResult.Feil.UventetFeilVedOpprettingAvPeriodehistorikk(
                    e,
                )
            }

            log.info("OppgaveId $oppgavereferanse sendt til godkjenning")

            return@transactionalSessionScope SendTilGodkjenningResult.Ok
        }

    private fun tilLegacySaksbehandler(saksbehandler: Saksbehandler): LegacySaksbehandler =
        saksbehandler.gjenopprett(tilgangskontroll = tilgangskontroll)

    private fun vurderVarsel(
        fødselsnummer: String,
        behandlingId: UUID,
        vedtaksperiodeId: UUID,
        varselId: UUID,
        varseltittel: String,
        varselkode: String,
        forrigeStatus: VarselDbDto.Varselstatus,
        gjeldendeStatus: VarselDbDto.Varselstatus,
        saksbehandlerIdent: String,
    ) {
        varselRepository.vurderVarselFor(varselId, gjeldendeStatus, saksbehandlerIdent)
        val varselEndret =
            VarselEndret(
                vedtaksperiodeId = vedtaksperiodeId,
                behandlingId = behandlingId,
                varselId = varselId,
                varseltittel = varseltittel,
                varselkode = varselkode,
                forrigeStatus = forrigeStatus.name,
                gjeldendeStatus = gjeldendeStatus.name,
            )
        meldingPubliserer.publiser(fødselsnummer, varselEndret, "varsel vurdert")
    }

    internal companion object {
        private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")
        private val log = LoggerFactory.getLogger(SaksbehandlerMediator::class.java.simpleName)
    }

    private fun Modellfeil.tilApiversjon(): no.nav.helse.spesialist.api.feilhåndtering.Modellfeil =
        when (this) {
            is no.nav.helse.modell.OppgaveIkkeTildelt -> OppgaveIkkeTildelt(oppgaveId)
            is OppgaveTildeltNoenAndre -> {
                val saksbehandler =
                    checkNotNull(
                        sessionFactory.transactionalSessionScope { session ->
                            session.saksbehandlerRepository.finn(SaksbehandlerOid(saksbehandlerOid))
                        },
                    ).let(this@SaksbehandlerMediator::tilLegacySaksbehandler)
                no.nav.helse.spesialist.api.feilhåndtering.OppgaveTildeltNoenAndre(
                    TildelingApiDto(saksbehandler.navn, saksbehandler.epostadresse, saksbehandler.oid),
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

            is AlleredeAnnullert -> no.nav.helse.spesialist.api.feilhåndtering.AlleredeAnnullert(handling.toDto().vedtaksperiodeId)

            is FinnerIkkePåVent -> FinnerIkkeLagtPåVent(oppgaveId)
        }

    private fun SaksbehandlerFraApi.tilSaksbehandler(): Saksbehandler =
        Saksbehandler(
            id = SaksbehandlerOid(oid),
            navn = navn,
            epost = epost,
            ident = ident,
        )

    private fun Saksbehandler.tilLegacySaksbehandler(saksbehandlergrupper: List<UUID>): LegacySaksbehandler =
        gjenopprett(TilgangskontrollørForApi(saksbehandlergrupper, tilgangsgrupper))

    private fun HandlingFraApi.tilModellversjon(saksbehandlerOid: SaksbehandlerOid): Handling =
        when (this) {
            is ApiArbeidsforholdOverstyringHandling -> this.tilModellversjon(saksbehandlerOid)
            is ApiInntektOgRefusjonOverstyring -> this.tilModellversjon(saksbehandlerOid)
            is ApiTidslinjeOverstyring -> this.tilModellversjon(saksbehandlerOid)
            is ApiSkjonnsfastsettelse -> this.tilModellversjon(saksbehandlerOid)
            is ApiMinimumSykdomsgrad -> this.tilModellversjon(saksbehandlerOid)
            is ApiAnnulleringData -> this.tilModellversjon()
            is TildelOppgave -> this.tilModellversjon()
            is AvmeldOppgave -> this.tilModellversjon()
            is ApiOpphevStans -> this.tilModellversjon()
            else -> throw IllegalStateException("Støtter ikke handling ${this::class.simpleName}")
        }

    private fun ApiPaVentRequest.tilModellversjon(): PåVent =
        when (this) {
            is ApiPaVentRequest.ApiLeggPaVent -> this.tilModellversjon()
            is ApiPaVentRequest.ApiFjernPaVent -> this.tilModellversjon()
            is ApiPaVentRequest.ApiFjernPaVentUtenHistorikkinnslag -> this.tilModellversjon()
            is ApiPaVentRequest.ApiEndrePaVent -> this.tilModellversjon()
        }

    private fun ApiArbeidsforholdOverstyringHandling.tilModellversjon(saksbehandlerOid: SaksbehandlerOid): OverstyrtArbeidsforhold =
        OverstyrtArbeidsforhold.ny(
            fødselsnummer = fodselsnummer,
            aktørId = aktorId,
            skjæringstidspunkt = skjaringstidspunkt,
            vedtaksperiodeId = vedtaksperiodeId,
            saksbehandlerOid = saksbehandlerOid,
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

    private fun ApiSkjonnsfastsettelse.tilModellversjon(saksbehandlerOid: SaksbehandlerOid): SkjønnsfastsattSykepengegrunnlag =
        SkjønnsfastsattSykepengegrunnlag.ny(
            aktørId = aktorId,
            fødselsnummer = fodselsnummer,
            skjæringstidspunkt = skjaringstidspunkt,
            vedtaksperiodeId = vedtaksperiodeId,
            saksbehandlerOid = saksbehandlerOid,
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

    private fun ApiMinimumSykdomsgrad.tilModellversjon(saksbehandlerOid: SaksbehandlerOid): MinimumSykdomsgrad =
        MinimumSykdomsgrad.ny(
            aktørId = aktorId,
            fødselsnummer = fodselsnummer,
            saksbehandlerOid = saksbehandlerOid,
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
            vedtaksperiodeId = initierendeVedtaksperiodeId,
        )

    private fun ApiInntektOgRefusjonOverstyring.tilModellversjon(saksbehandlerOid: SaksbehandlerOid): OverstyrtInntektOgRefusjon =
        OverstyrtInntektOgRefusjon.ny(
            aktørId = aktorId,
            fødselsnummer = fodselsnummer,
            skjæringstidspunkt = skjaringstidspunkt,
            vedtaksperiodeId = vedtaksperiodeId,
            saksbehandlerOid = saksbehandlerOid,
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

    private fun ApiTidslinjeOverstyring.tilModellversjon(saksbehandlerOid: SaksbehandlerOid): OverstyrtTidslinje =
        OverstyrtTidslinje.ny(
            vedtaksperiodeId = vedtaksperiodeId,
            aktørId = aktorId,
            fødselsnummer = fodselsnummer,
            organisasjonsnummer = organisasjonsnummer,
            saksbehandlerOid = saksbehandlerOid,
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

    private fun ApiAnnulleringData.tilModellversjon(): Annullering =
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

    private fun ApiPaVentRequest.ApiLeggPaVent.tilModellversjon(): LeggPåVent =
        LeggPåVent(
            fødselsnummer = apiOppgaveService.fødselsnummer(oppgaveId),
            oppgaveId = oppgaveId,
            behandlingId = apiOppgaveService.spleisBehandlingId(oppgaveId),
            frist = frist,
            skalTildeles = skalTildeles,
            notatTekst = notatTekst,
            årsaker = årsaker.map { årsak -> PåVentÅrsak(key = årsak._key, årsak = årsak.arsak) },
        )

    private fun ApiPaVentRequest.ApiEndrePaVent.tilModellversjon(): EndrePåVent {
        return EndrePåVent(
            fødselsnummer = apiOppgaveService.fødselsnummer(oppgaveId),
            oppgaveId = oppgaveId,
            behandlingId = apiOppgaveService.spleisBehandlingId(oppgaveId),
            frist = frist,
            skalTildeles = skalTildeles,
            notatTekst = notatTekst,
            årsaker = årsaker.map { årsak -> PåVentÅrsak(key = årsak._key, årsak = årsak.arsak) },
        )
    }

    private fun ApiPaVentRequest.ApiFjernPaVent.tilModellversjon(): FjernPåVent = FjernPåVent(oppgaveId)

    private fun ApiPaVentRequest.ApiFjernPaVentUtenHistorikkinnslag.tilModellversjon(): FjernPåVentUtenHistorikkinnslag =
        FjernPåVentUtenHistorikkinnslag(oppgaveId)

    private fun TildelOppgave.tilModellversjon(): no.nav.helse.modell.saksbehandler.handlinger.TildelOppgave =
        no.nav.helse.modell.saksbehandler.handlinger.TildelOppgave(this.oppgaveId)

    private fun AvmeldOppgave.tilModellversjon(): no.nav.helse.modell.saksbehandler.handlinger.AvmeldOppgave =
        no.nav.helse.modell.saksbehandler.handlinger.AvmeldOppgave(this.oppgaveId)

    private fun ApiOpphevStans.tilModellversjon(): OpphevStans = OpphevStans(this.fødselsnummer, this.begrunnelse)

    private fun Utfall.toDatabaseType() =
        when (this) {
            Utfall.AVSLAG -> VedtakBegrunnelseTypeFraDatabase.AVSLAG
            Utfall.DELVIS_INNVILGELSE -> VedtakBegrunnelseTypeFraDatabase.DELVIS_INNVILGELSE
            Utfall.INNVILGELSE -> VedtakBegrunnelseTypeFraDatabase.INNVILGELSE
        }

    private fun SaksbehandlerFraApi.toDto(): SaksbehandlerDto =
        SaksbehandlerDto(
            epostadresse = this.epost,
            oid = this.oid,
            navn = this.navn,
            ident = this.ident,
        )
}

internal fun overstyringUnitOfWork(
    overstyring: Overstyring,
    saksbehandler: Saksbehandler,
    sessionFactory: SessionFactory,
    overstyringBlock: () -> Unit,
) = sessionFactory.transactionalSessionScope { session ->
    sikkerlogg.info("Utfører overstyring ${overstyring.loggnavn()} på vegne av saksbehandler $saksbehandler")
    session.saksbehandlerRepository.lagre(saksbehandler)

    val fødselsnummer = overstyring.fødselsnummer
    sikkerlogg.info("Reserverer person $fødselsnummer til saksbehandler $saksbehandler")
    session.reservasjonDao.reserverPerson(saksbehandler.id().value, fødselsnummer)

    val totrinnsvurdering =
        session.totrinnsvurderingRepository.finn(fødselsnummer) ?: Totrinnsvurdering.ny(
            overstyring.vedtaksperiodeId,
            fødselsnummer,
        )
    totrinnsvurdering.nyOverstyring(overstyring)
    session.totrinnsvurderingRepository.lagre(totrinnsvurdering)

    overstyringBlock()
}
