@file:UseContextualSerialization(
    BigDecimal::class,
    Boolean::class,
    LocalDate::class,
    LocalDateTime::class,
    UUID::class,
    YearMonth::class,
)

package no.nav.helse.spesialist.api.rest.saksbilde

import kotlinx.serialization.Serializable
import kotlinx.serialization.UseContextualSerialization
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.UUID

@Serializable
enum class ApiEgenskap {
    RISK_QA,
    FORTROLIG_ADRESSE,
    STRENGT_FORTROLIG_ADRESSE,
    EGEN_ANSATT,
    BESLUTTER,
    SPESIALSAK,
    REVURDERING,
    SOKNAD,
    STIKKPROVE,
    UTBETALING_TIL_SYKMELDT,
    DELVIS_REFUSJON,
    UTBETALING_TIL_ARBEIDSGIVER,
    INGEN_UTBETALING,
    EN_ARBEIDSGIVER,
    FLERE_ARBEIDSGIVERE,
    FORLENGELSE,
    FORSTEGANGSBEHANDLING,
    INFOTRYGDFORLENGELSE,
    OVERGANG_FRA_IT,
    UTLAND,
    HASTER,
    RETUR,
    VERGEMAL,
    SKJONNSFASTSETTELSE,
    PA_VENT,
    TILBAKEDATERT,
    GOSYS,
    MANGLER_IM,
    MEDLEMSKAP,
    GRUNNBELOPSREGULERING,
    SELVSTENDIG_NAERINGSDRIVENDE,
    ARBEIDSTAKER,
    JORDBRUKER_REINDRIFT,
    FORSIKRING,
}

@Serializable
enum class ApiKategori {
    Mottaker,
    Inntektskilde,
    Inntektsforhold,
    Arbeidssituasjon,
    Oppgavetype,
    Ukategorisert,
    Periodetype,
    Status,
}

@Serializable
data class ApiOppgaveegenskap(
    val egenskap: ApiEgenskap,
    val kategori: ApiKategori,
)

@Serializable
data class ApiOppgaveForPeriodevisning(
    val id: String,
)

@Serializable
data class ApiTotrinnsvurdering(
    val erRetur: Boolean,
    val saksbehandler: UUID?,
    val beslutter: UUID?,
    val erBeslutteroppgave: Boolean,
)
