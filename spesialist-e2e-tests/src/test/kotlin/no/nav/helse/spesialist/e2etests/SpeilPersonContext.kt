package no.nav.helse.spesialist.e2etests

import com.fasterxml.jackson.databind.JsonNode
import io.ktor.client.HttpClient
import io.ktor.client.engine.apache.Apache
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.accept
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.jackson.JacksonConverter
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertTrue

class SpeilPersonContext(
    aktørId: String,
    private val saksbehandlerIdent: String,
    private val bearerAuthToken: String
) {
    private val person: JsonNode = fetchPerson(aktørId)

    fun saksbehandlerGodkjennerAlleVarsler() {
        person["arbeidsgivere"].flatMap { arbeidsgiver ->
            arbeidsgiver["generasjoner"].flatMap { generasjon ->
                generasjon["perioder"].flatMap { periode ->
                    periode["varsler"]
                }
            }
        }.forEach { varsel ->
            callGraphQL(
                operationName = "SettVarselStatus",
                variables = mapOf(
                    "generasjonIdString" to varsel["generasjonId"].asText(),
                    "varselkode" to varsel["kode"].asText(),
                    "ident" to saksbehandlerIdent,
                    "definisjonIdString" to varsel["definisjonId"].asText(),
                )
            )
        }
    }

    fun saksbehandlerTildelerSegSaken() {
        callGraphQL(
            operationName = "Tildeling",
            variables = mapOf(
                "oppgavereferanse" to getOppgaveId(),
            )
        )
    }

    fun saksbehandlerFatterVedtak() {
        callGraphQL(
            operationName = "FattVedtak",
            variables = mapOf(
                "oppgavereferanse" to getOppgaveId(),
                "begrunnelse" to "Fattet vedtak",
            )
        )
    }

    fun assertHarOppgaveegenskap(vararg forventedeEgenskaper: String) {
        val egenskaper =
            person["arbeidsgivere"][0]["generasjoner"][0]["perioder"][0]["egenskaper"].map { it["egenskap"].asText() }
        assertTrue(egenskaper.containsAll(forventedeEgenskaper.toSet())) { "Forventet å finne ${forventedeEgenskaper.toSet()} i $egenskaper" }
    }

    fun assertPeriodeHarIkkeOppgave() {
        assertTrue(person["arbeidsgivere"][0]["generasjoner"][0]["perioder"][0]["oppgave"].isNull) {
            "Forventet at oppgave var null for perioden, men den var: " +
                    person["arbeidsgivere"][0]["generasjoner"][0]["perioder"][0]["oppgave"].toPrettyString()
        }
    }

    private fun fetchPerson(aktørId: String): JsonNode {
        val fetchPersonResponse = callGraphQL(
            operationName = "FetchPerson",
            variables = mapOf(
                "aktorId" to aktørId,
            )
        )
        return fetchPersonResponse["data"]["person"]
            ?: error("Fikk ikke data.person i respons fra FetchPerson. Responsen var: ${fetchPersonResponse.toPrettyString()}")
    }

    private fun getOppgaveId(): String =
        person["arbeidsgivere"][0]["generasjoner"][0]["perioder"][0]["oppgave"]["id"].asText()

    private fun callGraphQL(operationName: String, variables: Map<String, Any>) = runBlocking {
        httpClient.post("http://localhost:${AbstractE2EIntegrationTest.port}/graphql") {
            contentType(ContentType.Application.Json)
            accept(ContentType.Application.Json)
            bearerAuth(bearerAuthToken)
            setBody(
                mapOf(
                    "query" to (this::class.java.getResourceAsStream("/graphql/$operationName.graphql")
                        ?.use { it.reader().readText() }
                        ?: error("Fant ikke $operationName.graphql")),
                    "operationName" to operationName,
                    "variables" to variables))
        }.bodyAsText().let(objectMapper::readTree)
    }

    companion object {
        private val httpClient: HttpClient =
            HttpClient(Apache) {
                install(ContentNegotiation) {
                    register(ContentType.Application.Json, JacksonConverter())
                }
                engine {
                    socketTimeout = 5_000
                    connectTimeout = 5_000
                    connectionRequestTimeout = 5_000
                }
            }
    }
}
