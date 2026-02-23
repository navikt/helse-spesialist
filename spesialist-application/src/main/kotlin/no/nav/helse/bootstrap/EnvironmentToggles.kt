package no.nav.helse.bootstrap

interface EnvironmentToggles {
    val kanBeslutteEgneSaker: Boolean
    val kanGodkjenneUtenBesluttertilgang: Boolean
    val kanSeForsikring: Boolean
    val devGcp: Boolean
}
