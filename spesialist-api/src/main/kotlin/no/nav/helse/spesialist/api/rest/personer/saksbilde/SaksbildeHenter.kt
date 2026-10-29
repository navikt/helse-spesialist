package no.nav.helse.spesialist.api.rest.personer.saksbilde

import io.ktor.utils.io.core.*
import no.nav.helse.db.Daos
import no.nav.helse.db.SessionContext
import no.nav.helse.db.VedtakBegrunnelseTypeFraDatabase
import no.nav.helse.mediator.asLocalDate
import no.nav.helse.mediator.isMissingOrNull
import no.nav.helse.modell.vilkårsprøving.InnrapportertInntekt
import no.nav.helse.spesialist.api.objectMapper
import no.nav.helse.spesialist.api.overstyring.*
import no.nav.helse.spesialist.api.periodehistorikk.PeriodehistorikkDto
import no.nav.helse.spesialist.api.periodehistorikk.PeriodehistorikkType
import no.nav.helse.spesialist.api.rest.saksbilde.*
import no.nav.helse.spesialist.application.*
import no.nav.helse.spesialist.application.logg.loggError
import no.nav.helse.spesialist.application.logg.loggInfo
import no.nav.helse.spesialist.application.snapshot.*
import no.nav.helse.spesialist.domain.*
import java.time.LocalDate
import java.util.*

class SnapshotKunneIkkeHentesException(
    cause: Throwable,
) : RuntimeException("Feil ved henting av snapshot for person", cause)

/**
 * Bygger saksbildet for en person. Tilgangskontroll og audit-logging er kallerens ansvar.
 */
