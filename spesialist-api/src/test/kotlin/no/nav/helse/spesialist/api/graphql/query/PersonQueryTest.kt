package no.nav.helse.spesialist.api.graphql.query

import graphql.GraphQLException
import io.mockk.clearMocks
import io.mockk.every
import no.nav.helse.spesialist.api.AbstractGraphQLApiTest
import no.nav.helse.spesialist.api.person.Adressebeskyttelse
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class PersonQueryTest : AbstractGraphQLApiTest() {

    @BeforeAll
    fun setup() {
        setupGraphQLServer()
    }

    @BeforeEach
    fun prepare() {
        mockSnapshot()
    }

    @AfterEach
    fun clean() {
        clearMocks(snapshotClient, egenAnsattApiDao)
    }

    @Test
    fun `henter person`() {
        opprettVedtaksperiode()

        val query = queryize("""{ person(fnr: "$FØDSELSNUMMER") { aktorId } }""")
        val body = runQuery(query)

        assertEquals(AKTØRID, body["data"]["person"]["aktorId"].asText())
    }

    @Test
    fun `får 400-feil når man ikke oppgir fødselsnummer eller aktørid i query`() {
        opprettVedtaksperiode()

        val query = queryize("""{ person(fnr: null) { aktorId } }""")
        val body = runQuery(query)

        assertEquals(400, body["errors"].first()["extensions"]["code"].asInt())
    }

    @Test
    fun `får 404-feil når personen man søker etter ikke finnes`() {
        val query = queryize("""{ person(fnr: "$FØDSELSNUMMER") { aktorId } }""")
        val body = runQuery(query)

        assertEquals(404, body["errors"].first()["extensions"]["code"].asInt())
    }

    @Test
    fun `kan slå opp kode7-personer med riktige tilganger`() {
        opprettVedtaksperiode(Adressebeskyttelse.Fortrolig)

        val query = queryize("""{ person(fnr: "$FØDSELSNUMMER") { aktorId } }""")
        val body = runQuery(query, kode7Saksbehandlergruppe)

        assertEquals(AKTØRID, body["data"]["person"]["aktorId"].asText())
    }

    @Test
    fun `får 403-feil ved oppslag av kode7-personer uten riktige tilganger`() {
        opprettVedtaksperiode(Adressebeskyttelse.Fortrolig)

        val query = queryize("""{ person(fnr: "$FØDSELSNUMMER") { aktorId } }""")
        val body = runQuery(query)

        assertEquals(403, body["errors"].first()["extensions"]["code"].asInt())
    }

    @Test
    fun `kan slå opp skjermede personer med riktige tilganger`() {
        every { egenAnsattApiDao.erEgenAnsatt(FØDSELSNUMMER) } returns true
        opprettVedtaksperiode()

        val query = queryize("""{ person(fnr: "$FØDSELSNUMMER") { aktorId } }""")
        val body = runQuery(query, skjermedePersonerGruppeId)

        assertEquals(AKTØRID, body["data"]["person"]["aktorId"].asText())
    }

    @Test
    fun `får 403-feil ved oppslag av skjermede personer`() {
        every { egenAnsattApiDao.erEgenAnsatt(FØDSELSNUMMER) } returns true
        opprettVedtaksperiode()

        val query = queryize("""{ person(fnr: "$FØDSELSNUMMER") { aktorId } }""")
        val body = runQuery(query)

        assertEquals(403, body["errors"].first()["extensions"]["code"].asInt())
    }

    @Test
    fun `får 501-feil når oppdatering av snapshot feiler`() {
        every { snapshotClient.hentSnapshot(FØDSELSNUMMER) } throws GraphQLException("Oops")
        opprettVedtaksperiode()

        val query = queryize("""{ person(fnr: "$FØDSELSNUMMER") { aktorId } }""")
        val body = runQuery(query)

        assertEquals(501, body["errors"].first()["extensions"]["code"].asInt())
    }

}