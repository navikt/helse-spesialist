package no.nav.helse.spesialist.client.personpseudoid

import no.nav.helse.spesialist.client.personpseudoid.ClientPersonPseudoIdModule.Configuration
import org.testcontainers.containers.GenericContainer
import org.testcontainers.utility.DockerImageName

class PersonPseudoIdTestFixture(
    moduleLabel: String,
) {
    private val valkey: GenericContainer<*> =
        GenericContainer(DockerImageName.parse("valkey/valkey:latest"))
            .withReuse(true)
            .withLabel("app", "spesialist")
            .withLabel("module", moduleLabel)
            .withLabel("code-location", javaClass.canonicalName)
            .apply {
                withExposedPorts(6379)
                withCommand("valkey-server", "--requirepass", "password")
                start()
            }

    val moduleConfiguration =
        Configuration(
            brukernavn = "default",
            passord = "password",
            connectionString = "valkey://${valkey.host}:${valkey.getMappedPort(6379)}",
        )
}
