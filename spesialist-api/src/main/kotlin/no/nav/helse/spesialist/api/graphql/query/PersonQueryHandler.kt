package no.nav.helse.spesialist.api.graphql.query

import graphql.execution.DataFetcherResult
import graphql.schema.DataFetchingEnvironment
import no.nav.helse.db.AnnulleringRepository
import no.nav.helse.db.SessionContext
import no.nav.helse.db.SessionFactory
import no.nav.helse.db.StansAutomatiskBehandlingSaksbehandlerDao
import no.nav.helse.db.VedtakBegrunnelseDao
import no.nav.helse.db.api.ArbeidsgiverApiDao
import no.nav.helse.db.api.NotatApiDao
import no.nav.helse.db.api.OppgaveApiDao
import no.nav.helse.db.api.OverstyringApiDao
import no.nav.helse.db.api.PeriodehistorikkApiDao
import no.nav.helse.db.api.PersonApiDao
import no.nav.helse.db.api.PåVentApiDao
import no.nav.helse.db.api.RisikovurderingApiDao
import no.nav.helse.db.api.TildelingApiDao
import no.nav.helse.db.api.VarselApiRepository
import no.nav.helse.db.api.VergemålApiDao
import no.nav.helse.mediator.SaksbehandlerMediator
import no.nav.helse.mediator.oppgave.ApiOppgaveService
import no.nav.helse.spesialist.api.Personhåndterer
import no.nav.helse.spesialist.api.StansAutomatiskBehandlinghåndterer
import no.nav.helse.spesialist.api.auditLogTeller
import no.nav.helse.spesialist.api.graphql.ContextValues
import no.nav.helse.spesialist.api.graphql.byggRespons
import no.nav.helse.spesialist.api.graphql.graphqlErrorException
import no.nav.helse.spesialist.api.graphql.mapping.tilVilkarsgrunnlagV2
import no.nav.helse.spesialist.api.graphql.resolvers.ApiBeregnetPeriodeResolver
import no.nav.helse.spesialist.api.graphql.resolvers.ApiSelvstendigNaeringResolver
import no.nav.helse.spesialist.api.graphql.resolvers.ApiUberegnetPeriodeResolver
import no.nav.helse.spesialist.api.graphql.schema.ApiAnnetFodselsnummer
import no.nav.helse.spesialist.api.graphql.schema.ApiArbeidsforhold
import no.nav.helse.spesialist.api.graphql.schema.ApiArbeidsforholdoverstyring
import no.nav.helse.spesialist.api.graphql.schema.ApiArbeidsgiver
import no.nav.helse.spesialist.api.graphql.schema.ApiArbeidsgiverInntekterFraAOrdningen
import no.nav.helse.spesialist.api.graphql.schema.ApiBehandling
import no.nav.helse.spesialist.api.graphql.schema.ApiBeregnetPeriode
import no.nav.helse.spesialist.api.graphql.schema.ApiDagoverstyring
import no.nav.helse.spesialist.api.graphql.schema.ApiDagtype
import no.nav.helse.spesialist.api.graphql.schema.ApiEnhet
import no.nav.helse.spesialist.api.graphql.schema.ApiGhostPeriode
import no.nav.helse.spesialist.api.graphql.schema.ApiInfotrygdutbetaling
import no.nav.helse.spesialist.api.graphql.schema.ApiInntektFraAOrdningen
import no.nav.helse.spesialist.api.graphql.schema.ApiInntektoverstyring
import no.nav.helse.spesialist.api.graphql.schema.ApiMinimumSykdomsgradOverstyring
import no.nav.helse.spesialist.api.graphql.schema.ApiPerson
import no.nav.helse.spesialist.api.graphql.schema.ApiSaksbehandler
import no.nav.helse.spesialist.api.graphql.schema.ApiSelvstendigNaering
import no.nav.helse.spesialist.api.graphql.schema.ApiSkjonnsfastsettingstype
import no.nav.helse.spesialist.api.graphql.schema.ApiSykepengegrunnlagskjonnsfastsetting
import no.nav.helse.spesialist.api.graphql.schema.ApiTildeling
import no.nav.helse.spesialist.api.graphql.schema.ApiTilleggsinfoForInntektskilde
import no.nav.helse.spesialist.api.graphql.schema.ApiUberegnetPeriode
import no.nav.helse.spesialist.api.objectMapper
import no.nav.helse.spesialist.api.overstyring.Dagtype
import no.nav.helse.spesialist.api.overstyring.OverstyringArbeidsforholdDto
import no.nav.helse.spesialist.api.overstyring.OverstyringInntektDto
import no.nav.helse.spesialist.api.overstyring.OverstyringMinimumSykdomsgradDto
import no.nav.helse.spesialist.api.overstyring.OverstyringTidslinjeDto
import no.nav.helse.spesialist.api.overstyring.Skjonnsfastsettingstype
import no.nav.helse.spesialist.api.overstyring.SkjønnsfastsettingSykepengegrunnlagDto
import no.nav.helse.spesialist.api.snapshot.SnapshotService
import no.nav.helse.spesialist.api.tildeling.TildelingApiDto
import no.nav.helse.spesialist.application.PersonPseudoId
import no.nav.helse.spesialist.application.SaksbehandlerRepository
import no.nav.helse.spesialist.application.logg.logg
import no.nav.helse.spesialist.application.logg.loggInfo
import no.nav.helse.spesialist.application.logg.loggThrowable
import no.nav.helse.spesialist.application.logg.sikkerlogg
import no.nav.helse.spesialist.application.snapshot.SnapshotBeregnetPeriode
import no.nav.helse.spesialist.application.snapshot.SnapshotGhostPeriode
import no.nav.helse.spesialist.application.snapshot.SnapshotUberegnetPeriode
import no.nav.helse.spesialist.domain.ArbeidsgiverIdentifikator
import no.nav.helse.spesialist.domain.Identitetsnummer
import no.nav.helse.spesialist.domain.Saksbehandler
import no.nav.helse.spesialist.domain.tilgangskontroll.Tilgangsgruppe
import org.slf4j.LoggerFactory
import org.slf4j.event.Level

