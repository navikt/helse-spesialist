package no.nav.helse.spesialist.api.graphql.schema

import java.time.LocalDate
import java.time.LocalDateTime

data class Soknad(
    val type: Soknadstype?,
    val arbeidGjenopptatt: LocalDate?,
    val sykmeldingSkrevet: LocalDateTime?,
    val egenmeldingsdagerFraSykmelding: List<LocalDate>?,
    val soknadsperioder: List<Soknadsperioder>?,
    val sporsmal: List<Sporsmal>?,
)

@Suppress("ktlint:standard:enum-entry-name-case")
enum class Soknadstype {
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

data class Soknadsperioder(
    val fom: LocalDate,
    val tom: LocalDate,
    val grad: Int?,
    val faktiskGrad: Int?,
    val sykmeldingsgrad: Int?,
)

data class Sporsmal(
    val tag: String? = null,
    val sporsmalstekst: String? = null,
    val undertekst: String? = null,
    val svartype: Svartype? = null,
    val svar: List<Svar>? = null,
    val undersporsmal: List<Sporsmal>? = null,
    val kriterieForVisningAvUndersporsmal: Visningskriterium? = null,
)

enum class Svartype {
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

enum class Visningskriterium {
    NEI,
    JA,
    CHECKED,
    UKJENT,
}

data class Svar(
    val verdi: String? = null,
)

data class DokumentInntektsmelding(
    val begrunnelseForReduksjonEllerIkkeUtbetalt: String?,
    val bruttoUtbetalt: Double?,
    val beregnetInntekt: Double?,
    val inntektsdato: LocalDate?,
    val refusjon: Refusjon?,
    val endringIRefusjoner: List<EndringIRefusjon>?,
    val opphoerAvNaturalytelser: List<OpphoerAvNaturalytelse>?,
    val gjenopptakelseNaturalytelser: List<GjenopptakelseNaturalytelse>?,
    val arbeidsgiverperioder: List<IMPeriode>?,
    val ferieperioder: List<IMPeriode>?,
    val foersteFravaersdag: LocalDate?,
    val naerRelasjon: Boolean?,
    val innsenderFulltNavn: String?,
    val innsenderTelefon: String?,
    val inntektEndringAarsak: InntektEndringAarsak? = null,
    val avsenderSystem: AvsenderSystem? = null,
)

data class Refusjon(
    val beloepPrMnd: Double?,
    val opphoersdato: LocalDate?,
)

data class EndringIRefusjon(
    val endringsdato: LocalDate?,
    val beloep: Double?,
)

data class OpphoerAvNaturalytelse(
    val naturalytelse: Naturalytelse? = null,
    val fom: LocalDate? = null,
    val beloepPrMnd: Double? = null,
)

enum class Naturalytelse {
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

data class GjenopptakelseNaturalytelse(
    val naturalytelse: Naturalytelse? = null,
    val fom: LocalDate? = null,
    val beloepPrMnd: Double? = null,
)

data class IMPeriode(
    val fom: LocalDate?,
    val tom: LocalDate?,
)

data class InntektEndringAarsak(
    val aarsak: String,
    val perioder: List<IMPeriode>? = null,
    val gjelderFra: LocalDate? = null,
    val bleKjent: LocalDate? = null,
)

data class AvsenderSystem(
    val navn: String? = null,
    val versjon: String? = null,
)
