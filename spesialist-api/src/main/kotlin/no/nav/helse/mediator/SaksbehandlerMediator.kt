package no.nav.helse.mediator

import graphql.schema.DataFetchingEnvironment
import net.logstash.logback.argument.StructuredArguments
import no.nav.helse.MeldingPubliserer
import no.nav.helse.bootstrap.EnvironmentToggles
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
import no.nav.helse.modell.FinnerIkkePåVent
import no.nav.helse.modell.ManglerTilgang
import no.nav.helse.modell.Modellfeil
import no.nav.helse.modell.OppgaveAlleredeSendtBeslutter
import no.nav.helse.modell.OppgaveAlleredeSendtIRetur
import no.nav.helse.modell.OppgaveKreverVurderingAvToSaksbehandlere
import no.nav.helse.modell.OppgaveTildeltNoenAndre
import no.nav.helse.modell.melding.VarselEndret
import no.nav.helse.modell.periodehistorikk.Historikkinnslag
import no.nav.helse.modell.saksbehandler.handlinger.Arbeidsforhold
import no.nav.helse.modell.saksbehandler.handlinger.EndrePåVent
import no.nav.helse.modell.saksbehandler.handlinger.FjernPåVent
import no.nav.helse.modell.saksbehandler.handlinger.FjernPåVentUtenHistorikkinnslag
import no.nav.helse.modell.saksbehandler.handlinger.Handling
import no.nav.helse.modell.saksbehandler.handlinger.HandlingType
import no.nav.helse.modell.saksbehandler.handlinger.LeggPåVent
import no.nav.helse.modell.saksbehandler.handlinger.MinimumSykdomsgrad
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
import no.nav.helse.modell.totrinnsvurdering.Totrinnsvurdering
import no.nav.helse.modell.totrinnsvurdering.TotrinnsvurderingTilstand.AVVENTER_BESLUTTER
import no.nav.helse.modell.vedtak.Utfall
import no.nav.helse.modell.vilkårsprøving.Lovhjemmel
import no.nav.helse.spesialist.api.SendIReturResult
import no.nav.helse.spesialist.api.SendTilGodkjenningResult
import no.nav.helse.spesialist.api.feilhåndtering.FinnerIkkeLagtPåVent
import no.nav.helse.spesialist.api.feilhåndtering.IkkeTilgang
import no.nav.helse.spesialist.api.feilhåndtering.ManglerVurderingAvVarsler
import no.nav.helse.spesialist.api.feilhåndtering.OppgaveIkkeTildelt
import no.nav.helse.spesialist.api.graphql.ContextValues
import no.nav.helse.spesialist.api.graphql.mutation.VedtakMutationHandler.VedtakResultat
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
import no.nav.helse.spesialist.api.saksbehandler.handlinger.AvmeldOppgave
import no.nav.helse.spesialist.api.saksbehandler.handlinger.HandlingFraApi
import no.nav.helse.spesialist.api.saksbehandler.handlinger.TildelOppgave
import no.nav.helse.spesialist.api.tildeling.TildelingApiDto
import no.nav.helse.spesialist.application.Outbox
import no.nav.helse.spesialist.application.TotrinnsvurderingRepository
import no.nav.helse.spesialist.application.logg.logg
import no.nav.helse.spesialist.application.logg.loggInfo
import no.nav.helse.spesialist.application.logg.loggThrowable
import no.nav.helse.spesialist.application.logg.sikkerlogg
import no.nav.helse.spesialist.domain.Saksbehandler
import no.nav.helse.spesialist.domain.SaksbehandlerOid
import no.nav.helse.spesialist.domain.SpleisBehandlingId
import no.nav.helse.spesialist.domain.legacy.SaksbehandlerWrapper
import no.nav.helse.spesialist.domain.tilgangskontroll.Tilgangsgruppe
import no.nav.helse.tell
import java.util.UUID
import no.nav.helse.spesialist.api.feilhåndtering.Modellfeil as ApiModellfeil