class PersonQueryHandler(
    private val personApiDao: PersonApiDao,
    private val vergemålApiDao: VergemålApiDao,
    private val tildelingApiDao: TildelingApiDao,
    private val arbeidsgiverApiDao: ArbeidsgiverApiDao,
    private val overstyringApiDao: OverstyringApiDao,
    private val risikovurderingApiDao: RisikovurderingApiDao,
    private val varselRepository: VarselApiRepository,
    private val oppgaveApiDao: OppgaveApiDao,
    private val periodehistorikkApiDao: PeriodehistorikkApiDao,
    private val notatDao: NotatApiDao,
    private val påVentApiDao: PåVentApiDao,
    private val apiOppgaveService: ApiOppgaveService,
    private val saksbehandlerMediator: SaksbehandlerMediator,
    private val stansAutomatiskBehandlinghåndterer: StansAutomatiskBehandlinghåndterer,
    private val personhåndterer: Personhåndterer,
    private val snapshotService: SnapshotService,
    private val sessionFactory: SessionFactory,
    private val vedtakBegrunnelseDao: VedtakBegrunnelseDao,
    private val stansAutomatiskBehandlingSaksbehandlerDao: StansAutomatiskBehandlingSaksbehandlerDao,
    private val annulleringRepository: AnnulleringRepository,
    private val saksbehandlerRepository: SaksbehandlerRepository,
) : PersonQuerySchema {
    private val auditLog = LoggerFactory.getLogger("auditLogger")

    override suspend fun person(
        personPseudoId: String,
        env: DataFetchingEnvironment,
    ): DataFetcherResult<ApiPerson?> =
        sessionFactory.transactionalSessionScope { transaction ->
            hentPerson(
                personPseudoId =
                    runCatching {
                        PersonPseudoId.fraString(personPseudoId)
                    }.getOrElse { badRequest("Ugyldig format på personPseudoId") },
                transaction = transaction,
                saksbehandler = env.graphQlContext.get<Saksbehandler>(ContextValues.SAKSBEHANDLER),
                tilgangsgrupper = env.graphQlContext.get<Set<Tilgangsgruppe>>(ContextValues.TILGANGSGRUPPER),
            )
        }

    private fun hentPerson(
        personPseudoId: PersonPseudoId,
        transaction: SessionContext,
        saksbehandler: Saksbehandler,
        tilgangsgrupper: Set<Tilgangsgruppe>,
    ): DataFetcherResult<ApiPerson?> {
        val identitetsnummer = (
            transaction.personPseudoIdDao.hentIdentitetsnummer(personPseudoId)
                ?: run {
                    loggInfo("Fant ikke person basert på personPseudoId: ${personPseudoId.value}")
                    auditLoggPersonIkkeFunnet(
                        saksbehandler = saksbehandler,
                        duid = personPseudoId.value.toString(),
                        msg = "Finner ikke data for person med identifikator ${personPseudoId.value}",
                    )
                    notFound("PseudoId er ugyldig eller utgått")
                }
        )

        loggInfo("Personoppslag på person", "identitetsnummer: $identitetsnummer")

        val personEntity =
            transaction.personRepository.finn(identitetsnummer)
                ?: personIkkeFunnet(saksbehandler, identitetsnummer)

        if (!personEntity.harDataNødvendigForVisning()) {
            if (!transaction.personKlargjoresDao.klargjøringPågår(identitetsnummer.value)) {
                personhåndterer.klargjørPersonForVisning(identitetsnummer.value)
                transaction.personKlargjoresDao.personKlargjøres(identitetsnummer.value)
            }

            personIkkeKlarTilVisning(saksbehandler, identitetsnummer)
        }

        if (!personEntity.kanSeesAvSaksbehandlerMedGrupper(tilgangsgrupper)) {
            manglerTilgangTilPerson(saksbehandler, identitetsnummer)
        }

        // Best effort for å finne ut om saksbehandler har tilgang til oppgaven som gjelder
        // Litt vanskelig å få pent så lenge vi har dynamisk resolving av resten, og tilsynelatende "mange" oppgaver
        val harTilgangTilOppgave =
            oppgaveApiDao.finnOppgaveId(identitetsnummer.value)?.let { oppgaveId ->
                transaction.oppgaveRepository
                    .finn(oppgaveId)
                    ?.kanSeesAv(saksbehandler, tilgangsgrupper)
            } ?: true

        if (!harTilgangTilOppgave) {
            logg.warn("Saksbehandler mangler tilgang til aktiv oppgave på denne personen")
            manglerTilgangTilPerson(saksbehandler, identitetsnummer)
        }

        val personinfoOgSnapshot =
            runCatching { snapshotService.hentSnapshot(identitetsnummer.value) }
                .getOrElse { e ->
                    loggThrowable(
                        message = "Klarte ikke hente snapshot fra Spleis",
                        securelogDetails = "identitetsnummer=${identitetsnummer.value}",
                        throwable = e,
                    )
                    klarteIkkeHentePerson(saksbehandler, identitetsnummer)
                }?.takeUnless { it.second.arbeidsgivere.isEmpty() }

        if (personinfoOgSnapshot == null) {
            personIkkeFunnet(saksbehandler, identitetsnummer)
        }

        val (personinfo, snapshot) = personinfoOgSnapshot

        fun finnNavnForOrganisasjonsnummer(organisasjonsnummer: String): String =
            transaction.arbeidsgiverRepository
                .finn(ArbeidsgiverIdentifikator.fraString(organisasjonsnummer))
                ?.navn
                ?.navn
                ?: "navn er utilgjengelig"

        return ApiPerson(
            versjon = snapshot.versjon,
            aktorId = snapshot.aktorId,
            fodselsnummer = snapshot.fodselsnummer,
            andreFodselsnummer =
                personApiDao
                    .finnFødselsnumre(personEntity.aktørId)
                    .toSet()
                    .filterNot { fnr -> fnr == identitetsnummer.value }
                    .associateWith { fnr ->
                        transaction.personPseudoIdDao.nyPersonPseudoId(
                            identitetsnummer = Identitetsnummer.fraString(fnr),
                        )
                    }.entries
                    .map { (fødselsnummer, pseudoId) -> Identitetsnummer.fraString(fødselsnummer) to pseudoId }
                    .toSet()
                    .toList()
                    .map { ApiAnnetFodselsnummer(it.first.value, it.second.value) },
            dodsdato = snapshot.dodsdato,
            personinfo =
                personinfo.copy(
                    unntattFraAutomatisering =
                        stansAutomatiskBehandlinghåndterer.unntattFraAutomatiskGodkjenning(
                            identitetsnummer.value,
                        ),
                    fullmakt = vergemålApiDao.harFullmakt(identitetsnummer.value),
                    automatiskBehandlingStansetAvSaksbehandler =
                        stansAutomatiskBehandlingSaksbehandlerDao.erStanset(
                            identitetsnummer.value,
                        ),
                ),
            enhet = personApiDao.finnEnhet(snapshot.fodselsnummer).let { ApiEnhet(it.id, it.navn) },
            tildeling = tildelingApiDao.tildelingForPerson(snapshot.fodselsnummer)?.tilTildeling(),
            tilleggsinfoForInntektskilder =
                snapshot.vilkarsgrunnlag
                    .flatMap { vilkårsgrunnlag ->
                        val avviksvurdering =
                            transaction.avviksvurderingRepository.hentAvviksvurdering(vilkårsgrunnlag.id)
                        (
                            avviksvurdering?.sammenligningsgrunnlag?.innrapporterteInntekter?.map { innrapportertInntekt ->
                                innrapportertInntekt.arbeidsgiverreferanse
                            } ?: emptyList()
                        ) + vilkårsgrunnlag.inntekter.map { inntekt -> inntekt.arbeidsgiver }
                    }.toSet()
                    .map { organisasjonsnummer ->
                        ApiTilleggsinfoForInntektskilde(
                            orgnummer = organisasjonsnummer,
                            navn = finnNavnForOrganisasjonsnummer(organisasjonsnummer),
                        )
                    },
            arbeidsgivere =
                run {
                    val overstyringer = overstyringApiDao.finnOverstyringer(snapshot.fodselsnummer)

                    snapshot.arbeidsgivere
                        .filterNot { it.organisasjonsnummer == "SELVSTENDIG" }
                        .map { arbeidsgiver ->
                            val organisasjonsnummer = arbeidsgiver.organisasjonsnummer
                            val navn = finnNavnForOrganisasjonsnummer(organisasjonsnummer)
                            val ghostPerioder =
                                arbeidsgiver.ghostPerioder.map {
                                    it.tilGhostPeriode(organisasjonsnummer)
                                }
                            val fødselsnummer = snapshot.fodselsnummer
                            val behandlinger = arbeidsgiver.behandlinger
                            val risikovurderinger = risikovurderingApiDao.finnRisikovurderinger(identitetsnummer.value)
                            ApiArbeidsgiver(
                                organisasjonsnummer = organisasjonsnummer,
                                navn = navn,
                                ghostPerioder = ghostPerioder,
                                behandlinger =
                                    behandlinger.mapIndexed { index, behandling ->
                                        val oppgaveId = oppgaveApiDao.finnOppgaveId(fødselsnummer)
                                        val perioderSomSkalViseAktiveVarsler =
                                            varselRepository.perioderSomSkalViseVarsler(oppgaveId)
                                        ApiBehandling(
                                            id = behandling.id,
                                            perioder =
                                                behandling.perioder.map {
                                                    when (it) {
                                                        is SnapshotUberegnetPeriode ->
                                                            ApiUberegnetPeriode(
                                                                resolver =
                                                                    ApiUberegnetPeriodeResolver(
                                                                        varselRepository = varselRepository,
                                                                        periode = it,
                                                                        skalViseAktiveVarsler =
                                                                            index == 0 &&
                                                                                perioderSomSkalViseAktiveVarsler.contains(
                                                                                    it.vedtaksperiodeId,
                                                                                ),
                                                                        notatDao = notatDao,
                                                                        index = index,
                                                                    ),
                                                            )

                                                        is SnapshotBeregnetPeriode ->
                                                            ApiBeregnetPeriode(
                                                                resolver =
                                                                    ApiBeregnetPeriodeResolver(
                                                                        fødselsnummer = fødselsnummer,
                                                                        orgnummer = organisasjonsnummer,
                                                                        periode = it,
                                                                        apiOppgaveService = apiOppgaveService,
                                                                        saksbehandlerMediator = saksbehandlerMediator,
                                                                        risikovurderinger = risikovurderinger,
                                                                        varselRepository = varselRepository,
                                                                        oppgaveApiDao = oppgaveApiDao,
                                                                        periodehistorikkApiDao = periodehistorikkApiDao,
                                                                        notatDao = notatDao,
                                                                        påVentApiDao = påVentApiDao,
                                                                        erSisteBehandling = index == 0,
                                                                        index = index,
                                                                        vedtakBegrunnelseDao = vedtakBegrunnelseDao,
                                                                        sessionFactory = sessionFactory,
                                                                        annulleringRepository = annulleringRepository,
                                                                        saksbehandlerRepository = saksbehandlerRepository,
                                                                    ),
                                                            )

                                                        else -> throw Exception("Ukjent tidslinjeperiode")
                                                    }
                                                },
                                        )
                                    },
                                overstyringer =
                                    overstyringer
                                        .filter { it.relevantFor(organisasjonsnummer) }
                                        .map { overstyring ->
                                            when (overstyring) {
                                                is OverstyringTidslinjeDto -> overstyring.tilDagoverstyring()
                                                is OverstyringArbeidsforholdDto -> overstyring.tilArbeidsforholdoverstyring()
                                                is OverstyringInntektDto -> overstyring.tilInntektoverstyring()
                                                is SkjønnsfastsettingSykepengegrunnlagDto -> overstyring.tilSykepengegrunnlagSkjønnsfastsetting()
                                                is OverstyringMinimumSykdomsgradDto -> overstyring.tilMinimumSykdomsgradOverstyring()
                                            }
                                        },
                                arbeidsforhold =
                                    arbeidsgiverApiDao
                                        .finnArbeidsforhold(
                                            fødselsnummer,
                                            organisasjonsnummer,
                                        ).map {
                                            ApiArbeidsforhold(
                                                stillingstittel = it.stillingstittel,
                                                stillingsprosent = it.stillingsprosent,
                                                startdato = it.startdato,
                                                sluttdato = it.sluttdato,
                                            )
                                        },
                                inntekterFraAordningen =
                                    arbeidsgiverApiDao
                                        .finnArbeidsgiverInntekterFraAordningen(
                                            fødselsnummer,
                                            organisasjonsnummer,
                                        ).map { fraAO ->
                                            ApiArbeidsgiverInntekterFraAOrdningen(
                                                skjaeringstidspunkt = fraAO.skjaeringstidspunkt,
                                                inntekter =
                                                    fraAO.inntekter.map { inntekt ->
                                                        ApiInntektFraAOrdningen(
                                                            maned = inntekt.maned,
                                                            sum = inntekt.sum,
                                                        )
                                                    },
                                            )
                                        },
                            )
                        }
                },
            selvstendigNaering =
                run {
                    val overstyringer = overstyringApiDao.finnOverstyringer(snapshot.fodselsnummer)
                    val selvstendig =
                        snapshot.arbeidsgivere
                            .firstOrNull { it.organisasjonsnummer == "SELVSTENDIG" }
                            ?: return@run null

                    ApiSelvstendigNaering(
                        resolver =
                            ApiSelvstendigNaeringResolver(
                                behandlinger = selvstendig.behandlinger,
                                fødselsnummer = snapshot.fodselsnummer,
                                apiOppgaveService = apiOppgaveService,
                                saksbehandlerMediator = saksbehandlerMediator,
                                risikovurderinger = risikovurderingApiDao.finnRisikovurderinger(identitetsnummer.value),
                                varselRepository = varselRepository,
                                oppgaveApiDao = oppgaveApiDao,
                                periodehistorikkApiDao = periodehistorikkApiDao,
                                notatDao = notatDao,
                                påVentApiDao = påVentApiDao,
                                overstyringer =
                                    overstyringer
                                        .filter { it.relevantFor(selvstendig.organisasjonsnummer) }
                                        .map { overstyring ->
                                            when (overstyring) {
                                                is OverstyringTidslinjeDto -> overstyring.tilDagoverstyring()
                                                is OverstyringArbeidsforholdDto -> overstyring.tilArbeidsforholdoverstyring()
                                                is OverstyringInntektDto -> overstyring.tilInntektoverstyring()
                                                is SkjønnsfastsettingSykepengegrunnlagDto -> overstyring.tilSykepengegrunnlagSkjønnsfastsetting()
                                                is OverstyringMinimumSykdomsgradDto -> overstyring.tilMinimumSykdomsgradOverstyring()
                                            }
                                        },
                                vedtakBegrunnelseDao = vedtakBegrunnelseDao,
                                sessionFactory = sessionFactory,
                                annulleringRepository = annulleringRepository,
                                saksbehandlerRepository = saksbehandlerRepository,
                            ),
                    )
                },
            infotrygdutbetalinger =
                personApiDao
                    .finnInfotrygdutbetalinger(snapshot.fodselsnummer)
                    ?.let { jsonString ->
                        objectMapper
                            .readTree(jsonString)
                            .filterNot { it["typetekst"].asText().lowercase() == "sanksjon" }
                            .map {
                                ApiInfotrygdutbetaling(
                                    fom = it["fom"].asText(),
                                    tom = it["tom"].asText(),
                                    grad = it["grad"].asText(),
                                    dagsats = it["dagsats"].asDouble(),
                                    typetekst = it["typetekst"].asText(),
                                    organisasjonsnummer = it["organisasjonsnummer"].asText(),
                                )
                            }
                    },
            vilkarsgrunnlagV2 = snapshot.vilkarsgrunnlag.map { it.tilVilkarsgrunnlagV2(transaction.avviksvurderingRepository) },
        ).let {
            auditLoggOk(saksbehandler, identitetsnummer)
            byggRespons(it)
        }
    }

    private fun TildelingApiDto.tilTildeling(): ApiTildeling =
        ApiTildeling(
            navn = navn,
            epost = epost,
            oid = oid,
        )

    private fun klarteIkkeHentePerson(
        saksbehandler: Saksbehandler,
        identitetsnummer: Identitetsnummer,
    ): Nothing {
        auditLoggPersonIkkeFunnet(
            saksbehandler = saksbehandler,
            identitetsnummer = identitetsnummer,
            msg = "Feil ved henting av snapshot for person",
        )
        internalServerError("Feil ved henting av snapshot for person")
    }

    private fun manglerTilgangTilPerson(
        saksbehandler: Saksbehandler,
        identitetsnummer: Identitetsnummer,
    ): Nothing {
        auditLoggManglendeTilgang(saksbehandler, identitetsnummer)
        forbidden("Har ikke tilgang til person")
    }

    private fun personIkkeKlarTilVisning(
        saksbehandler: Saksbehandler,
        identitetsnummer: Identitetsnummer,
    ): Nothing {
        auditLoggManglendeTilgang(saksbehandler, identitetsnummer)
        conflict("Personen er ikke klar for visning ennå")
    }

    private fun personIkkeFunnet(
        saksbehandler: Saksbehandler,
        identitetsnummer: Identitetsnummer,
    ): Nothing {
        auditLoggPersonIkkeFunnet(
            saksbehandler = saksbehandler,
            identitetsnummer = identitetsnummer,
            msg = "Finner ikke data for person med identifikator ${identitetsnummer.value}",
        )
        notFound("Fant ikke data for person")
    }

    private fun auditLoggOk(
        saksbehandler: Saksbehandler,
        identitetsnummer: Identitetsnummer,
    ) {
        auditLogg(saksbehandler, identitetsnummer.value, "", Level.INFO)
    }

    private fun auditLoggPersonIkkeFunnet(
        saksbehandler: Saksbehandler,
        identitetsnummer: Identitetsnummer,
        msg: String,
    ) {
        auditLoggPersonIkkeFunnet(saksbehandler, identitetsnummer.value, msg)
    }

    private fun auditLoggPersonIkkeFunnet(
        saksbehandler: Saksbehandler,
        duid: String,
        msg: String,
    ) {
        auditLogg(saksbehandler, duid, " msg=$msg", Level.WARN)
    }

    private fun auditLoggManglendeTilgang(
        saksbehandler: Saksbehandler,
        identitetsnummer: Identitetsnummer,
    ) {
        auditLogg(saksbehandler, identitetsnummer.value, " flexString1=Deny", Level.WARN)
    }

    private fun auditLogg(
        saksbehandler: Saksbehandler,
        duid: String,
        suffix: String,
        level: Level,
    ) {
        auditLogTeller.increment()
        val message =
            "end=${System.currentTimeMillis()}" +
                " suid=${saksbehandler.ident.value}" +
                " duid=$duid" +
                " operation=PersonQuery" +
                suffix
        auditLog.atLevel(level).log(message)
        sikkerlogg.debug("audit-logget: $level - $message")
    }
}

