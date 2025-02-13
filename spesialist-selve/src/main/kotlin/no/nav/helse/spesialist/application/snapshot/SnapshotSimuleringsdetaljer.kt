package no.nav.helse.spesialist.application.snapshot

import java.time.LocalDate

data class SnapshotSimuleringsdetaljer(
    val belop: Int,
    val antallSats: Int,
    val faktiskFom: LocalDate,
    val faktiskTom: LocalDate,
    val klassekode: String,
    val klassekodeBeskrivelse: String,
    val konto: String,
    val refunderesOrgNr: String,
    val sats: Double,
    val tilbakeforing: Boolean,
    val typeSats: String,
    val uforegrad: Int,
    val utbetalingstype: String,
)
