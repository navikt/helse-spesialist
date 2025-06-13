package no.nav.helse.spesialist.api.graphql.resolvers

import com.expediagroup.graphql.generator.annotations.GraphQLIgnore
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.JsonNode
import com.github.navikt.tbd_libs.jackson.asLocalDate
import com.github.navikt.tbd_libs.jackson.isMissingOrNull
import io.ktor.utils.io.core.toByteArray
import no.nav.helse.FeatureToggles
import no.nav.helse.db.SessionFactory
import no.nav.helse.db.VedtakBegrunnelseDao
import no.nav.helse.db.VedtakBegrunnelseTypeFraDatabase
import no.nav.helse.db.api.NotatApiDao
import no.nav.helse.db.api.OppgaveApiDao
import no.nav.helse.db.api.PeriodehistorikkApiDao
import no.nav.helse.db.api.PåVentApiDao
import no.nav.helse.db.api.VarselApiRepository
import no.nav.helse.mediator.SaksbehandlerMediator
import no.nav.helse.mediator.oppgave.ApiOppgaveService
import no.nav.helse.modell.totrinnsvurdering.TotrinnsvurderingTilstand.AVVENTER_BESLUTTER
import no.nav.helse.modell.totrinnsvurdering.TotrinnsvurderingTilstand.AVVENTER_SAKSBEHANDLER
import no.nav.helse.spesialist.api.graphql.mapping.tilApiDag
import no.nav.helse.spesialist.api.graphql.mapping.tilApiHendelse
import no.nav.helse.spesialist.api.graphql.mapping.tilApiInntektstype
import no.nav.helse.spesialist.api.graphql.mapping.tilApiNotat
import no.nav.helse.spesialist.api.graphql.mapping.tilApiPeriodehistorikkType
import no.nav.helse.spesialist.api.graphql.mapping.tilApiPeriodetilstand
import no.nav.helse.spesialist.api.graphql.mapping.tilApiPeriodetype
import no.nav.helse.spesialist.api.graphql.mapping.toVarselDto
import no.nav.helse.spesialist.api.graphql.schema.ApiAlder
import no.nav.helse.spesialist.api.graphql.schema.ApiAnnullering
import no.nav.helse.spesialist.api.graphql.schema.ApiAvslag
import no.nav.helse.spesialist.api.graphql.schema.ApiAvslagstype
import no.nav.helse.spesialist.api.graphql.schema.ApiDag
import no.nav.helse.spesialist.api.graphql.schema.ApiEndrePaVent
import no.nav.helse.spesialist.api.graphql.schema.ApiFaresignal
import no.nav.helse.spesialist.api.graphql.schema.ApiFjernetFraPaVent
import no.nav.helse.spesialist.api.graphql.schema.ApiHandling
import no.nav.helse.spesialist.api.graphql.schema.ApiHendelse
import no.nav.helse.spesialist.api.graphql.schema.ApiHistorikkinnslag
import no.nav.helse.spesialist.api.graphql.schema.ApiInntektstype
import no.nav.helse.spesialist.api.graphql.schema.ApiKommentar
import no.nav.helse.spesialist.api.graphql.schema.ApiLagtPaVent
import no.nav.helse.spesialist.api.graphql.schema.ApiNotat
import no.nav.helse.spesialist.api.graphql.schema.ApiOppgaveForPeriodevisning
import no.nav.helse.spesialist.api.graphql.schema.ApiOppgaveegenskap
import no.nav.helse.spesialist.api.graphql.schema.ApiOpphevStansAutomatiskBehandlingSaksbehandler
import no.nav.helse.spesialist.api.graphql.schema.ApiPaVent
import no.nav.helse.spesialist.api.graphql.schema.ApiPensjonsgivendeInntekt
import no.nav.helse.spesialist.api.graphql.schema.ApiPeriodeHistorikkElementNy
import no.nav.helse.spesialist.api.graphql.schema.ApiPeriodehandling
import no.nav.helse.spesialist.api.graphql.schema.ApiPeriodetilstand
import no.nav.helse.spesialist.api.graphql.schema.ApiPeriodetype
import no.nav.helse.spesialist.api.graphql.schema.ApiPeriodevilkar
import no.nav.helse.spesialist.api.graphql.schema.ApiRisikovurdering
import no.nav.helse.spesialist.api.graphql.schema.ApiSimulering
import no.nav.helse.spesialist.api.graphql.schema.ApiSimuleringsdetaljer
import no.nav.helse.spesialist.api.graphql.schema.ApiSimuleringslinje
import no.nav.helse.spesialist.api.graphql.schema.ApiSimuleringsperiode
import no.nav.helse.spesialist.api.graphql.schema.ApiSimuleringsutbetaling
import no.nav.helse.spesialist.api.graphql.schema.ApiStansAutomatiskBehandlingSaksbehandler
import no.nav.helse.spesialist.api.graphql.schema.ApiSykepengedager
import no.nav.helse.spesialist.api.graphql.schema.ApiTotrinnsvurdering
import no.nav.helse.spesialist.api.graphql.schema.ApiTotrinnsvurderingRetur
import no.nav.helse.spesialist.api.graphql.schema.ApiUtbetaling
import no.nav.helse.spesialist.api.graphql.schema.ApiUtbetalingstatus
import no.nav.helse.spesialist.api.graphql.schema.ApiUtbetalingtype
import no.nav.helse.spesialist.api.graphql.schema.ApiVarselDTO
import no.nav.helse.spesialist.api.graphql.schema.ApiVedtakBegrunnelse
import no.nav.helse.spesialist.api.graphql.schema.ApiVedtakUtfall
import no.nav.helse.spesialist.api.graphql.schema.ApiVurdering
import no.nav.helse.spesialist.api.graphql.schema.BeregnetPeriodeSchema
import no.nav.helse.spesialist.api.objectMapper
import no.nav.helse.spesialist.api.oppgave.OppgaveForPeriodevisningDto
import no.nav.helse.spesialist.api.periodehistorikk.PeriodehistorikkType
import no.nav.helse.spesialist.api.risikovurdering.RisikovurderingApiDto
import no.nav.helse.spesialist.application.snapshot.SnapshotBeregnetPeriode
import no.nav.helse.spesialist.application.snapshot.SnapshotOppdrag
import no.nav.helse.spesialist.application.snapshot.SnapshotUtbetalingstatus
import no.nav.helse.spesialist.application.snapshot.SnapshotUtbetalingtype
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@GraphQLIgnore
data class ApiBeregnetPeriodeResolver(
    private val fødselsnummer: String,
    private val orgnummer: String,
    private val periode: SnapshotBeregnetPeriode,
    private val apiOppgaveService: ApiOppgaveService,
    private val saksbehandlerMediator: SaksbehandlerMediator,
    private val risikovurderinger: Map<UUID, RisikovurderingApiDto>,
    private val varselRepository: VarselApiRepository,
    private val oppgaveApiDao: OppgaveApiDao,
    private val periodehistorikkApiDao: PeriodehistorikkApiDao,
    private val notatDao: NotatApiDao,
    private val påVentApiDao: PåVentApiDao,
    private val erSisteGenerasjon: Boolean,
    private val index: Int,
    private val vedtakBegrunnelseDao: VedtakBegrunnelseDao,
    private val sessionFactory: SessionFactory,
    private val featureToggles: FeatureToggles,
) : BeregnetPeriodeSchema {
    private val periodetilstand = periode.periodetilstand.tilApiPeriodetilstand(erSisteGenerasjon)

    override fun behandlingId(): UUID = periode.behandlingId

    override fun erForkastet(): Boolean = periode.erForkastet

    override fun fom(): LocalDate = periode.fom

    override fun tom(): LocalDate = periode.tom

    override fun id(): UUID = UUID.nameUUIDFromBytes(vedtaksperiodeId().toString().toByteArray() + index.toByte())

    override fun inntektstype(): ApiInntektstype = periode.inntektstype.tilApiInntektstype()

    override fun opprettet(): LocalDateTime = periode.opprettet

    override fun periodetype(): ApiPeriodetype = periode.tilApiPeriodetype()

    override fun tidslinje(): List<ApiDag> = periode.tidslinje.map { it.tilApiDag() }

    override fun vedtaksperiodeId(): UUID = periode.vedtaksperiodeId

    override fun periodetilstand(): ApiPeriodetilstand = periodetilstand

    override fun handlinger() = byggHandlinger()

    override fun egenskaper(): List<ApiOppgaveegenskap> = apiOppgaveService.hentEgenskaper(periode.vedtaksperiodeId, periode.utbetaling.id)

    private fun byggHandlinger(): List<ApiHandling> =
        if (periodetilstand != ApiPeriodetilstand.TilGodkjenning) {
            listOf(
                ApiHandling(ApiPeriodehandling.UTBETALE, false, "perioden er ikke til godkjenning"),
            )
        } else {
            val handlinger = listOf(ApiHandling(ApiPeriodehandling.UTBETALE, true))
            handlinger +
                when (oppgaveDto?.kanAvvises) {
                    true -> ApiHandling(ApiPeriodehandling.AVVISE, true)
                    else -> ApiHandling(ApiPeriodehandling.AVVISE, false, "Spleis støtter ikke å avvise perioden")
                }
        }

    override fun hendelser(): List<ApiHendelse> = periode.hendelser.map { it.tilApiHendelse() }

    override fun notater(): List<ApiNotat> = notatDao.finnNotater(vedtaksperiodeId()).map { it.tilApiNotat() }

    private fun mapLagtPåVentJson(json: String): Triple<List<String>, LocalDate?, String?> {
        val node = objectMapper.readTree(json)
        val påVentÅrsaker = node["årsaker"].map { it["årsak"].asText() }
        val frist = node["frist"]?.takeUnless { it.isMissingOrNull() }?.asLocalDate()
        val notattekst = node["notattekst"]?.takeUnless { it.isMissingOrNull() }?.asText()
        return Triple(påVentÅrsaker, frist, notattekst)
    }

    private fun mapNotattekstJson(json: String): String? {
        val node = objectMapper.readTree(json)
        val notattekst = node["notattekst"]?.takeUnless { it.isMissingOrNull() }?.asText()
        return notattekst
    }

    override fun historikkinnslag(): List<ApiHistorikkinnslag> =
        periodehistorikkApiDao
            .finn(utbetaling().id)
            .map {
                when (it.type) {
                    PeriodehistorikkType.LEGG_PA_VENT -> {
                        val (påVentÅrsaker, frist, notattekst) = mapLagtPåVentJson(json = it.json)
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
                                notatDao.finnKommentarer(it.dialogRef!!.toLong()).map { kommentar ->
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
                        val (påVentÅrsaker, frist, notattekst) = mapLagtPåVentJson(json = it.json)
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
                                notatDao.finnKommentarer(it.dialogRef!!.toLong()).map { kommentar ->
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

                    PeriodehistorikkType.FJERN_FRA_PA_VENT ->
                        ApiFjernetFraPaVent(
                            id = it.id,
                            type = it.type.tilApiPeriodehistorikkType(),
                            timestamp = it.timestamp,
                            saksbehandlerIdent = it.saksbehandlerIdent,
                            dialogRef = it.dialogRef,
                        )

                    PeriodehistorikkType.TOTRINNSVURDERING_RETUR -> {
                        val notattekst = mapNotattekstJson(json = it.json)
                        ApiTotrinnsvurderingRetur(
                            id = it.id,
                            type = it.type.tilApiPeriodehistorikkType(),
                            saksbehandlerIdent = it.saksbehandlerIdent,
                            timestamp = it.timestamp,
                            dialogRef = it.dialogRef,
                            notattekst = notattekst,
                            kommentarer =
                                it.dialogRef?.let { dialogRef ->
                                    notatDao.finnKommentarer(dialogRef.toLong()).map { kommentar ->
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
                        val notattekst = mapNotattekstJson(json = it.json)
                        ApiStansAutomatiskBehandlingSaksbehandler(
                            id = it.id,
                            type = it.type.tilApiPeriodehistorikkType(),
                            saksbehandlerIdent = it.saksbehandlerIdent,
                            timestamp = it.timestamp,
                            dialogRef = it.dialogRef,
                            notattekst = notattekst,
                            kommentarer =
                                notatDao.finnKommentarer(it.dialogRef!!.toLong()).map { kommentar ->
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
                        val notattekst = mapNotattekstJson(json = it.json)
                        ApiOpphevStansAutomatiskBehandlingSaksbehandler(
                            id = it.id,
                            type = it.type.tilApiPeriodehistorikkType(),
                            saksbehandlerIdent = it.saksbehandlerIdent,
                            timestamp = it.timestamp,
                            dialogRef = it.dialogRef,
                            notattekst = notattekst,
                            kommentarer =
                                notatDao.finnKommentarer(it.dialogRef!!.toLong()).map { kommentar ->
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

                    else ->
                        ApiPeriodeHistorikkElementNy(
                            id = it.id,
                            type = it.type.tilApiPeriodehistorikkType(),
                            saksbehandlerIdent = it.saksbehandlerIdent,
                            timestamp = it.timestamp,
                            dialogRef = it.dialogRef,
                        )
                }
            }

    override fun beregningId(): UUID = periode.beregningId

    override fun forbrukteSykedager(): Int? = periode.forbrukteSykedager

    override fun gjenstaendeSykedager(): Int? = periode.gjenstaendeSykedager

    override fun maksdato(): LocalDate = periode.maksdato

    override fun periodevilkar(): ApiPeriodevilkar =
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
        )

    override fun skjaeringstidspunkt(): LocalDate = periode.skjaeringstidspunkt

    override fun utbetaling(): ApiUtbetaling =
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
        }

    override fun vilkarsgrunnlagId(): UUID? = periode.vilkarsgrunnlagId

    override fun risikovurdering(): ApiRisikovurdering? =
        risikovurderinger[vedtaksperiodeId()]?.let { vurdering ->
            ApiRisikovurdering(
                funn = vurdering.funn.tilFaresignaler(),
                kontrollertOk = vurdering.kontrollertOk.tilFaresignaler(),
            )
        }

    override fun varsler(): List<ApiVarselDTO> =
        if (erSisteGenerasjon) {
            varselRepository
                .finnVarslerSomIkkeErInaktiveForSisteGenerasjon(
                    vedtaksperiodeId(),
                    periode.utbetaling.id,
                ).map { it.toVarselDto() }
        } else {
            varselRepository
                .finnVarslerSomIkkeErInaktiveFor(
                    vedtaksperiodeId(),
                    periode.utbetaling.id,
                ).map { it.toVarselDto() }
        }

    private val oppgaveDto: OppgaveForPeriodevisningDto? by lazy {
        if (erSisteGenerasjon) oppgaveApiDao.finnPeriodeoppgave(periode.vedtaksperiodeId) else null
    }

    override fun oppgave() = oppgaveDto?.let { oppgaveDto -> ApiOppgaveForPeriodevisning(id = oppgaveDto.id) }

    override fun totrinnsvurdering(): ApiTotrinnsvurdering? {
        if (oppgaveDto == null) return null
        return sessionFactory.transactionalSessionScope { sessionContext ->
            sessionContext.totrinnsvurderingRepository.finnAktivForPerson(fødselsnummer)?.let {
                ApiTotrinnsvurdering(
                    erRetur = it.tilstand == AVVENTER_SAKSBEHANDLER && it.saksbehandler != null,
                    saksbehandler = it.saksbehandler?.value,
                    beslutter = it.beslutter?.value,
                    erBeslutteroppgave = it.tilstand == AVVENTER_BESLUTTER,
                )
            }
        }
    }

    override fun paVent(): ApiPaVent? =
        påVentApiDao.hentAktivPåVent(vedtaksperiodeId())?.let {
            ApiPaVent(
                frist = it.frist,
                oid = it.oid,
            )
        }

    override fun avslag(): List<ApiAvslag> =
        vedtakBegrunnelseDao.finnAlleVedtakBegrunnelser(
            vedtaksperiodeId = periode.vedtaksperiodeId,
            utbetalingId = periode.utbetaling.id,
        )
            .filter {
                it.type in
                    setOf(
                        VedtakBegrunnelseTypeFraDatabase.AVSLAG,
                        VedtakBegrunnelseTypeFraDatabase.DELVIS_INNVILGELSE,
                    )
            }
            .map { vedtakBegrunnelse ->
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
            }

    override fun vedtakBegrunnelser(): List<ApiVedtakBegrunnelse> =
        vedtakBegrunnelseDao.finnAlleVedtakBegrunnelser(
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
        }

    override fun annullering(): ApiAnnullering? =
        if (erSisteGenerasjon) {
            saksbehandlerMediator.hentAnnullering(
                periode.utbetaling.arbeidsgiverFagsystemId,
                periode.utbetaling.personFagsystemId,
            )?.let {
                ApiAnnullering(
                    saksbehandlerIdent = it.saksbehandlerIdent,
                    arbeidsgiverFagsystemId = it.arbeidsgiverFagsystemId,
                    personFagsystemId = it.personFagsystemId,
                    tidspunkt = it.tidspunkt,
                    arsaker = it.arsaker,
                    begrunnelse = it.begrunnelse,
                )
            }
        } else {
            null
        }

    override fun pensjonsgivendeInntekter(): List<ApiPensjonsgivendeInntekt> =
        periode.pensjonsgivendeInntekter.map {
            ApiPensjonsgivendeInntekt(
                arligBelop = it.arligBelop,
                inntektsar = it.inntektsar,
            )
        }
}

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

private fun List<JsonNode>.tilFaresignaler(): List<ApiFaresignal> =
    map { objectMapper.readValue(it.traverse(), object : TypeReference<ApiFaresignal>() {}) }
