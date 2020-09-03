package no.nav.helse.tildeling

import com.opentable.db.postgres.embedded.EmbeddedPostgres
import no.nav.helse.AzureAdAppConfig
import no.nav.helse.OidcDiscovery
import no.nav.helse.api.JwtStub
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.io.TempDir
import java.net.ServerSocket
import java.nio.file.Path
import javax.sql.DataSource
import kotlin.properties.Delegates

class TildelingApiTest {
    private lateinit var embeddedPostgres: EmbeddedPostgres
    private lateinit var dataSource: DataSource
    private var vedtakId by Delegates.notNull<Long>()

    private val httpPort = ServerSocket(0).use { it.localPort }

    private val jwtStub = JwtStub()
    private val clientId = "client_id"
    private val speilClientId = "speil_id"
    private val issuer = "https://jwt-provider-domain"
    private val requiredGroup = "required_group"

    private val oidcDiscovery = OidcDiscovery(token_endpoint = "token_endpoint", jwks_uri = "en_uri", issuer = issuer)
    private val azureConfig = AzureAdAppConfig(clientId = clientId, speilClientId = speilClientId, requiredGroup = requiredGroup)
    private val jwkProvider = jwtStub.getJwkProviderMock()

    @BeforeAll
    fun setup(@TempDir postgresPath: Path) {
        embeddedPostgres = EmbeddedPostgres.builder()
            .setOverrideWorkingDirectory(postgresPath.toFile())
            .setDataDirectory(postgresPath.resolve("datadir"))
            .start()
        dataSource = embeddedPostgres.setupDataSource()
        vedtakId = dataSource.opprettVedtak().toLong()
    }

    @AfterAll
    fun tearDown() {
        embeddedPostgres.close()
    }
}
