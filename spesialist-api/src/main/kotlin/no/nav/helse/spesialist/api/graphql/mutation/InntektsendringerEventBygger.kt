package no.nav.helse.spesialist.api.graphql.mutation

import no.nav.helse.modell.melding.InntektsendringerEvent
import no.nav.helse.spesialist.domain.Periode.Companion.tilPerioder
import no.nav.helse.spesialist.domain.tilkommeninntekt.TilkommenInntekt
import java.math.BigDecimal
import java.time.LocalDate

object InntektsendringerEventBygger {
    fun forNy(tilkommenInntekt: TilkommenInntekt): InntektsendringerEvent =
        InntektsendringerEvent(
            inntektskilder =
                listOf(
                    InntektsendringerEvent.Inntektskilde(
                        inntektskilde = tilkommenInntekt.organisasjonsnummer,
                        inntekter =
                            tilkommenInntekt.dager.tilInntekter(
                                dagsbeløp = tilkommenInntekt.dagbeløp(),
                            ),
                        nullstill = emptyList(),
                    ),
                ),
        )

    fun forEndring(
        arbeidsgiverFør: String,
        arbeidsgiverEtter: String,
        dagerFør: Set<LocalDate>,
        dagerEtter: Set<LocalDate>,
        dagsbeløpFør: BigDecimal,
        dagsbeløpEtter: BigDecimal,
    ): InntektsendringerEvent? =
        if (arbeidsgiverFør != arbeidsgiverEtter) {
            InntektsendringerEvent(
                inntektskilder =
                    listOf(
                        InntektsendringerEvent.Inntektskilde(
                            inntektskilde = arbeidsgiverFør,
                            inntekter = emptyList(),
                            nullstill = dagerFør.tilNullstillinger(),
                        ),
                        InntektsendringerEvent.Inntektskilde(
                            inntektskilde = arbeidsgiverEtter,
                            inntekter = dagerEtter.tilInntekter(dagsbeløpEtter),
                            nullstill = emptyList(),
                        ),
                    ),
            )
        } else if (dagsbeløpFør != dagsbeløpEtter) {
            InntektsendringerEvent(
                inntektskilder =
                    listOf(
                        InntektsendringerEvent.Inntektskilde(
                            inntektskilde = arbeidsgiverEtter,
                            inntekter = dagerEtter.tilInntekter(dagsbeløpEtter),
                            nullstill = dagerFør.minus(dagerEtter).tilNullstillinger(),
                        ),
                    ),
            )
        } else if (dagerFør != dagerEtter) {
            InntektsendringerEvent(
                inntektskilder =
                    listOf(
                        InntektsendringerEvent.Inntektskilde(
                            inntektskilde = arbeidsgiverEtter,
                            inntekter = dagerEtter.minus(dagerFør).tilInntekter(dagsbeløpEtter),
                            nullstill = dagerFør.minus(dagerEtter).tilNullstillinger(),
                        ),
                    ),
            )
        } else {
            null
        }

    fun forFjernet(tilkommenInntekt: TilkommenInntekt): InntektsendringerEvent =
        InntektsendringerEvent(
            inntektskilder =
                listOf(
                    InntektsendringerEvent.Inntektskilde(
                        inntektskilde = tilkommenInntekt.organisasjonsnummer,
                        inntekter = emptyList(),
                        nullstill = tilkommenInntekt.dager.tilNullstillinger(),
                    ),
                ),
        )

    private fun Set<LocalDate>.tilInntekter(dagsbeløp: BigDecimal) =
        sorted().tilPerioder().map { periode ->
            InntektsendringerEvent.Inntektskilde.Inntekt(
                fom = periode.fom,
                tom = periode.tom,
                dagsbeløp = dagsbeløp,
            )
        }

    private fun Set<LocalDate>.tilNullstillinger() =
        tilPerioder().map { periode ->
            InntektsendringerEvent.Inntektskilde.Nullstilling(
                fom = periode.fom,
                tom = periode.tom,
            )
        }
}
