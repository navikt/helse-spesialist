package no.nav.helse

import io.ktor.client.HttpClient
import io.ktor.client.features.json.JacksonSerializer
import io.ktor.client.features.json.JsonFeature
import io.ktor.util.KtorExperimentalAPI
import kotlinx.coroutines.runBlocking
import no.nav.helse.mediator.kafka.SpleisBehovMediator
import no.nav.helse.mediator.kafka.meldinger.GodkjenningMessage
import no.nav.helse.mediator.kafka.meldinger.PersoninfoMessage
import no.nav.helse.modell.dao.*
import no.nav.helse.modell.dao.SpeilSnapshotRestDao
import no.nav.helse.modell.dao.VedtakDao
import no.nav.helse.rapids_rivers.RapidApplication
import java.nio.file.Files
import java.nio.file.Paths

@KtorExperimentalAPI
fun main(): Unit = runBlocking {
    val dataSourceBuilder = DataSourceBuilder(System.getenv())
    dataSourceBuilder.migrate()
    val dataSource = dataSourceBuilder.getDataSource(DataSourceBuilder.Role.User)
    val personDao = PersonDao(dataSource)
    val arbeidsgiverDao = ArbeidsgiverDao(dataSource)
    val vedtakDao = VedtakDao(dataSource)
    val spleisClient = HttpClient {
        install(JsonFeature) { serializer = JacksonSerializer() } }
    val oidcDiscovery = AzureAad(spleisClient).oidcDiscovery(System.getenv("AZURE_CONFIG_URL"))
    val accessTokenClient = AccessTokenClient(oidcDiscovery.token_endpoint, readClientId(), readClientSecret(), spleisClient)
    val snapshotDao = SnapshotDao(dataSource)
    val speilSnapshotRestDao = SpeilSnapshotRestDao(spleisClient, accessTokenClient, System.getenv("SPLEIS_CLIENT_ID"))
    val oppgaveDao = OppgaveDao(dataSource)

    RapidApplication.create(System.getenv()).apply {
        val spleisBehovMediator = SpleisBehovMediator()

        GodkjenningMessage.Factory(
            rapidsConnection = this,
            personDao = personDao,
            arbeidsgiverDao = arbeidsgiverDao,
            vedtakDao = vedtakDao,
            snapshotDao = snapshotDao,
            spleisBehovMediator = spleisBehovMediator,
            speilSnapshotRestDao = speilSnapshotRestDao,
            oppgaveDao = oppgaveDao
        )
        PersoninfoMessage.Factory(this, spleisBehovMediator)
    }.start()
}


val azureMountPath: String = "/var/run/secrets/nais.io/azure"

fun readClientId(): String {
    return Files.readString(Paths.get(azureMountPath, "client_id"))
}

fun readClientSecret(): String {
    return Files.readString(Paths.get(azureMountPath, "client_secret"))
}
