package no.nav.helse.migrering

import java.time.LocalDate

val Int.januar: LocalDate get() = LocalDate.of(2018, 1, this)
val Int.februar: LocalDate get() = LocalDate.of(2018, 2, this)
