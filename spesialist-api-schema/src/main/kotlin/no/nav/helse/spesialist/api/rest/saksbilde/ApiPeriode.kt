@file:UseContextualSerialization(
    BigDecimal::class,
    Boolean::class,
    LocalDate::class,
    LocalDateTime::class,
    UUID::class,
    YearMonth::class,
)

package no.nav.helse.spesialist.api.rest.saksbilde

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonTypeInfo
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseContextualSerialization
import kotlinx.serialization.json.JsonClassDiscriminator
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.UUID

@Serializable
enum class ApiInntektstype { ENARBEIDSGIVER, FLEREARBEIDSGIVERE }

@Serializable
enum class ApiPeriodetype {
    FORLENGELSE,
    FORSTEGANGSBEHANDLING,
    INFOTRYGDFORLENGELSE,
    OVERGANG_FRA_IT,
}

@Serializable
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
    AvventerAnnullering,
    AvventerInntektsopplysninger,
    Ukjent,
}

@Serializable
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

@Serializable
enum class ApiUtbetalingtype {
    ANNULLERING,
    ETTERUTBETALING,
    FERIEPENGER,
    REVURDERING,
    UTBETALING,
    UKJENT,
}

@Serializable
enum class ApiSaksbildeVarselstatus {
    AKTIV,
    VURDERT,
    GODKJENT,
    AVVIST,
}

@Serializable
data class ApiVurdering(
    val automatisk: Boolean,
    val godkjent: Boolean,
    val ident: String,
    val tidsstempel: LocalDateTime,
)

@Serializable
data class ApiSimuleringslinje(
    val fom: LocalDate,
    val tom: LocalDate,
    val dagsats: Int,
    val grad: Int,
)

@Serializable
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

@Serializable
data class ApiSimuleringsutbetaling(
    val mottakerId: String,
    val mottakerNavn: String,
    val forfall: LocalDate,
    val feilkonto: Boolean,
    val detaljer: List<ApiSimuleringsdetaljer>,
)

@Serializable
data class ApiSimuleringsperiode(
    val fom: LocalDate,
    val tom: LocalDate,
    val utbetalinger: List<ApiSimuleringsutbetaling>,
)

@Serializable
data class ApiSimulering(
    val fagsystemId: String,
    val tidsstempel: LocalDateTime,
    val utbetalingslinjer: List<ApiSimuleringslinje>,
    val totalbelop: Int?,
    val perioder: List<ApiSimuleringsperiode>?,
)

@Serializable
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

@Serializable
data class ApiPeriodevilkar(
    val alder: ApiAlder,
    val sykepengedager: ApiSykepengedager,
)

@Serializable
data class ApiSaksbildeKommentar(
    val id: Int,
    val tekst: String,
    val opprettet: LocalDateTime,
    val saksbehandlerident: String,
    val feilregistrert_tidspunkt: LocalDateTime?,
)

