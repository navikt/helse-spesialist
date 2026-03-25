package no.nav.helse.spesialist.api.graphql

import no.nav.helse.MeldingPubliserer
import no.nav.helse.db.Daos
import no.nav.helse.db.SessionContext
import no.nav.helse.db.SessionFactory
import no.nav.helse.db.VedtakBegrunnelseFraDatabase
import no.nav.helse.db.VedtakBegrunnelseTypeFraDatabase
import no.nav.helse.db.api.VedtaksperiodeDbDto.Companion.harAktiveVarsler
import no.nav.helse.mediator.Subsumsjonsmelder
import no.nav.helse.mediator.oppgave.OppgaveService
import no.nav.helse.mediator.overstyring.Saksbehandlingsmelder
import no.nav.helse.modell.FinnerIkkePåVent
import no.nav.helse.modell.ManglerTilgang
import no.nav.helse.modell.Modellfeil
import no.nav.helse.modell.OppgaveAlleredeSendtBeslutter
import no.nav.helse.modell.OppgaveAlleredeSendtIRetur
import no.nav.helse.modell.OppgaveKreverVurderingAvToSaksbehandlere
import no.nav.helse.modell.OppgaveTildeltNoenAndre
import no.nav.helse.modell.periodehistorikk.Historikkinnslag
import no.nav.helse.modell.saksbehandler.handlinger.Handling
import no.nav.helse.modell.saksbehandler.handlinger.Oppgavehandling
import no.nav.helse.modell.saksbehandler.handlinger.Personhandling
import no.nav.helse.modell.vedtak.Utfall
import no.nav.helse.modell.vilkårsprøving.Lovhjemmel
import no.nav.helse.spesialist.api.graphql.schema.ApiArbeidsforholdOverstyringHandling
import no.nav.helse.spesialist.api.graphql.schema.ApiInntektOgRefusjonOverstyring
import no.nav.helse.spesialist.api.graphql.schema.ApiSkjonnsfastsettelse
import no.nav.helse.spesialist.api.graphql.schema.ApiSkjonnsfastsettelse.ApiSkjonnsfastsettelseArbeidsgiver.ApiSkjonnsfastsettelseType.ANNET
import no.nav.helse.spesialist.api.graphql.schema.ApiSkjonnsfastsettelse.ApiSkjonnsfastsettelseArbeidsgiver.ApiSkjonnsfastsettelseType.OMREGNET_ARSINNTEKT
import no.nav.helse.spesialist.api.graphql.schema.ApiSkjonnsfastsettelse.ApiSkjonnsfastsettelseArbeidsgiver.ApiSkjonnsfastsettelseType.RAPPORTERT_ARSINNTEKT
import no.nav.helse.spesialist.api.graphql.schema.ApiTidslinjeOverstyring
import no.nav.helse.spesialist.api.saksbehandler.handlinger.AvmeldOppgave
import no.nav.helse.spesialist.api.saksbehandler.handlinger.HandlingFraApi
import no.nav.helse.spesialist.api.saksbehandler.handlinger.TildelOppgave
import no.nav.helse.spesialist.api.tildeling.TildelingApiDto
import no.nav.helse.spesialist.application.logg.MdcKey
import no.nav.helse.spesialist.application.logg.loggError
import no.nav.helse.spesialist.application.logg.loggInfo
import no.nav.helse.spesialist.domain.Saksbehandler
import no.nav.helse.spesialist.domain.SaksbehandlerOid
import no.nav.helse.spesialist.domain.SpleisBehandlingId
import no.nav.helse.spesialist.domain.Totrinnsvurdering
import no.nav.helse.spesialist.domain.legacy.SaksbehandlerWrapper
import no.nav.helse.spesialist.domain.oppgave.OppgaveId
import no.nav.helse.spesialist.domain.overstyringer.Arbeidsforhold
import no.nav.helse.spesialist.domain.overstyringer.Overstyring
import no.nav.helse.spesialist.domain.overstyringer.OverstyrtArbeidsforhold
import no.nav.helse.spesialist.domain.overstyringer.OverstyrtArbeidsgiver
import no.nav.helse.spesialist.domain.overstyringer.OverstyrtInntektOgRefusjon
import no.nav.helse.spesialist.domain.overstyringer.OverstyrtTidslinje
import no.nav.helse.spesialist.domain.overstyringer.OverstyrtTidslinjedag
import no.nav.helse.spesialist.domain.overstyringer.Refusjonselement
import no.nav.helse.spesialist.domain.overstyringer.SkjønnsfastsattArbeidsgiver
import no.nav.helse.spesialist.domain.overstyringer.SkjønnsfastsattSykepengegrunnlag
import no.nav.helse.spesialist.domain.tilgangskontroll.Brukerrolle
import no.nav.helse.tell
import java.util.UUID
import no.nav.helse.spesialist.api.graphql.Modellfeil as ApiModellfeil

