package no.nav.helse.modell.automatisering

sealed interface Automatiseringsresultat {
    data object KanAutomatiseres : Automatiseringsresultat

    data object KanAutomatisereSpesialsak : Automatiseringsresultat

    data class Stikkprøve(val årsak: String) : Automatiseringsresultat

    data class KanIkkeAutomatiseres(val problemer: List<String>) : Automatiseringsresultat
}