class SaksbildeHenter(
    private val daos: Daos,
    private val apiOppgaveService: ApiOppgaveService,
    private val snapshothenter: Snapshothenter,
    private val personPseudoIdProvider: PersonPseudoIdProvider,
    personinfoHenter: PersoninfoHenter,
) {
    private val personinfoKlargjører = PersoninfoKlargjører(personinfoHenter)

    fun hent(
        personEntity: Person,
        transaction: SessionContext,
    ): ApiSaksbildePerson {
        val identitetsnummer = personEntity.id
        loggInfo("Personoppslag på person", "identitetsnummer" to identitetsnummer)
        klargjørPersoninfoOmNødvendig(personEntity, transaction, identitetsnummer)

        val oppgave = transaction.oppgaveRepository.finnAktivForPersonOrNull(identitetsnummer)

        val snapshot =
            try {
                loggInfo("Henter snapshot for person", "identitetsnummer" to identitetsnummer)
                snapshothenter.hentPerson(identitetsnummer.value)
            } catch (e: Exception) {
                loggError("Klarte ikke hente snapshot fra Spleis", e, "identitetsnummer" to identitetsnummer)
                throw SnapshotKunneIkkeHentesException(e)
            }

        if (snapshot.fodselsnummer != identitetsnummer.value) {
            error("Fikk snapshot for et annet identitetsnummer enn det som ble etterspurt")
        }

        val perioderSomSkalViseAktiveVarsler = daos.varselApiRepository.perioderSomSkalViseVarsler(oppgave?.id?.value)

        return mapTilApiPerson(
            personEntity = personEntity,
            identitetsnummer = identitetsnummer,
            transaction = transaction,
            snapshot = snapshot,
            perioderSomSkalViseAktiveVarsler = perioderSomSkalViseAktiveVarsler,
        )
    }

    private fun klargjørPersoninfoOmNødvendig(
        personEntity: Person,
        transaction: SessionContext,
        identitetsnummer: Identitetsnummer,
    ) {
        if (personEntity.info != null) return

        when (val resultat = personinfoKlargjører.klargjør(personEntity)) {
            PersoninfoKlargjører.KlargjøringResultat.Klargjort -> {
                transaction.personRepository.lagre(personEntity)
            }

            PersoninfoKlargjører.KlargjøringResultat.IkkeFunnet -> {}

            is PersoninfoKlargjører.KlargjøringResultat.OppslagFeilet -> {
                loggError("Klarte ikke å hente personinfo", resultat.feil, "identitetsnummer" to identitetsnummer)
            }
        }
    }

    private fun mapTilApiPerson(
        personEntity: Person,
        identitetsnummer: Identitetsnummer,
        transaction: SessionContext,
        snapshot: SnapshotPerson,
        perioderSomSkalViseAktiveVarsler: Set<UUID>,
    ): ApiSaksbildePerson =
        ApiSaksbildePerson(
            aktorId = personEntity.aktørId,
            fodselsnummer = identitetsnummer.value,
            andreFodselsnummer = andreFødselsnumre(transaction, personEntity, identitetsnummer),
            dodsdato = snapshot.dodsdato,
            tilleggsinfoForInntektskilder =
                snapshot.vilkarsgrunnlag
                    .flatMap { vilkårsgrunnlag ->
                        transaction.avviksvurderingRepository
                            .hentAvviksvurdering(vilkårsgrunnlag.id)
                            ?.sammenligningsgrunnlag
                            ?.innrapporterteInntekter
                            ?.map(InnrapportertInntekt::arbeidsgiverreferanse)
                            .orEmpty()
                            .plus(vilkårsgrunnlag.inntekter.map(SnapshotArbeidsgiverinntekt::arbeidsgiver))
                    }.distinct()
                    .map { organisasjonsnummer ->
                        ApiTilleggsinfoForInntektskilde(
                            orgnummer = organisasjonsnummer,
                            navn = finnArbeidsgivernavn(organisasjonsnummer, transaction) ?: "navn er utilgjengelig",
                        )
                    },
            arbeidsgivere =
                run {
                    snapshot.arbeidsgivere
                        .filterNot { it.organisasjonsnummer == "SELVSTENDIG" }
                        .map { arbeidsgiver ->
                            ApiArbeidsgiver(
                                organisasjonsnummer = arbeidsgiver.organisasjonsnummer,
                                navn =
                                    finnArbeidsgivernavn(arbeidsgiver.organisasjonsnummer, transaction)
                                        ?: "navn er utilgjengelig",
                                ghostPerioder = arbeidsgiver.ghostPerioder.map { it.tilGhostPeriode(arbeidsgiver.organisasjonsnummer) },
                                behandlinger =
                                    arbeidsgiver.behandlinger.mapIndexed { behandlingIndex, behandling ->
                                        ApiBehandling(
                                            id = behandling.id,
                                            perioder =
                                                behandling.perioder.map { periode ->
                                                    when (periode) {
                                                        is SnapshotUberegnetPeriode -> {
                                                            periode.tilUberegnetPeriode(
                                                                behandlingIndex = behandlingIndex,
                                                                perioderSomSkalViseAktiveVarsler = perioderSomSkalViseAktiveVarsler,
                                                                transaction,
                                                            )
                                                        }

                                                        is SnapshotBeregnetPeriode -> {
                                                            val periodetilstand =
                                                                periode.periodetilstand.tilApiPeriodetilstand(
                                                                    behandlingIndex == 0,
                                                                )

                                                            ApiBeregnetPeriode(
                                                                behandlingId = periode.behandlingId,
                                                                erForkastet = periode.erForkastet,
                                                                fom = periode.fom,
                                                                tom = periode.tom,
                                                                id =
                                                                    genererPeriodeid(
                                                                        vedtaksperiodeId = periode.vedtaksperiodeId,
                                                                        behandlingIndex = behandlingIndex,
                                                                    ),
                                                                inntektstype = periode.inntektstype.tilApiInntektstype(),
                                                                opprettet = periode.opprettet,
                                                                periodetype = periode.tilApiPeriodetype(),
                                                                tidslinje = periode.tidslinje.map { it.tilApiDag() },
                                                                vedtaksperiodeId = periode.vedtaksperiodeId,
                                                                periodetilstand = periodetilstand,
                                                                skjaeringstidspunkt = periode.skjaeringstidspunkt,
                                                                grunnbelopsjustertGjennomsnitt = periode.grunnbeløpsjustertGjennomsnitt,
                                                                varsler =
                                                                    if (behandlingIndex == 0) {
                                                                        daos.varselApiRepository
                                                                            .finnVarslerSomIkkeErInaktiveForSisteBehandling(
                                                                                periode.vedtaksperiodeId,
                                                                                periode.utbetaling.id,
                                                                            ).map { it.toVarselDto() }
                                                                    } else {
                                                                        daos.varselApiRepository
                                                                            .finnVarslerSomIkkeErInaktiveFor(
                                                                                periode.vedtaksperiodeId,
                                                                                periode.utbetaling.id,
                                                                            ).map { it.toVarselDto() }
                                                                    },
                                                                hendelser = periode.hendelser.map { it.tilApiHendelse() },
                                                                egenskaper =
                                                                    apiOppgaveService.hentEgenskaper(
                                                                        periode.vedtaksperiodeId,
                                                                        periode.utbetaling.id,
                                                                    ),
                                                                historikkinnslag =
                                                                    daos.periodehistorikkApiDao
                                                                        .finn(
                                                                            utbetalingId = periode.utbetaling.id,
                                                                            spleisBehandlingId = periode.behandlingId,
                                                                        ).map { it.toApiHistorikkinnslag(transaction.dialogRepository) },
                                                                forbrukteSykedager = periode.forbrukteSykedager,
                                                                gjenstaendeSykedager = periode.gjenstaendeSykedager,
                                                                maksdato = periode.maksdato,
                                                                periodevilkar =
                                                                    ApiPeriodevilkar(
                                                                        alder =
                                                                            periode.periodevilkar.alder.let {
                                                                                ApiAlder(
                                                                                    alderSisteSykedag = it.alderSisteSykedag,
                                                                                    oppfylt = it.oppfylt,
                                                                                )
                                                                            },
                                                                        sykepengedager =
                                                                            periode.periodevilkar.sykepengedager.let {
                                                                                ApiSykepengedager(
                                                                                    forbrukteSykedager = it.forbrukteSykedager,
                                                                                    gjenstaendeSykedager = it.gjenstaendeSykedager,
                                                                                    maksdato = it.maksdato,
                                                                                    oppfylt = it.oppfylt,
                                                                                    skjaeringstidspunkt = it.skjaeringstidspunkt,
                                                                                )
                                                                            },
                                                                    ),
                                                                utbetaling = periode.utbetaling.tilApiUtbetaling(),
                                                                vilkarsgrunnlagId = periode.vilkarsgrunnlagId,
                                                                avslag =
                                                                    daos.vedtakBegrunnelseDao
                                                                        .finnAlleVedtakBegrunnelser(
                                                                            vedtaksperiodeId = periode.vedtaksperiodeId,
                                                                            utbetalingId = periode.utbetaling.id,
                                                                        ).filter {
                                                                            it.type in
                                                                                setOf(
                                                                                    VedtakBegrunnelseTypeFraDatabase.AVSLAG,
                                                                                    VedtakBegrunnelseTypeFraDatabase.DELVIS_INNVILGELSE,
                                                                                )
                                                                        }.map { vedtakBegrunnelse ->
                                                                            ApiAvslag(
                                                                                type =
                                                                                    when (vedtakBegrunnelse.type) {
                                                                                        VedtakBegrunnelseTypeFraDatabase.AVSLAG -> ApiAvslagstype.AVSLAG
                                                                                        VedtakBegrunnelseTypeFraDatabase.DELVIS_INNVILGELSE -> ApiAvslagstype.DELVIS_AVSLAG
                                                                                        else -> error("")
                                                                                    },
                                                                                begrunnelse = vedtakBegrunnelse.begrunnelse,
                                                                                opprettet = vedtakBegrunnelse.opprettet,
                                                                                saksbehandlerIdent = vedtakBegrunnelse.saksbehandlerIdent,
                                                                                invalidert = vedtakBegrunnelse.invalidert,
                                                                            )
                                                                        },
                                                                vedtakBegrunnelser =
                                                                    daos.vedtakBegrunnelseDao
                                                                        .finnAlleVedtakBegrunnelser(
                                                                            vedtaksperiodeId = periode.vedtaksperiodeId,
                                                                            utbetalingId = periode.utbetaling.id,
                                                                        ).map { vedtakBegrunnelse ->
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
                                                                        },
                                                                pensjonsgivendeInntekter =
                                                                    periode.pensjonsgivendeInntekter.map {
                                                                        ApiPensjonsgivendeInntekt(
                                                                            arligBelop = it.arligBelop,
                                                                            inntektsar = it.inntektsar,
                                                                        )
                                                                    },
                                                                annulleringskandidater =
                                                                    periode.annulleringskandidater.map {
                                                                        ApiAnnulleringskandidat(
                                                                            fom = it.fom,
                                                                            organisasjonsnummer = it.organisasjonsnummer,
                                                                            tom = it.tom,
                                                                            vedtaksperiodeId = it.vedtaksperiodeId,
                                                                        )
                                                                    },
                                                            )
                                                        }

                                                        else -> {
                                                            throw Exception("Ukjent tidslinjeperiode")
                                                        }
                                                    }
                                                },
                                        )
                                    },
                            )
                        }
                },
            selvstendigNaering =
                snapshot.arbeidsgivere
                    .firstOrNull { it.organisasjonsnummer == "SELVSTENDIG" }
                    ?.let { selvstendig ->
                        ApiSelvstendigNaering(
                            behandlinger =
                                selvstendig.behandlinger.mapIndexed { behandlingIndex, behandling ->
                                    ApiBehandling(
                                        id = behandling.id,
                                        perioder =
                                            behandling.perioder.map { periode ->
                                                when (periode) {
                                                    is SnapshotUberegnetPeriode -> {
                                                        periode.tilUberegnetPeriode(
                                                            behandlingIndex = behandlingIndex,
                                                            perioderSomSkalViseAktiveVarsler = perioderSomSkalViseAktiveVarsler,
                                                            transaction,
                                                        )
                                                    }

                                                    is SnapshotBeregnetPeriode -> {
                                                        val periodetilstand =
                                                            periode.periodetilstand.tilApiPeriodetilstand(
                                                                behandlingIndex == 0,
                                                            )

                                                        ApiBeregnetPeriode(
                                                            behandlingId = periode.behandlingId,
                                                            erForkastet = periode.erForkastet,
                                                            fom = periode.fom,
                                                            tom = periode.tom,
                                                            id =
                                                                genererPeriodeid(
                                                                    vedtaksperiodeId = periode.vedtaksperiodeId,
                                                                    behandlingIndex = behandlingIndex,
                                                                ),
                                                            inntektstype = periode.inntektstype.tilApiInntektstype(),
                                                            opprettet = periode.opprettet,
                                                            periodetype = periode.tilApiPeriodetype(),
                                                            tidslinje = periode.tidslinje.map { it.tilApiDag() },
                                                            vedtaksperiodeId = periode.vedtaksperiodeId,
                                                            periodetilstand = periodetilstand,
                                                            skjaeringstidspunkt = periode.skjaeringstidspunkt,
                                                            grunnbelopsjustertGjennomsnitt = periode.grunnbeløpsjustertGjennomsnitt,
                                                            varsler =
                                                                if (behandlingIndex == 0) {
                                                                    daos.varselApiRepository
                                                                        .finnVarslerSomIkkeErInaktiveForSisteBehandling(
                                                                            periode.vedtaksperiodeId,
                                                                            periode.utbetaling.id,
                                                                        ).map { it.toVarselDto() }
                                                                } else {
                                                                    daos.varselApiRepository
                                                                        .finnVarslerSomIkkeErInaktiveFor(
                                                                            periode.vedtaksperiodeId,
                                                                            periode.utbetaling.id,
                                                                        ).map { it.toVarselDto() }
                                                                },
                                                            hendelser = periode.hendelser.map { it.tilApiHendelse() },
                                                            egenskaper =
                                                                apiOppgaveService.hentEgenskaper(
                                                                    periode.vedtaksperiodeId,
                                                                    periode.utbetaling.id,
                                                                ),
                                                            historikkinnslag =
                                                                daos.periodehistorikkApiDao
                                                                    .finn(
                                                                        utbetalingId = periode.utbetaling.id,
                                                                        spleisBehandlingId = periode.behandlingId,
                                                                    ).map { it.toApiHistorikkinnslag(transaction.dialogRepository) },
                                                            forbrukteSykedager = periode.forbrukteSykedager,
                                                            gjenstaendeSykedager = periode.gjenstaendeSykedager,
                                                            maksdato = periode.maksdato,
                                                            periodevilkar =
                                                                ApiPeriodevilkar(
                                                                    alder =
                                                                        periode.periodevilkar.alder.let {
                                                                            ApiAlder(
                                                                                alderSisteSykedag = it.alderSisteSykedag,
                                                                                oppfylt = it.oppfylt,
                                                                            )
                                                                        },
                                                                    sykepengedager =
                                                                        periode.periodevilkar.sykepengedager.let {
                                                                            ApiSykepengedager(
                                                                                forbrukteSykedager = it.forbrukteSykedager,
                                                                                gjenstaendeSykedager = it.gjenstaendeSykedager,
                                                                                maksdato = it.maksdato,
                                                                                oppfylt = it.oppfylt,
                                                                                skjaeringstidspunkt = it.skjaeringstidspunkt,
                                                                            )
                                                                        },
                                                                ),
                                                            utbetaling = periode.utbetaling.tilApiUtbetaling(),
                                                            vilkarsgrunnlagId = periode.vilkarsgrunnlagId,
                                                            avslag =
                                                                daos.vedtakBegrunnelseDao
                                                                    .finnAlleVedtakBegrunnelser(
                                                                        vedtaksperiodeId = periode.vedtaksperiodeId,
                                                                        utbetalingId = periode.utbetaling.id,
                                                                    ).filter {
                                                                        it.type in
                                                                            setOf(
                                                                                VedtakBegrunnelseTypeFraDatabase.AVSLAG,
                                                                                VedtakBegrunnelseTypeFraDatabase.DELVIS_INNVILGELSE,
                                                                            )
                                                                    }.map { vedtakBegrunnelse ->
                                                                        ApiAvslag(
                                                                            type =
                                                                                when (vedtakBegrunnelse.type) {
                                                                                    VedtakBegrunnelseTypeFraDatabase.AVSLAG -> ApiAvslagstype.AVSLAG
                                                                                    VedtakBegrunnelseTypeFraDatabase.DELVIS_INNVILGELSE -> ApiAvslagstype.DELVIS_AVSLAG
                                                                                    else -> error("")
                                                                                },
                                                                            begrunnelse = vedtakBegrunnelse.begrunnelse,
                                                                            opprettet = vedtakBegrunnelse.opprettet,
                                                                            saksbehandlerIdent = vedtakBegrunnelse.saksbehandlerIdent,
                                                                            invalidert = vedtakBegrunnelse.invalidert,
                                                                        )
                                                                    },
                                                            vedtakBegrunnelser =
                                                                daos.vedtakBegrunnelseDao
                                                                    .finnAlleVedtakBegrunnelser(
                                                                        vedtaksperiodeId = periode.vedtaksperiodeId,
                                                                        utbetalingId = periode.utbetaling.id,
                                                                    ).map { vedtakBegrunnelse ->
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
                                                                    },
                                                            pensjonsgivendeInntekter =
                                                                periode.pensjonsgivendeInntekter.map {
                                                                    ApiPensjonsgivendeInntekt(
                                                                        arligBelop = it.arligBelop,
                                                                        inntektsar = it.inntektsar,
                                                                    )
                                                                },
                                                            annulleringskandidater =
                                                                periode.annulleringskandidater.map {
                                                                    ApiAnnulleringskandidat(
                                                                        fom = it.fom,
                                                                        organisasjonsnummer = it.organisasjonsnummer,
                                                                        tom = it.tom,
                                                                        vedtaksperiodeId = it.vedtaksperiodeId,
                                                                    )
                                                                },
                                                        )
                                                    }

                                                    else -> {
                                                        throw Exception("Ukjent tidslinjeperiode")
                                                    }
                                                }
                                            },
                                    )
                                },
                        )
                    },
            vilkarsgrunnlagV2 = snapshot.vilkarsgrunnlag.map { it.tilVilkarsgrunnlagV2(transaction.avviksvurderingRepository) },
        )

    private fun andreFødselsnumre(
        transaction: SessionContext,
        personEntity: Person,
        identitetsnummer: Identitetsnummer,
    ): List<ApiAnnetFodselsnummer> =
        transaction.personRepository
            .finnAlleMedAktørId(personEntity.aktørId)
            .asSequence()
            .map(Person::id)
            .filterNot { it == identitetsnummer }
            .distinct()
            .map {
                ApiAnnetFodselsnummer(
                    fodselsnummer = it.value,
                    personPseudoId = personPseudoIdProvider.nyPersonPseudoId(identitetsnummer = it).value,
                )
            }.toList()

    private fun PeriodehistorikkDto.toApiHistorikkinnslag(dialogRepository: DialogRepository): ApiHistorikkinnslag =
        when (type) {
            PeriodehistorikkType.LEGG_PA_VENT -> {
                val (påVentÅrsaker, frist, notattekst) =
                    mapLagtPåVentJson(json = json)
                ApiLagtPaVent(
                    id = id,
                    historikktype = type.tilApiPeriodehistorikkType(),
                    timestamp = timestamp,
                    saksbehandlerIdent = saksbehandlerIdent,
                    dialogRef = dialogRef,
                    arsaker = påVentÅrsaker,
                    frist = frist,
                    notattekst = notattekst,
                    kommentarer = hentKommentarer(dialogRepository),
                )
            }

            PeriodehistorikkType.ENDRE_PA_VENT -> {
                val (påVentÅrsaker, frist, notattekst) =
                    mapLagtPåVentJson(json = json)
                ApiEndrePaVent(
                    id = id,
                    historikktype = type.tilApiPeriodehistorikkType(),
                    timestamp = timestamp,
                    saksbehandlerIdent = saksbehandlerIdent,
                    dialogRef = dialogRef,
                    arsaker = påVentÅrsaker,
                    frist = frist,
                    notattekst = notattekst,
                    kommentarer = hentKommentarer(dialogRepository),
                )
            }

            PeriodehistorikkType.FJERN_FRA_PA_VENT -> {
                ApiFjernetFraPaVent(
                    id = id,
                    historikktype = type.tilApiPeriodehistorikkType(),
                    timestamp = timestamp,
                    saksbehandlerIdent = saksbehandlerIdent,
                    dialogRef = dialogRef,
                )
            }

            PeriodehistorikkType.TOTRINNSVURDERING_RETUR -> {
                val notattekst =
                    mapNotattekstJson(json = json)
                ApiTotrinnsvurderingRetur(
                    id = id,
                    historikktype = type.tilApiPeriodehistorikkType(),
                    saksbehandlerIdent = saksbehandlerIdent,
                    timestamp = timestamp,
                    dialogRef = dialogRef,
                    notattekst = notattekst,
                    kommentarer = hentKommentarer(dialogRepository),
                )
            }

            PeriodehistorikkType.STANS_AUTOMATISK_BEHANDLING_SAKSBEHANDLER -> {
                val notattekst =
                    mapNotattekstJson(json = json)
                ApiStansAutomatiskBehandlingSaksbehandler(
                    id = id,
                    historikktype = type.tilApiPeriodehistorikkType(),
                    saksbehandlerIdent = saksbehandlerIdent,
                    timestamp = timestamp,
                    dialogRef = dialogRef,
                    notattekst = notattekst,
                    kommentarer = hentKommentarer(dialogRepository),
                )
            }

            PeriodehistorikkType.OPPHEV_STANS_AUTOMATISK_BEHANDLING_SAKSBEHANDLER -> {
                val notattekst =
                    mapNotattekstJson(json = json)
                ApiOpphevStansAutomatiskBehandlingSaksbehandler(
                    id = id,
                    historikktype = type.tilApiPeriodehistorikkType(),
                    saksbehandlerIdent = saksbehandlerIdent,
                    timestamp = timestamp,
                    dialogRef = dialogRef,
                    notattekst = notattekst,
                    kommentarer = hentKommentarer(dialogRepository),
                )
            }

            else -> {
                ApiPeriodeHistorikkElementNy(
                    id = id,
                    historikktype = type.tilApiPeriodehistorikkType(),
                    saksbehandlerIdent = saksbehandlerIdent,
                    timestamp = timestamp,
                    dialogRef = dialogRef,
                )
            }
        }

    private fun PeriodehistorikkDto.hentKommentarer(dialogRepository: DialogRepository): List<ApiSaksbildeKommentar> =
        dialogRef
            ?.toLong()
            ?.let(::DialogId)
            ?.let(dialogRepository::finnOrNull)
            ?.kommentarer
            .orEmpty()
            .map { kommentar ->
                ApiSaksbildeKommentar(
                    id = kommentar.id().value,
                    tekst = kommentar.tekst,
                    opprettet = kommentar.opprettetTidspunkt,
                    saksbehandlerident = kommentar.saksbehandlerident.value,
                    feilregistrert_tidspunkt = kommentar.feilregistrertTidspunkt,
                )
            }

    private fun genererPeriodeid(
        vedtaksperiodeId: UUID,
        behandlingIndex: Int,
    ): UUID = UUID.nameUUIDFromBytes(vedtaksperiodeId.toString().toByteArray() + behandlingIndex.toByte())

    private fun SnapshotUberegnetPeriode.tilUberegnetPeriode(
        behandlingIndex: Int,
        perioderSomSkalViseAktiveVarsler: Set<UUID>,
        transaction: SessionContext,
    ): ApiUberegnetPeriode =
        ApiUberegnetPeriode(
            behandlingId = behandlingId,
            erForkastet = erForkastet,
            fom = fom,
            tom = tom,
            id = genererPeriodeid(vedtaksperiodeId = vedtaksperiodeId, behandlingIndex = behandlingIndex),
            inntektstype = inntektstype.tilApiInntektstype(),
            opprettet = opprettet,
            periodetype = tilApiPeriodetype(),
            tidslinje = tidslinje.map { it.tilApiDag() },
            vedtaksperiodeId = vedtaksperiodeId,
            periodetilstand = periodetilstand.tilApiPeriodetilstand(true),
            skjaeringstidspunkt = skjaeringstidspunkt,
            varsler =
                if (behandlingIndex == 0 && vedtaksperiodeId in perioderSomSkalViseAktiveVarsler) {
                    daos.varselApiRepository
                        .finnVarslerForUberegnetPeriode(vedtaksperiodeId)
                        .map { it.toVarselDto() }
                } else {
                    daos.varselApiRepository
                        .finnGodkjenteVarslerForUberegnetPeriode(vedtaksperiodeId)
                        .map { it.toVarselDto() }
                },
            hendelser = hendelser.map { it.tilApiHendelse() },
            historikkinnslag =
                daos.periodehistorikkApiDao
                    .finn(
                        utbetalingId = null,
                        spleisBehandlingId = behandlingId,
                    ).map { it.toApiHistorikkinnslag(transaction.dialogRepository) },
        )

    private fun finnArbeidsgivernavn(
        organisasjonsnummer: String,
        transaction: SessionContext,
    ): String? =
        transaction.arbeidsgiverRepository
            .finnOrNull(ArbeidsgiverIdentifikator.fraString(organisasjonsnummer))
            ?.navn
            ?.navn
}

