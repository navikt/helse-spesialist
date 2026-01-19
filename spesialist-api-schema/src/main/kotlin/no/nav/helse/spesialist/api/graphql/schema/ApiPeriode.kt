package no.nav.helse.spesialist.api.graphql.schema

import com.expediagroup.graphql.generator.annotations.GraphQLName
import java.math.BigDecimal
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
    AvventerAnnullering,
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

@GraphQLName("StansAutomatiskBehandlingSaksbehandler")
data class ApiStansAutomatiskBehandlingSaksbehandler(
    override val id: Int,
    override val type: ApiPeriodehistorikkType,
    override val timestamp: LocalDateTime,
    override val saksbehandlerIdent: String?,
    override val dialogRef: Int?,
    val notattekst: String?,
    val kommentarer: List<ApiKommentar>,
) : ApiHistorikkinnslag

@GraphQLName("OpphevStansAutomatiskBehandlingSaksbehandler")
data class ApiOpphevStansAutomatiskBehandlingSaksbehandler(
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
    val id: UUID,
    val behandlingId: UUID,
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
}

@GraphQLName("UberegnetPeriode")
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
    val notater: List<ApiNotat>,
) : ApiPeriode

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
    val oppgave: ApiOppgaveForPeriodevisning?,
    val handlinger: List<ApiHandling>,
    val egenskaper: List<ApiOppgaveegenskap>,
    val notater: List<ApiNotat>,
    val historikkinnslag: List<ApiHistorikkinnslag>,
    val beregningId: UUID,
    val forbrukteSykedager: Int?,
    val gjenstaendeSykedager: Int?,
    val maksdato: LocalDate,
    val periodevilkar: ApiPeriodevilkar,
    val utbetaling: ApiUtbetaling,
    val vilkarsgrunnlagId: UUID?,
    val risikovurdering: ApiRisikovurdering?,
    val totrinnsvurdering: ApiTotrinnsvurdering?,
    val paVent: ApiPaVent?,
    val avslag: List<ApiAvslag>,
    val vedtakBegrunnelser: List<ApiVedtakBegrunnelse>,
    val annullering: ApiAnnullering?,
    val pensjonsgivendeInntekter: List<ApiPensjonsgivendeInntekt>,
    val annulleringskandidater: List<ApiAnnulleringskandidat>,
) : ApiPeriode

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

@GraphQLName("PensjonsgivendeInntekt")
data class ApiPensjonsgivendeInntekt(
    val arligBelop: BigDecimal,
    val inntektsar: Int,
)

@GraphQLName("Annulleringskandidat")
data class ApiAnnulleringskandidat(
    val fom: LocalDate,
    val organisasjonsnummer: String,
    val tom: LocalDate,
    val vedtaksperiodeId: UUID,
)
