package no.nav.helse.spesialist.api.graphql.query

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.convertValue
import com.fasterxml.jackson.module.kotlin.treeToValue
import graphql.GraphQLException
import io.mockk.every
import no.nav.helse.spesialist.api.AbstractGraphQLApiTest
import no.nav.helse.spesialist.api.DatabaseIntegrationTest.Periode.Companion.til
import no.nav.helse.spesialist.api.graphql.GraphQLTestdata.opprettBeregnetPeriode
import no.nav.helse.spesialist.api.graphql.GraphQLTestdata.opprettSnapshotArbeidsgiver
import no.nav.helse.spesialist.api.graphql.GraphQLTestdata.opprettSnapshotGenerasjon
import no.nav.helse.spesialist.api.graphql.GraphQLTestdata.opprettSnapshotHendelse
import no.nav.helse.spesialist.api.graphql.GraphQLTestdata.opprettUberegnetPeriode
import no.nav.helse.spesialist.api.graphql.schema.ApiAvslag
import no.nav.helse.spesialist.api.graphql.schema.ApiAvslagstype
import no.nav.helse.spesialist.api.graphql.schema.ApiHandling
import no.nav.helse.spesialist.api.graphql.schema.ApiPeriodehandling
import no.nav.helse.spesialist.api.januar
import no.nav.helse.spesialist.api.objectMapper
import no.nav.helse.spesialist.api.person.Adressebeskyttelse
import no.nav.helse.spleis.graphql.enums.GraphQLPeriodetilstand
import no.nav.helse.spleis.graphql.hentsnapshot.GraphQLBeregnetPeriode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.ResourceLock
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.util.UUID

internal class PersonQueryHandlerTest : AbstractGraphQLApiTest() {

    @Test
    @ResourceLock("auditlogg-lytter")
    fun `henter person`() {
        mockSnapshot()
        val logglytter = Logglytter()
        opprettVedtaksperiode(opprettPerson(), opprettArbeidsgiver())

        val body = runQuery("""{ person(fnr: "$FØDSELSNUMMER") { aktorId } }""")

        assertEquals(AKTØRID, body["data"]["person"]["aktorId"].asText())
        logglytter.assertBleLogget("suid=${SAKSBEHANDLER.ident} duid=$FØDSELSNUMMER operation=PersonQuery", Level.INFO)
    }

    @Test
    fun `får 400-feil når man ikke oppgir fødselsnummer eller aktørid i query`() {
        val body = runQuery("""{ person(fnr: null) { aktorId } }""")

        assertEquals(400, body["errors"].first()["extensions"]["code"].asInt())
    }

    @Test
    fun `får 400-feil når verdien i aktorId-feltet ikke har riktig lengde`() {
        val body = runQuery("""{ person(aktorId: "$FØDSELSNUMMER" ) { aktorId } }""")

        val error = body["errors"].first()
        assertTrue(error["message"].asText().contains("Feil lengde på parameter aktorId"))
        assertEquals(400, error["extensions"]["code"].asInt())
    }

    @Test
    @ResourceLock("auditlogg-lytter")
    fun `får 404-feil når personen man søker etter ikke finnes`() {
        val logglytter = Logglytter()
        val body = runQuery("""{ person(fnr: "$FØDSELSNUMMER") { aktorId } }""")

        assertEquals(404, body["errors"].first()["extensions"]["code"].asInt())
        logglytter.assertBleLogget("suid=${SAKSBEHANDLER.ident} duid=$FØDSELSNUMMER operation=PersonQuery msg=Finner ikke data for person med identifikator $FØDSELSNUMMER", Level.WARN)
    }

    @Test
    @ResourceLock("auditlogg-lytter")
    fun `får 404-feil når personen man søker etter på aktørId ikke finnes`() {
        val logglytter = Logglytter()
        val body = runQuery("""{ person(aktorId: "$AKTØRID") { aktorId } }""")

        assertEquals(404, body["errors"].first()["extensions"]["code"].asInt())
        logglytter.assertBleLogget("suid=${SAKSBEHANDLER.ident} duid=$AKTØRID operation=PersonQuery msg=Finner ikke data for person med identifikator $AKTØRID", Level.WARN)
    }

