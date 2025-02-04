package no.nav.helse.modell

sealed interface Id

data object NyId : Id

data class EksisterendeId(val value: Long) : Id
