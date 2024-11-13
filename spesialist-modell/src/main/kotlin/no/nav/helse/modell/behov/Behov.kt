package no.nav.helse.modell.behov

import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

sealed interface Behov {
    fun behovName(): String

    fun somJsonMessage(): Map<String, Any>

    data object Vergemål : Behov {
        override fun behovName() = "Vergemål"

        override fun somJsonMessage() = emptyMap<String, Any>()
    }

    data object Fullmakt : Behov {
        override fun behovName() = "Fullmakt"

        override fun somJsonMessage() = emptyMap<String, Any>()
    }

    data object EgenAnsatt : Behov {
        override fun behovName() = "EgenAnsatt"

        override fun somJsonMessage() = emptyMap<String, Any>()
    }

    data class ÅpneOppgaver(val ikkeEldreEnn: LocalDate) : Behov {
        override fun behovName() = "ÅpneOppgaver"

        override fun somJsonMessage() = mapOf("ikkeEldreEnn" to ikkeEldreEnn)
    }

    data class Infotrygdutbetalinger(val fom: LocalDate, val tom: LocalDate) : Behov {
        override fun behovName() = "HentInfotrygdutbetalinger"

        override fun somJsonMessage() =
            mapOf(
                "historikkFom" to fom,
                "historikkTom" to tom,
            )
    }

    data object Personinfo : Behov {
        override fun behovName() = "HentPersoninfoV2"

        override fun somJsonMessage() = emptyMap<String, Any>()
    }

    data object Enhet : Behov {
        override fun behovName() = "HentEnhet"

        override fun somJsonMessage() = emptyMap<String, Any>()
    }

    object Arbeidsgiverinformasjon {
        data class OrdinærArbeidsgiver(val organisasjonsnumre: List<String>) : Behov {
            override fun behovName() = "Arbeidsgiverinformasjon"

            override fun somJsonMessage() =
                mapOf(
                    "organisasjonsnummer" to organisasjonsnumre,
                )
        }

        data class Enkeltpersonforetak(val identer: List<String>) : Behov {
            override fun behovName() = "HentPersoninfoV2"

            override fun somJsonMessage() =
                mapOf(
                    "ident" to identer,
                )
        }
    }

    data class Arbeidsforhold(val fødselsnummer: String, val organisasjonsnummer: String) : Behov {
        override fun behovName() = "Arbeidsforhold"

        override fun somJsonMessage() =
            mapOf(
                "fødselsnummer" to fødselsnummer,
                "organisasjonsnummer" to organisasjonsnummer,
            )
    }

    data class InntekterForSykepengegrunnlag(val fom: YearMonth, val tom: YearMonth) : Behov {
        override fun behovName() = "InntekterForSykepengegrunnlag"

        override fun somJsonMessage() =
            mapOf(
                "beregningStart" to fom.toString(),
                "beregningSlutt" to tom.toString(),
            )
    }

    data class Risikovurdering(
        val vedtaksperiodeId: UUID,
        val organisasjonsnummer: String,
        val førstegangsbehandling: Boolean,
        val kunRefusjon: Boolean,
    ) : Behov {
        override fun behovName() = "Risikovurdering"

        override fun somJsonMessage() =
            mapOf(
                "vedtaksperiodeId" to vedtaksperiodeId,
                "organisasjonsnummer" to organisasjonsnummer,
                "førstegangsbehandling" to førstegangsbehandling,
                "kunRefusjon" to kunRefusjon,
            )
    }
}