    @Test
    @ResourceLock("auditlogg-lytter")
    fun `får 404-feil når personen ikke har noen arbeidsgivere`() {
        val logglytter = Logglytter()
        mockSnapshot(arbeidsgivere = emptyList())

        val body = runQuery("""{ person(aktorId: "$AKTØRID") { aktorId } }""")

        assertEquals(404, body["errors"].first()["extensions"]["code"].asInt())
        logglytter.assertBleLogget("suid=${SAKSBEHANDLER.ident} duid=$AKTØRID operation=PersonQuery msg=Finner ikke data for person med identifikator $AKTØRID", Level.WARN)
    }

    @Test
    @ResourceLock("auditlogg-lytter")
    fun `får 400-feil når det mangler både fødselsnummer og aktørId`() {
        val logglytter = Logglytter()
        val body = runQuery("""{ person(fnr: null, aktorId: null) { aktorId } }""")

        assertEquals(400, body["errors"].first()["extensions"]["code"].asInt())
        logglytter.assertIngenLoggingFor(FØDSELSNUMMER, AKTØRID)
    }

    @Test
    @ResourceLock("auditlogg-lytter")
    fun `får personens fødselsnummer-identer når hen har flere`() {
        val dNummer = "41017012345"
        opprettPerson(fødselsnummer = dNummer, aktørId = AKTØRID)
        opprettVedtaksperiode(opprettPerson(fødselsnummer = FØDSELSNUMMER, aktørId = AKTØRID), opprettArbeidsgiver())
        val logglytter = Logglytter()
        val body = runQuery("""{ person(aktorId: "$AKTØRID") { fodselsnummer } }""")

        val extensions = body["errors"].first()["extensions"]
        assertEquals(500, extensions["code"].asInt())
        assertEquals(setOf(dNummer, FØDSELSNUMMER), extensions["fodselsnumre"].map(JsonNode::asText).toSet())
        logglytter.assertBleLogget("suid=${SAKSBEHANDLER.ident} duid=$AKTØRID operation=PersonQuery msg=Mer enn ett fødselsnummer for personen", Level.WARN)
    }

    @Test
    @ResourceLock("auditlogg-lytter")
    fun `kan slå opp kode7-personer med riktige tilganger`() {
        mockSnapshot()
        opprettVedtaksperiode(opprettPerson(adressebeskyttelse = Adressebeskyttelse.Fortrolig), opprettArbeidsgiver())

        val logglytter = Logglytter()
        val body = runQuery("""{ person(fnr: "$FØDSELSNUMMER") { aktorId } }""", kode7Saksbehandlergruppe)

        assertEquals(AKTØRID, body["data"]["person"]["aktorId"].asText())
        logglytter.assertBleLogget("suid=${SAKSBEHANDLER.ident} duid=$FØDSELSNUMMER", Level.INFO)
    }

    @Test
    @ResourceLock("auditlogg-lytter")
    fun `får 403-feil ved oppslag av kode7-personer uten riktige tilganger`() {
        opprettVedtaksperiode(opprettPerson(adressebeskyttelse = Adressebeskyttelse.Fortrolig), opprettArbeidsgiver())

        val logglytter = Logglytter()
        val body = runQuery("""{ person(fnr: "$FØDSELSNUMMER") { aktorId } }""")

        assertEquals(403, body["errors"].first()["extensions"]["code"].asInt())
        logglytter.assertBleLogget("suid=${SAKSBEHANDLER.ident} duid=$FØDSELSNUMMER operation=PersonQuery flexString1=Deny", Level.WARN)
    }

