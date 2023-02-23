package no.nav.helse.spesialist.api.graphql.query

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.treeToValue
import graphql.GraphQLException
import io.mockk.clearMocks
import io.mockk.every
import java.util.UUID
import no.nav.helse.spesialist.api.AbstractGraphQLApiTest
import no.nav.helse.spesialist.api.objectMapper
import no.nav.helse.spesialist.api.person.Adressebeskyttelse
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class PersonQueryTest : AbstractGraphQLApiTest() {

    @AfterEach
    fun clean() {
        clearMocks(snapshotClient, egenAnsattApiDao)
    }

    @Test
    fun `henter person`() {
        mockSnapshot()
        opprettVedtaksperiode(opprettPerson(), opprettArbeidsgiver())

        val body = runQuery("""{ person(fnr: "$FØDSELSNUMMER") { aktorId } }""")

        assertEquals(AKTØRID, body["data"]["person"]["aktorId"].asText())
    }

    @Test
    fun `får 400-feil når man ikke oppgir fødselsnummer eller aktørid i query`() {
        mockSnapshot()
        opprettVedtaksperiode(opprettPerson(), opprettArbeidsgiver())

        val body = runQuery("""{ person(fnr: null) { aktorId } }""")

        assertEquals(400, body["errors"].first()["extensions"]["code"].asInt())
    }

    @Test
    fun `får 404-feil når personen man søker etter ikke finnes`() {
        mockSnapshot()
        val body = runQuery("""{ person(fnr: "$FØDSELSNUMMER") { aktorId } }""")

        assertEquals(404, body["errors"].first()["extensions"]["code"].asInt())
    }

    @Test
    fun `kan slå opp kode7-personer med riktige tilganger`() {
        mockSnapshot()
        opprettVedtaksperiode(opprettPerson(adressebeskyttelse = Adressebeskyttelse.Fortrolig), opprettArbeidsgiver())

        val body = runQuery("""{ person(fnr: "$FØDSELSNUMMER") { aktorId } }""", kode7Saksbehandlergruppe)

        assertEquals(AKTØRID, body["data"]["person"]["aktorId"].asText())
    }

    @Test
    fun `får 403-feil ved oppslag av kode7-personer uten riktige tilganger`() {
        mockSnapshot()
        opprettVedtaksperiode(opprettPerson(adressebeskyttelse = Adressebeskyttelse.Fortrolig), opprettArbeidsgiver())

        val body = runQuery("""{ person(fnr: "$FØDSELSNUMMER") { aktorId } }""")

        assertEquals(403, body["errors"].first()["extensions"]["code"].asInt())
    }

    @Test
    fun `kan slå opp skjermede personer med riktige tilganger`() {
        mockSnapshot()
        every { egenAnsattApiDao.erEgenAnsatt(FØDSELSNUMMER) } returns true
        opprettVedtaksperiode(opprettPerson(), opprettArbeidsgiver())

        val body = runQuery("""{ person(fnr: "$FØDSELSNUMMER") { aktorId } }""", skjermedePersonerGruppeId)

        assertEquals(AKTØRID, body["data"]["person"]["aktorId"].asText())
    }

    @Test
    fun `får 403-feil ved oppslag av skjermede personer`() {
        mockSnapshot()
        every { egenAnsattApiDao.erEgenAnsatt(FØDSELSNUMMER) } returns true
        opprettVedtaksperiode(opprettPerson(), opprettArbeidsgiver())

        val body = runQuery("""{ person(fnr: "$FØDSELSNUMMER") { aktorId } }""")

        assertEquals(403, body["errors"].first()["extensions"]["code"].asInt())
    }

    @Test
    fun `får 501-feil når oppdatering av snapshot feiler`() {
        mockSnapshot()
        every { snapshotClient.hentSnapshot(FØDSELSNUMMER) } throws GraphQLException("Oops")
        opprettVedtaksperiode(opprettPerson(), opprettArbeidsgiver())

        val body = runQuery("""{ person(fnr: "$FØDSELSNUMMER") { aktorId } }""")

        assertEquals(501, body["errors"].first()["extensions"]["code"].asInt())
    }

    @Test
    fun `Uberegnet periode tilstøtende en periode med oppgave tar med seg varsler`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val generasjonRef = nyGenerasjon(vedtaksperiodeId = vedtaksperiodeId)
        val uberegnetPeriode = opprettUberegnetPeriode("2023-01-02", "2023-01-03", vedtaksperiodeId)
        val beregnetPeriode = opprettBeregnetPeriode("2023-01-04", "2023-01-05", PERIODE.id)
        val snapshotGenerasjon = opprettSnapshotGenerasjon(listOf(beregnetPeriode, uberegnetPeriode))
        val arbeidsgiver = opprettSnapshotArbeidsgiver(listOf(snapshotGenerasjon))
        mockSnapshot(arbeidsgivere = listOf(arbeidsgiver))
        opprettVedtaksperiode(opprettPerson(), opprettArbeidsgiver())
        opprettVarseldefinisjon()
        nyttVarsel(vedtaksperiodeId = vedtaksperiodeId, generasjonRef = generasjonRef, status = "AKTIV")

        val periode = runPersonQuery().plukkUtPeriodeMed(vedtaksperiodeId)

        assertTrue(periode.varslerForGenerasjon.isNotEmpty())
    }

    @Test
    fun `Uberegnet periode som ikke er tilstøtende en periode med oppgave tar ikke med seg varsler`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val generasjonRef = nyGenerasjon(vedtaksperiodeId = vedtaksperiodeId)
        val uberegnetPeriode = opprettUberegnetPeriode("2023-01-02", "2023-01-03", vedtaksperiodeId)
        val beregnetPeriode = opprettBeregnetPeriode("2023-01-05", "2023-01-06", PERIODE.id)
        val snapshotGenerasjon = opprettSnapshotGenerasjon(listOf(beregnetPeriode, uberegnetPeriode))
        val arbeidsgiver = opprettSnapshotArbeidsgiver(listOf(snapshotGenerasjon))
        mockSnapshot(arbeidsgivere = listOf(arbeidsgiver))
        opprettVedtaksperiode(opprettPerson(), opprettArbeidsgiver())
        opprettVarseldefinisjon()
        nyttVarsel(vedtaksperiodeId = vedtaksperiodeId, generasjonRef = generasjonRef, status = "AKTIV")

        val periode = runPersonQuery().plukkUtPeriodeMed(vedtaksperiodeId)

        assertTrue(periode.varslerForGenerasjon.isEmpty())
    }

    @Test
    fun `Bare siste generasjon av en uberegnet periode skal ta med seg varsler`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val snapshotGenerasjonId1 = UUID.randomUUID()
        val snapshotGenerasjonId2 = UUID.randomUUID()
        val generasjonRef1 = nyGenerasjon(vedtaksperiodeId = vedtaksperiodeId)
        val generasjonRef2 = nyGenerasjon(vedtaksperiodeId = vedtaksperiodeId)
        val uberegnetPeriode = opprettUberegnetPeriode("2023-01-02", "2023-01-03", vedtaksperiodeId)
        val beregnetPeriode = opprettBeregnetPeriode("2023-01-04", "2023-01-05", PERIODE.id)
        val snapshotGenerasjon1 = opprettSnapshotGenerasjon(listOf(beregnetPeriode, uberegnetPeriode), snapshotGenerasjonId1)
        val snapshotGenerasjon2 = opprettSnapshotGenerasjon(listOf(beregnetPeriode, uberegnetPeriode), snapshotGenerasjonId2)
        val arbeidsgiver = opprettSnapshotArbeidsgiver(listOf(snapshotGenerasjon1, snapshotGenerasjon2))
        mockSnapshot(arbeidsgivere = listOf(arbeidsgiver))
        opprettVedtaksperiode(opprettPerson(), opprettArbeidsgiver())
        opprettVarseldefinisjon()
        nyttVarsel(vedtaksperiodeId = vedtaksperiodeId, generasjonRef = generasjonRef1, status = "AKTIV")
        nyttVarsel(vedtaksperiodeId = vedtaksperiodeId, generasjonRef = generasjonRef2, status = "AKTIV")

        val førsteGenerasjonAvPeriode = runPersonQuery().plukkUtPeriodeMed(vedtaksperiodeId, snapshotGenerasjonId2)
        val sisteGenerasjonAvPeriode = runPersonQuery().plukkUtPeriodeMed(vedtaksperiodeId, snapshotGenerasjonId1)

        assertTrue(førsteGenerasjonAvPeriode.varslerForGenerasjon.isEmpty())
        assertTrue(sisteGenerasjonAvPeriode.varslerForGenerasjon.isNotEmpty())
    }

    private fun runPersonQuery() = runQuery(
        """{ 
                person(fnr: "$FØDSELSNUMMER") { 
                    aktorId
                    arbeidsgivere {
                        generasjoner {
                            id
                            perioder {
                                ... on UberegnetPeriode {
                                    vedtaksperiodeId
                                    varslerForGenerasjon {
                                        generasjonId
                                        kode
                                    }                      
                                }
                                ... on BeregnetPeriode {
                                    vedtaksperiodeId
                                    varslerForGenerasjon {
                                        generasjonId
                                        kode
                                    }                      
                                }
                            }                    
                        }               
                    }
                } 
            }"""
    )

    private fun JsonNode.plukkUtPeriodeMed(vedtaksperiodeId: UUID): PersonQueryTestPeriode {
        val jsonNode = this["data"]["person"]["arbeidsgivere"].first()["generasjoner"].first()["perioder"]
        val perioder = objectMapper.treeToValue<List<PersonQueryTestPeriode>>(jsonNode)
        return perioder.first { periode -> vedtaksperiodeId == periode.vedtaksperiodeId }
    }

    private fun JsonNode.plukkUtPeriodeMed(vedtaksperiodeId: UUID, generasjonId: UUID): PersonQueryTestPeriode {
        val jsonNode = this["data"]["person"]["arbeidsgivere"].first()["generasjoner"]
        val generasjoner = objectMapper.treeToValue<List<PersonQueryTestGenerasjon>>(jsonNode)
        val generasjon = generasjoner.first { generasjon -> generasjonId == generasjon.id }
        return generasjon.perioder.first { periode -> vedtaksperiodeId == periode.vedtaksperiodeId }
    }

    private class PersonQueryTestGenerasjon(
        val id: UUID,
        val perioder: List<PersonQueryTestPeriode>
    )

    private class PersonQueryTestPeriode(
        val vedtaksperiodeId: UUID,
        val varslerForGenerasjon: List<PersonQueryTestVarsel>
    )

    private class PersonQueryTestVarsel(
        val generasjonId: UUID,
        val kode: String,
    )
}