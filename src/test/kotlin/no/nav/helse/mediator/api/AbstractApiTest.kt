package no.nav.helse.mediator.api

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.routing.*
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.TestInstance
import java.util.*

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class AbstractApiTest {

    private lateinit var server: ApiTestUtil.TestServerRuntime
    protected lateinit var client: HttpClient

    protected fun setupServer(λ: Route.() -> Unit) {
        server = ApiTestUtil.TestServer(λ = λ).start()
        client = server.restClient()
    }

    @AfterAll
    protected fun tearDown() {
        server.close()
    }



}