private fun badRequest(message: String): Nothing = throw graphqlErrorException(400, message)

private fun forbidden(message: String): Nothing = throw graphqlErrorException(403, message)

private fun notFound(message: String): Nothing = throw graphqlErrorException(404, message)

private fun conflict(message: String): Nothing = throw graphqlErrorException(409, message)

private fun internalServerError(message: String): Nothing = throw graphqlErrorException(500, message)

private fun OverstyringTidslinjeDto.tilDagoverstyring() =
    ApiDagoverstyring(
        hendelseId = hendelseId,
        begrunnelse = begrunnelse,
        timestamp = timestamp,
        saksbehandler =
            ApiSaksbehandler(
                navn = saksbehandlerNavn,
                ident = saksbehandlerIdent,
            ),
        dager =
            overstyrteDager.map { dag ->
                ApiDagoverstyring.ApiOverstyrtDag(
                    dato = dag.dato,
                    type = dag.type.tilApiDagtype(),
                    fraType = dag.fraType?.tilApiDagtype(),
                    grad = dag.grad,
                    fraGrad = dag.fraGrad,
                )
            },
        ferdigstilt = ferdigstilt,
        vedtaksperiodeId = vedtaksperiodeId,
    )

private fun Dagtype.tilApiDagtype() =
    when (this) {
        Dagtype.Sykedag -> ApiDagtype.Sykedag
        Dagtype.SykedagNav -> ApiDagtype.SykedagNav
        Dagtype.Feriedag -> ApiDagtype.Feriedag
        Dagtype.Egenmeldingsdag -> ApiDagtype.Egenmeldingsdag
        Dagtype.Permisjonsdag -> ApiDagtype.Permisjonsdag
        Dagtype.Arbeidsdag -> ApiDagtype.Arbeidsdag
        Dagtype.ArbeidIkkeGjenopptattDag -> ApiDagtype.ArbeidIkkeGjenopptattDag
        Dagtype.Foreldrepengerdag -> ApiDagtype.Foreldrepengerdag
        Dagtype.AAPdag -> ApiDagtype.AAPdag
        Dagtype.Omsorgspengerdag -> ApiDagtype.Omsorgspengerdag
        Dagtype.Pleiepengerdag -> ApiDagtype.Pleiepengerdag
        Dagtype.Svangerskapspengerdag -> ApiDagtype.Svangerskapspengerdag
        Dagtype.Opplaringspengerdag -> ApiDagtype.Opplaringspengerdag
        Dagtype.Dagpengerdag -> ApiDagtype.Dagpengerdag
        Dagtype.Avvistdag -> ApiDagtype.Avvistdag
        Dagtype.Helg -> ApiDagtype.Helg
        Dagtype.MeldingTilNavdag -> ApiDagtype.MeldingTilNavdag
    }

