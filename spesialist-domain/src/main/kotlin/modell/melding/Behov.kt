package no.nav.helse.modell.melding

import no.nav.helse.modell.vedtaksperiode.Yrkesaktivitetstype
import no.nav.helse.modell.vilkårsprøving.OmregnetÅrsinntekt
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

sealed interface Behov : UtgåendeMelding {
    data object Vergemål : Behov

    data object Fullmakt : Behov

    data object EgenAnsatt : Behov

    data class ÅpneOppgaver(
        val ikkeEldreEnn: LocalDate,
    ) : Behov

    data class Infotrygdutbetalinger(
        val fom: LocalDate,
        val tom: LocalDate,
    ) : Behov

    data object Personinfo : Behov

    data object Enhet : Behov

    sealed interface Arbeidsgiverinformasjon : Behov {
        data class Enkeltpersonforetak(
            val identer: List<String>,
        ) : Arbeidsgiverinformasjon

        data class OrdinærArbeidsgiver(
            val organisasjonsnumre: List<String>,
        ) : Arbeidsgiverinformasjon
    }

    data class Arbeidsforhold(
        val fødselsnummer: String,
        val organisasjonsnummer: String,
    ) : Behov

    data class InntekterForSykepengegrunnlag(
        val fom: YearMonth,
        val tom: YearMonth,
    ) : Behov

    data class Avviksvurdering(
        val omregnedeÅrsinntekter: List<OmregnetÅrsinntekt>,
        val vilkårsgrunnlagId: UUID,
        val skjæringstidspunkt: LocalDate,
        val organisasjonsnummer: String,
        val vedtaksperiodeId: UUID,
    ) : Behov

    data class Risikovurdering(
        val vedtaksperiodeId: UUID,
        val organisasjonsnummer: String,
        val yrkesaktivitetstype: Yrkesaktivitetstype,
        val førstegangsbehandling: Boolean,
        val kunRefusjon: Boolean,
        val inntekt: InntektTilRisk?,
    ) : Behov
}

data class InntektTilRisk(
    val omregnetÅrsinntekt: Double,
    val inntektskilde: String,
)