private fun SnapshotGhostPeriode.tilGhostPeriode(organisasjonsnummer: String): ApiGhostPeriode =
    ApiGhostPeriode(
        fom = fom,
        tom = tom,
        skjaeringstidspunkt = skjaeringstidspunkt,
        vilkarsgrunnlagId = vilkarsgrunnlagId,
        deaktivert = deaktivert,
        organisasjonsnummer = organisasjonsnummer,
    )

private fun SnapshotOppdrag.tilSimulering(): ApiSimulering =
    ApiSimulering(
        fagsystemId = fagsystemId,
        tidsstempel = tidsstempel,
        utbetalingslinjer =
            utbetalingslinjer.map { linje ->
                ApiSimuleringslinje(
                    fom = linje.fom,
                    tom = linje.tom,
                    dagsats = linje.dagsats,
                    grad = linje.grad,
                )
            },
        totalbelop = simulering?.totalbelop,
        perioder =
            simulering?.perioder?.map { periode ->
                ApiSimuleringsperiode(
                    fom = periode.fom,
                    tom = periode.tom,
                    utbetalinger =
                        periode.utbetalinger.map { utbetaling ->
                            ApiSimuleringsutbetaling(
                                mottakerNavn = utbetaling.utbetalesTilNavn,
                                mottakerId = utbetaling.utbetalesTilId,
                                forfall = utbetaling.forfall,
                                feilkonto = utbetaling.feilkonto,
                                detaljer =
                                    utbetaling.detaljer.map { detaljer ->
                                        ApiSimuleringsdetaljer(
                                            fom = detaljer.faktiskFom,
                                            tom = detaljer.faktiskTom,
                                            belop = detaljer.belop,
                                            antallSats = detaljer.antallSats,
                                            klassekode = detaljer.klassekode,
                                            klassekodebeskrivelse = detaljer.klassekodeBeskrivelse,
                                            konto = detaljer.konto,
                                            refunderesOrgNr = detaljer.refunderesOrgNr,
                                            sats = detaljer.sats,
                                            tilbakeforing = detaljer.tilbakeforing,
                                            typeSats = detaljer.typeSats,
                                            uforegrad = detaljer.uforegrad,
                                            utbetalingstype = detaljer.utbetalingstype,
                                        )
                                    },
                            )
                        },
                )
            },
    )

