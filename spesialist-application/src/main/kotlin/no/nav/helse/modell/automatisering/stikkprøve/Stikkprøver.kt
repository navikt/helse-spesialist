package no.nav.helse.modell.automatisering.stikkprøve

import no.nav.helse.modell.vedtaksperiode.Yrkesaktivitetstype
import kotlin.random.Random

typealias PlukkTilManuell<String> = (String?) -> Boolean

class Stikkprøver(
    val configuration: Configuration,
) {
    internal fun avgjørStikkprøve(
        UTS: Boolean,
        flereArbeidsgivere: Boolean,
        førstegangsbehandling: Boolean,
        yrkesaktivitetstype: Yrkesaktivitetstype,
    ): String? {
        when (yrkesaktivitetstype) {
            Yrkesaktivitetstype.ARBEIDSTAKER -> {
                when {
                    UTS -> {
                        when {
                            flereArbeidsgivere -> {
                                when {
                                    førstegangsbehandling && configuration.utsFlereArbeidsgivereFørstegangsbehandling() -> return "UTS, flere arbeidsgivere, førstegangsbehandling"
                                    !førstegangsbehandling && configuration.utsFlereArbeidsgivereForlengelse() -> return "UTS, flere arbeidsgivere, forlengelse"
                                }
                            }

                            !flereArbeidsgivere -> {
                                when {
                                    førstegangsbehandling && configuration.utsEnArbeidsgiverFørstegangsbehandling() -> return "UTS, en arbeidsgiver, førstegangsbehandling"
                                    !førstegangsbehandling && configuration.utsEnArbeidsgiverForlengelse() -> return "UTS, en arbeidsgiver, forlengelse"
                                }
                            }
                        }
                    }

                    flereArbeidsgivere -> {
                        when {
                            førstegangsbehandling && configuration.fullRefusjonFlereArbeidsgivereFørstegangsbehandling() -> return "Refusjon, flere arbeidsgivere, førstegangsbehandling"
                            !førstegangsbehandling && configuration.fullRefusjonFlereArbeidsgivereForlengelse() -> return "Refusjon, flere arbeidsgivere, forlengelse"
                        }
                    }

                    configuration.fullRefusjonEnArbeidsgiver() -> {
                        return "Refusjon, en arbeidsgiver"
                    }
                }
            }

            Yrkesaktivitetstype.SELVSTENDIG -> {
                if (!førstegangsbehandling && configuration.selvstendigNæringsdrivendeForlengelse()) return "Forlengelse, selvstendig næringsdrivende"
            }

            else -> {
                error("Støtter ikke behandling av personer med yrkesaktivitetstype $yrkesaktivitetstype")
            }
        }
        return null
    }

    interface Configuration {
        fun utsFlereArbeidsgivereFørstegangsbehandling(): Boolean

        fun utsFlereArbeidsgivereForlengelse(): Boolean

        fun selvstendigNæringsdrivendeForlengelse(): Boolean

        fun utsEnArbeidsgiverFørstegangsbehandling(): Boolean

        fun utsEnArbeidsgiverForlengelse(): Boolean

        fun fullRefusjonFlereArbeidsgivereFørstegangsbehandling(): Boolean

        fun fullRefusjonFlereArbeidsgivereForlengelse(): Boolean

        fun fullRefusjonEnArbeidsgiver(): Boolean

        companion object {
            fun fromEnv(env: Map<String, String>) =
                object : Configuration {
                    override fun utsFlereArbeidsgivereFørstegangsbehandling() = plukkTilManuell(env["STIKKPROEVER_UTS_FLERE_AG_FGB_DIVISOR"])

                    override fun utsFlereArbeidsgivereForlengelse() = plukkTilManuell(env["STIKKPROEVER_UTS_FLERE_AG_FORLENGELSE_DIVISOR"])

                    override fun selvstendigNæringsdrivendeForlengelse() = plukkTilManuell(env["STIKKPROEVER_SN_FORLENGELSE_DIVISOR"])

                    override fun utsEnArbeidsgiverFørstegangsbehandling() = plukkTilManuell(env["STIKKPROEVER_UTS_EN_AG_FGB_DIVISOR"])

                    override fun utsEnArbeidsgiverForlengelse() = plukkTilManuell(env["STIKKPROEVER_UTS_EN_AG_FORLENGELSE_DIVISOR"])

                    override fun fullRefusjonFlereArbeidsgivereFørstegangsbehandling() = plukkTilManuell(env["STIKKPROEVER_FULL_REFUSJON_FLERE_AG_FGB_DIVISOR"])

                    override fun fullRefusjonFlereArbeidsgivereForlengelse() = plukkTilManuell(env["STIKKPROEVER_FULL_REFUSJON_FLERE_AG_FORLENGELSE_DIVISOR"])

                    override fun fullRefusjonEnArbeidsgiver() = plukkTilManuell(env["STIKKPROEVER_FULL_REFUSJON_EN_AG_DIVISOR"])
                }

            private val plukkTilManuell: PlukkTilManuell<String> = (
                {
                    it?.let {
                        val divisor = it.toInt()
                        require(divisor > 0) { "Her er et vennlig tips: ikke prøv å dele på 0" }
                        Random.nextInt(divisor) == 0
                    } == true
                }
            )
        }
    }
}
