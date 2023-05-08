package no.nav.helse.spesialist.api.graphql.query

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.treeToValue
import graphql.GraphQLException
import io.mockk.clearMocks
import io.mockk.every
import java.time.LocalDate
import java.util.UUID
import no.nav.helse.spesialist.api.AbstractGraphQLApiTest
import no.nav.helse.spesialist.api.graphql.enums.GraphQLPeriodetilstand
import no.nav.helse.spesialist.api.graphql.hentsnapshot.GraphQLBeregnetPeriode
import no.nav.helse.spesialist.api.graphql.schema.Periodehandling
import no.nav.helse.spesialist.api.januar
import no.nav.helse.spesialist.api.objectMapper
import no.nav.helse.spesialist.api.oppgave.Oppgavetype
import no.nav.helse.spesialist.api.person.Adressebeskyttelse
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
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
        val body = runQuery("""{ person(fnr: null) { aktorId } }""")

        assertEquals(400, body["errors"].first()["extensions"]["code"].asInt())
    }

    @Test
    fun `får 404-feil når personen man søker etter ikke finnes`() {
        val body = runQuery("""{ person(fnr: "$FØDSELSNUMMER") { aktorId } }""")

        assertEquals(404, body["errors"].first()["extensions"]["code"].asInt())
    }

    @Test
    fun `får personens fødselsnummer-identer når hen har flere`() {
        val dNummer = "41017012345"
        opprettPerson(fødselsnummer = dNummer, aktørId = AKTØRID)
        opprettVedtaksperiode(opprettPerson(fødselsnummer = FØDSELSNUMMER, aktørId = AKTØRID), opprettArbeidsgiver())
        val body = runQuery("""{ person(aktorId: "$AKTØRID") { fodselsnummer } }""")

        val extensions = body["errors"].first()["extensions"]
        assertEquals(500, extensions["code"].asInt())
        assertEquals(setOf(dNummer, FØDSELSNUMMER), extensions["fodselsnumre"].map(JsonNode::asText).toSet())
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
        every { egenAnsattApiDao.erEgenAnsatt(FØDSELSNUMMER) } returns true
        opprettVedtaksperiode(opprettPerson(), opprettArbeidsgiver())

        val body = runQuery("""{ person(fnr: "$FØDSELSNUMMER") { aktorId } }""")

        assertEquals(403, body["errors"].first()["extensions"]["code"].asInt())
    }

    @Test
    fun `får 501-feil når oppdatering av snapshot feiler`() {
        every { snapshotClient.hentSnapshot(FØDSELSNUMMER) } throws GraphQLException("Oops")
        opprettVedtaksperiode(opprettPerson(), opprettArbeidsgiver())

        val body = runQuery("""{ person(fnr: "$FØDSELSNUMMER") { aktorId } }""")

        assertEquals(501, body["errors"].first()["extensions"]["code"].asInt())
    }

    @Test
    fun `Uberegnet periode tilstøtende en periode med oppgave tar med seg varsler`() {
        val personRef = opprettPerson()
        val arbeidsgiverRef = opprettArbeidsgiver()
        val uberegnetPeriode = periode("2023-01-02", "2023-01-03")
        val periodeMedOppgave = periode("2023-01-04", "2023-01-05")
        opprettVedtaksperiode(personRef, arbeidsgiverRef, periode = periodeMedOppgave, skjæringstidspunkt = 2.januar)
        opprettVedtak(personRef, arbeidsgiverRef, periode = uberegnetPeriode, skjæringstidspunkt = 2.januar)
        val generasjonId = UUID.randomUUID()
        val generasjonRef = nyGenerasjon(vedtaksperiodeId = uberegnetPeriode.id, generasjonId = generasjonId, periode = uberegnetPeriode, skjæringstidspunkt = 2.januar)
        val graphQLUberegnetPeriode = opprettUberegnetPeriode("2023-01-02", "2023-01-03", uberegnetPeriode.id)
        val graphQLperiodeMedOppgave = opprettBeregnetPeriode("2023-01-04", "2023-01-05", periodeMedOppgave.id)
        val snapshotGenerasjon = opprettSnapshotGenerasjon(listOf(graphQLUberegnetPeriode, graphQLperiodeMedOppgave))
        val arbeidsgiver = opprettSnapshotArbeidsgiver(listOf(snapshotGenerasjon))
        mockSnapshot(arbeidsgivere = listOf(arbeidsgiver))
        opprettVarseldefinisjon()
        nyttVarsel(vedtaksperiodeId = uberegnetPeriode.id, generasjonRef = generasjonRef, status = "AKTIV")
        val periode = runPersonQuery().plukkUtPeriodeMed(uberegnetPeriode.id)
        val forventetVarsel = setOf(PersonQueryTestVarsel(generasjonId, "EN_KODE"))

        assertEquals(forventetVarsel, periode.varslerForGenerasjon)
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
    fun `Bare siste generasjon av en uberegnet periode skal ta med seg aktive varsler`() {
        val snapshotGenerasjonId1 = UUID.randomUUID()
        val snapshotGenerasjonId2 = UUID.randomUUID()
        val personRef = opprettPerson()
        val arbeidsgiverRef = opprettArbeidsgiver()
        val uberegnetPeriode = periode("2023-01-02", "2023-01-03")
        val periodeMedOppgave = periode("2023-01-04", "2023-01-05")
        opprettVedtaksperiode(personRef, arbeidsgiverRef, periode = periodeMedOppgave, skjæringstidspunkt = 2.januar)
        opprettVedtak(personRef, arbeidsgiverRef, periode = uberegnetPeriode, skjæringstidspunkt = 2.januar)
        val generasjonId1 = UUID.randomUUID()
        val generasjonId2 = UUID.randomUUID()
        val generasjonRef1 = nyGenerasjon(vedtaksperiodeId = uberegnetPeriode.id, generasjonId = generasjonId1, periode = uberegnetPeriode, skjæringstidspunkt = 2.januar)
        val generasjonRef2 = nyGenerasjon(vedtaksperiodeId = uberegnetPeriode.id, generasjonId = generasjonId2, periode = uberegnetPeriode, skjæringstidspunkt = 2.januar)
        val graphQLUberegnetPeriode = opprettUberegnetPeriode("2023-01-02", "2023-01-03", uberegnetPeriode.id)
        val graphQLperiodeMedOppgave = opprettBeregnetPeriode("2023-01-04", "2023-01-05", periodeMedOppgave.id)
        val snapshotGenerasjon1 = opprettSnapshotGenerasjon(listOf(graphQLUberegnetPeriode, graphQLperiodeMedOppgave), snapshotGenerasjonId1)
        val snapshotGenerasjon2 = opprettSnapshotGenerasjon(listOf(graphQLUberegnetPeriode, graphQLperiodeMedOppgave), snapshotGenerasjonId2)
        val arbeidsgiver = opprettSnapshotArbeidsgiver(listOf(snapshotGenerasjon1, snapshotGenerasjon2))
        mockSnapshot(arbeidsgivere = listOf(arbeidsgiver))
        opprettVarseldefinisjon()
        nyttVarsel(vedtaksperiodeId = uberegnetPeriode.id, generasjonRef = generasjonRef1, status = "AKTIV")
        nyttVarsel(vedtaksperiodeId = uberegnetPeriode.id, generasjonRef = generasjonRef2, status = "AKTIV")
        val førsteGenerasjonAvPeriode = runPersonQuery().plukkUtPeriodeMed(uberegnetPeriode.id, snapshotGenerasjonId2)
        val sisteGenerasjonAvPeriode = runPersonQuery().plukkUtPeriodeMed(uberegnetPeriode.id, snapshotGenerasjonId1)
        val forventedeVarsler = setOf(PersonQueryTestVarsel(generasjonId1, "EN_KODE"), PersonQueryTestVarsel(generasjonId2, "EN_KODE"))

        assertTrue(førsteGenerasjonAvPeriode.varslerForGenerasjon.isEmpty())
        assertEquals(forventedeVarsler, sisteGenerasjonAvPeriode.varslerForGenerasjon)
    }

    @Test
    fun `inkluderer handlinger`() {
        val personRef = opprettPerson()
        val arbeidsgiverRef = opprettArbeidsgiver()
        val periode1 = periode("2023-01-01", "2023-01-23")
        val periode2 = periode("2023-01-24", "2023-01-31")
        val vedtakId = opprettVedtaksperiode(personRef, arbeidsgiverRef, periode = periode1)
        ferdigstillOppgave(vedtakId)
        opprettVedtaksperiode(personRef, arbeidsgiverRef, periode = periode2)
        val førstePeriode = periode1.tilBeregnetPeriode().copy(periodetilstand = GraphQLPeriodetilstand.UTBETALT)
        val andrePeriode = periode2.tilBeregnetPeriode()
        val snapshotGenerasjon = opprettSnapshotGenerasjon(listOf(førstePeriode, andrePeriode))
        val arbeidsgiver = opprettSnapshotArbeidsgiver(listOf(snapshotGenerasjon))
        mockSnapshot(arbeidsgivere = listOf(arbeidsgiver))

        val body = runPersonQuery()

        val perioder = body["data"]["person"]["arbeidsgivere"].first()["generasjoner"].first()["perioder"]
        assertEquals(2, perioder.size())
        val responseperiode1 = perioder.first { it["vedtaksperiodeId"].textValue() == periode1.id.toString() }
        val responseperiode2 = perioder.first { it["vedtaksperiodeId"].textValue() == periode2.id.toString() }
        assertFalse(responseperiode1["handlinger"].first { it["type"].textValue() == Periodehandling.UTBETALE.name }["tillatt"].booleanValue())
        assertTrue(responseperiode2["handlinger"].first { it["type"].textValue() == Periodehandling.UTBETALE.name }["tillatt"].booleanValue())
    }

    @Test
    fun `utbetaling av risk-oppgave ikke tillatt hvis ikke tilgang til risk`() {
        val personRef = opprettPerson()
        val arbeidsgiverRef = opprettArbeidsgiver()
        opprettVedtaksperiode(personRef, arbeidsgiverRef, oppgavetype = Oppgavetype.RISK_QA)
        val (id, fom, tom) = PERIODE
        val graphQLperiodeMedOppgave = opprettBeregnetPeriode(fom.toString(), tom.toString(), id)
        val snapshotGenerasjon = opprettSnapshotGenerasjon(listOf(graphQLperiodeMedOppgave))
        val arbeidsgiver = opprettSnapshotArbeidsgiver(listOf(snapshotGenerasjon))
        mockSnapshot(arbeidsgivere = listOf(arbeidsgiver))

        val body = runPersonQuery(null)

        val periode = body["data"]["person"]["arbeidsgivere"].first()["generasjoner"].first()["perioder"].first()
        assertFalse(periode["handlinger"].isEmpty)
        assertFalse(periode["handlinger"].first { it["type"].textValue() == Periodehandling.UTBETALE.name }["tillatt"].booleanValue())
    }

    @Test
    fun `utbetaling av risk-oppgave tillatt hvis tilgang til risk`() {
        val personRef = opprettPerson()
        val arbeidsgiverRef = opprettArbeidsgiver()
        opprettVedtaksperiode(personRef, arbeidsgiverRef, oppgavetype = Oppgavetype.RISK_QA)
        val (id, fom, tom) = PERIODE
        val graphQLperiodeMedOppgave = opprettBeregnetPeriode(fom.toString(), tom.toString(), id)
        val snapshotGenerasjon = opprettSnapshotGenerasjon(listOf(graphQLperiodeMedOppgave))
        val arbeidsgiver = opprettSnapshotArbeidsgiver(listOf(snapshotGenerasjon))
        mockSnapshot(arbeidsgivere = listOf(arbeidsgiver))

        val body = runPersonQuery(riskSaksbehandlergruppe)

        val periode = body["data"]["person"]["arbeidsgivere"].first()["generasjoner"].first()["perioder"].first()
        assertFalse(periode["handlinger"].isEmpty)
        assertTrue(periode["handlinger"].first { it["type"].textValue() == Periodehandling.UTBETALE.name }["tillatt"].booleanValue())
    }

    private fun runPersonQuery(group: UUID? = null) = runQuery(
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
                                    handlinger { type, tillatt }
                                    varslerForGenerasjon {
                                        generasjonId
                                        kode
                                    }                      
                                }
                            }                    
                        }               
                    }
                } 
            }""",
        group
    )

    private fun periode(fom: String, tom: String): Periode {
        return Periode(
            UUID.randomUUID(),
            LocalDate.parse(fom),
            LocalDate.parse(tom)
        )
    }

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

    private data class PersonQueryTestGenerasjon(
        val id: UUID,
        val perioder: List<PersonQueryTestPeriode>
    )

    private data class PersonQueryTestPeriode(
        val vedtaksperiodeId: UUID,
        val varslerForGenerasjon: Set<PersonQueryTestVarsel>
    )

    private data class PersonQueryTestVarsel(
        val generasjonId: UUID,
        val kode: String,
    )
    private fun Periode.tilBeregnetPeriode(): GraphQLBeregnetPeriode =
        opprettBeregnetPeriode(fom.toString(), tom.toString(), id)
}
