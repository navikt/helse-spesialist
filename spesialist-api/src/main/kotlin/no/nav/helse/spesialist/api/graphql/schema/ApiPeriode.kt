package no.nav.helse.spesialist.api.graphql.schema

import com.expediagroup.graphql.generator.annotations.GraphQLIgnore
import com.expediagroup.graphql.generator.annotations.GraphQLName
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.JsonNode
import com.github.navikt.tbd_libs.jackson.asLocalDate
import com.github.navikt.tbd_libs.jackson.isMissingOrNull
import io.ktor.utils.io.core.toByteArray
import no.nav.helse.db.api.NotatApiDao
import no.nav.helse.db.api.OppgaveApiDao
import no.nav.helse.db.api.PeriodehistorikkApiDao
import no.nav.helse.db.api.PåVentApiDao
import no.nav.helse.db.api.TotrinnsvurderingApiDao
import no.nav.helse.db.api.VarselApiRepository
import no.nav.helse.mediator.oppgave.ApiOppgaveService
import no.nav.helse.spesialist.api.Saksbehandlerhåndterer
import no.nav.helse.spesialist.api.Toggle
import no.nav.helse.spesialist.api.graphql.mapping.tilApiDag
import no.nav.helse.spesialist.api.graphql.mapping.tilApiHendelse
import no.nav.helse.spesialist.api.graphql.mapping.tilSkjematype
import no.nav.helse.spesialist.api.graphql.mapping.toVarselDto
import no.nav.helse.spesialist.api.objectMapper
import no.nav.helse.spesialist.api.oppgave.OppgaveForPeriodevisningDto
import no.nav.helse.spesialist.api.periodehistorikk.PeriodehistorikkType
import no.nav.helse.spesialist.api.risikovurdering.RisikovurderingApiDto
import no.nav.helse.spleis.graphql.enums.GraphQLInntektstype
import no.nav.helse.spleis.graphql.enums.GraphQLPeriodetilstand
import no.nav.helse.spleis.graphql.enums.GraphQLPeriodetype
import no.nav.helse.spleis.graphql.enums.GraphQLUtbetalingstatus
import no.nav.helse.spleis.graphql.hentsnapshot.Alder
import no.nav.helse.spleis.graphql.hentsnapshot.GraphQLBeregnetPeriode
import no.nav.helse.spleis.graphql.hentsnapshot.GraphQLOppdrag
import no.nav.helse.spleis.graphql.hentsnapshot.GraphQLTidslinjeperiode
import no.nav.helse.spleis.graphql.hentsnapshot.Sykepengedager
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.spleis.graphql.enums.Utbetalingtype as GraphQLUtbetalingtype

@GraphQLName("Inntektstype")
enum class ApiInntektstype { ENARBEIDSGIVER, FLEREARBEIDSGIVERE }

@GraphQLName("Periodetype")
enum class ApiPeriodetype {
    FORLENGELSE,
    FORSTEGANGSBEHANDLING,
    INFOTRYGDFORLENGELSE,
    OVERGANG_FRA_IT,
}

@GraphQLName("Periodetilstand")
enum class ApiPeriodetilstand {
    UtbetalingFeilet,
    RevurderingFeilet,
    AnnulleringFeilet,
    TilAnnullering,
    TilUtbetaling,
    Annullert,
    Utbetalt,
    IngenUtbetaling,
    TilInfotrygd,
    ForberederGodkjenning,
    ManglerInformasjon,
    TilGodkjenning,
    VenterPaEnAnnenPeriode,
    UtbetaltVenterPaEnAnnenPeriode,
    TilSkjonnsfastsettelse,
    AvventerInntektsopplysninger,
    Ukjent,
}

@GraphQLName("Utbetalingstatus")
enum class ApiUtbetalingstatus {
    ANNULLERT,
    FORKASTET,
    GODKJENT,
    GODKJENTUTENUTBETALING,
    IKKEGODKJENT,
    OVERFORT,
    SENDT,
    UBETALT,
    UTBETALINGFEILET,
    UTBETALT,
    UKJENT,
}

@GraphQLName("Utbetalingtype")
enum class ApiUtbetalingtype {
    ANNULLERING,
    ETTERUTBETALING,
    FERIEPENGER,
    REVURDERING,
    UTBETALING,
    UKJENT,
}

@GraphQLName("Varselstatus")
enum class ApiVarselstatus {
    AKTIV,
    VURDERT,
    GODKJENT,
    AVVIST,
}

