package no.nav.helse.spesialist.client.personpseudoid

class ClientPersonPseudoIdModule(
    configuration: Configuration,
) {
    data class Configuration(
        val brukernavn: String,
        val passord: String,
        val connectionString: String,
    )
}