private fun OverstyringInntektDto.tilInntektoverstyring() =
    ApiInntektoverstyring(
        hendelseId = hendelseId,
        timestamp = timestamp,
        saksbehandler =
            ApiSaksbehandler(
                navn = saksbehandlerNavn,
                ident = saksbehandlerIdent,
            ),
        inntekt =
            ApiInntektoverstyring.ApiOverstyrtInntekt(
                forklaring = forklaring,
                begrunnelse = begrunnelse,
                manedligInntekt = månedligInntekt,
                fraManedligInntekt = fraMånedligInntekt,
                skjaeringstidspunkt = skjæringstidspunkt,
                refusjonsopplysninger =
                    refusjonsopplysninger?.map {
                        ApiInntektoverstyring.ApiRefusjonsopplysning(
                            fom = it.fom,
                            tom = it.tom,
                            belop = it.beløp,
                        )
                    } ?: emptyList(),
                fraRefusjonsopplysninger =
                    fraRefusjonsopplysninger?.map {
                        ApiInntektoverstyring.ApiRefusjonsopplysning(
                            fom = it.fom,
                            tom = it.tom,
                            belop = it.beløp,
                        )
                    } ?: emptyList(),
            ),
        ferdigstilt = ferdigstilt,
        vedtaksperiodeId = vedtaksperiodeId,
    )