class SaksbehandlerMediator(
    daos: Daos,
    private val versjonAvKode: String,
    private val meldingPubliserer: MeldingPubliserer,
    private val oppgaveService: OppgaveService,
    private val sessionFactory: SessionFactory,
) {
    private val behandlingRepository = daos.behandlingApiRepository
    private val vedtakBegrunnelseDao = daos.vedtakBegrunnelseDao

    fun finnMdcParametreForOppgaveId(oppgaveId: String): List<Pair<MdcKey, String?>> =
        sessionFactory.transactionalSessionScope { transaction ->
            val oppgave = transaction.oppgaveRepository.finn(oppgaveId.toLong())

            val spleisBehandlingId = oppgave?.behandlingId
            val vedtaksperiodeId = spleisBehandlingId?.let(transaction.behandlingRepository::finn)?.vedtaksperiodeId
            val identitetsnummer =
                vedtaksperiodeId
                    ?.let(transaction.vedtaksperiodeRepository::finn)
                    ?.identitetsnummer

            listOf(
                MdcKey.SPLEIS_BEHANDLING_ID to spleisBehandlingId?.value?.toString(),
                MdcKey.VEDTAKSPERIODE_ID to vedtaksperiodeId?.value?.toString(),
                MdcKey.IDENTITETSNUMMER to identitetsnummer?.value,
            )
        }

    fun håndter(
        handlingFraApi: HandlingFraApi,
        saksbehandler: Saksbehandler,
        brukerroller: Set<Brukerrolle>,
    ) {
        val modellhandling =
            handlingFraApi.tilModellversjon(
                saksbehandlerOid = saksbehandler.id,
                brukerroller = brukerroller,
            )
        sessionFactory.transactionalSessionScope { it.saksbehandlerRepository.lagre(saksbehandler) }
        tell(modellhandling)
        val saksbehandlerWrapper = SaksbehandlerWrapper(saksbehandler = saksbehandler)
        saksbehandlerWrapper.register(Saksbehandlingsmelder(meldingPubliserer))
        saksbehandlerWrapper.register(Subsumsjonsmelder(versjonAvKode, meldingPubliserer))

        loggInfo(
            "Utfører handling ${modellhandling.loggnavn()} på vegne av saksbehandler",
            "saksbehandlerIdent" to saksbehandler.ident.value,
        )
        when (modellhandling) {
            is Overstyring -> {
                overstyringUnitOfWork(
                    loggnavn = modellhandling.loggnavn(),
                    overstyring = modellhandling,
                    saksbehandler = saksbehandler,
                    sessionFactory = sessionFactory,
                )
                modellhandling.utførAv(saksbehandlerWrapper)
            }

            is Oppgavehandling -> {
                håndter(modellhandling, saksbehandlerWrapper)
            }

            is Personhandling -> {
                håndter(modellhandling, saksbehandlerWrapper)
            }

            else -> {
                modellhandling.utførAv(saksbehandlerWrapper)
            }
        }
        loggInfo("Handling ${modellhandling.loggnavn()} utført")
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

    private fun håndter(
        handling: Oppgavehandling,
        saksbehandlerWrapper: SaksbehandlerWrapper,
    ) {
        try {
            oppgaveService.utfør(handling, saksbehandlerWrapper)
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
        loggInfo(
            "Oppgave sendes i retur av beslutter",
            "oppgaveId" to oppgavereferanse,
            "beslutterIdent" to besluttendeSaksbehandler.ident.value,
        )

        sessionFactory.transactionalSessionScope { session ->
            val oppgave = session.oppgaveRepository.finn(OppgaveId(oppgavereferanse)) ?: error("Fant ikke oppgave")
            val vedtaksperiode = session.vedtaksperiodeRepository.finn(oppgave.vedtaksperiodeId) ?: error("Fant ikke vedtaksperiode")
            try {
                val fødselsnummer = vedtaksperiode.identitetsnummer.value
                val totrinnsvurdering = session.totrinnsvurderingRepository.finnAktivForPerson(fødselsnummer)
                checkNotNull(totrinnsvurdering) {
                    "Forventer at det eksisterer en aktiv totrinnsvurdering når oppgave sendes i retur"
                }
                val opprinneligSaksbehandler =
                    checkNotNull(
                        totrinnsvurdering.saksbehandler
                            ?.let(session.saksbehandlerRepository::finn),
                    ) {
                        "Opprinnelig saksbehandler kan ikke være null ved retur av beslutteroppgave"
                    }

                oppgave.sendIRetur(opprinneligSaksbehandler)
                totrinnsvurdering.sendIRetur(oppgavereferanse, besluttendeSaksbehandler.id)
                session.oppgaveRepository.lagre(oppgave)
                session.totrinnsvurderingRepository.lagre(totrinnsvurdering)
            } catch (modellfeil: Modellfeil) {
                return@transactionalSessionScope SendIReturResult.Feil.KunneIkkeSendeIRetur(modellfeil.tilApiversjon())
            }
            try {
                val påVent = session.påVentRepository.finnFor(oppgave.vedtaksperiodeId)
                if (påVent != null) {
                    oppgave.fjernFraPåVent()
                    session.påVentRepository.slett(påVent.id())
                    session.oppgaveRepository.lagre(oppgave)
                }
            } catch (modellfeil: ApiModellfeil) {
                return@transactionalSessionScope SendIReturResult.Feil.KunneIkkeLeggePåVent(modellfeil)
            }
            try {
                val dialogRef = session.dialogDao.lagre()
                val innslag =
                    Historikkinnslag.totrinnsvurderingRetur(
                        notattekst = notatTekst,
                        saksbehandler = besluttendeSaksbehandler,
                        dialogRef = dialogRef,
                    )
                session.periodehistorikkDao.lagreMedOppgaveId(innslag, oppgavereferanse)
            } catch (e: Exception) {
                return@transactionalSessionScope SendIReturResult.Feil.KunneIkkeOppretteHistorikkinnslag(e)
            }
        }

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

            val oppgave = sessionContext.oppgaveRepository.finn(OppgaveId(oppgavereferanse)) ?: error("Fant ikke oppgave")
            val vedtaksperiode = sessionContext.vedtaksperiodeRepository.finn(oppgave.vedtaksperiodeId) ?: error("Fant ikke vedtaksperiode")
            try {
                håndterVedtakBegrunnelse(
                    utfall = hentUtfallFraBehandling(oppgave.behandlingId.value, sessionContext),
                    begrunnelse = begrunnelse,
                    oppgaveId = oppgavereferanse,
                    saksbehandlerOid = saksbehandler.id.value,
                )
            } catch (e: Exception) {
                return@transactionalSessionScope SendTilGodkjenningResult.Feil.KunneIkkeHåndtereBegrunnelse(e)
            }

            try {
                val fødselsnummer = vedtaksperiode.identitetsnummer.value
                val totrinnsvurdering = sessionContext.totrinnsvurderingRepository.finnAktivForPerson(fødselsnummer)
                checkNotNull(totrinnsvurdering) {
                    "Forventer at det eksisterer en aktiv totrinnsvurdering når oppgave sendes til beslutter"
                }

                val beslutter =
                    totrinnsvurdering.beslutter
                        ?.let(sessionContext.saksbehandlerRepository::finn)
                oppgave.sendTilBeslutter(beslutter)
                sessionContext.oppgaveRepository.lagre(oppgave)
                totrinnsvurdering.sendTilBeslutter(oppgavereferanse, saksbehandler.id)
                sessionContext.totrinnsvurderingRepository.lagre(totrinnsvurdering)
            } catch (modellfeil: Modellfeil) {
                loggError("Feil ved sending til beslutter", modellfeil)
                return@transactionalSessionScope SendTilGodkjenningResult.Feil.KunneIkkeSendeTilBeslutter(modellfeil.tilApiversjon())
            } catch (e: Exception) {
                loggError("Feil ved sending til beslutter", e)
                return@transactionalSessionScope SendTilGodkjenningResult.Feil.UventetFeilVedSendigTilBeslutter(e)
            }

            try {
                val påVent = sessionContext.påVentRepository.finnFor(oppgave.vedtaksperiodeId)
                if (påVent != null) {
                    oppgave.fjernFraPåVent()
                    sessionContext.påVentRepository.slett(påVent.id())
                    sessionContext.oppgaveRepository.lagre(oppgave)
                }
            } catch (modellfeil: ApiModellfeil) {
                return@transactionalSessionScope SendTilGodkjenningResult.Feil.KunneIkkeFjerneFraPåVent(modellfeil)
            } catch (e: Exception) {
                return@transactionalSessionScope SendTilGodkjenningResult.Feil.UventetFeilVedFjernFraPåVent(e)
            }

            loggInfo(
                "Oppgave sendes til godkjenning av saksbehandler",
                "oppgaveId" to oppgavereferanse,
                "saksbehandlerIdent" to saksbehandler.ident.value,
            )

            try {
                val innslag = Historikkinnslag.avventerTotrinnsvurdering(saksbehandler)
                sessionContext.periodehistorikkDao.lagreMedOppgaveId(innslag, oppgavereferanse)
            } catch (e: Exception) {
                return@transactionalSessionScope SendTilGodkjenningResult.Feil.UventetFeilVedOpprettingAvPeriodehistorikk(
                    e,
                )
            }

            loggInfo(
                "Oppgave sendes til godkjenning av saksbehandler",
                "oppgaveId" to oppgavereferanse,
                "saksbehandlerIdent" to saksbehandler.ident.value,
            )

            return@transactionalSessionScope SendTilGodkjenningResult.Ok
        }

    private fun Modellfeil.tilApiversjon(): ApiModellfeil =
        when (this) {
            is no.nav.helse.modell.OppgaveIkkeTildelt -> {
                OppgaveIkkeTildelt(oppgaveId)
            }

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
                OppgaveTildeltNoenAndre(
                    TildelingApiDto(
                        saksbehandler.saksbehandler.navn,
                        saksbehandler.saksbehandler.epost,
                        saksbehandler.saksbehandler.id.value,
                    ),
                )
            }

            is OppgaveAlleredeSendtBeslutter -> {
                no.nav.helse.spesialist.api.graphql.OppgaveAlleredeSendtBeslutter(
                    oppgaveId,
                )
            }

            is OppgaveAlleredeSendtIRetur -> {
                no.nav.helse.spesialist.api.graphql.OppgaveAlleredeSendtIRetur(
                    oppgaveId,
                )
            }

            is OppgaveKreverVurderingAvToSaksbehandlere -> {
                no.nav.helse.spesialist.api.graphql.OppgaveKreverVurderingAvToSaksbehandlere(
                    oppgaveId,
                )
            }

            is ManglerTilgang -> {
                IkkeTilgang(oid, oppgaveId)
            }

            is FinnerIkkePåVent -> {
                FinnerIkkeLagtPåVent(oppgaveId)
            }
        }

    private fun HandlingFraApi.tilModellversjon(
        saksbehandlerOid: SaksbehandlerOid,
        brukerroller: Set<Brukerrolle>,
    ): Handling =
        when (this) {
            is ApiArbeidsforholdOverstyringHandling -> this.tilModellversjon(saksbehandlerOid)
            is ApiInntektOgRefusjonOverstyring -> this.tilModellversjon(saksbehandlerOid)
            is ApiTidslinjeOverstyring -> this.tilModellversjon(saksbehandlerOid)
            is ApiSkjonnsfastsettelse -> this.tilModellversjon(saksbehandlerOid)
            is TildelOppgave -> this.tilModellversjon(brukerroller)
            is AvmeldOppgave -> this.tilModellversjon()
            else -> throw IllegalStateException("Støtter ikke handling ${this::class.simpleName}")
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

    private fun TildelOppgave.tilModellversjon(
        brukerroller: Set<Brukerrolle>,
    ): no.nav.helse.modell.saksbehandler.handlinger.TildelOppgave =
        no.nav.helse.modell.saksbehandler.handlinger
            .TildelOppgave(this.oppgaveId, brukerroller)

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
    loggnavn: String,
) {
    sessionFactory.transactionalSessionScope { session ->
        overstyring.loggInfo(
            "Utfører overstyring $loggnavn på vegne av saksbehandler",
            "saksbehandlerIdent" to saksbehandler.ident.value,
        )
        session.saksbehandlerRepository.lagre(saksbehandler)

        val fødselsnummer = overstyring.fødselsnummer
        overstyring.loggInfo(
            "Reserverer person til saksbehandler",
            "fødselsnummer" to fødselsnummer,
            "saksbehandlerIdent" to saksbehandler.ident.value,
        )
        session.reservasjonDao.reserverPerson(saksbehandler.id.value, fødselsnummer)

        val totrinnsvurdering =
            session.totrinnsvurderingRepository.finnAktivForPerson(fødselsnummer) ?: Totrinnsvurdering.ny(fødselsnummer)
        totrinnsvurdering.nyOverstyring(overstyring)
        session.totrinnsvurderingRepository.lagre(totrinnsvurdering)
    }
}