class SaksbehandlerMediator(
    daos: Daos,
    private val versjonAvKode: String,
    private val meldingPubliserer: MeldingPubliserer,
    private val oppgaveService: OppgaveService,
    private val apiOppgaveService: ApiOppgaveService,
    private val environmentToggles: EnvironmentToggles,
    private val sessionFactory: SessionFactory,
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

    fun <T> utførHandling(
        handlingType: HandlingType,
        env: DataFetchingEnvironment,
        block: (saksbehandler: Saksbehandler, tilgangsgrupper: Set<Tilgangsgruppe>, transaction: SessionContext, outbox: Outbox) -> T,
    ): T =
        utførHandling(
            handlingType = handlingType,
            saksbehandler = env.graphQlContext.get(ContextValues.SAKSBEHANDLER),
            tilgangsgrupper = env.graphQlContext.get(ContextValues.TILGANGSGRUPPER),
            block = block,
        )

    fun <T> utførHandling(
        handlingType: HandlingType,
        saksbehandler: Saksbehandler,
        tilgangsgrupper: Set<Tilgangsgruppe>,
        block: (saksbehandler: Saksbehandler, tilgangsgrupper: Set<Tilgangsgruppe>, transaction: SessionContext, outbox: Outbox) -> T,
    ): T =
        withMDC(
            mapOf(
                "saksbehandlerOid" to saksbehandler.id().value.toString(),
                "handlingId" to UUID.randomUUID().toString(),
            ),
        ) {
            sessionFactory.transactionalSessionScope { it.saksbehandlerRepository.lagre(saksbehandler = saksbehandler) }
            tell(handlingType)

            val navnPåHandling = handlingType.name.lowercase()
            loggInfo(
                melding = "Utfører handling $navnPåHandling på vegne av saksbehandler",
                sikkerloggDetaljer = "epostadresse=${saksbehandler.epost}, oid=${saksbehandler.id().value}",
            )
            val outbox = Outbox()
            val returnValue =
                runCatching {
                    sessionFactory
                        .transactionalSessionScope { transaction ->
                            block(saksbehandler, tilgangsgrupper, transaction, outbox)
                        }.also { outbox.sendAlle(meldingPubliserer) }
                }.onFailure { throwable ->
                    loggThrowable("Handling $navnPåHandling feilet", throwable)
                    throw throwable
                }
            loggInfo("Handling $navnPåHandling utført")
            returnValue.getOrThrow()
        }

    fun håndter(
        handlingFraApi: HandlingFraApi,
        saksbehandler: Saksbehandler,
        tilgangsgrupper: Set<Tilgangsgruppe>,
    ) {
        val modellhandling =
            handlingFraApi.tilModellversjon(
                saksbehandlerOid = saksbehandler.id(),
                saksbehandlerTilgangsgrupper = tilgangsgrupper,
            )
        sessionFactory.transactionalSessionScope { it.saksbehandlerRepository.lagre(saksbehandler) }
        tell(modellhandling)
        val saksbehandlerWrapper = SaksbehandlerWrapper(saksbehandler = saksbehandler)
        saksbehandlerWrapper.register(Saksbehandlingsmelder(meldingPubliserer))
        saksbehandlerWrapper.register(Subsumsjonsmelder(versjonAvKode, meldingPubliserer))
        val handlingId = UUID.randomUUID()

        withMDC(
            mapOf(
                "saksbehandlerOid" to saksbehandler.id().value.toString(),
                "handlingId" to handlingId.toString(),
            ),
        ) {
            sikkerlogg.info("Utfører handling ${modellhandling.loggnavn()} på vegne av saksbehandler $saksbehandler")
            when (modellhandling) {
                is Overstyring -> {
                    overstyringUnitOfWork(
                        overstyring = modellhandling,
                        saksbehandler = saksbehandler,
                        sessionFactory = sessionFactory,
                    )
                    modellhandling.utførAv(saksbehandlerWrapper)
                }

                is Oppgavehandling -> håndter(modellhandling, saksbehandlerWrapper)
                is PåVent -> error("dette burde ikke skje")
                is Personhandling -> håndter(modellhandling, saksbehandlerWrapper)
                else -> modellhandling.utførAv(saksbehandlerWrapper)
            }
            sikkerlogg.info("Handling ${modellhandling.loggnavn()} utført")
        }
    }

    fun vedtak(
        saksbehandler: Saksbehandler,
        tilgangsgrupper: Set<Tilgangsgruppe>,
        oppgavereferanse: Long,
        begrunnelse: String?,
    ): VedtakResultat =
        sessionFactory.transactionalSessionScope { sessionContext ->
            val spleisBehandlingId = apiOppgaveService.spleisBehandlingId(oppgavereferanse)
            val fødselsnummer = oppgaveApiDao.finnFødselsnummer(oppgavereferanse)
            if (!apiOppgaveService.venterPåSaksbehandler(oppgavereferanse)) {
                VedtakResultat.Feil.IkkeÅpenOppgave()
            } else {
                håndterTotrinnsvurderingBeslutning(
                    fødselsnummer = fødselsnummer,
                    saksbehandler = saksbehandler,
                    tilgangsgrupper = tilgangsgrupper,
                    totrinnsvurderingRepository = sessionContext.totrinnsvurderingRepository,
                ) ?: håndterGodkjenning(oppgavereferanse, fødselsnummer, spleisBehandlingId, saksbehandler).also {
                    håndterVedtakBegrunnelse(
                        utfall = hentUtfallFraBehandling(spleisBehandlingId, sessionContext),
                        begrunnelse = begrunnelse,
                        oppgaveId = oppgavereferanse,
                        saksbehandlerOid = saksbehandler.id().value,
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
        saksbehandler: Saksbehandler,
        tilgangsgrupper: Set<Tilgangsgruppe>,
        oppgavereferanse: Long,
    ): VedtakResultat =
        sessionFactory.transactionalSessionScope { sessionContext ->
            val spleisBehandlingId = apiOppgaveService.spleisBehandlingId(oppgavereferanse)
            val fødselsnummer = oppgaveApiDao.finnFødselsnummer(oppgavereferanse)
            if (!apiOppgaveService.venterPåSaksbehandler(oppgavereferanse)) {
                VedtakResultat.Feil.IkkeÅpenOppgave()
            } else {
                håndterTotrinnsvurderingBeslutning(
                    fødselsnummer = fødselsnummer,
                    saksbehandler = saksbehandler,
                    tilgangsgrupper = tilgangsgrupper,
                    totrinnsvurderingRepository = sessionContext.totrinnsvurderingRepository,
                ) ?: håndterAvvisning(oppgavereferanse, fødselsnummer, spleisBehandlingId, saksbehandler)
            }
        }

    private fun håndterAvvisning(
        oppgavereferanse: Long,
        fødselsnummer: String,
        spleisBehandlingId: UUID,
        saksbehandler: Saksbehandler,
    ): VedtakResultat.Ok {
        val periodeTilGodkjenning = behandlingRepository.periodeTilGodkjenning(oppgavereferanse)
        periodeTilGodkjenning.avvisVarsler(
            fødselsnummer = fødselsnummer,
            behandlingId = spleisBehandlingId,
            ident = saksbehandler.ident,
            godkjenner = this::vurderVarsel,
        )

        oppgaveService.fjernFraPåVent(oppgavereferanse)
        påVentDao.slettPåVent(oppgavereferanse)
        return VedtakResultat.Ok(spleisBehandlingId)
    }

    private fun håndterGodkjenning(
        oppgavereferanse: Long,
        fødselsnummer: String,
        spleisBehandlingId: UUID,
        saksbehandler: Saksbehandler,
    ): VedtakResultat {
        val perioderTilBehandling = behandlingRepository.perioderTilBehandling(oppgavereferanse)
        val periodeTilGodkjenning = behandlingRepository.periodeTilGodkjenning(oppgavereferanse)
        return when {
            perioderTilBehandling.harAktiveVarsler() -> VedtakResultat.Feil.HarAktiveVarsler(oppgavereferanse)
            periodeTilGodkjenning.overlapperMedInfotrygd() ->
                VedtakResultat.Feil.OverlapperMedInfotrygd(saksbehandler.ident)

            else -> {
                perioderTilBehandling.godkjennVarsler(
                    fødselsnummer = fødselsnummer,
                    behandlingId = spleisBehandlingId,
                    ident = saksbehandler.ident,
                    godkjenner = this::vurderVarsel,
                )

                oppgaveService.fjernFraPåVent(oppgavereferanse)
                påVentDao.slettPåVent(oppgavereferanse)
                VedtakResultat.Ok(spleisBehandlingId)
            }
        }
    }

    private fun håndterTotrinnsvurderingBeslutning(
        fødselsnummer: String,
        saksbehandler: Saksbehandler,
        tilgangsgrupper: Set<Tilgangsgruppe>,
        totrinnsvurderingRepository: TotrinnsvurderingRepository,
    ): VedtakResultat.Feil.BeslutterFeil? {
        val totrinnsvurdering = totrinnsvurderingRepository.finnAktivForPerson(fødselsnummer)
        val feil =
            if (totrinnsvurdering?.tilstand == AVVENTER_BESLUTTER) {
                if (Tilgangsgruppe.BESLUTTER !in tilgangsgrupper && !environmentToggles.kanGodkjenneUtenBesluttertilgang) {
                    VedtakResultat.Feil.BeslutterFeil.TrengerBeslutterRolle()
                } else if (totrinnsvurdering.saksbehandler?.value == saksbehandler.id().value && !environmentToggles.kanBeslutteEgneSaker) {
                    VedtakResultat.Feil.BeslutterFeil.KanIkkeBeslutteEgenOppgave()
                } else {
                    totrinnsvurdering.settBeslutter(saksbehandler.id())
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
        saksbehandler: Saksbehandler,
    ) {
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
            val saksbehandlerWrapper = SaksbehandlerWrapper(saksbehandler = saksbehandler)
            when (modellhandling) {
                is LeggPåVent -> leggPåVent(modellhandling, saksbehandlerWrapper)
                is FjernPåVent -> fjernFraPåVent(modellhandling, saksbehandlerWrapper)
                is FjernPåVentUtenHistorikkinnslag ->
                    fjernFraPåVentUtenHistorikkinnslag(
                        modellhandling,
                    )

                is EndrePåVent -> endrePåVent(modellhandling, saksbehandlerWrapper)
            }
            sikkerlogg.info(
                "Handling ${modellhandling.loggnavn()} utført på oppgave ${modellhandling.oppgaveId} på vegne av saksbehandler $saksbehandler",
            )
        }
    }

    private fun håndter(
        handling: Oppgavehandling,
        saksbehandlerWrapper: SaksbehandlerWrapper,
    ) {
        try {
            oppgaveService.avbrytOppgave(handling, saksbehandlerWrapper)
        } catch (e: Modellfeil) {
            throw e.tilApiversjon()
        }
    }

    private fun håndter(
        handling: Personhandling,
        saksbehandlerWrapper: SaksbehandlerWrapper,
    ) = try {
        handling.utførAv(saksbehandlerWrapper)
    } catch (e: Modellfeil) {
        throw e.tilApiversjon()
    }

    private fun leggPåVent(
        handling: LeggPåVent,
        saksbehandlerWrapper: SaksbehandlerWrapper,
    ) {
        try {
            val dialogRef = dialogDao.lagre()
            val innslag =
                Historikkinnslag.lagtPåVentInnslag(
                    notattekst = handling.notatTekst,
                    saksbehandler = saksbehandlerWrapper.saksbehandler,
                    årsaker = handling.årsaker,
                    frist = handling.frist,
                    dialogRef = dialogRef,
                )
            periodehistorikkDao.lagreMedOppgaveId(innslag, handling.oppgaveId)
            oppgaveService.leggPåVent(handling, saksbehandlerWrapper)
            PåVentRepository(påVentDao).leggPåVent(saksbehandlerWrapper.saksbehandler.id().value, handling, dialogRef)
            saksbehandlerWrapper.register(Saksbehandlingsmelder(meldingPubliserer))
            handling.utførAv(saksbehandlerWrapper)
        } catch (e: Modellfeil) {
            throw e.tilApiversjon()
        }
    }

    private fun endrePåVent(
        handling: EndrePåVent,
        saksbehandlerWrapper: SaksbehandlerWrapper,
    ) {
        if (!påVentDao.erPåVent(handling.oppgaveId)) throw FinnerIkkeLagtPåVent(handling.oppgaveId)

        val dialogRef = dialogDao.lagre()
        val innslag =
            Historikkinnslag.endrePåVentInnslag(
                notattekst = handling.notatTekst,
                saksbehandler = saksbehandlerWrapper.saksbehandler,
                årsaker = handling.årsaker,
                frist = handling.frist,
                dialogRef = dialogRef,
            )
        periodehistorikkDao.lagreMedOppgaveId(innslag, handling.oppgaveId)
        oppgaveService.endrePåVent(handling, saksbehandlerWrapper)
        PåVentRepository(påVentDao).endrePåVent(saksbehandlerWrapper.saksbehandler.id().value, handling, dialogRef)

        saksbehandlerWrapper.register(Saksbehandlingsmelder(meldingPubliserer))
        handling.utførAv(saksbehandlerWrapper)
    }

    private fun fjernFraPåVent(
        handling: FjernPåVent,
        saksbehandlerWrapper: SaksbehandlerWrapper,
    ) {
        if (!påVentDao.erPåVent(handling.oppgaveId)) {
            sikkerlogg.info("Oppgave ${handling.oppgaveId} er ikke på vent")
            return
        }
        try {
            val innslag = Historikkinnslag.fjernetFraPåVentInnslag(saksbehandlerWrapper.saksbehandler)
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
        saksbehandler: Saksbehandler,
        personidentifikator: String,
    ) {
        sessionFactory.transactionalSessionScope { it.saksbehandlerRepository.lagre(saksbehandler) }
        abonnementDao.opprettAbonnement(saksbehandler.id().value, personidentifikator)
    }

    fun hentAbonnerteOpptegnelser(
        saksbehandler: Saksbehandler,
        sisteSekvensId: Int,
    ): List<ApiOpptegnelse> {
        sessionFactory.transactionalSessionScope { it.saksbehandlerRepository.lagre(saksbehandler) }
        abonnementDao.registrerSistekvensnummer(saksbehandler.id().value, sisteSekvensId)
        return opptegnelseRepository.finnOpptegnelser(saksbehandler.id().value).toApiOpptegnelser()
    }

    fun hentAbonnerteOpptegnelser(saksbehandler: Saksbehandler): List<ApiOpptegnelse> =
        sessionFactory
            .transactionalSessionScope { session ->
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

    fun sendIRetur(
        oppgavereferanse: Long,
        besluttendeSaksbehandler: Saksbehandler,
        notatTekst: String,
    ): SendIReturResult {
        sikkerlogg.info(
            "Oppgave med {} sendes i retur av beslutter med {}",
            StructuredArguments.kv("oppgaveId", oppgavereferanse),
            StructuredArguments.kv("oid", besluttendeSaksbehandler.id().value),
        )

        try {
            sessionFactory.transactionalSessionScope { session ->
                val fødselsnummer = oppgaveService.finnFødselsnummer(oppgavereferanse)
                val totrinnsvurdering = session.totrinnsvurderingRepository.finnAktivForPerson(fødselsnummer)
                checkNotNull(totrinnsvurdering) {
                    "Forventer at det eksisterer en aktiv totrinnsvurdering når oppgave sendes i retur"
                }
                val opprinneligSaksbehandler =
                    checkNotNull(
                        totrinnsvurdering.saksbehandler
                            ?.let(session.saksbehandlerRepository::finn)
                            ?.let { SaksbehandlerWrapper(saksbehandler = it) },
                    ) {
                        "Opprinnelig saksbehandler kan ikke være null ved retur av beslutteroppgave"
                    }

                apiOppgaveService.sendIRetur(oppgavereferanse, opprinneligSaksbehandler)
                totrinnsvurdering.sendIRetur(oppgavereferanse, SaksbehandlerOid(besluttendeSaksbehandler.id().value))
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
                    saksbehandler = besluttendeSaksbehandler,
                    dialogRef = dialogRef,
                )
            periodehistorikkDao.lagreMedOppgaveId(innslag, oppgavereferanse)
        } catch (e: Exception) {
            return SendIReturResult.Feil.KunneIkkeOppretteHistorikkinnslag(e)
        }

        logg.info("OppgaveId $oppgavereferanse sendt i retur")

        return SendIReturResult.Ok
    }

    fun håndterTotrinnsvurdering(
        oppgavereferanse: Long,
        saksbehandler: Saksbehandler,
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
                    saksbehandlerOid = saksbehandler.id().value,
                )
            } catch (e: Exception) {
                return@transactionalSessionScope SendTilGodkjenningResult.Feil.KunneIkkeHåndtereBegrunnelse(e)
            }

            try {
                sessionFactory.transactionalSessionScope { session ->
                    val fødselsnummer = oppgaveService.finnFødselsnummer(oppgavereferanse)
                    val totrinnsvurdering = session.totrinnsvurderingRepository.finnAktivForPerson(fødselsnummer)
                    checkNotNull(totrinnsvurdering) {
                        "Forventer at det eksisterer en aktiv totrinnsvurdering når oppgave sendes til beslutter"
                    }

                    val beslutter =
                        totrinnsvurdering.beslutter
                            ?.let(session.saksbehandlerRepository::finn)
                            ?.let { SaksbehandlerWrapper(saksbehandler = it) }
                    apiOppgaveService.sendTilBeslutter(oppgavereferanse, beslutter)
                    totrinnsvurdering.sendTilBeslutter(oppgavereferanse, saksbehandler.id())
                    session.totrinnsvurderingRepository.lagre(totrinnsvurdering)
                }
            } catch (modellfeil: Modellfeil) {
                sikkerlogg.error("Feil ved sending til beslutter", modellfeil)
                return@transactionalSessionScope SendTilGodkjenningResult.Feil.KunneIkkeSendeTilBeslutter(modellfeil.tilApiversjon())
            } catch (e: Exception) {
                sikkerlogg.error("Feil ved sending til beslutter", e)
                return@transactionalSessionScope SendTilGodkjenningResult.Feil.UventetFeilVedSendigTilBeslutter(e)
            }

            try {
                påVent(ApiPaVentRequest.ApiFjernPaVentUtenHistorikkinnslag(oppgavereferanse), saksbehandler)
            } catch (modellfeil: ApiModellfeil) {
                return@transactionalSessionScope SendTilGodkjenningResult.Feil.KunneIkkeFjerneFraPåVent(modellfeil)
            } catch (e: Exception) {
                return@transactionalSessionScope SendTilGodkjenningResult.Feil.UventetFeilVedFjernFraPåVent(e)
            }

            sikkerlogg.info(
                "Oppgave med {} sendes til godkjenning av saksbehandler med {}",
                StructuredArguments.kv("oppgaveId", oppgavereferanse),
                StructuredArguments.kv("oid", saksbehandler.id().value),
            )

            try {
                val innslag = Historikkinnslag.avventerTotrinnsvurdering(saksbehandler)
                periodehistorikkDao.lagreMedOppgaveId(innslag, oppgavereferanse)
            } catch (e: Exception) {
                return@transactionalSessionScope SendTilGodkjenningResult.Feil.UventetFeilVedOpprettingAvPeriodehistorikk(
                    e,
                )
            }

            logg.info("OppgaveId $oppgavereferanse sendt til godkjenning")

            return@transactionalSessionScope SendTilGodkjenningResult.Ok
        }

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

    private fun Modellfeil.tilApiversjon(): ApiModellfeil =
        when (this) {
            is no.nav.helse.modell.OppgaveIkkeTildelt -> OppgaveIkkeTildelt(oppgaveId)
            is OppgaveTildeltNoenAndre -> {
                val saksbehandler =
                    SaksbehandlerWrapper(
                        saksbehandler =
                            checkNotNull(
                                sessionFactory.transactionalSessionScope { session ->
                                    session.saksbehandlerRepository.finn(SaksbehandlerOid(saksbehandlerOid))
                                },
                            ),
                    )
                no.nav.helse.spesialist.api.feilhåndtering.OppgaveTildeltNoenAndre(
                    TildelingApiDto(
                        saksbehandler.saksbehandler.navn,
                        saksbehandler.saksbehandler.epost,
                        saksbehandler.saksbehandler.id().value,
                    ),
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

            is FinnerIkkePåVent -> FinnerIkkeLagtPåVent(oppgaveId)
        }

    private fun HandlingFraApi.tilModellversjon(
        saksbehandlerOid: SaksbehandlerOid,
        saksbehandlerTilgangsgrupper: Set<Tilgangsgruppe>,
    ): Handling =
        when (this) {
            is ApiArbeidsforholdOverstyringHandling -> this.tilModellversjon(saksbehandlerOid)
            is ApiInntektOgRefusjonOverstyring -> this.tilModellversjon(saksbehandlerOid)
            is ApiTidslinjeOverstyring -> this.tilModellversjon(saksbehandlerOid)
            is ApiSkjonnsfastsettelse -> this.tilModellversjon(saksbehandlerOid)
            is ApiMinimumSykdomsgrad -> this.tilModellversjon(saksbehandlerOid)
            is TildelOppgave -> this.tilModellversjon(saksbehandlerTilgangsgrupper)
            is AvmeldOppgave -> this.tilModellversjon()
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

    private fun ApiPaVentRequest.ApiEndrePaVent.tilModellversjon(): EndrePåVent =
        EndrePåVent(
            fødselsnummer = apiOppgaveService.fødselsnummer(oppgaveId),
            oppgaveId = oppgaveId,
            behandlingId = apiOppgaveService.spleisBehandlingId(oppgaveId),
            frist = frist,
            skalTildeles = skalTildeles,
            notatTekst = notatTekst,
            årsaker = årsaker.map { årsak -> PåVentÅrsak(key = årsak._key, årsak = årsak.arsak) },
        )

    private fun ApiPaVentRequest.ApiFjernPaVent.tilModellversjon(): FjernPåVent = FjernPåVent(oppgaveId)

    private fun ApiPaVentRequest.ApiFjernPaVentUtenHistorikkinnslag.tilModellversjon(): FjernPåVentUtenHistorikkinnslag = FjernPåVentUtenHistorikkinnslag(oppgaveId)

    private fun TildelOppgave.tilModellversjon(
        saksbehandlerTilgangsgrupper: Set<Tilgangsgruppe>,
    ): no.nav.helse.modell.saksbehandler.handlinger.TildelOppgave =
        no.nav.helse.modell.saksbehandler.handlinger
            .TildelOppgave(this.oppgaveId, saksbehandlerTilgangsgrupper)

    private fun AvmeldOppgave.tilModellversjon(): no.nav.helse.modell.saksbehandler.handlinger.AvmeldOppgave =
        no.nav.helse.modell.saksbehandler.handlinger
            .AvmeldOppgave(this.oppgaveId)

    private fun Utfall.toDatabaseType() =
        when (this) {
            Utfall.AVSLAG -> VedtakBegrunnelseTypeFraDatabase.AVSLAG
            Utfall.DELVIS_INNVILGELSE -> VedtakBegrunnelseTypeFraDatabase.DELVIS_INNVILGELSE
            Utfall.INNVILGELSE -> VedtakBegrunnelseTypeFraDatabase.INNVILGELSE
        }
}

private fun overstyringUnitOfWork(
    overstyring: Overstyring,
    saksbehandler: Saksbehandler,
    sessionFactory: SessionFactory,
) {
    sessionFactory.transactionalSessionScope { session ->
        sikkerlogg.info("Utfører overstyring ${overstyring.loggnavn()} på vegne av saksbehandler $saksbehandler")
        session.saksbehandlerRepository.lagre(saksbehandler)

        val fødselsnummer = overstyring.fødselsnummer
        sikkerlogg.info("Reserverer person $fødselsnummer til saksbehandler $saksbehandler")
        session.reservasjonDao.reserverPerson(saksbehandler.id().value, fødselsnummer)

        val totrinnsvurdering =
            session.totrinnsvurderingRepository.finnAktivForPerson(fødselsnummer) ?: Totrinnsvurdering.ny(fødselsnummer)
        totrinnsvurdering.nyOverstyring(overstyring)
        session.totrinnsvurderingRepository.lagre(totrinnsvurdering)
    }
}