@GraphQLName("Vurdering")
data class ApiVurdering(
    val automatisk: Boolean,
    val godkjent: Boolean,
    val ident: String,
    val tidsstempel: LocalDateTime,
)

@GraphQLName("Simuleringslinje")
data class ApiSimuleringslinje(
    val fom: LocalDate,
    val tom: LocalDate,
    val dagsats: Int,
    val grad: Int,
)

@GraphQLName("Simuleringsdetaljer")
data class ApiSimuleringsdetaljer(
    val fom: LocalDate,
    val tom: LocalDate,
    val belop: Int,
    val antallSats: Int,
    val klassekode: String,
    val klassekodebeskrivelse: String,
    val konto: String,
    val refunderesOrgNr: String,
    val sats: Double,
    val tilbakeforing: Boolean,
    val typeSats: String,
    val uforegrad: Int,
    val utbetalingstype: String,
)

@GraphQLName("Simuleringsutbetaling")
data class ApiSimuleringsutbetaling(
    val mottakerId: String,
    val mottakerNavn: String,
    val forfall: LocalDate,
    val feilkonto: Boolean,
    val detaljer: List<ApiSimuleringsdetaljer>,
)

@GraphQLName("Simuleringsperiode")
data class ApiSimuleringsperiode(
    val fom: LocalDate,
    val tom: LocalDate,
    val utbetalinger: List<ApiSimuleringsutbetaling>,
)

@GraphQLName("Simulering")
data class ApiSimulering(
    val fagsystemId: String,
    val tidsstempel: LocalDateTime,
    val utbetalingslinjer: List<ApiSimuleringslinje>,
    val totalbelop: Int?,
    val perioder: List<ApiSimuleringsperiode>?,
)

@GraphQLName("Utbetaling")
data class ApiUtbetaling(
    val id: UUID,
    val arbeidsgiverFagsystemId: String,
    val arbeidsgiverNettoBelop: Int,
    val personFagsystemId: String,
    val personNettoBelop: Int,
    val status: ApiUtbetalingstatus,
    val type: ApiUtbetalingtype,
    val vurdering: ApiVurdering?,
    val arbeidsgiversimulering: ApiSimulering?,
    val personsimulering: ApiSimulering?,
)

@GraphQLName("Periodevilkar")
data class ApiPeriodevilkar(
    val alder: Alder,
    val sykepengedager: Sykepengedager,
)

@GraphQLName("Kommentar")
data class ApiKommentar(
    val id: Int,
    val tekst: String,
    val opprettet: LocalDateTime,
    val saksbehandlerident: String,
    val feilregistrert_tidspunkt: LocalDateTime?,
)

@GraphQLName("Notater")
data class ApiNotater(
    val id: UUID,
    val notater: List<ApiNotat>,
)

@GraphQLName("Notat")
data class ApiNotat(
    val id: Int,
    val dialogRef: Int,
    val tekst: String,
    val opprettet: LocalDateTime,
    val saksbehandlerOid: UUID,
    val saksbehandlerNavn: String,
    val saksbehandlerEpost: String,
    val saksbehandlerIdent: String,
    val vedtaksperiodeId: UUID,
    val feilregistrert: Boolean,
    val feilregistrert_tidspunkt: LocalDateTime?,
    val type: ApiNotatType,
    val kommentarer: List<ApiKommentar>,
)

@GraphQLName("NotatType")
enum class ApiNotatType {
    Retur,
    Generelt,
    PaaVent,
    OpphevStans,
}

@GraphQLName("Historikkinnslag")
sealed interface ApiHistorikkinnslag {
    val id: Int
    val type: PeriodehistorikkType
    val timestamp: LocalDateTime
    val saksbehandlerIdent: String?
    val dialogRef: Int?
}

@GraphQLName("LagtPaVent")
data class ApiLagtPaVent(
    override val id: Int,
    override val type: PeriodehistorikkType,
    override val timestamp: LocalDateTime,
    override val saksbehandlerIdent: String?,
    override val dialogRef: Int?,
    val arsaker: List<String>,
    val frist: LocalDate?,
    val notattekst: String?,
    val kommentarer: List<ApiKommentar>,
) : ApiHistorikkinnslag

@GraphQLName("EndrePaVent")
data class ApiEndrePaVent(
    override val id: Int,
    override val type: PeriodehistorikkType,
    override val timestamp: LocalDateTime,
    override val saksbehandlerIdent: String?,
    override val dialogRef: Int?,
    val arsaker: List<String>,
    val frist: LocalDate?,
    val notattekst: String?,
    val kommentarer: List<ApiKommentar>,
) : ApiHistorikkinnslag

