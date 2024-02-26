package no.nav.helse.spesialist.api.graphql.schema

data class Soknad(
    val type: Soknadstype?,
    val arbeidGjenopptatt: DateString?,
    val sykmeldingSkrevet: DateTimeString?,
    val egenmeldingsdagerFraSykmelding: List<DateString>?,
    val soknadsperioder: List<Soknadsperioder>?,
    val sporsmal: List<Sporsmal>?,
)

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
    val fom: DateString,
    val tom: DateString,
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
    UKJENT
}

enum class Visningskriterium {
    NEI,
    JA,
    CHECKED,
    UKJENT
}

data class Svar(
    val verdi: String? = null,
)

data class DokumentInntektsmelding(
    val begrunnelseForReduksjonEllerIkkeUtbetalt: String?,
    val bruttoUtbetalt: Double?,
    val beregnetInntekt: Double?,
    val inntektsdato: DateString?,
    val refusjon: Refusjon?,
    val endringIRefusjoner: List<EndringIRefusjon>?,
    val opphoerAvNaturalytelser: List<OpphoerAvNaturalytelse>?,
    val gjenopptakelseNaturalytelser: List<GjenopptakelseNaturalytelse>?,
    val arbeidsgiverperioder: List<IMPeriode>?,
    val ferieperioder: List<IMPeriode>?,
    val foersteFravaersdag: DateString?,
    val naerRelasjon: Boolean?,
    val innsenderFulltNavn: String?,
    val innsenderTelefon: String?,
    val inntektEndringAarsak: InntektEndringAarsak? = null,
    val avsenderSystem: AvsenderSystem? = null,
)

data class Refusjon(
    val beloepPrMnd: Double?,
    val opphoersdato: DateString?,
)

data class EndringIRefusjon(
    val endringsdato: DateString?,
    val beloep: Double?,
)

data class OpphoerAvNaturalytelse(
    val naturalytelse: Naturalytelse? = null,
    val fom: DateString? = null,
    val beloepPrMnd: Double? = null,
)

enum class Naturalytelse {
    KOSTDOEGN, LOSJI, ANNET, SKATTEPLIKTIGDELFORSIKRINGER, BIL, KOSTDAGER, RENTEFORDELLAAN, BOLIG, ELEKTRONISKKOMMUNIKASJON, AKSJERGRUNNFONDSBEVISTILUNDERKURS, OPSJONER, KOSTBESPARELSEIHJEMMET, FRITRANSPORT, BEDRIFTSBARNEHAGEPLASS, TILSKUDDBARNEHAGEPLASS, BESOEKSREISERHJEMMETANNET, INNBETALINGTILUTENLANDSKPENSJONSORDNING, YRKEBILTJENESTLIGBEHOVLISTEPRIS, YRKEBILTJENESTLIGBEHOVKILOMETER, UKJENT
}

data class GjenopptakelseNaturalytelse(
    val naturalytelse: Naturalytelse? = null,
    val fom: DateString? = null,
    val beloepPrMnd: Double? = null,
)

data class IMPeriode(
    val fom: DateString?,
    val tom: DateString?,
)

data class InntektEndringAarsak(
    val aarsak: String,
    val perioder: List<IMPeriode>? = null,
    val gjelderFra: DateString? = null,
    val bleKjent: DateString? = null,
)

data class AvsenderSystem(
    val navn: String? = null,
    val versjon: String? = null,
)