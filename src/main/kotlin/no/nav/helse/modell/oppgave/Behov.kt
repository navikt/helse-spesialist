package no.nav.helse.modell.oppgave

import java.util.*

data class Behov(val type: Behovtype, val fødselsnummer: String, val spleisBehovId: UUID)

enum class Behovtype{HENT_ENHET, HENT_NAVN}
