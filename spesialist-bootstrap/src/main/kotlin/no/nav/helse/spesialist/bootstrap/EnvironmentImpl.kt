package no.nav.helse.spesialist.bootstrap

import no.nav.helse.bootstrap.Environment

class EnvironmentImpl(private val env: Map<String, String> = System.getenv()) :
    Map<String, String> by env,
    Environment {
    override val erDev = "dev-gcp" == System.getenv("NAIS_CLUSTER_NAME")
    override val erProd = "prod-gcp" == System.getenv("NAIS_CLUSTER_NAME")
}
