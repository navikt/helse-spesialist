package no.nav.helse.spesialist.api.graphql.schema

data class Soknad(
    val arbeidGjenopptatt: DateString?,
    val sykmeldingSkrevet: DateTimeString?,
    val egenmeldingsdagerFraSykmelding: List<DateString>?,
    val soknadsperioder: List<Soknadsperioder>?,
    val sporsmal: List<Sporsmal>?
)

data class Soknadsperioder(
    val fom: DateString,
    val tom: DateString,
    val grad: Int,
    val faktiskGrad: Int?,
)

data class Sporsmal(
    val tag: String? = null,
    val sporsmalstekst: String? = null,
    val undertekst: String? = null,
    val svartype: Svartype? = null,
    val svar: List<Svar>? = null,
    val undersporsmal: List<Sporsmal?>? = null,
    val kriterieForVisningAvUndersporsmal: Visningskriterium? = null
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
    val verdi: String? = null
)