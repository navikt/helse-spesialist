package no.nav.helse.spesialist.api.rest.tilkommeninntekt

import no.nav.helse.modell.melding.InntektsendringerEvent
import no.nav.helse.spesialist.domain.Periode.Companion.tilPerioder
import java.math.BigDecimal
import java.time.LocalDate
import java.util.SortedSet

object InntektsendringerEventBygger {
    data class PubliserbarTilstand(
        val fjernet: Boolean,
        val inntektskilde: String,
        val dagerTilGradering: SortedSet<LocalDate>,
        val dagsbeløp: BigDecimal,
    )

    fun forNy(
        inntektskilde: String,
        dagerTilGradering: SortedSet<LocalDate>,
        dagsbeløp: BigDecimal,
    ): InntektsendringerEvent =
        InntektsendringerEvent(
            inntektskilder =
                listOf(
                    InntektsendringerEvent.Inntektskilde(
                        inntektskilde = inntektskilde,
                        inntekter = dagerTilGradering.tilInntekter(dagsbeløp = dagsbeløp),
                        nullstill = emptyList(),
                    ),
                ),
        )

    fun forTilstandsendring(
        tidligerePublisertTilstand: PubliserbarTilstand,
        nåværendeTilstand: PubliserbarTilstand,
    ): InntektsendringerEvent? =
        if (tidligerePublisertTilstand.fjernet != nåværendeTilstand.fjernet) {
            if (nåværendeTilstand.fjernet) {
                forFjernet(tidligerePublisertTilstand.inntektskilde, tidligerePublisertTilstand.dagerTilGradering)
            } else {
                forNy(nåværendeTilstand.inntektskilde, nåværendeTilstand.dagerTilGradering, nåværendeTilstand.dagsbeløp)
            }
        } else {
            forEndret(
                arbeidsgiverFør = tidligerePublisertTilstand.inntektskilde,
                arbeidsgiverEtter = nåværendeTilstand.inntektskilde,
                dagerFør = tidligerePublisertTilstand.dagerTilGradering,
                dagerEtter = nåværendeTilstand.dagerTilGradering,
                dagsbeløpFør = tidligerePublisertTilstand.dagsbeløp,
                dagsbeløpEtter = nåværendeTilstand.dagsbeløp,
            )
        }

    fun forEndret(
        arbeidsgiverFør: String,
        arbeidsgiverEtter: String,
        dagerFør: SortedSet<LocalDate>,
        dagerEtter: SortedSet<LocalDate>,
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
                            nullstill = dagerFør.minus(dagerEtter).toSortedSet().tilNullstillinger(),
                        ),
                    ),
            )
        } else if (dagerFør != dagerEtter) {
            InntektsendringerEvent(
                inntektskilder =
                    listOf(
                        InntektsendringerEvent.Inntektskilde(
                            inntektskilde = arbeidsgiverEtter,
                            inntekter = dagerEtter.minus(dagerFør).toSortedSet().tilInntekter(dagsbeløpEtter),
                            nullstill = dagerFør.minus(dagerEtter).toSortedSet().tilNullstillinger(),
                        ),
                    ),
            )
        } else {
            null
        }

    fun forFjernet(
        inntektskilde: String,
        dagerTilGradering: SortedSet<LocalDate>,
    ): InntektsendringerEvent =
        InntektsendringerEvent(
            inntektskilder =
                listOf(
                    InntektsendringerEvent.Inntektskilde(
                        inntektskilde = inntektskilde,
                        inntekter = emptyList(),
                        nullstill = dagerTilGradering.tilNullstillinger(),
                    ),
                ),
        )

    private fun SortedSet<LocalDate>.tilInntekter(dagsbeløp: BigDecimal) =
        tilPerioder().map { periode ->
            InntektsendringerEvent.Inntektskilde.Inntekt(
                fom = periode.fom,
                tom = periode.tom,
                dagsbeløp = dagsbeløp,
            )
        }

    private fun SortedSet<LocalDate>.tilNullstillinger() =
        tilPerioder().map { periode ->
            InntektsendringerEvent.Inntektskilde.Nullstilling(
                fom = periode.fom,
                tom = periode.tom,
            )
        }
}
