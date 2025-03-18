package no.nav.helse.spesialist.bootstrap

import no.nav.helse.bootstrap.EnvironmentToggles

class EnvironmentTogglesImpl(env: Map<String, String>) : EnvironmentToggles {
    override val brukDummyForKRR = env.containsKey("BRUK_DUMMY_FOR_KONTAKT_OG_RESERVASJONSREGISTERET")
    override val ignorerMeldingerForUkjentePersoner = env.containsKey("IGNORER_MELDINGER_FOR_UKJENTE_PERSONER")
    override val kanBeslutteEgneSaker = env.containsKey("TILLAT_GODKJENNING_AV_EGEN_SAK")
    override val kanGodkjenneUtenBesluttertilgang = env.containsKey("TILLAT_GODKJENNING_UTEN_BESLUTTERTILGANG")
}