@OptIn(ExperimentalSerializationApi::class)
@JsonClassDiscriminator("type")
@JsonTypeInfo(use = JsonTypeInfo.Id.SIMPLE_NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@Serializable
sealed interface ApiHistorikkinnslag {
    val id: Int

    val historikktype: ApiPeriodehistorikkType
    val timestamp: LocalDateTime
    val saksbehandlerIdent: String?
    val dialogRef: Int?
}

@Serializable
@SerialName("ApiLagtPaVent")
data class ApiLagtPaVent(
    override val id: Int,
    override val historikktype: ApiPeriodehistorikkType,
    override val timestamp: LocalDateTime,
    override val saksbehandlerIdent: String?,
    override val dialogRef: Int?,
    val arsaker: List<String>,
    val frist: LocalDate?,
    val notattekst: String?,
    val kommentarer: List<ApiSaksbildeKommentar>,
) : ApiHistorikkinnslag

@Serializable
@SerialName("ApiEndrePaVent")
data class ApiEndrePaVent(
    override val id: Int,
    override val historikktype: ApiPeriodehistorikkType,
    override val timestamp: LocalDateTime,
    override val saksbehandlerIdent: String?,
    override val dialogRef: Int?,
    val arsaker: List<String>,
    val frist: LocalDate?,
    val notattekst: String?,
    val kommentarer: List<ApiSaksbildeKommentar>,
) : ApiHistorikkinnslag

@Serializable
@SerialName("ApiFjernetFraPaVent")
data class ApiFjernetFraPaVent(
    override val id: Int,
    override val historikktype: ApiPeriodehistorikkType,
    override val timestamp: LocalDateTime,
    override val saksbehandlerIdent: String?,
    override val dialogRef: Int?,
) : ApiHistorikkinnslag

@Serializable
@SerialName("ApiTotrinnsvurderingRetur")
data class ApiTotrinnsvurderingRetur(
    override val id: Int,
    override val historikktype: ApiPeriodehistorikkType,
    override val timestamp: LocalDateTime,
    override val saksbehandlerIdent: String?,
    override val dialogRef: Int?,
    val notattekst: String?,
    val kommentarer: List<ApiSaksbildeKommentar>,
) : ApiHistorikkinnslag

@Serializable
@SerialName("ApiStansAutomatiskBehandlingSaksbehandler")
data class ApiStansAutomatiskBehandlingSaksbehandler(
    override val id: Int,
    override val historikktype: ApiPeriodehistorikkType,
    override val timestamp: LocalDateTime,
    override val saksbehandlerIdent: String?,
    override val dialogRef: Int?,
    val notattekst: String?,
    val kommentarer: List<ApiSaksbildeKommentar>,
) : ApiHistorikkinnslag

@Serializable
@SerialName("ApiOpphevStansAutomatiskBehandlingSaksbehandler")
data class ApiOpphevStansAutomatiskBehandlingSaksbehandler(
    override val id: Int,
    override val historikktype: ApiPeriodehistorikkType,
    override val timestamp: LocalDateTime,
    override val saksbehandlerIdent: String?,
    override val dialogRef: Int?,
    val notattekst: String?,
    val kommentarer: List<ApiSaksbildeKommentar>,
) : ApiHistorikkinnslag

@Serializable
@SerialName("ApiPeriodeHistorikkElementNy")
data class ApiPeriodeHistorikkElementNy(
    override val id: Int,
    override val historikktype: ApiPeriodehistorikkType,
    override val timestamp: LocalDateTime,
    override val saksbehandlerIdent: String?,
    override val dialogRef: Int?,
) : ApiHistorikkinnslag

@Serializable
data class ApiFaresignal(
    val beskrivelse: String,
    val kategori: List<String>,
)

@Serializable
data class ApiRisikovurdering(
    val funn: List<ApiFaresignal>?,
    val kontrollertOk: List<ApiFaresignal>,
)

@Serializable
data class ApiVarselDTO(
    val id: UUID,
    val behandlingId: UUID,
    val definisjonId: UUID,
    val opprettet: LocalDateTime,
    val kode: String,
    val tittel: String,
    val forklaring: String?,
    val handling: String?,
    val vurdering: ApiVarselvurderingDTO?,
)

@Serializable
data class ApiVarselvurderingDTO(
    val ident: String,
    val tidsstempel: LocalDateTime,
    val status: ApiSaksbildeVarselstatus,
)

@OptIn(ExperimentalSerializationApi::class)
@JsonClassDiscriminator("type")
@JsonTypeInfo(use = JsonTypeInfo.Id.SIMPLE_NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@Serializable
sealed interface ApiPeriode {
    val behandlingId: UUID
    val erForkastet: Boolean
    val fom: LocalDate
    val tom: LocalDate
    val id: UUID
    val inntektstype: ApiInntektstype
    val opprettet: LocalDateTime
    val periodetype: ApiPeriodetype
    val tidslinje: List<ApiDag>
    val vedtaksperiodeId: UUID
    val periodetilstand: ApiPeriodetilstand
    val skjaeringstidspunkt: LocalDate
    val varsler: List<ApiVarselDTO>
    val hendelser: List<ApiHendelse>
    val historikkinnslag: List<ApiHistorikkinnslag>
}

@Serializable
@SerialName("ApiUberegnetPeriode")
data class ApiUberegnetPeriode(
    override val behandlingId: UUID,
    override val erForkastet: Boolean,
    override val fom: LocalDate,
    override val tom: LocalDate,
    override val id: UUID,
    override val inntektstype: ApiInntektstype,
    override val opprettet: LocalDateTime,
    override val periodetype: ApiPeriodetype,
    override val tidslinje: List<ApiDag>,
    override val vedtaksperiodeId: UUID,
    override val periodetilstand: ApiPeriodetilstand,
    override val skjaeringstidspunkt: LocalDate,
    override val varsler: List<ApiVarselDTO>,
    override val hendelser: List<ApiHendelse>,
    override val historikkinnslag: List<ApiHistorikkinnslag>,
) : ApiPeriode

@Serializable
@SerialName("ApiBeregnetPeriode")
class ApiBeregnetPeriode(
    override val behandlingId: UUID,
    override val erForkastet: Boolean,
    override val fom: LocalDate,
    override val tom: LocalDate,
    override val id: UUID,
    override val inntektstype: ApiInntektstype,
    override val opprettet: LocalDateTime,
    override val periodetype: ApiPeriodetype,
    override val tidslinje: List<ApiDag>,
    override val vedtaksperiodeId: UUID,
    override val periodetilstand: ApiPeriodetilstand,
    override val skjaeringstidspunkt: LocalDate,
    override val varsler: List<ApiVarselDTO>,
    override val hendelser: List<ApiHendelse>,
    override val historikkinnslag: List<ApiHistorikkinnslag>,
    @get:JsonFormat(shape = JsonFormat.Shape.STRING)
    val grunnbelopsjustertGjennomsnitt: BigDecimal?,
    val egenskaper: List<ApiOppgaveegenskap>,
    val forbrukteSykedager: Int?,
    val gjenstaendeSykedager: Int?,
    val maksdato: LocalDate,
    val periodevilkar: ApiPeriodevilkar,
    val utbetaling: ApiUtbetaling,
    val vilkarsgrunnlagId: UUID?,
    val risikovurdering: ApiRisikovurdering?,
    val paVent: ApiPaVent?,
    val avslag: List<ApiAvslag>,
    val vedtakBegrunnelser: List<ApiVedtakBegrunnelse>,
    val annullering: ApiAnnullering?,
    val pensjonsgivendeInntekter: List<ApiPensjonsgivendeInntekt>,
    val annulleringskandidater: List<ApiAnnulleringskandidat>,
) : ApiPeriode

@Serializable
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

@Serializable
data class ApiAlder(
    val alderSisteSykedag: Int,
    val oppfylt: Boolean,
)

@Serializable
data class ApiSykepengedager(
    val forbrukteSykedager: Int? = null,
    val gjenstaendeSykedager: Int? = null,
    val maksdato: LocalDate,
    val oppfylt: Boolean,
    val skjaeringstidspunkt: LocalDate,
)

@Serializable
data class ApiPensjonsgivendeInntekt(
    @get:JsonFormat(shape = JsonFormat.Shape.STRING)
    val arligBelop: BigDecimal,
    val inntektsar: Int,
)

@Serializable
data class ApiAnnulleringskandidat(
    val fom: LocalDate,
    val organisasjonsnummer: String,
    val tom: LocalDate,
    val vedtaksperiodeId: UUID,
)
