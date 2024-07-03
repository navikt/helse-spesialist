package no.nav.helse.bootstrap

/**
 * Støtter å hente verdien av miljøvariabler.
 *
 * Kan instansieres og brukes hvor som helst, bortsett fra hvis man trenger verdier som er lagt til i tillegg til
 * det som ligger i System.getenv(), da må den sendes med dit den trengs.
 */
class Environment(private val env: Map<String, String> = System.getenv()) : Map<String, String> by env {
    val erLokal = env.containsKey("LOKAL_UTVIKLING")
    val erDev = "dev-gcp" == System.getenv("NAIS_CLUSTER_NAME")
    val erProd = "prod-gcp" == System.getenv("NAIS_CLUSTER_NAME")
}
