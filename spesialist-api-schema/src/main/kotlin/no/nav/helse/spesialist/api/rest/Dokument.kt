@file:UseContextualSerialization(
    BigDecimal::class,
    Boolean::class,
    Instant::class,
    LocalDate::class,
    LocalDateTime::class,
    UUID::class,
)

package no.nav.helse.spesialist.api.rest

import kotlinx.serialization.Serializable
import kotlinx.serialization.UseContextualSerialization
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@Serializable
data class ApiSoknad(
    val type: ApiSoknadstype?,
    val arbeidGjenopptatt: LocalDate?,
    val sykmeldingSkrevet: LocalDateTime?,
    val egenmeldingsdagerFraSykmelding: List<LocalDate>?,
    val soknadsperioder: List<ApiSoknadsperioder>?,
    val sporsmal: List<ApiSporsmal>?,
    val selvstendigNaringsdrivende: ApiSoknadSelvstendigNaringsdrivende?,
)

@Suppress("ktlint:standard:enum-entry-name-case")
@Serializable
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

@Serializable
data class ApiSoknadsperioder(
    val fom: LocalDate,
    val tom: LocalDate,
    val grad: Int?,
    val faktiskGrad: Int?,
    val sykmeldingsgrad: Int?,
)

@Serializable
data class ApiSoknadSelvstendigNaringsdrivende(
    val inntekt: List<ApiInntektsar>,
    val ventetid: ApiVentetidPeriode?,
) {
    @Serializable
    data class ApiVentetidPeriode(
        val fom: LocalDate,
        val tom: LocalDate,
    )

    @Serializable
    data class ApiInntektsar(
        val ar: String,
        val pensjonsgivendeInntektAvLonnsinntekt: Int,
        val pensjonsgivendeInntektAvLonnsinntektBarePensjonsdel: Int,
        val pensjonsgivendeInntektAvNaringsinntekt: Int,
        val pensjonsgivendeInntektAvNaringsinntektFraFiskeFangstEllerFamiliebarnehage: Int,
        val erFerdigLignet: Boolean,
    )
}

@Serializable
data class ApiSporsmal(
    val tag: String? = null,
    val sporsmalstekst: String? = null,
    val undertekst: String? = null,
    val svartype: ApiSvartype? = null,
    val svar: List<ApiSvar>? = null,
    val undersporsmal: List<ApiSporsmal>? = null,
    val kriterieForVisningAvUndersporsmal: ApiVisningskriterium? = null,
)

@Serializable
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
    AAR_MANED,
    UKJENT,
}

@Serializable
enum class ApiVisningskriterium {
    NEI,
    JA,
    CHECKED,
    UKJENT,
}

@Serializable
data class ApiSvar(
    val verdi: String? = null,
)

@Serializable
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
    val inntektEndringAarsaker: List<ApiInntektEndringAarsak>?,
    val avsenderSystem: ApiAvsenderSystem? = null,
)

@Serializable
data class ApiRefusjon(
    val beloepPrMnd: Double?,
    val opphoersdato: LocalDate?,
)

@Serializable
data class ApiEndringIRefusjon(
    val endringsdato: LocalDate?,
    val beloep: Double?,
)

@Serializable
data class ApiOpphoerAvNaturalytelse(
    val naturalytelse: ApiNaturalytelse? = null,
    val fom: LocalDate? = null,
    val beloepPrMnd: Double? = null,
)

@Serializable
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

@Serializable
data class ApiGjenopptakelseNaturalytelse(
    val naturalytelse: ApiNaturalytelse? = null,
    val fom: LocalDate? = null,
    val beloepPrMnd: Double? = null,
)

@Serializable
data class ApiIMPeriode(
    val fom: LocalDate?,
    val tom: LocalDate?,
)

@Serializable
data class ApiInntektEndringAarsak(
    val aarsak: String,
    val perioder: List<ApiIMPeriode>? = null,
    val gjelderFra: LocalDate? = null,
    val bleKjent: LocalDate? = null,
)

@Serializable
data class ApiAvsenderSystem(
    val navn: String? = null,
    val versjon: String? = null,
)
