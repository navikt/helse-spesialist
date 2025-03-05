package no.nav.helse.bootstrap

interface Environment : Map<String, String> {
    val brukDummyForKRR: Boolean
    val ignorerMeldingerForUkjentePersoner: Boolean
    val kanBeslutteEgneSaker: Boolean
    val kanGodkjenneUtenBesluttertilgang: Boolean
}