    @Test
    @ResourceLock("auditlogg-lytter")
    fun `kan slå opp skjermede personer med riktige tilganger`() {
        mockSnapshot()
        every { egenAnsattApiDao.erEgenAnsatt(FØDSELSNUMMER) } returns true
        opprettVedtaksperiode(opprettPerson(), opprettArbeidsgiver())

        val logglytter = Logglytter()
        val body = runQuery("""{ person(fnr: "$FØDSELSNUMMER") { aktorId } }""", skjermedePersonerGruppeId)

        assertEquals(AKTØRID, body["data"]["person"]["aktorId"].asText())
        logglytter.assertBleLogget("suid=${SAKSBEHANDLER.ident} duid=$FØDSELSNUMMER operation=PersonQuery", Level.INFO)
    }

    @Test
    @ResourceLock("auditlogg-lytter")
    fun `får 403-feil ved oppslag av skjermede personer`() {
        every { egenAnsattApiDao.erEgenAnsatt(FØDSELSNUMMER) } returns true
        opprettVedtaksperiode(opprettPerson(), opprettArbeidsgiver())

        val logglytter = Logglytter()
        val body = runQuery("""{ person(fnr: "$FØDSELSNUMMER") { aktorId } }""")

        assertEquals(403, body["errors"].first()["extensions"]["code"].asInt())
        logglytter.assertBleLogget("suid=${SAKSBEHANDLER.ident} duid=$FØDSELSNUMMER operation=PersonQuery flexString1=Deny",Level.WARN)
    }

    @Test
    @ResourceLock("auditlogg-lytter")
    fun `får 501-feil når oppdatering av snapshot feiler`() {
        every { spleisClient.hentPerson(FØDSELSNUMMER) } throws GraphQLException("Oops")
        opprettVedtaksperiode(opprettPerson(), opprettArbeidsgiver())

        val logglytter = Logglytter()
        val body = runQuery("""{ person(fnr: "$FØDSELSNUMMER") { aktorId } }""")

        assertEquals(501, body["errors"].first()["extensions"]["code"].asInt())
        logglytter.assertBleLogget(
                    "suid=${SAKSBEHANDLER.ident} duid=$FØDSELSNUMMER operation=PersonQuery msg=Feil ved henting av snapshot for person", Level.WARN)
    }

    @Test
    fun `Uberegnet periode tilstøtende en periode med oppgave tar med seg varsler`() {
        val personRef = opprettPerson()
        val arbeidsgiverRef = opprettArbeidsgiver()
        val uberegnetPeriode = 2.januar(2023) til 3.januar(2023)
        val periodeMedOppgave = 4.januar(2023) til 5.januar(2023)
        opprettVedtaksperiode(personRef, arbeidsgiverRef, periode = periodeMedOppgave, skjæringstidspunkt = 2.januar)
        opprettVedtak(personRef, arbeidsgiverRef, periode = uberegnetPeriode, skjæringstidspunkt = 2.januar)
        val generasjonId = UUID.randomUUID()
        val generasjonRef =
            nyGenerasjon(
                vedtaksperiodeId = uberegnetPeriode.id,
                generasjonId = generasjonId,
                periode = uberegnetPeriode,
                skjæringstidspunkt = 2.januar,
            )
        val graphQLUberegnetPeriode = opprettUberegnetPeriode(2.januar(2023), 3.januar(2023), uberegnetPeriode.id)
        val graphQLperiodeMedOppgave = opprettBeregnetPeriode(4.januar(2023), 5.januar(2023), periodeMedOppgave.id)
        val snapshotGenerasjon = opprettSnapshotGenerasjon(listOf(graphQLUberegnetPeriode, graphQLperiodeMedOppgave))
        val arbeidsgiver = opprettSnapshotArbeidsgiver(ORGANISASJONSNUMMER, listOf(snapshotGenerasjon))
        mockSnapshot(arbeidsgivere = listOf(arbeidsgiver))
        opprettVarseldefinisjon()
        nyttVarsel(vedtaksperiodeId = uberegnetPeriode.id, generasjonRef = generasjonRef, status = "AKTIV")
        val periode = runPersonQuery().plukkUtPeriodeMed(uberegnetPeriode.id)
        val forventetVarsel = setOf(PersonQueryTestVarsel(generasjonId, "EN_KODE"))

        assertEquals(forventetVarsel, periode.varsler)
    }

