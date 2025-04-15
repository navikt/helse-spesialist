package no.nav.helse.spesialist.api.graphql.schema

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.generator.annotations.GraphQLIgnore
import com.expediagroup.graphql.generator.annotations.GraphQLName
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

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
    val alder: ApiAlder,
    val sykepengedager: ApiSykepengedager,
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
    val type: ApiPeriodehistorikkType
    val timestamp: LocalDateTime
    val saksbehandlerIdent: String?
    val dialogRef: Int?
}

@GraphQLName("LagtPaVent")
data class ApiLagtPaVent(
    override val id: Int,
    override val type: ApiPeriodehistorikkType,
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
    override val type: ApiPeriodehistorikkType,
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
    override val type: ApiPeriodehistorikkType,
    override val timestamp: LocalDateTime,
    override val saksbehandlerIdent: String?,
    override val dialogRef: Int?,
) : ApiHistorikkinnslag

@GraphQLName("TotrinnsvurderingRetur")
data class ApiTotrinnsvurderingRetur(
    override val id: Int,
    override val type: ApiPeriodehistorikkType,
    override val timestamp: LocalDateTime,
    override val saksbehandlerIdent: String?,
    override val dialogRef: Int?,
    val notattekst: String?,
    val kommentarer: List<ApiKommentar>,
) : ApiHistorikkinnslag

@GraphQLName("PeriodeHistorikkElementNy")
data class ApiPeriodeHistorikkElementNy(
    override val id: Int,
    override val type: ApiPeriodehistorikkType,
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

@GraphQLName("Inntekt")
data class ApiInntekt(
    @GraphQLDescription("En id som identifiserer inntektskilden, f.eks. organisasjonsnummer")
    val inntektskilde: String,
    val periodiserteInntekter: List<ApiPeriodisertInntekt>,
) {
    @GraphQLName("PeriodisertInntekt")
    data class ApiPeriodisertInntekt(
        val fom: LocalDate,
        val tom: LocalDate,
        val dagligBelop: Double,
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

@GraphQLIgnore
interface BeregnetPeriodeSchema : ApiPeriode {
    fun oppgave(): ApiOppgaveForPeriodevisning?

    fun handlinger(): List<ApiHandling>

    fun egenskaper(): List<ApiOppgaveegenskap>

    fun notater(): List<ApiNotat>

    fun historikkinnslag(): List<ApiHistorikkinnslag>

    fun beregningId(): UUID

    fun forbrukteSykedager(): Int?

    fun gjenstaendeSykedager(): Int?

    fun maksdato(): LocalDate

    fun periodevilkar(): ApiPeriodevilkar

    fun utbetaling(): ApiUtbetaling

    fun vilkarsgrunnlagId(): UUID?

    fun risikovurdering(): ApiRisikovurdering?

    fun totrinnsvurdering(): ApiTotrinnsvurdering?

    fun paVent(): ApiPaVent?

    fun avslag(): List<ApiAvslag>

    fun vedtakBegrunnelser(): List<ApiVedtakBegrunnelse>

    fun annullering(): ApiAnnullering?

    @GraphQLDescription("Andre inntekter, f.eks tilkommen inntekt")
    fun inntekter(): List<ApiInntekt>
}

@GraphQLName("BeregnetPeriode")
class ApiBeregnetPeriode(private val resolver: BeregnetPeriodeSchema) : BeregnetPeriodeSchema by resolver

@GraphQLName("PeriodehistorikkType")
enum class ApiPeriodehistorikkType {
    TOTRINNSVURDERING_TIL_GODKJENNING,
    TOTRINNSVURDERING_RETUR,
    TOTRINNSVURDERING_ATTESTERT,
    VEDTAKSPERIODE_REBEREGNET,
    LEGG_PA_VENT,
    ENDRE_PA_VENT,
    FJERN_FRA_PA_VENT,
    STANS_AUTOMATISK_BEHANDLING,
    STANS_AUTOMATISK_BEHANDLING_SAKSBEHANDLER,
    OPPHEV_STANS_AUTOMATISK_BEHANDLING_SAKSBEHANDLER,
}

@GraphQLName("Alder")
data class ApiAlder(
    val alderSisteSykedag: Int,
    val oppfylt: Boolean,
)

@GraphQLName("Sykepengedager")
data class ApiSykepengedager(
    val forbrukteSykedager: Int? = null,
    val gjenstaendeSykedager: Int? = null,
    val maksdato: LocalDate,
    val oppfylt: Boolean,
    val skjaeringstidspunkt: LocalDate,
)