@GraphQLName("FjernetFraPaVent")
data class ApiFjernetFraPaVent(
    override val id: Int,
    override val type: PeriodehistorikkType,
    override val timestamp: LocalDateTime,
    override val saksbehandlerIdent: String?,
    override val dialogRef: Int?,
) : ApiHistorikkinnslag

@GraphQLName("TotrinnsvurderingRetur")
data class ApiTotrinnsvurderingRetur(
    override val id: Int,
    override val type: PeriodehistorikkType,
    override val timestamp: LocalDateTime,
    override val saksbehandlerIdent: String?,
    override val dialogRef: Int?,
    val notattekst: String?,
    val kommentarer: List<ApiKommentar>,
) : ApiHistorikkinnslag

@GraphQLName("PeriodeHistorikkElementNy")
data class ApiPeriodeHistorikkElementNy(
    override val id: Int,
    override val type: PeriodehistorikkType,
    override val timestamp: LocalDateTime,
    override val saksbehandlerIdent: String?,
    override val dialogRef: Int?,
) : ApiHistorikkinnslag

@GraphQLName("Faresignal")
data class ApiFaresignal(
    val beskrivelse: String,
    val kategori: List<String>,
)

@GraphQLName("Risikovurdering")
data class ApiRisikovurdering(
    val funn: List<ApiFaresignal>?,
    val kontrollertOk: List<ApiFaresignal>,
)

@GraphQLName("VarselDTO")
data class ApiVarselDTO(
    val generasjonId: UUID,
    val definisjonId: UUID,
    val opprettet: LocalDateTime,
    val kode: String,
    val tittel: String,
    val forklaring: String?,
    val handling: String?,
    val vurdering: ApiVarselvurderingDTO?,
) {
    @GraphQLName("VarselvurderingDTO")
    data class ApiVarselvurderingDTO(
        val ident: String,
        val tidsstempel: LocalDateTime,
        val status: ApiVarselstatus,
    )
}

@GraphQLName("Periode")
interface ApiPeriode {
    fun behandlingId(): UUID

    fun erForkastet(): Boolean

    fun fom(): LocalDate

    fun tom(): LocalDate

    fun id(): UUID

    fun inntektstype(): ApiInntektstype

    fun opprettet(): LocalDateTime

    fun periodetype(): ApiPeriodetype

    fun tidslinje(): List<ApiDag>

    fun vedtaksperiodeId(): UUID

    fun periodetilstand(): ApiPeriodetilstand

    fun skjaeringstidspunkt(): LocalDate

    fun varsler(): List<ApiVarselDTO>

    fun hendelser(): List<ApiHendelse>

    @GraphQLIgnore
    fun periodetilstand(
        tilstand: GraphQLPeriodetilstand,
        erSisteGenerasjon: Boolean,
    ) = when (tilstand) {
        GraphQLPeriodetilstand.ANNULLERINGFEILET -> ApiPeriodetilstand.AnnulleringFeilet
        GraphQLPeriodetilstand.ANNULLERT -> ApiPeriodetilstand.Annullert
        GraphQLPeriodetilstand.INGENUTBETALING -> ApiPeriodetilstand.IngenUtbetaling
        GraphQLPeriodetilstand.REVURDERINGFEILET -> ApiPeriodetilstand.RevurderingFeilet
        GraphQLPeriodetilstand.TILANNULLERING -> ApiPeriodetilstand.TilAnnullering
        GraphQLPeriodetilstand.TILINFOTRYGD -> ApiPeriodetilstand.TilInfotrygd
        GraphQLPeriodetilstand.TILUTBETALING -> ApiPeriodetilstand.TilUtbetaling
        GraphQLPeriodetilstand.UTBETALT -> ApiPeriodetilstand.Utbetalt
        GraphQLPeriodetilstand.FORBEREDERGODKJENNING -> ApiPeriodetilstand.ForberederGodkjenning
        GraphQLPeriodetilstand.MANGLERINFORMASJON -> ApiPeriodetilstand.ManglerInformasjon
        GraphQLPeriodetilstand.TILGODKJENNING -> ApiPeriodetilstand.TilGodkjenning
        GraphQLPeriodetilstand.UTBETALINGFEILET -> ApiPeriodetilstand.UtbetalingFeilet
        GraphQLPeriodetilstand.VENTERPAANNENPERIODE -> ApiPeriodetilstand.VenterPaEnAnnenPeriode
        GraphQLPeriodetilstand.UTBETALTVENTERPAANNENPERIODE -> {
            if (Toggle.BehandleEnOgEnPeriode.enabled && erSisteGenerasjon) {
                ApiPeriodetilstand.VenterPaEnAnnenPeriode
            } else {
                ApiPeriodetilstand.UtbetaltVenterPaEnAnnenPeriode
            }
        }

        GraphQLPeriodetilstand.AVVENTERINNTEKTSOPPLYSNINGER -> ApiPeriodetilstand.AvventerInntektsopplysninger
        GraphQLPeriodetilstand.TILSKJONNSFASTSETTELSE -> ApiPeriodetilstand.TilSkjonnsfastsettelse
        else -> ApiPeriodetilstand.Ukjent
    }