    @Test
    fun `Uberegnet periode som ikke er tilstøtende en periode med oppgave tar ikke med seg varsler`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val generasjonRef = nyGenerasjon(vedtaksperiodeId = vedtaksperiodeId)
        val uberegnetPeriode = opprettUberegnetPeriode(2.januar(2023), 3.januar(2023), vedtaksperiodeId)
        val beregnetPeriode = opprettBeregnetPeriode(5.januar(2023), 6.januar(2023), PERIODE.id)
        val snapshotGenerasjon = opprettSnapshotGenerasjon(listOf(beregnetPeriode, uberegnetPeriode))
        val arbeidsgiver = opprettSnapshotArbeidsgiver(ORGANISASJONSNUMMER, listOf(snapshotGenerasjon))
        mockSnapshot(arbeidsgivere = listOf(arbeidsgiver))
        opprettVedtaksperiode(opprettPerson(), opprettArbeidsgiver())
        opprettVarseldefinisjon()
        nyttVarsel(vedtaksperiodeId = vedtaksperiodeId, generasjonRef = generasjonRef, status = "AKTIV")

        val periode = runPersonQuery().plukkUtPeriodeMed(vedtaksperiodeId)

        assertTrue(periode.varsler.isEmpty())
    }

    @Test
    fun `Bare siste generasjon av en uberegnet periode skal ta med seg aktive varsler`() {
        val snapshotGenerasjonId1 = UUID.randomUUID()
        val snapshotGenerasjonId2 = UUID.randomUUID()
        val personRef = opprettPerson()
        val arbeidsgiverRef = opprettArbeidsgiver()
        val uberegnetPeriode = 2.januar(2023) til 3.januar(2023)
        val periodeMedOppgave = 4.januar(2023) til 5.januar(2023)
        opprettVedtaksperiode(personRef, arbeidsgiverRef, periode = periodeMedOppgave, skjæringstidspunkt = 2.januar)
        opprettVedtak(personRef, arbeidsgiverRef, periode = uberegnetPeriode, skjæringstidspunkt = 2.januar)
        val generasjonId1 = UUID.randomUUID()
        val generasjonId2 = UUID.randomUUID()
        val generasjonRef1 =
            nyGenerasjon(
                vedtaksperiodeId = uberegnetPeriode.id,
                generasjonId = generasjonId1,
                periode = uberegnetPeriode,
                skjæringstidspunkt = 2.januar,
            )
        val generasjonRef2 =
            nyGenerasjon(
                vedtaksperiodeId = uberegnetPeriode.id,
                generasjonId = generasjonId2,
                periode = uberegnetPeriode,
                skjæringstidspunkt = 2.januar,
            )
        val graphQLUberegnetPeriode = opprettUberegnetPeriode(2.januar(2023), 3.januar(2023), uberegnetPeriode.id)
        val graphQLperiodeMedOppgave = opprettBeregnetPeriode(4.januar(2023), 5.januar(2023), periodeMedOppgave.id)
        val snapshotGenerasjon1 =
            opprettSnapshotGenerasjon(listOf(graphQLUberegnetPeriode, graphQLperiodeMedOppgave), snapshotGenerasjonId1)
        val snapshotGenerasjon2 =
            opprettSnapshotGenerasjon(listOf(graphQLUberegnetPeriode, graphQLperiodeMedOppgave), snapshotGenerasjonId2)
        val arbeidsgiver = opprettSnapshotArbeidsgiver(ORGANISASJONSNUMMER, listOf(snapshotGenerasjon1, snapshotGenerasjon2))
        mockSnapshot(arbeidsgivere = listOf(arbeidsgiver))
        opprettVarseldefinisjon()
        nyttVarsel(vedtaksperiodeId = uberegnetPeriode.id, generasjonRef = generasjonRef1, status = "AKTIV")
        nyttVarsel(vedtaksperiodeId = uberegnetPeriode.id, generasjonRef = generasjonRef2, status = "AKTIV")
        val førsteGenerasjonAvPeriode = runPersonQuery().plukkUtPeriodeMed(uberegnetPeriode.id, snapshotGenerasjonId2)
        val sisteGenerasjonAvPeriode = runPersonQuery().plukkUtPeriodeMed(uberegnetPeriode.id, snapshotGenerasjonId1)
        val forventedeVarsler = setOf(PersonQueryTestVarsel(generasjonId1, "EN_KODE"), PersonQueryTestVarsel(generasjonId2, "EN_KODE"))

        assertTrue(førsteGenerasjonAvPeriode.varsler.isEmpty())
        assertEquals(forventedeVarsler, sisteGenerasjonAvPeriode.varsler)
    }

    @Test
    fun `inkluderer handlinger`() {
        val personRef = opprettPerson()
        val arbeidsgiverRef = opprettArbeidsgiver()
        val periode1 = 1.januar(2023) til 23.januar(2023)
        val periode2 = 24.januar(2023) til 31.januar(2023)
        val vedtakId = opprettVedtaksperiode(personRef, arbeidsgiverRef, periode = periode1)
        ferdigstillOppgave(vedtakId)
        opprettVedtaksperiode(personRef, arbeidsgiverRef, periode = periode2)
        val førstePeriode = periode1.tilBeregnetPeriode().copy(periodetilstand = GraphQLPeriodetilstand.UTBETALT)
        val andrePeriode = periode2.tilBeregnetPeriode()
        val snapshotGenerasjon = opprettSnapshotGenerasjon(listOf(førstePeriode, andrePeriode))
        val arbeidsgiver = opprettSnapshotArbeidsgiver(ORGANISASJONSNUMMER, listOf(snapshotGenerasjon))
        mockSnapshot(arbeidsgivere = listOf(arbeidsgiver))

        val body = runPersonQuery()

        val perioder = body["data"]["person"]["arbeidsgivere"].first()["generasjoner"].first()["perioder"]
        assertEquals(2, perioder.size())
        val responseperiode1 = perioder.first { it["vedtaksperiodeId"].textValue() == periode1.id.toString() }
        val responseperiode2 = perioder.first { it["vedtaksperiodeId"].textValue() == periode2.id.toString() }
        assertFalse(
            responseperiode1["handlinger"].first { it["type"].textValue() == ApiPeriodehandling.UTBETALE.name }["tillatt"].booleanValue(),
        )
        assertTrue(
            responseperiode2["handlinger"].first { it["type"].textValue() == ApiPeriodehandling.UTBETALE.name }["tillatt"].booleanValue(),
        )
    }

    @Test
    fun `utbetaling av risk-oppgave tillatt for alle`() {
        val personRef = opprettPerson()
        val arbeidsgiverRef = opprettArbeidsgiver()
        opprettVedtaksperiode(personRef, arbeidsgiverRef)
        val (id, fom, tom) = PERIODE
        val graphQLperiodeMedOppgave = opprettBeregnetPeriode(fom, tom, id)
        val snapshotGenerasjon = opprettSnapshotGenerasjon(listOf(graphQLperiodeMedOppgave))
        val arbeidsgiver = opprettSnapshotArbeidsgiver(ORGANISASJONSNUMMER, listOf(snapshotGenerasjon))
        mockSnapshot(arbeidsgivere = listOf(arbeidsgiver))

        val body = runPersonQuery()

        val periode = body["data"]["person"]["arbeidsgivere"].first()["generasjoner"].first()["perioder"].first()
        assertFalse(periode["handlinger"].isEmpty)
        assertTrue(periode["handlinger"].first { it["type"].textValue() == ApiPeriodehandling.UTBETALE.name }["tillatt"].booleanValue())
    }

    @Test
    fun `periode med hendelse`() {
        val personRef = opprettPerson()
        val arbeidsgiverRef = opprettArbeidsgiver()
        opprettVedtaksperiode(personRef, arbeidsgiverRef)
        val (id, fom, tom) = PERIODE
        val eksternDokumentId = UUID.randomUUID()
        val graphQLHendelse = opprettSnapshotHendelse(eksternDokumentId)
        val graphQLperiodeMedOppgave = opprettBeregnetPeriode(fom, tom, id, hendelser = listOf(graphQLHendelse))
        val snapshotGenerasjon = opprettSnapshotGenerasjon(listOf(graphQLperiodeMedOppgave))
        val arbeidsgiver = opprettSnapshotArbeidsgiver(ORGANISASJONSNUMMER, listOf(snapshotGenerasjon))
        mockSnapshot(arbeidsgivere = listOf(arbeidsgiver))

        val body = runPersonQuery(null)

        val hendelse = body["data"]["person"]["arbeidsgivere"].first()["generasjoner"].first()["perioder"].first()["hendelser"].first()
        assertNotNull(hendelse)
        assertEquals(eksternDokumentId.toString(), hendelse["eksternDokumentId"].textValue())
    }

    @Test
    fun `periode med avslag`() {
        val avslagsbegrunnelse = "En individuell begrunnelse"
        every {
            saksbehandlerMediator.hentAvslag(any(), any())
        } returns
            setOf(
                ApiAvslag(
                    type = ApiAvslagstype.AVSLAG,
                    begrunnelse = avslagsbegrunnelse,
                    opprettet = LocalDateTime.now(),
                    saksbehandlerIdent = "AIDENT",
                    invalidert = false,
                ),
            )

        val personRef = opprettPerson()
        val arbeidsgiverRef = opprettArbeidsgiver()
        val snapshotGenerasjonId1 = UUID.randomUUID()
        opprettVedtaksperiode(personRef, arbeidsgiverRef)
        val graphQLperiodeMedOppgave = opprettBeregnetPeriode(4.januar(2023), 5.januar(2023), PERIODE.id)
        val snapshotGenerasjon = opprettSnapshotGenerasjon(listOf(graphQLperiodeMedOppgave), snapshotGenerasjonId1)
        val arbeidsgiver = opprettSnapshotArbeidsgiver(ORGANISASJONSNUMMER, listOf(snapshotGenerasjon))
        mockSnapshot(arbeidsgivere = listOf(arbeidsgiver))

        val body = runPersonQuery(null)

        val avslag = body["data"]["person"]["arbeidsgivere"].first()["generasjoner"].first()["perioder"].first()["avslag"].first()
        assertNotNull(avslag)
        assertEquals(ApiAvslagstype.AVSLAG, enumValueOf<ApiAvslagstype>(avslag["type"].textValue()))
        assertEquals(avslagsbegrunnelse, avslag["begrunnelse"].textValue())
        assertEquals("AIDENT", avslag["saksbehandlerIdent"].textValue())
        assertFalse(avslag["invalidert"].booleanValue())
    }

    @Test
    fun `kanAvvises-flagget inkluderes i GraphQL-svaret`() {
        opprettVedtaksperiode(opprettPerson(), opprettArbeidsgiver(), kanAvvises = false)
        val (id, fom, tom) = PERIODE
        val graphQLperiodeMedOppgave = opprettBeregnetPeriode(fom, tom, id)
        val snapshotGenerasjon = opprettSnapshotGenerasjon(listOf(graphQLperiodeMedOppgave))
        val arbeidsgiver = opprettSnapshotArbeidsgiver(ORGANISASJONSNUMMER, listOf(snapshotGenerasjon))
        mockSnapshot(arbeidsgivere = listOf(arbeidsgiver))

        val body = runPersonQuery()

        val periode = body["data"]["person"]["arbeidsgivere"].first()["generasjoner"].first()["perioder"].first()
        val forventedeHandlinger =
            setOf(
                ApiHandling(ApiPeriodehandling.UTBETALE, true, null),
                ApiHandling(ApiPeriodehandling.AVVISE, false, "Spleis støtter ikke å avvise perioden"),
            )
        assertFalse(periode["handlinger"].isEmpty)
        assertEquals(objectMapper.convertValue<Set<ObjectNode>>(forventedeHandlinger), periode["handlinger"].toSet())
    }

    private fun runPersonQuery(group: UUID? = null) =
        runQuery(
            """
            { 
                person(fnr: "$FØDSELSNUMMER") { 
                    aktorId
                    arbeidsgivere {
                        generasjoner {
                            id
                            perioder {
                                ... on UberegnetPeriode {
                                    vedtaksperiodeId
                                    varsler {
                                        generasjonId
                                        kode
                                    }                      
                                }
                                ... on BeregnetPeriode {
                                    vedtaksperiodeId
                                    handlinger { type, tillatt, begrunnelse }
                                    varsler {
                                        generasjonId
                                        kode
                                    }         
                                    avslag {
                                        type, begrunnelse, opprettet, saksbehandlerIdent, invalidert
                                    }
                                }
                                hendelser {
                                    ... on SoknadArbeidsledig {
                                        id
                                        fom
                                        tom
                                        eksternDokumentId
                                        rapportertDato
                                        sendtNav
                                        type
                                    }
                                    ... on SoknadNav {
                                        id
                                        fom
                                        tom
                                        eksternDokumentId
                                        rapportertDato
                                        sendtNav
                                        type
                                    }
                                }
                            }               
                        }    
                    }
                } 
            }
        """,
            group,
        )

    private fun JsonNode.plukkUtPeriodeMed(vedtaksperiodeId: UUID): PersonQueryTestPeriode {
        val jsonNode = this["data"]["person"]["arbeidsgivere"].first()["generasjoner"].first()["perioder"]
        val perioder = objectMapper.treeToValue<List<PersonQueryTestPeriode>>(jsonNode)
        return perioder.first { periode -> vedtaksperiodeId == periode.vedtaksperiodeId }
    }

    private fun JsonNode.plukkUtPeriodeMed(
        vedtaksperiodeId: UUID,
        generasjonId: UUID,
    ): PersonQueryTestPeriode {
        val jsonNode = this["data"]["person"]["arbeidsgivere"].first()["generasjoner"]
        val generasjoner = objectMapper.treeToValue<List<PersonQueryTestGenerasjon>>(jsonNode)
        val generasjon = generasjoner.first { generasjon -> generasjonId == generasjon.id }
        return generasjon.perioder.first { periode -> vedtaksperiodeId == periode.vedtaksperiodeId }
    }

    private data class PersonQueryTestGenerasjon(
        val id: UUID,
        val perioder: List<PersonQueryTestPeriode>,
    )

    private data class PersonQueryTestPeriode(
        val vedtaksperiodeId: UUID,
        val varsler: Set<PersonQueryTestVarsel>,
    )

    private data class PersonQueryTestVarsel(
        val generasjonId: UUID,
        val kode: String,
    )

    private fun Periode.tilBeregnetPeriode(): GraphQLBeregnetPeriode = opprettBeregnetPeriode(fom, tom, id)

    private class Logglytter {
        private val appender = ListAppender<ILoggingEvent>().apply {
            (LoggerFactory.getLogger("auditLogger") as Logger).addAppender(this)
            start()
        }

        fun assertBleLogget(melding: String, level: Level) =
            assertTrue(appender.list.count { it.message.contains(melding) && it.level == level } == 1) {
                "Forventet ett innslag med $level og melding=$melding, dette ble logget: ${appender.list}"
            }

        fun assertIngenLoggingFor(fødselsnummer: String, aktørid: String) =
            assertTrue(appender.list.none { it.message.contains(fødselsnummer) || it.message.contains(aktørid)} ) {
                "Forventet at det ikke skulle være logget noe for $fødselsnummer/$aktørid, dette ble logget: ${appender.list}"
            }
    }
}