private fun mapLagtPåVentJson(json: String): Triple<List<String>, LocalDate?, String?> {
    val node = objectMapper.readTree(json)
    val påVentÅrsaker = node["årsaker"].toList().map { it["årsak"].asString() }
    val frist =
        node["frist"]
            ?.takeUnless { it.isMissingOrNull() }
            ?.asLocalDate()
    val notattekst =
        node["notattekst"]
            ?.takeUnless { it.isMissingOrNull() }
            ?.asString()
    return Triple(påVentÅrsaker, frist, notattekst)
}

private fun mapNotattekstJson(json: String): String? {
    val node = objectMapper.readTree(json)
    val notattekst =
        node["notattekst"]
            ?.takeUnless { it.isMissingOrNull() }
            ?.asString()
    return notattekst
}

private fun SnapshotUtbetaling.tilApiUtbetaling(): ApiUtbetaling =
    ApiUtbetaling(
        id = id,
        arbeidsgiverFagsystemId = arbeidsgiverFagsystemId,
        arbeidsgiverNettoBelop = arbeidsgiverNettoBelop,
        personFagsystemId = personFagsystemId,
        personNettoBelop = personNettoBelop,
        status = statusEnum.tilApiUtbetalingstatus(),
        type = typeEnum.tilApiUtbetalingtype(),
        vurdering = vurdering?.tilApiVurdering(),
        arbeidsgiversimulering = arbeidsgiveroppdrag?.tilSimulering(),
        personsimulering = personoppdrag?.tilSimulering(),
    )

