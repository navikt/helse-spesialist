package no.nav.helse.spesialist.api.graphql.query

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.JsonNode
import com.github.navikt.tbd_libs.jackson.asLocalDate
import com.github.navikt.tbd_libs.jackson.isMissingOrNull
import graphql.execution.DataFetcherResult
import graphql.schema.DataFetchingEnvironment
import io.ktor.utils.io.core.toByteArray
import no.nav.helse.db.Daos
import no.nav.helse.db.SessionContext
import no.nav.helse.db.SessionFactory
import no.nav.helse.db.VedtakBegrunnelseTypeFraDatabase
import no.nav.helse.mediator.oppgave.ApiOppgaveService
import no.nav.helse.modell.totrinnsvurdering.TotrinnsvurderingTilstand.AVVENTER_BESLUTTER
import no.nav.helse.modell.totrinnsvurdering.TotrinnsvurderingTilstand.AVVENTER_SAKSBEHANDLER
import no.nav.helse.spesialist.api.Personhåndterer
import no.nav.helse.spesialist.api.StansAutomatiskBehandlinghåndterer
import no.nav.helse.spesialist.api.auditLogTeller
import no.nav.helse.spesialist.api.graphql.ContextValues
import no.nav.helse.spesialist.api.graphql.byggRespons
import no.nav.helse.spesialist.api.graphql.graphqlErrorException
import no.nav.helse.spesialist.api.graphql.mapping.tilApiDag
import no.nav.helse.spesialist.api.graphql.mapping.tilApiHendelse
import no.nav.helse.spesialist.api.graphql.mapping.tilApiInntektstype
import no.nav.helse.spesialist.api.graphql.mapping.tilApiNotat
import no.nav.helse.spesialist.api.graphql.mapping.tilApiPeriodehistorikkType
import no.nav.helse.spesialist.api.graphql.mapping.tilApiPeriodetilstand
import no.nav.helse.spesialist.api.graphql.mapping.tilApiPeriodetype
import no.nav.helse.spesialist.api.graphql.mapping.tilVilkarsgrunnlagV2
import no.nav.helse.spesialist.api.graphql.mapping.toVarselDto
import no.nav.helse.spesialist.api.graphql.schema.ApiAdressebeskyttelse
import no.nav.helse.spesialist.api.graphql.schema.ApiAlder
import no.nav.helse.spesialist.api.graphql.schema.ApiAnnetFodselsnummer
import no.nav.helse.spesialist.api.graphql.schema.ApiAnnullering
import no.nav.helse.spesialist.api.graphql.schema.ApiAnnulleringskandidat
import no.nav.helse.spesialist.api.graphql.schema.ApiArbeidsforhold
import no.nav.helse.spesialist.api.graphql.schema.ApiArbeidsforholdoverstyring
import no.nav.helse.spesialist.api.graphql.schema.ApiArbeidsgiver
import no.nav.helse.spesialist.api.graphql.schema.ApiArbeidsgiverInntekterFraAOrdningen
import no.nav.helse.spesialist.api.graphql.schema.ApiAvslag
import no.nav.helse.spesialist.api.graphql.schema.ApiAvslagstype
import no.nav.helse.spesialist.api.graphql.schema.ApiBehandling
import no.nav.helse.spesialist.api.graphql.schema.ApiBeregnetPeriode
import no.nav.helse.spesialist.api.graphql.schema.ApiDagoverstyring
import no.nav.helse.spesialist.api.graphql.schema.ApiDagtype
import no.nav.helse.spesialist.api.graphql.schema.ApiEndrePaVent
import no.nav.helse.spesialist.api.graphql.schema.ApiEnhet
import no.nav.helse.spesialist.api.graphql.schema.ApiFaresignal
import no.nav.helse.spesialist.api.graphql.schema.ApiFjernetFraPaVent
import no.nav.helse.spesialist.api.graphql.schema.ApiGhostPeriode
import no.nav.helse.spesialist.api.graphql.schema.ApiHandling
import no.nav.helse.spesialist.api.graphql.schema.ApiInfotrygdutbetaling
import no.nav.helse.spesialist.api.graphql.schema.ApiInntektFraAOrdningen
import no.nav.helse.spesialist.api.graphql.schema.ApiInntektoverstyring
import no.nav.helse.spesialist.api.graphql.schema.ApiKjonn
import no.nav.helse.spesialist.api.graphql.schema.ApiKommentar
import no.nav.helse.spesialist.api.graphql.schema.ApiLagtPaVent
import no.nav.helse.spesialist.api.graphql.schema.ApiMinimumSykdomsgradOverstyring
import no.nav.helse.spesialist.api.graphql.schema.ApiOppgaveForPeriodevisning
import no.nav.helse.spesialist.api.graphql.schema.ApiOpphevStansAutomatiskBehandlingSaksbehandler
import no.nav.helse.spesialist.api.graphql.schema.ApiPaVent
import no.nav.helse.spesialist.api.graphql.schema.ApiPensjonsgivendeInntekt
import no.nav.helse.spesialist.api.graphql.schema.ApiPeriodeHistorikkElementNy
import no.nav.helse.spesialist.api.graphql.schema.ApiPeriodehandling
import no.nav.helse.spesialist.api.graphql.schema.ApiPeriodetilstand
import no.nav.helse.spesialist.api.graphql.schema.ApiPeriodevilkar
import no.nav.helse.spesialist.api.graphql.schema.ApiPerson
import no.nav.helse.spesialist.api.graphql.schema.ApiPersoninfo
import no.nav.helse.spesialist.api.graphql.schema.ApiRisikovurdering
import no.nav.helse.spesialist.api.graphql.schema.ApiSaksbehandler
import no.nav.helse.spesialist.api.graphql.schema.ApiSelvstendigNaering
import no.nav.helse.spesialist.api.graphql.schema.ApiSimulering
import no.nav.helse.spesialist.api.graphql.schema.ApiSimuleringsdetaljer
import no.nav.helse.spesialist.api.graphql.schema.ApiSimuleringslinje
import no.nav.helse.spesialist.api.graphql.schema.ApiSimuleringsperiode
import no.nav.helse.spesialist.api.graphql.schema.ApiSimuleringsutbetaling
import no.nav.helse.spesialist.api.graphql.schema.ApiSkjonnsfastsettingstype
import no.nav.helse.spesialist.api.graphql.schema.ApiStansAutomatiskBehandlingSaksbehandler
import no.nav.helse.spesialist.api.graphql.schema.ApiSykepengedager
import no.nav.helse.spesialist.api.graphql.schema.ApiSykepengegrunnlagskjonnsfastsetting
import no.nav.helse.spesialist.api.graphql.schema.ApiTildeling
import no.nav.helse.spesialist.api.graphql.schema.ApiTilleggsinfoForInntektskilde
import no.nav.helse.spesialist.api.graphql.schema.ApiTotrinnsvurdering
import no.nav.helse.spesialist.api.graphql.schema.ApiTotrinnsvurderingRetur
import no.nav.helse.spesialist.api.graphql.schema.ApiUberegnetPeriode
import no.nav.helse.spesialist.api.graphql.schema.ApiUtbetaling
import no.nav.helse.spesialist.api.graphql.schema.ApiUtbetalingstatus
import no.nav.helse.spesialist.api.graphql.schema.ApiUtbetalingtype
import no.nav.helse.spesialist.api.graphql.schema.ApiVedtakBegrunnelse
import no.nav.helse.spesialist.api.graphql.schema.ApiVedtakUtfall
import no.nav.helse.spesialist.api.graphql.schema.ApiVurdering
import no.nav.helse.spesialist.api.objectMapper
import no.nav.helse.spesialist.api.oppgave.OppgaveForPeriodevisningDto
import no.nav.helse.spesialist.api.overstyring.Dagtype
import no.nav.helse.spesialist.api.overstyring.OverstyringArbeidsforholdDto
import no.nav.helse.spesialist.api.overstyring.OverstyringInntektDto
import no.nav.helse.spesialist.api.overstyring.OverstyringMinimumSykdomsgradDto
import no.nav.helse.spesialist.api.overstyring.OverstyringTidslinjeDto
import no.nav.helse.spesialist.api.overstyring.Skjonnsfastsettingstype
import no.nav.helse.spesialist.api.overstyring.SkjønnsfastsettingSykepengegrunnlagDto
import no.nav.helse.spesialist.api.periodehistorikk.PeriodehistorikkType
import no.nav.helse.spesialist.api.tildeling.TildelingApiDto
import no.nav.helse.spesialist.application.PersonPseudoId
import no.nav.helse.spesialist.application.Snapshothenter
import no.nav.helse.spesialist.application.logg.logg
import no.nav.helse.spesialist.application.logg.loggInfo
import no.nav.helse.spesialist.application.logg.loggThrowable
import no.nav.helse.spesialist.application.logg.loggWarn
import no.nav.helse.spesialist.application.logg.sikkerlogg
import no.nav.helse.spesialist.application.snapshot.SnapshotBeregnetPeriode
import no.nav.helse.spesialist.application.snapshot.SnapshotGhostPeriode
import no.nav.helse.spesialist.application.snapshot.SnapshotOppdrag
import no.nav.helse.spesialist.application.snapshot.SnapshotUberegnetPeriode
import no.nav.helse.spesialist.application.snapshot.SnapshotUtbetalingstatus
import no.nav.helse.spesialist.application.snapshot.SnapshotUtbetalingtype
import no.nav.helse.spesialist.domain.ArbeidsgiverIdentifikator
import no.nav.helse.spesialist.domain.Identitetsnummer
import no.nav.helse.spesialist.domain.Person
import no.nav.helse.spesialist.domain.Personinfo
import no.nav.helse.spesialist.domain.Saksbehandler
import no.nav.helse.spesialist.domain.tilgangskontroll.Tilgangsgruppe
import org.slf4j.LoggerFactory
import org.slf4j.event.Level
import java.time.LocalDate
import java.util.UUID