private fun OverstyringArbeidsforholdDto.tilArbeidsforholdoverstyring() =
    ApiArbeidsforholdoverstyring(
        hendelseId = hendelseId,
        begrunnelse = begrunnelse,
        timestamp = timestamp,
        saksbehandler =
            ApiSaksbehandler(
                navn = saksbehandlerNavn,
                ident = saksbehandlerIdent,
            ),
        deaktivert = deaktivert,
        skjaeringstidspunkt = skjæringstidspunkt,
        forklaring = forklaring,
        ferdigstilt = ferdigstilt,
        vedtaksperiodeId = vedtaksperiodeId,
    )

private fun SkjønnsfastsettingSykepengegrunnlagDto.tilSykepengegrunnlagSkjønnsfastsetting() =
    ApiSykepengegrunnlagskjonnsfastsetting(
        hendelseId = hendelseId,
        timestamp = timestamp,
        saksbehandler =
            ApiSaksbehandler(
                navn = saksbehandlerNavn,
                ident = saksbehandlerIdent,
            ),
        skjonnsfastsatt =
            ApiSykepengegrunnlagskjonnsfastsetting.ApiSkjonnsfastsattSykepengegrunnlag(
                arsak = årsak,
                type = type.tilApiSkjonnsfastsettingstype(),
                begrunnelse = begrunnelse,
                begrunnelseMal = begrunnelseMal,
                begrunnelseFritekst = begrunnelseFritekst,
                begrunnelseKonklusjon = begrunnelseKonklusjon,
                arlig = årlig,
                fraArlig = fraÅrlig,
                skjaeringstidspunkt = skjæringstidspunkt,
            ),
        ferdigstilt = ferdigstilt,
        vedtaksperiodeId = vedtaksperiodeId,
    )

