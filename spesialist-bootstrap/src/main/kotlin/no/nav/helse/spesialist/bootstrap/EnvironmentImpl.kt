package no.nav.helse.spesialist.bootstrap

import no.nav.helse.bootstrap.Environment

class EnvironmentImpl(private val env: Map<String, String> = System.getenv()) :
    Map<String, String> by env,
    Environment {
    override val erLokal = env.containsKey("LOKAL_UTVIKLING")
}