class PersonQueryHandler(
    private val daos: Daos,
    private val apiOppgaveService: ApiOppgaveService,
    private val stansAutomatiskBehandlinghåndterer: StansAutomatiskBehandlinghåndterer,
    private val personhåndterer: Personhåndterer,
    private val snapshothenter: Snapshothenter,
    private val sessionFactory: SessionFactory,
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
            daos.oppgaveApiDao.finnOppgaveId(identitetsnummer.value)?.let { oppgaveId ->
                transaction.oppgaveRepository
                    .finn(oppgaveId)
                    ?.kanSeesAv(saksbehandler, tilgangsgrupper)
            } ?: true

        if (!harTilgangTilOppgave) {
            logg.warn("Saksbehandler mangler tilgang til aktiv oppgave på denne personen")
            manglerTilgangTilPerson(saksbehandler, identitetsnummer)
        }

        val snapshot =
            try {
                loggInfo("Henter snapshot for person", "fødselsnummer: ${identitetsnummer.value}")
                (
                    snapshothenter.hentPerson(identitetsnummer.value)
                        ?: null.also { loggWarn("Fikk ikke personsnapshot fra Spleis") }
                )
            } catch (e: Exception) {
                loggThrowable(
                    message = "Klarte ikke hente snapshot fra Spleis",
                    securelogDetails = "identitetsnummer=${identitetsnummer.value}",
                    throwable = e,
                )
                klarteIkkeHentePerson(saksbehandler, identitetsnummer)
            }?.takeUnless { it.arbeidsgivere.isEmpty() }

        if (snapshot == null) {
            personIkkeFunnet(saksbehandler, identitetsnummer)
        }

        return ApiPerson(
            versjon = snapshot.versjon,
            aktorId = snapshot.aktorId,
            fodselsnummer = snapshot.fodselsnummer,
            andreFodselsnummer =
                daos.personApiDao
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
            personinfo = personEntity.tilApiPersoninfo(),
            enhet = daos.personApiDao.finnEnhet(snapshot.fodselsnummer).let { ApiEnhet(it.id, it.navn) },
            tildeling = daos.tildelingApiDao.tildelingForPerson(snapshot.fodselsnummer)?.tilTildeling(),
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
                            navn =
                                transaction.arbeidsgiverRepository
                                    .finn(ArbeidsgiverIdentifikator.fraString(organisasjonsnummer))
                                    ?.navn
                                    ?.navn
                                    ?: "navn er utilgjengelig",
                        )
                    },
            arbeidsgivere =
                run {
                    val overstyringer = daos.overstyringApiDao.finnOverstyringer(snapshot.fodselsnummer)

                    snapshot.arbeidsgivere
                        .filterNot { it.organisasjonsnummer == "SELVSTENDIG" }
                        .map { arbeidsgiver ->
                            val orgnummer = arbeidsgiver.organisasjonsnummer
                            val navn =
                                transaction.arbeidsgiverRepository
                                    .finn(ArbeidsgiverIdentifikator.fraString(orgnummer))
                                    ?.navn
                                    ?.navn
                                    ?: "navn er utilgjengelig"
                            val ghostPerioder =
                                arbeidsgiver.ghostPerioder.map {
                                    it.tilGhostPeriode(orgnummer)
                                }
                            val fødselsnummer = snapshot.fodselsnummer
                            val behandlinger = arbeidsgiver.behandlinger
                            val risikovurderinger =
                                daos.risikovurderingApiDao.finnRisikovurderinger(identitetsnummer.value)
                            ApiArbeidsgiver(
                                organisasjonsnummer = orgnummer,
                                navn = navn,
                                ghostPerioder = ghostPerioder,
                                behandlinger =
                                    behandlinger.mapIndexed { index, behandling ->
                                        val oppgaveId = daos.oppgaveApiDao.finnOppgaveId(fødselsnummer)
                                        val perioderSomSkalViseAktiveVarsler =
                                            daos.varselApiRepository.perioderSomSkalViseVarsler(oppgaveId)
                                        ApiBehandling(
                                            id = behandling.id,
                                            perioder =
                                                behandling.perioder.map { periode ->
                                                    val erSisteBehandling = index == 0
                                                    when (periode) {
                                                        is SnapshotUberegnetPeriode -> {
                                                            val vedtaksperiodeId = periode.vedtaksperiodeId
                                                            val skalViseAktiveVarsler =
                                                                erSisteBehandling &&
                                                                    perioderSomSkalViseAktiveVarsler.contains(
                                                                        vedtaksperiodeId,
                                                                    )
                                                            ApiUberegnetPeriode(
                                                                behandlingId = periode.behandlingId,
                                                                erForkastet = periode.erForkastet,
                                                                fom = periode.fom,
                                                                tom = periode.tom,
                                                                id =
                                                                    UUID.nameUUIDFromBytes(
                                                                        vedtaksperiodeId
                                                                            .toString()
                                                                            .toByteArray() + index.toByte(),
                                                                    ),
                                                                inntektstype = periode.inntektstype.tilApiInntektstype(),
                                                                opprettet = periode.opprettet,
                                                                periodetype = periode.tilApiPeriodetype(),
                                                                tidslinje = periode.tidslinje.map { it.tilApiDag() },
                                                                vedtaksperiodeId = vedtaksperiodeId,
                                                                periodetilstand =
                                                                    periode.periodetilstand.tilApiPeriodetilstand(
                                                                        true,
                                                                    ),
                                                                skjaeringstidspunkt = periode.skjaeringstidspunkt,
                                                                hendelser = periode.hendelser.map { it.tilApiHendelse() },
                                                                varsler =
                                                                    if (skalViseAktiveVarsler) {
                                                                        daos.varselApiRepository
                                                                            .finnVarslerForUberegnetPeriode(
                                                                                vedtaksperiodeId,
                                                                            ).map { it.toVarselDto() }
                                                                    } else {
                                                                        daos.varselApiRepository
                                                                            .finnGodkjenteVarslerForUberegnetPeriode(
                                                                                vedtaksperiodeId,
                                                                            ).map { it.toVarselDto() }
                                                                    },
                                                                notater =
                                                                    daos.notatApiDao
                                                                        .finnNotater(vedtaksperiodeId)
                                                                        .map { it.tilApiNotat() },
                                                            )
                                                        }

                                                        is SnapshotBeregnetPeriode -> {
                                                            val periodetilstand =
                                                                periode.periodetilstand.tilApiPeriodetilstand(
                                                                    erSisteBehandling,
                                                                )
                                                            val vedtaksperiodeId = periode.vedtaksperiodeId
                                                            val oppgaveDto: OppgaveForPeriodevisningDto? by lazy {
                                                                if (erSisteBehandling) {
                                                                    daos.oppgaveApiDao.finnPeriodeoppgave(
                                                                        periode.vedtaksperiodeId,
                                                                    )
                                                                } else {
                                                                    null
                                                                }
                                                            }

                                                            fun byggHandlinger(): List<ApiHandling> =
                                                                if (periodetilstand != ApiPeriodetilstand.TilGodkjenning) {
                                                                    listOf(
                                                                        ApiHandling(
                                                                            ApiPeriodehandling.UTBETALE,
                                                                            false,
                                                                            "perioden er ikke til godkjenning",
                                                                        ),
                                                                    )
                                                                } else {
                                                                    val handlinger =
                                                                        listOf(
                                                                            ApiHandling(
                                                                                ApiPeriodehandling.UTBETALE,
                                                                                true,
                                                                            ),
                                                                        )
                                                                    handlinger +
                                                                        when (oppgaveDto?.kanAvvises) {
                                                                            true ->
                                                                                ApiHandling(
                                                                                    ApiPeriodehandling.AVVISE,
                                                                                    true,
                                                                                )

                                                                            else ->
                                                                                ApiHandling(
                                                                                    ApiPeriodehandling.AVVISE,
                                                                                    false,
                                                                                    "Spleis støtter ikke å avvise perioden",
                                                                                )
                                                                        }
                                                                }

                                                            fun mapLagtPåVentJson(json: String): Triple<List<String>, LocalDate?, String?> {
                                                                val node = objectMapper.readTree(json)
                                                                val påVentÅrsaker =
                                                                    node["årsaker"].map { it["årsak"].asText() }
                                                                val frist =
                                                                    node["frist"]
                                                                        ?.takeUnless { it.isMissingOrNull() }
                                                                        ?.asLocalDate()
                                                                val notattekst =
                                                                    node["notattekst"]
                                                                        ?.takeUnless { it.isMissingOrNull() }
                                                                        ?.asText()
                                                                return Triple(påVentÅrsaker, frist, notattekst)
                                                            }

                                                            fun mapNotattekstJson(json: String): String? {
                                                                val node = objectMapper.readTree(json)
                                                                val notattekst =
                                                                    node["notattekst"]
                                                                        ?.takeUnless { it.isMissingOrNull() }
                                                                        ?.asText()
                                                                return notattekst
                                                            }
                                                            ApiBeregnetPeriode(
                                                                behandlingId = periode.behandlingId,
                                                                erForkastet = periode.erForkastet,
                                                                fom = periode.fom,
                                                                tom = periode.tom,
                                                                id =
                                                                    UUID.nameUUIDFromBytes(
                                                                        vedtaksperiodeId
                                                                            .toString()
                                                                            .toByteArray() + index.toByte(),
                                                                    ),
                                                                inntektstype = periode.inntektstype.tilApiInntektstype(),
                                                                opprettet = periode.opprettet,
                                                                periodetype = periode.tilApiPeriodetype(),
                                                                tidslinje = periode.tidslinje.map { it.tilApiDag() },
                                                                vedtaksperiodeId = vedtaksperiodeId,
                                                                periodetilstand = periodetilstand,
                                                                handlinger = byggHandlinger(),
                                                                egenskaper =
                                                                    apiOppgaveService.hentEgenskaper(
                                                                        periode.vedtaksperiodeId,
                                                                        periode.utbetaling.id,
                                                                    ),
                                                                hendelser = periode.hendelser.map { it.tilApiHendelse() },
                                                                notater =
                                                                    daos.notatApiDao
                                                                        .finnNotater(vedtaksperiodeId)
                                                                        .map { it.tilApiNotat() },
                                                                historikkinnslag =
                                                                    daos.periodehistorikkApiDao
                                                                        .finn(periode.utbetaling.id)
                                                                        .map {
                                                                            when (it.type) {
                                                                                PeriodehistorikkType.LEGG_PA_VENT -> {
                                                                                    val (påVentÅrsaker, frist, notattekst) =
                                                                                        mapLagtPåVentJson(
                                                                                            json = it.json,
                                                                                        )
                                                                                    ApiLagtPaVent(
                                                                                        id = it.id,
                                                                                        type = it.type.tilApiPeriodehistorikkType(),
                                                                                        timestamp = it.timestamp,
                                                                                        saksbehandlerIdent = it.saksbehandlerIdent,
                                                                                        dialogRef = it.dialogRef,
                                                                                        arsaker = påVentÅrsaker,
                                                                                        frist = frist,
                                                                                        notattekst = notattekst,
                                                                                        kommentarer =
                                                                                            daos.notatApiDao
                                                                                                .finnKommentarer(it.dialogRef!!.toLong())
                                                                                                .map { kommentar ->
                                                                                                    ApiKommentar(
                                                                                                        id = kommentar.id,
                                                                                                        tekst = kommentar.tekst,
                                                                                                        opprettet = kommentar.opprettet,
                                                                                                        saksbehandlerident = kommentar.saksbehandlerident,
                                                                                                        feilregistrert_tidspunkt = kommentar.feilregistrertTidspunkt,
                                                                                                    )
                                                                                                },
                                                                                    )
                                                                                }

                                                                                PeriodehistorikkType.ENDRE_PA_VENT -> {
                                                                                    val (påVentÅrsaker, frist, notattekst) =
                                                                                        mapLagtPåVentJson(
                                                                                            json = it.json,
                                                                                        )
                                                                                    ApiEndrePaVent(
                                                                                        id = it.id,
                                                                                        type = it.type.tilApiPeriodehistorikkType(),
                                                                                        timestamp = it.timestamp,
                                                                                        saksbehandlerIdent = it.saksbehandlerIdent,
                                                                                        dialogRef = it.dialogRef,
                                                                                        arsaker = påVentÅrsaker,
                                                                                        frist = frist,
                                                                                        notattekst = notattekst,
                                                                                        kommentarer =
                                                                                            daos.notatApiDao
                                                                                                .finnKommentarer(it.dialogRef!!.toLong())
                                                                                                .map { kommentar ->
                                                                                                    ApiKommentar(
                                                                                                        id = kommentar.id,
                                                                                                        tekst = kommentar.tekst,
                                                                                                        opprettet = kommentar.opprettet,
                                                                                                        saksbehandlerident = kommentar.saksbehandlerident,
                                                                                                        feilregistrert_tidspunkt = kommentar.feilregistrertTidspunkt,
                                                                                                    )
                                                                                                },
                                                                                    )
                                                                                }

                                                                                PeriodehistorikkType.FJERN_FRA_PA_VENT -> {
                                                                                    ApiFjernetFraPaVent(
                                                                                        id = it.id,
                                                                                        type = it.type.tilApiPeriodehistorikkType(),
                                                                                        timestamp = it.timestamp,
                                                                                        saksbehandlerIdent = it.saksbehandlerIdent,
                                                                                        dialogRef = it.dialogRef,
                                                                                    )
                                                                                }

                                                                                PeriodehistorikkType.TOTRINNSVURDERING_RETUR -> {
                                                                                    val notattekst =
                                                                                        mapNotattekstJson(json = it.json)
                                                                                    ApiTotrinnsvurderingRetur(
                                                                                        id = it.id,
                                                                                        type = it.type.tilApiPeriodehistorikkType(),
                                                                                        saksbehandlerIdent = it.saksbehandlerIdent,
                                                                                        timestamp = it.timestamp,
                                                                                        dialogRef = it.dialogRef,
                                                                                        notattekst = notattekst,
                                                                                        kommentarer =
                                                                                            it.dialogRef?.let { dialogRef ->
                                                                                                daos.notatApiDao
                                                                                                    .finnKommentarer(
                                                                                                        dialogRef.toLong(),
                                                                                                    ).map { kommentar ->
                                                                                                        ApiKommentar(
                                                                                                            id = kommentar.id,
                                                                                                            tekst = kommentar.tekst,
                                                                                                            opprettet = kommentar.opprettet,
                                                                                                            saksbehandlerident = kommentar.saksbehandlerident,
                                                                                                            feilregistrert_tidspunkt = kommentar.feilregistrertTidspunkt,
                                                                                                        )
                                                                                                    }
                                                                                            } ?: emptyList(),
                                                                                    )
                                                                                }

                                                                                PeriodehistorikkType.STANS_AUTOMATISK_BEHANDLING_SAKSBEHANDLER -> {
                                                                                    val notattekst =
                                                                                        mapNotattekstJson(json = it.json)
                                                                                    ApiStansAutomatiskBehandlingSaksbehandler(
                                                                                        id = it.id,
                                                                                        type = it.type.tilApiPeriodehistorikkType(),
                                                                                        saksbehandlerIdent = it.saksbehandlerIdent,
                                                                                        timestamp = it.timestamp,
                                                                                        dialogRef = it.dialogRef,
                                                                                        notattekst = notattekst,
                                                                                        kommentarer =
                                                                                            daos.notatApiDao
                                                                                                .finnKommentarer(it.dialogRef!!.toLong())
                                                                                                .map { kommentar ->
                                                                                                    ApiKommentar(
                                                                                                        id = kommentar.id,
                                                                                                        tekst = kommentar.tekst,
                                                                                                        opprettet = kommentar.opprettet,
                                                                                                        saksbehandlerident = kommentar.saksbehandlerident,
                                                                                                        feilregistrert_tidspunkt = kommentar.feilregistrertTidspunkt,
                                                                                                    )
                                                                                                },
                                                                                    )
                                                                                }

                                                                                PeriodehistorikkType.OPPHEV_STANS_AUTOMATISK_BEHANDLING_SAKSBEHANDLER -> {
                                                                                    val notattekst =
                                                                                        mapNotattekstJson(json = it.json)
                                                                                    ApiOpphevStansAutomatiskBehandlingSaksbehandler(
                                                                                        id = it.id,
                                                                                        type = it.type.tilApiPeriodehistorikkType(),
                                                                                        saksbehandlerIdent = it.saksbehandlerIdent,
                                                                                        timestamp = it.timestamp,
                                                                                        dialogRef = it.dialogRef,
                                                                                        notattekst = notattekst,
                                                                                        kommentarer =
                                                                                            daos.notatApiDao
                                                                                                .finnKommentarer(it.dialogRef!!.toLong())
                                                                                                .map { kommentar ->
                                                                                                    ApiKommentar(
                                                                                                        id = kommentar.id,
                                                                                                        tekst = kommentar.tekst,
                                                                                                        opprettet = kommentar.opprettet,
                                                                                                        saksbehandlerident = kommentar.saksbehandlerident,
                                                                                                        feilregistrert_tidspunkt = kommentar.feilregistrertTidspunkt,
                                                                                                    )
                                                                                                },
                                                                                    )
                                                                                }

                                                                                else -> {
                                                                                    ApiPeriodeHistorikkElementNy(
                                                                                        id = it.id,
                                                                                        type = it.type.tilApiPeriodehistorikkType(),
                                                                                        saksbehandlerIdent = it.saksbehandlerIdent,
                                                                                        timestamp = it.timestamp,
                                                                                        dialogRef = it.dialogRef,
                                                                                    )
                                                                                }
                                                                            }
                                                                        },
                                                                beregningId = periode.beregningId,
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
                                                                skjaeringstidspunkt = periode.skjaeringstidspunkt,
                                                                utbetaling =
                                                                    periode.utbetaling.let {
                                                                        ApiUtbetaling(
                                                                            id = it.id,
                                                                            arbeidsgiverFagsystemId = it.arbeidsgiverFagsystemId,
                                                                            arbeidsgiverNettoBelop = it.arbeidsgiverNettoBelop,
                                                                            personFagsystemId = it.personFagsystemId,
                                                                            personNettoBelop = it.personNettoBelop,
                                                                            status =
                                                                                when (it.statusEnum) {
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
                                                                                },
                                                                            type =
                                                                                when (it.typeEnum) {
                                                                                    SnapshotUtbetalingtype.ANNULLERING -> ApiUtbetalingtype.ANNULLERING
                                                                                    SnapshotUtbetalingtype.ETTERUTBETALING -> ApiUtbetalingtype.ETTERUTBETALING
                                                                                    SnapshotUtbetalingtype.FERIEPENGER -> ApiUtbetalingtype.FERIEPENGER
                                                                                    SnapshotUtbetalingtype.REVURDERING -> ApiUtbetalingtype.REVURDERING
                                                                                    SnapshotUtbetalingtype.UTBETALING -> ApiUtbetalingtype.UTBETALING
                                                                                    SnapshotUtbetalingtype.UNKNOWN_VALUE -> ApiUtbetalingtype.UKJENT
                                                                                },
                                                                            vurdering =
                                                                                it.vurdering?.let { vurdering ->
                                                                                    ApiVurdering(
                                                                                        automatisk = vurdering.automatisk,
                                                                                        godkjent = vurdering.godkjent,
                                                                                        ident = vurdering.ident,
                                                                                        tidsstempel = vurdering.tidsstempel,
                                                                                    )
                                                                                },
                                                                            arbeidsgiversimulering = it.arbeidsgiveroppdrag?.tilSimulering(),
                                                                            personsimulering = it.personoppdrag?.tilSimulering(),
                                                                        )
                                                                    },
                                                                vilkarsgrunnlagId = periode.vilkarsgrunnlagId,
                                                                risikovurdering =
                                                                    risikovurderinger[vedtaksperiodeId]?.let { vurdering ->
                                                                        ApiRisikovurdering(
                                                                            funn = vurdering.funn.tilFaresignaler(),
                                                                            kontrollertOk = vurdering.kontrollertOk.tilFaresignaler(),
                                                                        )
                                                                    },
                                                                varsler =
                                                                    if (erSisteBehandling) {
                                                                        daos.varselApiRepository
                                                                            .finnVarslerSomIkkeErInaktiveForSisteBehandling(
                                                                                vedtaksperiodeId,
                                                                                periode.utbetaling.id,
                                                                            ).map { it.toVarselDto() }
                                                                    } else {
                                                                        daos.varselApiRepository
                                                                            .finnVarslerSomIkkeErInaktiveFor(
                                                                                vedtaksperiodeId,
                                                                                periode.utbetaling.id,
                                                                            ).map { it.toVarselDto() }
                                                                    },
                                                                oppgave =
                                                                    oppgaveDto?.let { oppgaveDto ->
                                                                        ApiOppgaveForPeriodevisning(
                                                                            id = oppgaveDto.id,
                                                                        )
                                                                    },
                                                                totrinnsvurdering =
                                                                    run {
                                                                        if (oppgaveDto == null) {
                                                                            null
                                                                        } else {
                                                                            sessionFactory.transactionalSessionScope { sessionContext ->
                                                                                sessionContext.totrinnsvurderingRepository
                                                                                    .finnAktivForPerson(
                                                                                        fødselsnummer,
                                                                                    )?.let {
                                                                                        ApiTotrinnsvurdering(
                                                                                            erRetur = it.tilstand == AVVENTER_SAKSBEHANDLER && it.saksbehandler != null,
                                                                                            saksbehandler = it.saksbehandler?.value,
                                                                                            beslutter = it.beslutter?.value,
                                                                                            erBeslutteroppgave = it.tilstand == AVVENTER_BESLUTTER,
                                                                                        )
                                                                                    }
                                                                            }
                                                                        }
                                                                    },
                                                                paVent =
                                                                    daos.påVentApiDao
                                                                        .hentAktivPåVent(vedtaksperiodeId)
                                                                        ?.let {
                                                                            ApiPaVent(
                                                                                frist = it.frist,
                                                                                oid = it.oid,
                                                                            )
                                                                        },
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
                                                                annullering =
                                                                    if (erSisteBehandling) {
                                                                        daos.annulleringRepository
                                                                            .finnAnnullering(vedtaksperiodeId)
                                                                            ?.let {
                                                                                val saksbehandler =
                                                                                    daos.saksbehandlerRepository.finn(it.saksbehandlerOid)
                                                                                        ?: error("Fant ikke saksbehandler med ${it.saksbehandlerOid}")
                                                                                ApiAnnullering(
                                                                                    saksbehandlerIdent = saksbehandler.ident.value,
                                                                                    arbeidsgiverFagsystemId = it.arbeidsgiverFagsystemId,
                                                                                    personFagsystemId = it.personFagsystemId,
                                                                                    tidspunkt = it.tidspunkt,
                                                                                    arsaker = it.årsaker,
                                                                                    begrunnelse = it.kommentar,
                                                                                    vedtaksperiodeId = it.vedtaksperiodeId,
                                                                                )
                                                                            }
                                                                    } else {
                                                                        null
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

                                                        else -> throw Exception("Ukjent tidslinjeperiode")
                                                    }
                                                },
                                        )
                                    },
                                overstyringer =
                                    overstyringer
                                        .filter { it.relevantFor(orgnummer) }
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
                                    daos.arbeidsgiverApiDao
                                        .finnArbeidsforhold(
                                            fødselsnummer,
                                            orgnummer,
                                        ).map {
                                            ApiArbeidsforhold(
                                                stillingstittel = it.stillingstittel,
                                                stillingsprosent = it.stillingsprosent,
                                                startdato = it.startdato,
                                                sluttdato = it.sluttdato,
                                            )
                                        },
                                inntekterFraAordningen =
                                    daos.arbeidsgiverApiDao
                                        .finnArbeidsgiverInntekterFraAordningen(
                                            fødselsnummer,
                                            orgnummer,
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
                    val selvstendig =
                        snapshot.arbeidsgivere
                            .firstOrNull { it.organisasjonsnummer == "SELVSTENDIG" }
                            ?: return@run null

                    ApiSelvstendigNaering(
                        behandlinger =
                            selvstendig.behandlinger.mapIndexed { index, behandling ->
                                val oppgaveId = daos.oppgaveApiDao.finnOppgaveId(snapshot.fodselsnummer)
                                val perioderSomSkalViseAktiveVarsler =
                                    daos.varselApiRepository.perioderSomSkalViseVarsler(oppgaveId)
                                ApiBehandling(
                                    id = behandling.id,
                                    perioder =
                                        behandling.perioder.map { periode ->
                                            val erSisteBehandling = index == 0
                                            when (periode) {
                                                is SnapshotUberegnetPeriode -> {
                                                    val vedtaksperiodeId = periode.vedtaksperiodeId
                                                    val skalViseAktiveVarsler =
                                                        erSisteBehandling &&
                                                            perioderSomSkalViseAktiveVarsler.contains(
                                                                vedtaksperiodeId,
                                                            )
                                                    ApiUberegnetPeriode(
                                                        behandlingId = periode.behandlingId,
                                                        erForkastet = periode.erForkastet,
                                                        fom = periode.fom,
                                                        tom = periode.tom,
                                                        id =
                                                            UUID.nameUUIDFromBytes(
                                                                vedtaksperiodeId
                                                                    .toString()
                                                                    .toByteArray() + index.toByte(),
                                                            ),
                                                        inntektstype = periode.inntektstype.tilApiInntektstype(),
                                                        opprettet = periode.opprettet,
                                                        periodetype = periode.tilApiPeriodetype(),
                                                        tidslinje = periode.tidslinje.map { it.tilApiDag() },
                                                        vedtaksperiodeId = vedtaksperiodeId,
                                                        periodetilstand =
                                                            periode.periodetilstand
                                                                .tilApiPeriodetilstand(true),
                                                        skjaeringstidspunkt = periode.skjaeringstidspunkt,
                                                        hendelser = periode.hendelser.map { it.tilApiHendelse() },
                                                        varsler =
                                                            if (skalViseAktiveVarsler) {
                                                                daos.varselApiRepository
                                                                    .finnVarslerForUberegnetPeriode(
                                                                        vedtaksperiodeId,
                                                                    ).map { it.toVarselDto() }
                                                            } else {
                                                                daos.varselApiRepository
                                                                    .finnGodkjenteVarslerForUberegnetPeriode(
                                                                        vedtaksperiodeId,
                                                                    ).map { it.toVarselDto() }
                                                            },
                                                        notater =
                                                            daos.notatApiDao
                                                                .finnNotater(vedtaksperiodeId)
                                                                .map { it.tilApiNotat() },
                                                    )
                                                }

                                                is SnapshotBeregnetPeriode -> {
                                                    val fødselsnummer = snapshot.fodselsnummer
                                                    val orgnummer = "SELVSTENDIG"
                                                    val risikovurderinger =
                                                        daos.risikovurderingApiDao.finnRisikovurderinger(
                                                            identitetsnummer.value,
                                                        )

                                                    val periodetilstand =
                                                        periode.periodetilstand.tilApiPeriodetilstand(erSisteBehandling)
                                                    val vedtaksperiodeId = periode.vedtaksperiodeId
                                                    val oppgaveDto: OppgaveForPeriodevisningDto? by lazy {
                                                        if (erSisteBehandling) {
                                                            daos.oppgaveApiDao.finnPeriodeoppgave(
                                                                periode.vedtaksperiodeId,
                                                            )
                                                        } else {
                                                            null
                                                        }
                                                    }

                                                    fun byggHandlinger(): List<ApiHandling> =
                                                        if (periodetilstand != ApiPeriodetilstand.TilGodkjenning) {
                                                            listOf(
                                                                ApiHandling(
                                                                    ApiPeriodehandling.UTBETALE,
                                                                    false,
                                                                    "perioden er ikke til godkjenning",
                                                                ),
                                                            )
                                                        } else {
                                                            val handlinger =
                                                                listOf(ApiHandling(ApiPeriodehandling.UTBETALE, true))
                                                            handlinger +
                                                                when (oppgaveDto?.kanAvvises) {
                                                                    true ->
                                                                        ApiHandling(
                                                                            ApiPeriodehandling.AVVISE,
                                                                            true,
                                                                        )

                                                                    else ->
                                                                        ApiHandling(
                                                                            ApiPeriodehandling.AVVISE,
                                                                            false,
                                                                            "Spleis støtter ikke å avvise perioden",
                                                                        )
                                                                }
                                                        }

                                                    fun mapLagtPåVentJson(json: String): Triple<List<String>, LocalDate?, String?> {
                                                        val node = objectMapper.readTree(json)
                                                        val påVentÅrsaker = node["årsaker"].map { it["årsak"].asText() }
                                                        val frist =
                                                            node["frist"]
                                                                ?.takeUnless { it.isMissingOrNull() }
                                                                ?.asLocalDate()
                                                        val notattekst =
                                                            node["notattekst"]
                                                                ?.takeUnless { it.isMissingOrNull() }
                                                                ?.asText()
                                                        return Triple(påVentÅrsaker, frist, notattekst)
                                                    }

                                                    fun mapNotattekstJson(json: String): String? {
                                                        val node = objectMapper.readTree(json)
                                                        val notattekst =
                                                            node["notattekst"]
                                                                ?.takeUnless { it.isMissingOrNull() }
                                                                ?.asText()
                                                        return notattekst
                                                    }
                                                    ApiBeregnetPeriode(
                                                        behandlingId = periode.behandlingId,
                                                        erForkastet = periode.erForkastet,
                                                        fom = periode.fom,
                                                        tom = periode.tom,
                                                        id =
                                                            UUID.nameUUIDFromBytes(
                                                                vedtaksperiodeId
                                                                    .toString()
                                                                    .toByteArray() + index.toByte(),
                                                            ),
                                                        inntektstype = periode.inntektstype.tilApiInntektstype(),
                                                        opprettet = periode.opprettet,
                                                        periodetype = periode.tilApiPeriodetype(),
                                                        tidslinje = periode.tidslinje.map { it.tilApiDag() },
                                                        vedtaksperiodeId = vedtaksperiodeId,
                                                        periodetilstand = periodetilstand,
                                                        handlinger = byggHandlinger(),
                                                        egenskaper =
                                                            apiOppgaveService.hentEgenskaper(
                                                                periode.vedtaksperiodeId,
                                                                periode.utbetaling.id,
                                                            ),
                                                        hendelser = periode.hendelser.map { it.tilApiHendelse() },
                                                        notater =
                                                            daos.notatApiDao
                                                                .finnNotater(vedtaksperiodeId)
                                                                .map { it.tilApiNotat() },
                                                        historikkinnslag =
                                                            daos.periodehistorikkApiDao
                                                                .finn(periode.utbetaling.id)
                                                                .map {
                                                                    when (it.type) {
                                                                        PeriodehistorikkType.LEGG_PA_VENT -> {
                                                                            val (påVentÅrsaker, frist, notattekst) =
                                                                                mapLagtPåVentJson(
                                                                                    json = it.json,
                                                                                )
                                                                            ApiLagtPaVent(
                                                                                id = it.id,
                                                                                type = it.type.tilApiPeriodehistorikkType(),
                                                                                timestamp = it.timestamp,
                                                                                saksbehandlerIdent = it.saksbehandlerIdent,
                                                                                dialogRef = it.dialogRef,
                                                                                arsaker = påVentÅrsaker,
                                                                                frist = frist,
                                                                                notattekst = notattekst,
                                                                                kommentarer =
                                                                                    daos.notatApiDao
                                                                                        .finnKommentarer(it.dialogRef!!.toLong())
                                                                                        .map { kommentar ->
                                                                                            ApiKommentar(
                                                                                                id = kommentar.id,
                                                                                                tekst = kommentar.tekst,
                                                                                                opprettet = kommentar.opprettet,
                                                                                                saksbehandlerident = kommentar.saksbehandlerident,
                                                                                                feilregistrert_tidspunkt = kommentar.feilregistrertTidspunkt,
                                                                                            )
                                                                                        },
                                                                            )
                                                                        }

                                                                        PeriodehistorikkType.ENDRE_PA_VENT -> {
                                                                            val (påVentÅrsaker, frist, notattekst) =
                                                                                mapLagtPåVentJson(
                                                                                    json = it.json,
                                                                                )
                                                                            ApiEndrePaVent(
                                                                                id = it.id,
                                                                                type = it.type.tilApiPeriodehistorikkType(),
                                                                                timestamp = it.timestamp,
                                                                                saksbehandlerIdent = it.saksbehandlerIdent,
                                                                                dialogRef = it.dialogRef,
                                                                                arsaker = påVentÅrsaker,
                                                                                frist = frist,
                                                                                notattekst = notattekst,
                                                                                kommentarer =
                                                                                    daos.notatApiDao
                                                                                        .finnKommentarer(it.dialogRef!!.toLong())
                                                                                        .map { kommentar ->
                                                                                            ApiKommentar(
                                                                                                id = kommentar.id,
                                                                                                tekst = kommentar.tekst,
                                                                                                opprettet = kommentar.opprettet,
                                                                                                saksbehandlerident = kommentar.saksbehandlerident,
                                                                                                feilregistrert_tidspunkt = kommentar.feilregistrertTidspunkt,
                                                                                            )
                                                                                        },
                                                                            )
                                                                        }

                                                                        PeriodehistorikkType.FJERN_FRA_PA_VENT -> {
                                                                            ApiFjernetFraPaVent(
                                                                                id = it.id,
                                                                                type = it.type.tilApiPeriodehistorikkType(),
                                                                                timestamp = it.timestamp,
                                                                                saksbehandlerIdent = it.saksbehandlerIdent,
                                                                                dialogRef = it.dialogRef,
                                                                            )
                                                                        }

                                                                        PeriodehistorikkType.TOTRINNSVURDERING_RETUR -> {
                                                                            val notattekst =
                                                                                mapNotattekstJson(json = it.json)
                                                                            ApiTotrinnsvurderingRetur(
                                                                                id = it.id,
                                                                                type = it.type.tilApiPeriodehistorikkType(),
                                                                                saksbehandlerIdent = it.saksbehandlerIdent,
                                                                                timestamp = it.timestamp,
                                                                                dialogRef = it.dialogRef,
                                                                                notattekst = notattekst,
                                                                                kommentarer =
                                                                                    it.dialogRef?.let { dialogRef ->
                                                                                        daos.notatApiDao
                                                                                            .finnKommentarer(dialogRef.toLong())
                                                                                            .map { kommentar ->
                                                                                                ApiKommentar(
                                                                                                    id = kommentar.id,
                                                                                                    tekst = kommentar.tekst,
                                                                                                    opprettet = kommentar.opprettet,
                                                                                                    saksbehandlerident = kommentar.saksbehandlerident,
                                                                                                    feilregistrert_tidspunkt = kommentar.feilregistrertTidspunkt,
                                                                                                )
                                                                                            }
                                                                                    } ?: emptyList(),
                                                                            )
                                                                        }

                                                                        PeriodehistorikkType.STANS_AUTOMATISK_BEHANDLING_SAKSBEHANDLER -> {
                                                                            val notattekst =
                                                                                mapNotattekstJson(json = it.json)
                                                                            ApiStansAutomatiskBehandlingSaksbehandler(
                                                                                id = it.id,
                                                                                type = it.type.tilApiPeriodehistorikkType(),
                                                                                saksbehandlerIdent = it.saksbehandlerIdent,
                                                                                timestamp = it.timestamp,
                                                                                dialogRef = it.dialogRef,
                                                                                notattekst = notattekst,
                                                                                kommentarer =
                                                                                    daos.notatApiDao
                                                                                        .finnKommentarer(it.dialogRef!!.toLong())
                                                                                        .map { kommentar ->
                                                                                            ApiKommentar(
                                                                                                id = kommentar.id,
                                                                                                tekst = kommentar.tekst,
                                                                                                opprettet = kommentar.opprettet,
                                                                                                saksbehandlerident = kommentar.saksbehandlerident,
                                                                                                feilregistrert_tidspunkt = kommentar.feilregistrertTidspunkt,
                                                                                            )
                                                                                        },
                                                                            )
                                                                        }

                                                                        PeriodehistorikkType.OPPHEV_STANS_AUTOMATISK_BEHANDLING_SAKSBEHANDLER -> {
                                                                            val notattekst =
                                                                                mapNotattekstJson(json = it.json)
                                                                            ApiOpphevStansAutomatiskBehandlingSaksbehandler(
                                                                                id = it.id,
                                                                                type = it.type.tilApiPeriodehistorikkType(),
                                                                                saksbehandlerIdent = it.saksbehandlerIdent,
                                                                                timestamp = it.timestamp,
                                                                                dialogRef = it.dialogRef,
                                                                                notattekst = notattekst,
                                                                                kommentarer =
                                                                                    daos.notatApiDao
                                                                                        .finnKommentarer(it.dialogRef!!.toLong())
                                                                                        .map { kommentar ->
                                                                                            ApiKommentar(
                                                                                                id = kommentar.id,
                                                                                                tekst = kommentar.tekst,
                                                                                                opprettet = kommentar.opprettet,
                                                                                                saksbehandlerident = kommentar.saksbehandlerident,
                                                                                                feilregistrert_tidspunkt = kommentar.feilregistrertTidspunkt,
                                                                                            )
                                                                                        },
                                                                            )
                                                                        }

                                                                        else -> {
                                                                            ApiPeriodeHistorikkElementNy(
                                                                                id = it.id,
                                                                                type = it.type.tilApiPeriodehistorikkType(),
                                                                                saksbehandlerIdent = it.saksbehandlerIdent,
                                                                                timestamp = it.timestamp,
                                                                                dialogRef = it.dialogRef,
                                                                            )
                                                                        }
                                                                    }
                                                                },
                                                        beregningId = periode.beregningId,
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
                                                        skjaeringstidspunkt = periode.skjaeringstidspunkt,
                                                        utbetaling =
                                                            periode.utbetaling.let {
                                                                ApiUtbetaling(
                                                                    id = it.id,
                                                                    arbeidsgiverFagsystemId = it.arbeidsgiverFagsystemId,
                                                                    arbeidsgiverNettoBelop = it.arbeidsgiverNettoBelop,
                                                                    personFagsystemId = it.personFagsystemId,
                                                                    personNettoBelop = it.personNettoBelop,
                                                                    status =
                                                                        when (it.statusEnum) {
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
                                                                        },
                                                                    type =
                                                                        when (it.typeEnum) {
                                                                            SnapshotUtbetalingtype.ANNULLERING -> ApiUtbetalingtype.ANNULLERING
                                                                            SnapshotUtbetalingtype.ETTERUTBETALING -> ApiUtbetalingtype.ETTERUTBETALING
                                                                            SnapshotUtbetalingtype.FERIEPENGER -> ApiUtbetalingtype.FERIEPENGER
                                                                            SnapshotUtbetalingtype.REVURDERING -> ApiUtbetalingtype.REVURDERING
                                                                            SnapshotUtbetalingtype.UTBETALING -> ApiUtbetalingtype.UTBETALING
                                                                            SnapshotUtbetalingtype.UNKNOWN_VALUE -> ApiUtbetalingtype.UKJENT
                                                                        },
                                                                    vurdering =
                                                                        it.vurdering?.let { vurdering ->
                                                                            ApiVurdering(
                                                                                automatisk = vurdering.automatisk,
                                                                                godkjent = vurdering.godkjent,
                                                                                ident = vurdering.ident,
                                                                                tidsstempel = vurdering.tidsstempel,
                                                                            )
                                                                        },
                                                                    arbeidsgiversimulering = it.arbeidsgiveroppdrag?.tilSimulering(),
                                                                    personsimulering = it.personoppdrag?.tilSimulering(),
                                                                )
                                                            },
                                                        vilkarsgrunnlagId = periode.vilkarsgrunnlagId,
                                                        risikovurdering =
                                                            risikovurderinger[vedtaksperiodeId]?.let { vurdering ->
                                                                ApiRisikovurdering(
                                                                    funn = vurdering.funn.tilFaresignaler(),
                                                                    kontrollertOk = vurdering.kontrollertOk.tilFaresignaler(),
                                                                )
                                                            },
                                                        varsler =
                                                            if (erSisteBehandling) {
                                                                daos.varselApiRepository
                                                                    .finnVarslerSomIkkeErInaktiveForSisteBehandling(
                                                                        vedtaksperiodeId,
                                                                        periode.utbetaling.id,
                                                                    ).map { it.toVarselDto() }
                                                            } else {
                                                                daos.varselApiRepository
                                                                    .finnVarslerSomIkkeErInaktiveFor(
                                                                        vedtaksperiodeId,
                                                                        periode.utbetaling.id,
                                                                    ).map { it.toVarselDto() }
                                                            },
                                                        oppgave =
                                                            oppgaveDto?.let { oppgaveDto ->
                                                                ApiOppgaveForPeriodevisning(
                                                                    id = oppgaveDto.id,
                                                                )
                                                            },
                                                        totrinnsvurdering =
                                                            run {
                                                                if (oppgaveDto == null) {
                                                                    null
                                                                } else {
                                                                    sessionFactory.transactionalSessionScope { sessionContext ->
                                                                        sessionContext.totrinnsvurderingRepository
                                                                            .finnAktivForPerson(
                                                                                fødselsnummer,
                                                                            )?.let {
                                                                                ApiTotrinnsvurdering(
                                                                                    erRetur = it.tilstand == AVVENTER_SAKSBEHANDLER && it.saksbehandler != null,
                                                                                    saksbehandler = it.saksbehandler?.value,
                                                                                    beslutter = it.beslutter?.value,
                                                                                    erBeslutteroppgave = it.tilstand == AVVENTER_BESLUTTER,
                                                                                )
                                                                            }
                                                                    }
                                                                }
                                                            },
                                                        paVent =
                                                            daos.påVentApiDao.hentAktivPåVent(vedtaksperiodeId)?.let {
                                                                ApiPaVent(
                                                                    frist = it.frist,
                                                                    oid = it.oid,
                                                                )
                                                            },
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
                                                        annullering =
                                                            if (erSisteBehandling) {
                                                                daos.annulleringRepository
                                                                    .finnAnnullering(vedtaksperiodeId)
                                                                    ?.let {
                                                                        val saksbehandler =
                                                                            daos.saksbehandlerRepository.finn(it.saksbehandlerOid)
                                                                                ?: error("Fant ikke saksbehandler med ${it.saksbehandlerOid}")
                                                                        ApiAnnullering(
                                                                            saksbehandlerIdent = saksbehandler.ident.value,
                                                                            arbeidsgiverFagsystemId = it.arbeidsgiverFagsystemId,
                                                                            personFagsystemId = it.personFagsystemId,
                                                                            tidspunkt = it.tidspunkt,
                                                                            arsaker = it.årsaker,
                                                                            begrunnelse = it.kommentar,
                                                                            vedtaksperiodeId = it.vedtaksperiodeId,
                                                                        )
                                                                    }
                                                            } else {
                                                                null
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

                                                else -> throw Exception("Ukjent tidslinjeperiode")
                                            }
                                        },
                                )
                            },
                        overstyringer =
                            daos.overstyringApiDao
                                .finnOverstyringer(snapshot.fodselsnummer)
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
                    )
                },
            infotrygdutbetalinger =
                daos.personApiDao
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

    private fun Person.tilApiPersoninfo(): ApiPersoninfo {
        val personinfo = info ?: error("Fant ikke personinfo i databasen")

        return ApiPersoninfo(
            fornavn = personinfo.fornavn,
            mellomnavn = personinfo.mellomnavn,
            etternavn = personinfo.etternavn,
            fodselsdato = personinfo.fødselsdato!!,
            kjonn =
                when (personinfo.kjønn) {
                    Personinfo.Kjønn.Kvinne -> ApiKjonn.Kvinne
                    Personinfo.Kjønn.Mann -> ApiKjonn.Mann
                    Personinfo.Kjønn.Ukjent, null -> ApiKjonn.Ukjent
                },
            adressebeskyttelse =
                when (personinfo.adressebeskyttelse) {
                    Personinfo.Adressebeskyttelse.Ugradert -> ApiAdressebeskyttelse.Ugradert
                    Personinfo.Adressebeskyttelse.Fortrolig -> ApiAdressebeskyttelse.Fortrolig
                    Personinfo.Adressebeskyttelse.StrengtFortrolig -> ApiAdressebeskyttelse.StrengtFortrolig
                    Personinfo.Adressebeskyttelse.StrengtFortroligUtland -> ApiAdressebeskyttelse.StrengtFortroligUtland
                    Personinfo.Adressebeskyttelse.Ukjent -> ApiAdressebeskyttelse.Ukjent
                },
            unntattFraAutomatisering =
                stansAutomatiskBehandlinghåndterer.unntattFraAutomatiskGodkjenning(id.value),
            fullmakt = daos.vergemålApiDao.harFullmakt(id.value),
            automatiskBehandlingStansetAvSaksbehandler =
                daos.stansAutomatiskBehandlingSaksbehandlerDao.erStanset(id.value),
        )
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

private fun List<JsonNode>.tilFaresignaler(): List<ApiFaresignal> = map { objectMapper.readValue(it.traverse(), object : TypeReference<ApiFaresignal>() {}) }
