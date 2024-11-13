package no.nav.helse.modell.behov

import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

sealed interface Behov {
    data object Vergemål : Behov

    data object Fullmakt : Behov

    data object EgenAnsatt : Behov

    data class ÅpneOppgaver(val ikkeEldreEnn: LocalDate) : Behov

    data class Infotrygdutbetalinger(val fom: LocalDate, val tom: LocalDate) : Behov

    data class Personinfo(val andreIdenter: List<String>?) : Behov

    data object Enhet : Behov

    data class Arbeidsgiverinformasjon(val organisasjonsnumre: List<String>) : Behov

    data class Arbeidsforhold(val fødselsnummer: String, val organisasjonsnummer: String) : Behov

    data class InntekterForSykepengegrunnlag(val fom: YearMonth, val tom: YearMonth) : Behov

    data class Risikovurdering(
        val vedtaksperiodeId: UUID,
        val organisasjonsnummer: String,
        val førstegangsbehandling: Boolean,
        val kunRefusjon: Boolean,
    ) : Behov
}