private fun SnapshotVurdering.tilApiVurdering(): ApiVurdering =
    ApiVurdering(
        automatisk = automatisk,
        godkjent = godkjent,
        ident = ident,
        tidsstempel = tidsstempel,
    )

private fun SnapshotUtbetalingtype.tilApiUtbetalingtype(): ApiUtbetalingtype =
    when (this) {
        SnapshotUtbetalingtype.ANNULLERING -> ApiUtbetalingtype.ANNULLERING
        SnapshotUtbetalingtype.ETTERUTBETALING -> ApiUtbetalingtype.ETTERUTBETALING
        SnapshotUtbetalingtype.FERIEPENGER -> ApiUtbetalingtype.FERIEPENGER
        SnapshotUtbetalingtype.REVURDERING -> ApiUtbetalingtype.REVURDERING
        SnapshotUtbetalingtype.UTBETALING -> ApiUtbetalingtype.UTBETALING
        SnapshotUtbetalingtype.UNKNOWN_VALUE -> ApiUtbetalingtype.UKJENT
    }

private fun SnapshotUtbetalingstatus.tilApiUtbetalingstatus(): ApiUtbetalingstatus =
    when (this) {
        SnapshotUtbetalingstatus.ANNULLERT -> ApiUtbetalingstatus.ANNULLERT
        SnapshotUtbetalingstatus.FORKASTET -> ApiUtbetalingstatus.FORKASTET
        SnapshotUtbetalingstatus.GODKJENT -> ApiUtbetalingstatus.GODKJENT
        SnapshotUtbetalingstatus.GODKJENTUTENUTBETALING -> ApiUtbetalingstatus.GODKJENTUTENUTBETALING
        SnapshotUtbetalingstatus.IKKEGODKJENT -> ApiUtbetalingstatus.IKKEGODKJENT
        SnapshotUtbetalingstatus.OVERFORT -> ApiUtbetalingstatus.OVERFORT
        SnapshotUtbetalingstatus.SENDT -> ApiUtbetalingstatus.SENDT
        SnapshotUtbetalingstatus.UBETALT -> ApiUtbetalingstatus.UBETALT
        SnapshotUtbetalingstatus.UTBETALINGFEILET -> ApiUtbetalingstatus.UTBETALINGFEILET
        SnapshotUtbetalingstatus.UTBETALT -> ApiUtbetalingstatus.UTBETALT
        SnapshotUtbetalingstatus.UNKNOWN_VALUE -> ApiUtbetalingstatus.UKJENT
    }