private fun Skjonnsfastsettingstype.tilApiSkjonnsfastsettingstype() =
    when (this) {
        Skjonnsfastsettingstype.OMREGNET_ARSINNTEKT -> ApiSkjonnsfastsettingstype.OMREGNET_ARSINNTEKT
        Skjonnsfastsettingstype.RAPPORTERT_ARSINNTEKT -> ApiSkjonnsfastsettingstype.RAPPORTERT_ARSINNTEKT
        Skjonnsfastsettingstype.ANNET -> ApiSkjonnsfastsettingstype.ANNET
    }

private fun OverstyringMinimumSykdomsgradDto.tilMinimumSykdomsgradOverstyring() =
    ApiMinimumSykdomsgradOverstyring(
        hendelseId = hendelseId,
        timestamp = timestamp,
        saksbehandler =
            ApiSaksbehandler(
                navn = saksbehandlerNavn,
                ident = saksbehandlerIdent,
            ),
        minimumSykdomsgrad =
            ApiMinimumSykdomsgradOverstyring.ApiOverstyrtMinimumSykdomsgrad(
                perioderVurdertOk =
                    perioderVurdertOk.map {
                        ApiMinimumSykdomsgradOverstyring.ApiOverstyrtMinimumSykdomsgradPeriode(
                            fom = it.fom,
                            tom = it.tom,
                        )
                    },
                perioderVurdertIkkeOk =
                    perioderVurdertIkkeOk.map {
                        ApiMinimumSykdomsgradOverstyring.ApiOverstyrtMinimumSykdomsgradPeriode(
                            fom = it.fom,
                            tom = it.tom,
                        )
                    },
                begrunnelse = begrunnelse,
                initierendeVedtaksperiodeId = vedtaksperiodeId,
            ),
        ferdigstilt = ferdigstilt,
        vedtaksperiodeId = vedtaksperiodeId,
    )

private fun SnapshotGhostPeriode.tilGhostPeriode(organisasjonsnummer: String): ApiGhostPeriode =
    ApiGhostPeriode(
        fom = fom,
        tom = tom,
        skjaeringstidspunkt = skjaeringstidspunkt,
        vilkarsgrunnlagId = vilkarsgrunnlagId,
        deaktivert = deaktivert,
        organisasjonsnummer = organisasjonsnummer,
    )