    @GraphQLIgnore
    fun erForkastet(periode: GraphQLTidslinjeperiode): Boolean = periode.erForkastet

    @GraphQLIgnore
    fun fom(periode: GraphQLTidslinjeperiode): LocalDate = periode.fom

    @GraphQLIgnore
    fun tom(periode: GraphQLTidslinjeperiode): LocalDate = periode.tom

    @GraphQLIgnore
    fun inntektstype(periode: GraphQLTidslinjeperiode): ApiInntektstype =
        when (periode.inntektstype) {
            GraphQLInntektstype.ENARBEIDSGIVER -> ApiInntektstype.ENARBEIDSGIVER
            GraphQLInntektstype.FLEREARBEIDSGIVERE -> ApiInntektstype.FLEREARBEIDSGIVERE
            else -> throw Exception("Ukjent inntektstype ${periode.inntektstype}")
        }

    @GraphQLIgnore
    fun opprettet(periode: GraphQLTidslinjeperiode): LocalDateTime = periode.opprettet

    @GraphQLIgnore
    fun periodetype(periode: GraphQLTidslinjeperiode): ApiPeriodetype =
        when (periode.periodetype) {
            GraphQLPeriodetype.FORLENGELSE -> ApiPeriodetype.FORLENGELSE
            GraphQLPeriodetype.FORSTEGANGSBEHANDLING -> ApiPeriodetype.FORSTEGANGSBEHANDLING
            GraphQLPeriodetype.INFOTRYGDFORLENGELSE -> ApiPeriodetype.INFOTRYGDFORLENGELSE
            GraphQLPeriodetype.OVERGANGFRAIT -> ApiPeriodetype.OVERGANG_FRA_IT
            else -> throw Exception("Ukjent periodetype ${periode.periodetype}")
        }

