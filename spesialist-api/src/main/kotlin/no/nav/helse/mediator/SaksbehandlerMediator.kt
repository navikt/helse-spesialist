package no.nav.helse.mediator

import net.logstash.logback.argument.StructuredArguments
import no.nav.helse.FeatureToggles
import no.nav.helse.MeldingPubliserer
import no.nav.helse.bootstrap.Environment
import no.nav.helse.db.AnnulleringRepository
import no.nav.helse.db.Daos
import no.nav.helse.db.OpptegnelseDao
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
import no.nav.helse.modell.saksbehandler.Saksbehandler
import no.nav.helse.modell.saksbehandler.Saksbehandler.Companion.gjenopprett
import no.nav.helse.modell.saksbehandler.Saksbehandler.Companion.toDto
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
import no.nav.helse.modell.vilkårsprøving.Lovhjemmel
import no.nav.helse.spesialist.api.Saksbehandlerhåndterer
import no.nav.helse.spesialist.api.SendIReturResult
import no.nav.helse.spesialist.api.SendTilGodkjenningResult
import no.nav.helse.spesialist.api.bootstrap.Tilgangsgrupper
import no.nav.helse.spesialist.api.feilhåndtering.FinnerIkkeLagtPåVent
import no.nav.helse.spesialist.api.feilhåndtering.IkkeTilgang
import no.nav.helse.spesialist.api.feilhåndtering.ManglerVurderingAvVarsler
import no.nav.helse.spesialist.api.feilhåndtering.OppgaveIkkeTildelt
import no.nav.helse.spesialist.api.graphql.mutation.Avslagshandling
import no.nav.helse.spesialist.api.graphql.mutation.VedtakMutationHandler.VedtakResultat
import no.nav.helse.spesialist.api.graphql.schema.ApiAnnulleringData
import no.nav.helse.spesialist.api.graphql.schema.ApiArbeidsforholdOverstyringHandling
import no.nav.helse.spesialist.api.graphql.schema.ApiAvslag
import no.nav.helse.spesialist.api.graphql.schema.ApiAvslagstype
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
import no.nav.helse.spesialist.api.graphql.schema.ApiVedtakBegrunnelse
import no.nav.helse.spesialist.api.graphql.schema.ApiVedtakUtfall
import no.nav.helse.spesialist.api.saksbehandler.SaksbehandlerFraApi
import no.nav.helse.spesialist.api.saksbehandler.handlinger.ApiOpphevStans
import no.nav.helse.spesialist.api.saksbehandler.handlinger.AvmeldOppgave
import no.nav.helse.spesialist.api.saksbehandler.handlinger.HandlingFraApi
import no.nav.helse.spesialist.api.saksbehandler.handlinger.TildelOppgave
import no.nav.helse.spesialist.api.tildeling.TildelingApiDto
import no.nav.helse.spesialist.api.vedtak.GodkjenningDto
import no.nav.helse.spesialist.application.TotrinnsvurderingRepository
import no.nav.helse.spesialist.domain.SaksbehandlerOid
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
    private val env: Environment,
    private val featureToggles: FeatureToggles,
    private val sessionFactory: SessionFactory,
    private val tilgangskontroll: Tilgangskontroll,
) : Saksbehandlerhåndterer {
    private val generasjonRepository = daos.generasjonApiRepository
    private val varselRepository = daos.varselApiRepository
    private val oppgaveApiDao = daos.oppgaveApiDao
    private val opptegnelseRepository = daos.opptegnelseDao
    private val abonnementDao = daos.abonnementApiDao
    private val reservasjonDao = daos.reservasjonDao
    private val påVentDao = daos.påVentDao
    private val periodehistorikkDao = daos.periodehistorikkDao
    private val vedtakBegrunnelseDao = daos.vedtakBegrunnelseDao
    private val dialogDao = daos.dialogDao

    override fun håndter(
        handlingFraApi: HandlingFraApi,
        saksbehandlerFraApi: SaksbehandlerFraApi,
    ) {
        val saksbehandler = saksbehandlerFraApi.tilSaksbehandler()
        val modellhandling = handlingFraApi.tilModellversjon(saksbehandler.id().value)
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
                is Overstyring -> håndter(modellhandling, legacySaksbehandler)
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

    override fun vedtak(
        saksbehandlerFraApi: SaksbehandlerFraApi,
        oppgavereferanse: Long,
        godkjent: Boolean,
        avslag: no.nav.helse.spesialist.api.graphql.mutation.Avslag?,
    ): VedtakResultat {
        val legacySaksbehandler =
            saksbehandlerFraApi.tilSaksbehandler().tilLegacySaksbehandler(saksbehandlerFraApi.grupper)
        return vedtak(oppgavereferanse, legacySaksbehandler, godkjent).also {
            håndterAvslag(
                avslag = avslag,
                oppgaveId = oppgavereferanse,
                saksbehandlerOid = legacySaksbehandler.oid(),
            )
        }
    }

    override fun vedtak(
        saksbehandlerFraApi: SaksbehandlerFraApi,
        oppgavereferanse: Long,
        godkjent: Boolean,
        utfall: ApiVedtakUtfall,
        begrunnelse: String?,
    ): VedtakResultat {
        val legacySaksbehandler =
            saksbehandlerFraApi.tilSaksbehandler().tilLegacySaksbehandler(saksbehandlerFraApi.grupper)
        return vedtak(oppgavereferanse, legacySaksbehandler, godkjent).also {
            håndterVedtakBegrunnelse(
                utfall = utfall,
                begrunnelse = begrunnelse,
                oppgaveId = oppgavereferanse,
                saksbehandlerOid = legacySaksbehandler.oid(),
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
            saksbehandlerFraApi.tilSaksbehandler().tilLegacySaksbehandler(saksbehandlerFraApi.grupper),
            godkjent,
        )

    private fun eksisterendeTotrinnsvurdering(
        vedtaksperiodeId: UUID,
        fødselsnummer: String,
        totrinnsvurderingRepository: TotrinnsvurderingRepository,
    ): Totrinnsvurdering? {
        return if (featureToggles.skalBenytteNyTotrinnsvurderingsløsning()) {
            totrinnsvurderingRepository.finn(fødselsnummer)
        } else {
            totrinnsvurderingRepository.finn(vedtaksperiodeId)
        }
    }

    private fun vedtak(
        oppgavereferanse: Long,
        legacySaksbehandler: Saksbehandler,
        godkjent: Boolean,
    ): VedtakResultat {
        val erÅpenOppgave = apiOppgaveService.venterPåSaksbehandler(oppgavereferanse)
        val spleisBehandlingId = apiOppgaveService.spleisBehandlingId(oppgavereferanse)
        val vedtaksperiodeId = oppgaveService.finnVedtaksperiodeId(oppgavereferanse)
        val fødselsnummer = oppgaveApiDao.finnFødselsnummer(oppgavereferanse)
        if (!erÅpenOppgave) return VedtakResultat.Feil.IkkeÅpenOppgave()

        return sessionFactory.transactionalSessionScope { sessionContext ->
            val totrinnsvurderingRepository = sessionContext.totrinnsvurderingRepository
            val totrinnsvurdering =
                eksisterendeTotrinnsvurdering(vedtaksperiodeId, fødselsnummer, totrinnsvurderingRepository)
            if (totrinnsvurdering?.erBeslutteroppgave == true) {
                if (!legacySaksbehandler.harTilgangTil(listOf(Egenskap.BESLUTTER)) && !env.erDev) {
                    return@transactionalSessionScope VedtakResultat.Feil.BeslutterFeil.TrengerBeslutterRolle()
                }
                if (totrinnsvurdering.saksbehandler?.value == legacySaksbehandler.oid && !env.erDev) {
                    return@transactionalSessionScope VedtakResultat.Feil.BeslutterFeil.KanIkkeBeslutteEgenOppgave()
                }
                totrinnsvurdering.settBeslutter(SaksbehandlerOid(legacySaksbehandler.oid))
                totrinnsvurderingRepository.lagre(totrinnsvurdering, fødselsnummer)
            }

            if (godkjent) {
                val perioderTilBehandling = generasjonRepository.perioderTilBehandling(oppgavereferanse)
                if (perioderTilBehandling.harAktiveVarsler()) {
                    return@transactionalSessionScope VedtakResultat.Feil.HarAktiveVarsler(
                        oppgavereferanse,
                    )
                }

                perioderTilBehandling.godkjennVarsler(
                    fødselsnummer = fødselsnummer,
                    behandlingId = spleisBehandlingId,
                    ident = legacySaksbehandler.ident(),
                    godkjenner = this::vurderVarsel,
                )
            } else {
                val periodeTilGodkjenning = generasjonRepository.periodeTilGodkjenning(oppgavereferanse)
                periodeTilGodkjenning.avvisVarsler(
                    fødselsnummer = fødselsnummer,
                    behandlingId = spleisBehandlingId,
                    ident = legacySaksbehandler.ident(),
                    godkjenner = this::vurderVarsel,
                )
            }

            påVentDao.slettPåVent(oppgavereferanse)
            return@transactionalSessionScope VedtakResultat.Ok(spleisBehandlingId)
        }
    }

    override fun påVent(
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
        saksbehandler: Saksbehandler,
    ) {
        try {
            if (annulleringRepository.finnAnnullering(handling.toDto()) != null) throw AlleredeAnnullert(handling)
            annulleringRepository.lagreAnnullering(handling.toDto(), saksbehandler)
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
            oppgaveService.avbrytOppgave(handling, saksbehandler)
        } catch (e: Modellfeil) {
            throw e.tilApiversjon()
        }
    }

    private fun håndter(
        handling: OpphevStans,
        saksbehandler: Saksbehandler,
    ) = try {
        stansAutomatiskBehandlinghåndterer.håndter(handling, saksbehandler)
        handling.utførAv(saksbehandler)
    } catch (e: Modellfeil) {
        throw e.tilApiversjon()
    }

    private fun håndter(
        handling: Personhandling,
        saksbehandler: Saksbehandler,
    ) = try {
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
                Historikkinnslag.lagtPåVentInnslag(
                    notattekst = handling.notatTekst,
                    saksbehandler = saksbehandler.toDto(),
                    årsaker = handling.årsaker,
                    frist = handling.frist,
                    dialogRef = dialogRef,
                )
            periodehistorikkDao.lagreMedOppgaveId(innslag, handling.oppgaveId)
            oppgaveService.leggPåVent(handling, saksbehandler)
            PåVentRepository(påVentDao).leggPåVent(saksbehandler.oid(), handling, dialogRef)
            saksbehandler.register(Saksbehandlingsmelder(meldingPubliserer))
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
            Historikkinnslag.endrePåVentInnslag(
                notattekst = handling.notatTekst,
                saksbehandler = saksbehandler.toDto(),
                årsaker = handling.årsaker,
                frist = handling.frist,
                dialogRef = dialogRef,
            )
        periodehistorikkDao.lagreMedOppgaveId(innslag, handling.oppgaveId)
        oppgaveService.endrePåVent(handling, saksbehandler)
        PåVentRepository(påVentDao).endrePåVent(saksbehandler.oid(), handling, dialogRef)

        saksbehandler.register(Saksbehandlingsmelder(meldingPubliserer))
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
            val innslag = Historikkinnslag.fjernetFraPåVentInnslag(saksbehandler.toDto())
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
        val fødselsnummer = handling.fødselsnummer
        reservasjonDao.reserverPerson(saksbehandler.oid(), fødselsnummer)
        sikkerlogg.info("Reserverer person $fødselsnummer til saksbehandler $saksbehandler")
        sessionFactory.transactionalSessionScope { session ->
            if (featureToggles.skalBenytteNyTotrinnsvurderingsløsning()) {
                val totrinnsvurdering =
                    session.totrinnsvurderingRepository.finn(handling.fødselsnummer)
                        ?: Totrinnsvurdering.ny(handling.vedtaksperiodeId)
                totrinnsvurdering.settSaksbehandler(SaksbehandlerOid(saksbehandler.oid))
                totrinnsvurdering.nyOverstyring(handling)
                session.totrinnsvurderingRepository.lagre(totrinnsvurdering, fødselsnummer)
            } else {
                session.overstyringRepository.lagre(listOf(handling))
            }
        }
        handling.utførAv(saksbehandler)
    }

    override fun opprettAbonnement(
        saksbehandlerFraApi: SaksbehandlerFraApi,
        personidentifikator: String,
    ) {
        val saksbehandler = saksbehandlerFraApi.tilSaksbehandler()
        sessionFactory.transactionalSessionScope { it.saksbehandlerRepository.lagre(saksbehandler) }
        abonnementDao.opprettAbonnement(saksbehandler.id().value, personidentifikator)
    }

    override fun hentAbonnerteOpptegnelser(
        saksbehandlerFraApi: SaksbehandlerFraApi,
        sisteSekvensId: Int,
    ): List<ApiOpptegnelse> {
        val saksbehandler = saksbehandlerFraApi.tilSaksbehandler()
        sessionFactory.transactionalSessionScope { it.saksbehandlerRepository.lagre(saksbehandler) }
        abonnementDao.registrerSistekvensnummer(saksbehandler.id().value, sisteSekvensId)
        return opptegnelseRepository.finnOpptegnelser(saksbehandler.id().value).toApiOpptegnelser()
    }

    override fun hentAbonnerteOpptegnelser(saksbehandlerFraApi: SaksbehandlerFraApi): List<ApiOpptegnelse> =
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

    override fun hentAvslag(
        vedtaksperiodeId: UUID,
        utbetalingId: UUID,
    ): Set<ApiAvslag> =
        vedtakBegrunnelseDao.finnAlleVedtakBegrunnelser(vedtaksperiodeId, utbetalingId)
            .mapNotNull { vedtakBegrunnelse ->
                when (vedtakBegrunnelse.type) {
                    VedtakBegrunnelseTypeFraDatabase.AVSLAG -> ApiAvslagstype.AVSLAG
                    VedtakBegrunnelseTypeFraDatabase.DELVIS_INNVILGELSE -> ApiAvslagstype.DELVIS_AVSLAG
                    VedtakBegrunnelseTypeFraDatabase.INNVILGELSE -> null
                }?.let { type ->
                    ApiAvslag(
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
    ): List<ApiVedtakBegrunnelse> =
        vedtakBegrunnelseDao.finnAlleVedtakBegrunnelser(vedtaksperiodeId, utbetalingId)
            .map { vedtakBegrunnelse ->
                ApiVedtakBegrunnelse(
                    utfall =
                        when (vedtakBegrunnelse.type) {
                            VedtakBegrunnelseTypeFraDatabase.AVSLAG -> ApiVedtakUtfall.AVSLAG
                            VedtakBegrunnelseTypeFraDatabase.DELVIS_INNVILGELSE -> ApiVedtakUtfall.DELVIS_INNVILGELSE
                            VedtakBegrunnelseTypeFraDatabase.INNVILGELSE -> ApiVedtakUtfall.INNVILGELSE
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
        utfall: ApiVedtakUtfall,
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
                    begrunnelse = avslag.data.begrunnelse,
                    saksbehandlerOid = saksbehandlerOid,
                )
            }
        }
    }

    private fun håndterVedtakBegrunnelse(
        utfall: ApiVedtakUtfall,
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
    ): no.nav.helse.modell.Annullering? =
        annulleringRepository.finnAnnullering(
            arbeidsgiverFagsystemId = arbeidsgiverFagsystemId,
            personFagsystemId = personFagsystemId,
        )

    override fun sendIRetur(
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
                val vedtaksperiodeId = oppgaveService.finnVedtaksperiodeId(oppgavereferanse)
                val fødselsnummer = oppgaveService.finnFødselsnummer(oppgavereferanse)
                val totrinnsvurdering =
                    if (featureToggles.skalBenytteNyTotrinnsvurderingsløsning()) {
                        session.totrinnsvurderingRepository.finn(fødselsnummer)
                    } else {
                        session.totrinnsvurderingRepository.finn(vedtaksperiodeId)
                    }
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
                session.totrinnsvurderingRepository.lagre(totrinnsvurdering, fødselsnummer)
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

    override fun håndterTotrinnsvurdering(
        oppgavereferanse: Long,
        saksbehandlerFraApi: SaksbehandlerFraApi,
        utfall: ApiVedtakUtfall,
        begrunnelse: String?,
    ): SendTilGodkjenningResult {
        try {
            val perioderTilBehandling = generasjonRepository.perioderTilBehandling(oppgavereferanse)
            if (perioderTilBehandling.harAktiveVarsler()) {
                return SendTilGodkjenningResult.Feil.ManglerVurderingAvVarsler(
                    ManglerVurderingAvVarsler(
                        oppgavereferanse,
                    ),
                )
            }
        } catch (e: Exception) {
            return SendTilGodkjenningResult.Feil.KunneIkkeFinnePerioderTilBehandling(e)
        }

        try {
            håndterVedtakBegrunnelse(
                utfall = utfall,
                begrunnelse = begrunnelse,
                oppgaveId = oppgavereferanse,
                saksbehandlerOid = saksbehandlerFraApi.oid,
            )
        } catch (e: Exception) {
            return SendTilGodkjenningResult.Feil.KunneIkkeHåndtereBegrunnelse(e)
        }

        try {
            sessionFactory.transactionalSessionScope { session ->
                val vedtaksperiodeId = oppgaveService.finnVedtaksperiodeId(oppgavereferanse)
                val fødselsnummer = oppgaveService.finnFødselsnummer(oppgavereferanse)
                val totrinnsvurdering =
                    if (featureToggles.skalBenytteNyTotrinnsvurderingsløsning()) {
                        session.totrinnsvurderingRepository.finn(fødselsnummer)
                    } else {
                        session.totrinnsvurderingRepository.finn(vedtaksperiodeId)
                    }

                checkNotNull(totrinnsvurdering) {
                    "Forventer at det eksisterer en aktiv totrinnsvurdering når oppgave sendes til beslutter"
                }

                val beslutter =
                    totrinnsvurdering.beslutter?.let(session.saksbehandlerRepository::finn)
                        ?.let(this::tilLegacySaksbehandler)
                apiOppgaveService.sendTilBeslutter(oppgavereferanse, beslutter)
                totrinnsvurdering.sendTilBeslutter(oppgavereferanse, SaksbehandlerOid(saksbehandlerFraApi.oid))
                session.totrinnsvurderingRepository.lagre(totrinnsvurdering, fødselsnummer)
            }
        } catch (modellfeil: Modellfeil) {
            return SendTilGodkjenningResult.Feil.KunneIkkeSendeTilBeslutter(modellfeil.tilApiversjon())
        } catch (e: Exception) {
            return SendTilGodkjenningResult.Feil.UventetFeilVedSendigTilBeslutter(e)
        }

        try {
            påVent(ApiPaVentRequest.ApiFjernPaVentUtenHistorikkinnslag(oppgavereferanse), saksbehandlerFraApi)
        } catch (modellfeil: no.nav.helse.spesialist.api.feilhåndtering.Modellfeil) {
            return SendTilGodkjenningResult.Feil.KunneIkkeFjerneFraPåVent(modellfeil)
        } catch (e: Exception) {
            return SendTilGodkjenningResult.Feil.UventetFeilVedFjernFraPåVent(e)
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
            return SendTilGodkjenningResult.Feil.UventetFeilVedOpprettingAvPeriodehistorikk(e)
        }

        log.info("OppgaveId $oppgavereferanse sendt til godkjenning")

        return SendTilGodkjenningResult.Ok
    }

    private fun tilLegacySaksbehandler(saksbehandler: no.nav.helse.spesialist.domain.Saksbehandler): Saksbehandler =
        saksbehandler.gjenopprett(tilgangskontroll = tilgangskontroll)

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
                saksbehandler.ident,
                this::vurderVarsel,
            )
        } else {
            val periodeTilGodkjenning = generasjonRepository.periodeTilGodkjenning(godkjenning.oppgavereferanse)
            periodeTilGodkjenning.avvisVarsler(fødselsnummer, behandlingId, saksbehandler.ident, this::vurderVarsel)
        }

        påVentDao.slettPåVent(godkjenning.oppgavereferanse)
        håndterAvslag(
            avslag = godkjenning.avslag,
            oppgaveId = godkjenning.oppgavereferanse,
            saksbehandlerOid = saksbehandler.id().value,
        )
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

            is AlleredeAnnullert ->
                no.nav.helse.spesialist.api.feilhåndtering
                    .AlleredeAnnullert(handling.toDto().vedtaksperiodeId)

            is FinnerIkkePåVent -> FinnerIkkeLagtPåVent(oppgaveId)
        }

    private fun SaksbehandlerFraApi.tilSaksbehandler(): no.nav.helse.spesialist.domain.Saksbehandler =
        no.nav.helse.spesialist.domain.Saksbehandler.Factory.fraLagring(
            id = SaksbehandlerOid(oid),
            navn = navn,
            epost = epost,
            ident = ident,
        )

    private fun no.nav.helse.spesialist.domain.Saksbehandler.tilLegacySaksbehandler(saksbehandlergrupper: List<UUID>): Saksbehandler =
        gjenopprett(TilgangskontrollørForApi(saksbehandlergrupper, tilgangsgrupper))

    private fun HandlingFraApi.tilModellversjon(saksbehandlerOid: UUID): Handling =
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

    private fun ApiArbeidsforholdOverstyringHandling.tilModellversjon(saksbehandlerOid: UUID): OverstyrtArbeidsforhold =
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

    private fun ApiSkjonnsfastsettelse.tilModellversjon(saksbehandlerOid: UUID): SkjønnsfastsattSykepengegrunnlag =
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

    private fun ApiMinimumSykdomsgrad.tilModellversjon(saksbehandlerOid: UUID): MinimumSykdomsgrad =
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

    private fun ApiInntektOgRefusjonOverstyring.tilModellversjon(saksbehandlerOid: UUID): OverstyrtInntektOgRefusjon =
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

    private fun ApiTidslinjeOverstyring.tilModellversjon(saksbehandlerOid: UUID): OverstyrtTidslinje =
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
        no.nav.helse.modell.saksbehandler.handlinger
            .TildelOppgave(this.oppgaveId)

    private fun AvmeldOppgave.tilModellversjon(): no.nav.helse.modell.saksbehandler.handlinger.AvmeldOppgave =
        no.nav.helse.modell.saksbehandler.handlinger
            .AvmeldOppgave(this.oppgaveId)

    private fun ApiOpphevStans.tilModellversjon(): OpphevStans = OpphevStans(this.fødselsnummer, this.begrunnelse)

    private fun ApiAvslagstype.toVedtakBegrunnelseTypeFraDatabase() =
        when (this) {
            ApiAvslagstype.AVSLAG -> VedtakBegrunnelseTypeFraDatabase.AVSLAG
            ApiAvslagstype.DELVIS_AVSLAG -> VedtakBegrunnelseTypeFraDatabase.DELVIS_INNVILGELSE
        }

    private fun ApiVedtakUtfall.toDatabaseType() =
        when (this) {
            ApiVedtakUtfall.AVSLAG -> VedtakBegrunnelseTypeFraDatabase.AVSLAG
            ApiVedtakUtfall.DELVIS_INNVILGELSE -> VedtakBegrunnelseTypeFraDatabase.DELVIS_INNVILGELSE
            ApiVedtakUtfall.INNVILGELSE -> VedtakBegrunnelseTypeFraDatabase.INNVILGELSE
        }

    private fun SaksbehandlerFraApi.toDto(): SaksbehandlerDto =
        SaksbehandlerDto(
            epostadresse = this.epost,
            oid = this.oid,
            navn = this.navn,
            ident = this.ident,
        )
}
