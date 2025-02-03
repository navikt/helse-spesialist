package no.nav.helse.spesialist.api.graphql.schema

import com.expediagroup.graphql.generator.annotations.GraphQLName
import java.time.LocalDate
import java.time.LocalDateTime

@GraphQLName("Soknad")
data class ApiSoknad(
    val type: ApiSoknadstype?,
    val arbeidGjenopptatt: LocalDate?,
    val sykmeldingSkrevet: LocalDateTime?,
    val egenmeldingsdagerFraSykmelding: List<LocalDate>?,
    val soknadsperioder: List<ApiSoknadsperioder>?,
    val sporsmal: List<ApiSporsmal>?,
)

@Suppress("ktlint:standard:enum-entry-name-case")
@GraphQLName("Soknadstype")
enum class ApiSoknadstype {
    Selvstendig_og_frilanser,
    Opphold_utland,
    Arbeidstaker,
    Annet_arbeidsforhold,
    Arbeidsledig,
    Behandlingsdager,
    Reisetilskudd,
    Gradert_reisetilskudd,
    UKJENT,
}

@GraphQLName("Soknadsperioder")
data class ApiSoknadsperioder(
    val fom: LocalDate,
    val tom: LocalDate,
    val grad: Int?,
    val faktiskGrad: Int?,
    val sykmeldingsgrad: Int?,
)

@GraphQLName("Sporsmal")
data class ApiSporsmal(
    val tag: String? = null,
    val sporsmalstekst: String? = null,
    val undertekst: String? = null,
    val svartype: ApiSvartype? = null,
    val svar: List<ApiSvar>? = null,
    val undersporsmal: List<ApiSporsmal>? = null,
    val kriterieForVisningAvUndersporsmal: ApiVisningskriterium? = null,
)

@GraphQLName("Svartype")
enum class ApiSvartype {
    JA_NEI,
    CHECKBOX,
    CHECKBOX_GRUPPE,
    CHECKBOX_PANEL,
    DATO,
    PERIODE,
    PERIODER,
    TIMER,
    FRITEKST,
    IKKE_RELEVANT,
    BEKREFTELSESPUNKTER,
    OPPSUMMERING,
    PROSENT,
    RADIO_GRUPPE,
    RADIO_GRUPPE_TIMER_PROSENT,
    RADIO,
    TALL,
    RADIO_GRUPPE_UKEKALENDER,
    LAND,
    COMBOBOX_SINGLE,
    COMBOBOX_MULTI,
    INFO_BEHANDLINGSDAGER,
    KVITTERING,
    DATOER,
    BELOP,
    KILOMETER,
    GRUPPE_AV_UNDERSPORSMAL,
    UKJENT,
}

@GraphQLName("Visningskriterium")
enum class ApiVisningskriterium {
    NEI,
    JA,
    CHECKED,
    UKJENT,
}

@GraphQLName("Svar")
data class ApiSvar(
    val verdi: String? = null,
)

@GraphQLName("DokumentInntektsmelding")
data class ApiDokumentInntektsmelding(
    val arbeidsforholdId: String?,
    val virksomhetsnummer: String?,
    val begrunnelseForReduksjonEllerIkkeUtbetalt: String?,
    val bruttoUtbetalt: Double?,
    val beregnetInntekt: Double?,
    val refusjon: ApiRefusjon?,
    val endringIRefusjoner: List<ApiEndringIRefusjon>?,
    val opphoerAvNaturalytelser: List<ApiOpphoerAvNaturalytelse>?,
    val gjenopptakelseNaturalytelser: List<ApiGjenopptakelseNaturalytelse>?,
    val arbeidsgiverperioder: List<ApiIMPeriode>?,
    val ferieperioder: List<ApiIMPeriode>?,
    val foersteFravaersdag: LocalDate?,
    val naerRelasjon: Boolean?,
    val innsenderFulltNavn: String?,
    val innsenderTelefon: String?,
    val inntektEndringAarsak: ApiInntektEndringAarsak? = null,
    val avsenderSystem: ApiAvsenderSystem? = null,
)

@GraphQLName("Refusjon")
data class ApiRefusjon(
    val beloepPrMnd: Double?,
    val opphoersdato: LocalDate?,
)

@GraphQLName("EndringIRefusjon")
data class ApiEndringIRefusjon(
    val endringsdato: LocalDate?,
    val beloep: Double?,
)

@GraphQLName("OpphoerAvNaturalytelse")
data class ApiOpphoerAvNaturalytelse(
    val naturalytelse: ApiNaturalytelse? = null,
    val fom: LocalDate? = null,
    val beloepPrMnd: Double? = null,
)

@GraphQLName("Naturalytelse")
enum class ApiNaturalytelse {
    KOSTDOEGN,
    LOSJI,
    ANNET,
    SKATTEPLIKTIGDELFORSIKRINGER,
    BIL,
    KOSTDAGER,
    RENTEFORDELLAAN,
    BOLIG,
    ELEKTRONISKKOMMUNIKASJON,
    AKSJERGRUNNFONDSBEVISTILUNDERKURS,
    OPSJONER,
    KOSTBESPARELSEIHJEMMET,
    FRITRANSPORT,
    BEDRIFTSBARNEHAGEPLASS,
    TILSKUDDBARNEHAGEPLASS,
    BESOEKSREISERHJEMMETANNET,
    INNBETALINGTILUTENLANDSKPENSJONSORDNING,
    YRKEBILTJENESTLIGBEHOVLISTEPRIS,
    YRKEBILTJENESTLIGBEHOVKILOMETER,
    UKJENT,
}

@GraphQLName("GjenopptakelseNaturalytelse")
data class ApiGjenopptakelseNaturalytelse(
    val naturalytelse: ApiNaturalytelse? = null,
    val fom: LocalDate? = null,
    val beloepPrMnd: Double? = null,
)

@GraphQLName("IMPeriode")
data class ApiIMPeriode(
    val fom: LocalDate?,
    val tom: LocalDate?,
)

@GraphQLName("InntektEndringAarsak")
data class ApiInntektEndringAarsak(
    val aarsak: String,
    val perioder: List<ApiIMPeriode>? = null,
    val gjelderFra: LocalDate? = null,
    val bleKjent: LocalDate? = null,
)

@GraphQLName("AvsenderSystem")
data class ApiAvsenderSystem(
    val navn: String? = null,
    val versjon: String? = null,
)