    @GraphQLIgnore
    fun notater(
        notatDao: NotatApiDao,
        vedtaksperiodeId: UUID,
    ): List<ApiNotat> =
        notatDao.finnNotater(vedtaksperiodeId).map {
            ApiNotat(
                id = it.id,
                dialogRef = it.dialogRef,
                tekst = it.tekst,
                opprettet = it.opprettet,
                saksbehandlerOid = it.saksbehandlerOid,
                saksbehandlerNavn = it.saksbehandlerNavn,
                saksbehandlerEpost = it.saksbehandlerEpost,
                saksbehandlerIdent = it.saksbehandlerIdent,
                vedtaksperiodeId = it.vedtaksperiodeId,
                feilregistrert = it.feilregistrert,
                feilregistrert_tidspunkt = it.feilregistrert_tidspunkt,
                type = it.type.tilSkjematype(),
                kommentarer =
                    it.kommentarer.map { kommentar ->
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

    @GraphQLIgnore
    fun tidslinje(periode: GraphQLTidslinjeperiode): List<ApiDag> = periode.tidslinje.map { it.tilApiDag() }
}

@GraphQLIgnore
interface UberegnetPeriodeSchema : ApiPeriode {
    fun notater(): List<ApiNotat>
}

@GraphQLName("UberegnetPeriode")
class ApiUberegnetPeriode(private val resolver: UberegnetPeriodeSchema) : UberegnetPeriodeSchema by resolver

@GraphQLName("Periodehandling")
enum class ApiPeriodehandling {
    UTBETALE,
    AVVISE,
}

@GraphQLName("Handling")
data class ApiHandling(
    val type: ApiPeriodehandling,
    val tillatt: Boolean,
    val begrunnelse: String? = null,
)

@GraphQLName("BeregnetPeriode")
data class ApiBeregnetPeriode(
    private val orgnummer: String,
    private val periode: GraphQLBeregnetPeriode,
    private val apiOppgaveService: ApiOppgaveService,
    private val saksbehandlerhåndterer: Saksbehandlerhåndterer,
    private val risikovurderinger: Map<UUID, RisikovurderingApiDto>,
    private val varselRepository: VarselApiRepository,
    private val oppgaveApiDao: OppgaveApiDao,
    private val periodehistorikkApiDao: PeriodehistorikkApiDao,
    private val notatDao: NotatApiDao,
    private val totrinnsvurderingApiDao: TotrinnsvurderingApiDao,
    private val påVentApiDao: PåVentApiDao,
    private val erSisteGenerasjon: Boolean,
    private val index: Int,
) : ApiPeriode {
    private val periodetilstand = periodetilstand(periode.periodetilstand, erSisteGenerasjon)

    override fun behandlingId(): UUID = periode.behandlingId

    override fun erForkastet(): Boolean = erForkastet(periode)

    override fun fom(): LocalDate = fom(periode)

    override fun tom(): LocalDate = tom(periode)

    override fun id(): UUID = UUID.nameUUIDFromBytes(vedtaksperiodeId().toString().toByteArray() + index.toByte())

    override fun inntektstype(): ApiInntektstype = inntektstype(periode)

    override fun opprettet(): LocalDateTime = opprettet(periode)

    override fun periodetype(): ApiPeriodetype = periodetype(periode)

    override fun tidslinje(): List<ApiDag> = tidslinje(periode)

    override fun vedtaksperiodeId(): UUID = periode.vedtaksperiodeId

    override fun periodetilstand(): ApiPeriodetilstand = periodetilstand

    fun handlinger() = byggHandlinger()

    fun egenskaper(): List<ApiOppgaveegenskap> = apiOppgaveService.hentEgenskaper(periode.vedtaksperiodeId, periode.utbetaling.id)

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

    fun notater(): List<ApiNotat> = notater(notatDao, vedtaksperiodeId())

    private fun mapLagtPåVentJson(json: String): Triple<List<String>, LocalDate?, String?> {
        val node = objectMapper.readTree(json)
        val påVentÅrsaker = node["årsaker"].map { it["årsak"].asText() }
        val frist = node["frist"]?.takeUnless { it.isMissingOrNull() }?.asLocalDate()
        val notattekst = node["notattekst"]?.takeUnless { it.isMissingOrNull() }?.asText()
        return Triple(påVentÅrsaker, frist, notattekst)
    }

    private fun mapTotrinnsvurderingReturJson(json: String): String? {
        val node = objectMapper.readTree(json)
        val notattekst = node["notattekst"]?.takeUnless { it.isMissingOrNull() }?.asText()
        return notattekst
    }

    fun historikkinnslag(): List<ApiHistorikkinnslag> =
        periodehistorikkApiDao
            .finn(utbetaling().id)
            .map {
                when (it.type) {
                    PeriodehistorikkType.LEGG_PA_VENT -> {
                        val (påVentÅrsaker, frist, notattekst) = mapLagtPåVentJson(json = it.json)
                        ApiLagtPaVent(
                            id = it.id,
                            type = it.type,
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
                            type = it.type,
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
                            type = it.type,
                            timestamp = it.timestamp,
                            saksbehandlerIdent = it.saksbehandlerIdent,
                            dialogRef = it.dialogRef,
                        )

                    PeriodehistorikkType.TOTRINNSVURDERING_RETUR -> {
                        val notattekst = mapTotrinnsvurderingReturJson(json = it.json)
                        ApiTotrinnsvurderingRetur(
                            id = it.id,
                            type = it.type,
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

                    else ->
                        ApiPeriodeHistorikkElementNy(
                            id = it.id,
                            type = it.type,
                            saksbehandlerIdent = it.saksbehandlerIdent,
                            timestamp = it.timestamp,
                            dialogRef = it.dialogRef,
                        )
                }
            }

    fun beregningId(): UUID = periode.beregningId

    fun forbrukteSykedager(): Int? = periode.forbrukteSykedager

    fun gjenstaendeSykedager(): Int? = periode.gjenstaendeSykedager

    fun maksdato(): LocalDate = periode.maksdato

    fun periodevilkar(): ApiPeriodevilkar =
        ApiPeriodevilkar(
            alder = periode.periodevilkar.alder,
            sykepengedager = periode.periodevilkar.sykepengedager,
        )

    override fun skjaeringstidspunkt(): LocalDate = periode.skjaeringstidspunkt

    fun utbetaling(): ApiUtbetaling =
        periode.utbetaling.let {
            ApiUtbetaling(
                id = it.id,
                arbeidsgiverFagsystemId = it.arbeidsgiverFagsystemId,
                arbeidsgiverNettoBelop = it.arbeidsgiverNettoBelop,
                personFagsystemId = it.personFagsystemId,
                personNettoBelop = it.personNettoBelop,
                status =
                    when (it.statusEnum) {
                        GraphQLUtbetalingstatus.ANNULLERT -> ApiUtbetalingstatus.ANNULLERT
                        GraphQLUtbetalingstatus.FORKASTET -> ApiUtbetalingstatus.FORKASTET
                        GraphQLUtbetalingstatus.GODKJENT -> ApiUtbetalingstatus.GODKJENT
                        GraphQLUtbetalingstatus.GODKJENTUTENUTBETALING -> ApiUtbetalingstatus.GODKJENTUTENUTBETALING
                        GraphQLUtbetalingstatus.IKKEGODKJENT -> ApiUtbetalingstatus.IKKEGODKJENT
                        GraphQLUtbetalingstatus.OVERFORT -> ApiUtbetalingstatus.OVERFORT
                        GraphQLUtbetalingstatus.SENDT -> ApiUtbetalingstatus.SENDT
                        GraphQLUtbetalingstatus.UBETALT -> ApiUtbetalingstatus.UBETALT
                        GraphQLUtbetalingstatus.UTBETALINGFEILET -> ApiUtbetalingstatus.UTBETALINGFEILET
                        GraphQLUtbetalingstatus.UTBETALT -> ApiUtbetalingstatus.UTBETALT
                        GraphQLUtbetalingstatus.__UNKNOWN_VALUE -> ApiUtbetalingstatus.UKJENT
                    },
                type =
                    when (it.typeEnum) {
                        GraphQLUtbetalingtype.ANNULLERING -> ApiUtbetalingtype.ANNULLERING
                        GraphQLUtbetalingtype.ETTERUTBETALING -> ApiUtbetalingtype.ETTERUTBETALING
                        GraphQLUtbetalingtype.FERIEPENGER -> ApiUtbetalingtype.FERIEPENGER
                        GraphQLUtbetalingtype.REVURDERING -> ApiUtbetalingtype.REVURDERING
                        GraphQLUtbetalingtype.UTBETALING -> ApiUtbetalingtype.UTBETALING
                        GraphQLUtbetalingtype.__UNKNOWN_VALUE -> ApiUtbetalingtype.UKJENT
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

    fun vilkarsgrunnlagId(): UUID? = periode.vilkarsgrunnlagId

    fun risikovurdering(): ApiRisikovurdering? =
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
        oppgaveApiDao.finnPeriodeoppgave(periode.vedtaksperiodeId)
    }

    val oppgave = oppgaveDto?.let { oppgaveDto -> ApiOppgaveForPeriodevisning(id = oppgaveDto.id) }

    fun totrinnsvurdering(): ApiTotrinnsvurdering? =
        totrinnsvurderingApiDao.hentAktiv(vedtaksperiodeId())?.let {
            ApiTotrinnsvurdering(
                erRetur = it.erRetur,
                saksbehandler = it.saksbehandler,
                beslutter = it.beslutter,
                erBeslutteroppgave = !it.erRetur && it.saksbehandler != null,
            )
        }

    fun paVent(): ApiPaVent? =
        påVentApiDao.hentAktivPåVent(vedtaksperiodeId())?.let {
            ApiPaVent(
                frist = it.frist,
                oid = it.oid,
            )
        }

    fun avslag(): List<ApiAvslag> = saksbehandlerhåndterer.hentAvslag(periode.vedtaksperiodeId, periode.utbetaling.id).toList()

    fun vedtakBegrunnelser(): List<ApiVedtakBegrunnelse> =
        saksbehandlerhåndterer.hentVedtakBegrunnelser(
            periode.vedtaksperiodeId,
            periode.utbetaling.id,
        )

    fun annullering(): ApiAnnullering? =
        if (erSisteGenerasjon) {
            saksbehandlerhåndterer.hentAnnullering(
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
}

private fun GraphQLOppdrag.tilSimulering(): ApiSimulering =
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
