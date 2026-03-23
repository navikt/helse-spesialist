package no.nav.helse.mediator.meldinger.løsninger

import java.time.LocalDate

class Fullmaktløsning(
    val harFullmakt: Boolean,
)

fun LocalDate.isSameOrBefore(other: LocalDate) = this.isEqual(other) || this.isBefore(other)

fun LocalDate.isSameOrAfter(other: LocalDate) = this.isEqual(other) || this.isAfter(other)
