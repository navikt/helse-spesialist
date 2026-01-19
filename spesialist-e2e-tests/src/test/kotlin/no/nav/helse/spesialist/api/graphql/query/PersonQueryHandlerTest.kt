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
import no.nav.helse.modell.vedtak.Utfall
import no.nav.helse.spesialist.api.AbstractGraphQLApiTest
import no.nav.helse.spesialist.api.DatabaseIntegrationTest.Periode.Companion.til
import no.nav.helse.spesialist.api.graphql.GraphQLTestdata.opprettBeregnetPeriode
import no.nav.helse.spesialist.api.graphql.GraphQLTestdata.opprettSnapshotArbeidsgiver
import no.nav.helse.spesialist.api.graphql.GraphQLTestdata.opprettSnapshotGenerasjon
import no.nav.helse.spesialist.api.graphql.GraphQLTestdata.opprettSnapshotHendelse
import no.nav.helse.spesialist.api.graphql.GraphQLTestdata.opprettUberegnetPeriode
import no.nav.helse.spesialist.api.graphql.schema.ApiAvslagstype
import no.nav.helse.spesialist.api.graphql.schema.ApiHandling
import no.nav.helse.spesialist.api.graphql.schema.ApiPeriodehandling
import no.nav.helse.spesialist.api.objectMapper
import no.nav.helse.spesialist.api.person.Adressebeskyttelse
import no.nav.helse.spesialist.application.PersonPseudoId
import no.nav.helse.spesialist.domain.Arbeidsgiver
import no.nav.helse.spesialist.domain.ArbeidsgiverIdentifikator
import no.nav.helse.spesialist.domain.Identitetsnummer
import no.nav.helse.spesialist.domain.VedtakBegrunnelse
import no.nav.helse.spesialist.domain.testfixtures.jan
import no.nav.helse.spesialist.domain.testfixtures.lagBehandling
import no.nav.helse.spesialist.domain.testfixtures.lagOppgave
import no.nav.helse.spesialist.domain.testfixtures.lagOrganisasjonsnavn
import no.nav.helse.spesialist.domain.testfixtures.lagOrganisasjonsnummer
import no.nav.helse.spesialist.domain.testfixtures.lagVedtaksperiode
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagDNummer
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagFødselsnummer
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagPerson
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagSaksbehandler
import no.nav.helse.spesialist.domain.tilgangskontroll.Tilgangsgruppe
import no.nav.helse.spleis.graphql.enums.GraphQLPeriodetilstand
import no.nav.helse.spleis.graphql.hentsnapshot.GraphQLBeregnetPeriode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.ResourceLock
import org.slf4j.LoggerFactory
import java.util.UUID
import kotlin.test.assertContains

class PersonQueryHandlerTest : AbstractGraphQLApiTest() {
    @Test
    @ResourceLock("auditlogg-lytter")
    fun `henter person med personPseudoId som parameter`() {
        // Given
        mockSnapshot()
        val logglytter = Logglytter()
        opprettVedtaksperiode(opprettPerson())
        val pseudoId = opprettPersonPseudoId()

        // When
        val body = runQuery("""{ person(personPseudoId: "$pseudoId") { aktorId } }""")

        // Then
        assertEquals(AKTØRID, body["data"]["person"]["aktorId"].asText())
        logglytter.assertBleLogget(
            "suid=${SAKSBEHANDLER.ident.value} duid=$FØDSELSNUMMER operation=PersonQuery",
            Level.INFO
        )
    }

    @Test
    @ResourceLock("auditlogg-lytter")
    fun `får 404-feil når personen man søker etter ikke finnes`() {
        val logglytter = Logglytter()
        val personPseudoId = PersonPseudoId.ny().value
        val body = runQuery("""{ person(personPseudoId: "$personPseudoId") { aktorId } }""")

        assertEquals(404, body["errors"].first()["extensions"]["code"].asInt())
        assertEquals(
            "Exception while fetching data (/person) : PseudoId er ugyldig eller utgått",
            body["errors"].first()["message"].asText()
        )
        logglytter.assertBleLogget(
            "suid=${SAKSBEHANDLER.ident.value} duid=$personPseudoId operation=PersonQuery msg=Finner ikke data for person med identifikator $personPseudoId",
            Level.WARN,
        )
    }

    @Test
    @ResourceLock("auditlogg-lytter")
    fun `får 400-feil når personen man sender inn en personPseudoId på feil format`() {
        val body = runQuery("""{ person(personPseudoId: "abc123") { aktorId } }""")

        assertEquals(400, body["errors"].first()["extensions"]["code"].asInt())
        assertEquals(
            "Exception while fetching data (/person) : Ugyldig format på personPseudoId",
            body["errors"].first()["message"].asText()
        )
    }

    @Test
    @ResourceLock("auditlogg-lytter")
    fun `får 404-feil når personen ikke har noen arbeidsgivere`() {
        val logglytter = Logglytter()
        mockSnapshot(arbeidsgivere = emptyList())
        val pseudoId = opprettPersonPseudoId()

        val body = runQuery("""{ person(personPseudoId: "$pseudoId") { aktorId } }""")

        assertEquals(404, body["errors"].first()["extensions"]["code"].asInt())
        logglytter.assertBleLogget(
            "suid=${SAKSBEHANDLER.ident.value} duid=$FØDSELSNUMMER operation=PersonQuery msg=Finner ikke data for person med identifikator $FØDSELSNUMMER",
            Level.WARN,
        )
    }

    @Test
    @ResourceLock("auditlogg-lytter")
    fun `får personens fødselsnummer-identer når hen har flere`() {
        // Given
        mockSnapshot()
        val dNummer = lagDNummer()
        val logglytter = Logglytter()
        opprettPerson(fødselsnummer = dNummer, aktørId = AKTØRID)
        opprettVedtaksperiode(opprettPerson(fødselsnummer = FØDSELSNUMMER, aktørId = AKTØRID))
        val pseudoId = opprettPersonPseudoId()

        val body = runQuery("""{ person(personPseudoId: "$pseudoId") { andreFodselsnummer { fodselsnummer } } }""")

        assertEquals(1, body["data"]["person"]["andreFodselsnummer"].size())
        assertEquals(dNummer, body["data"]["person"]["andreFodselsnummer"][0]["fodselsnummer"].asText())
        logglytter.assertBleLogget(
            "suid=${SAKSBEHANDLER.ident.value} duid=$FØDSELSNUMMER operation=PersonQuery",
            Level.INFO
        )
    }

    @Test
    @ResourceLock("auditlogg-lytter")
    fun `kan slå opp kode7-personer med riktige tilganger`() {
        mockSnapshot()
        opprettVedtaksperiode(opprettPerson(adressebeskyttelse = Adressebeskyttelse.Fortrolig))

        val logglytter = Logglytter()
        val pseudoId = opprettPersonPseudoId()

        val body = runQuery(
            query = """{ person(personPseudoId: "$pseudoId") { aktorId } }""",
            tilgangsgruppe = Tilgangsgruppe.KODE_7,
        )

        assertEquals(AKTØRID, body["data"]["person"]["aktorId"].asText())
        logglytter.assertBleLogget("suid=${SAKSBEHANDLER.ident.value} duid=$FØDSELSNUMMER", Level.INFO)
    }

    @Test
    @ResourceLock("auditlogg-lytter")
    fun `får 403-feil ved oppslag av kode7-personer uten riktige tilganger`() {
        opprettVedtaksperiode(opprettPerson(adressebeskyttelse = Adressebeskyttelse.Fortrolig))

        val logglytter = Logglytter()
        val pseudoId = opprettPersonPseudoId()

        val body = runQuery("""{ person(personPseudoId: "$pseudoId") { aktorId } }""")

        assertEquals(403, body["errors"].first()["extensions"]["code"].asInt())
        logglytter.assertBleLogget(
            "suid=${SAKSBEHANDLER.ident.value} duid=$FØDSELSNUMMER operation=PersonQuery flexString1=Deny",
            Level.WARN,
        )
    }

    @Test
    @ResourceLock("auditlogg-lytter")
    fun `kan slå opp skjermede personer med riktige tilganger`() {
        mockSnapshot()
        opprettVedtaksperiode(opprettPerson(erEgenAnsatt = true))

        val logglytter = Logglytter()
        val pseudoId = opprettPersonPseudoId()

        val body = runQuery(
            query = """{ person(personPseudoId: "$pseudoId") { aktorId } }""",
            tilgangsgruppe = Tilgangsgruppe.EGEN_ANSATT,
        )

        assertEquals(AKTØRID, body["data"]["person"]["aktorId"].asText())
        logglytter.assertBleLogget(
            "suid=${SAKSBEHANDLER.ident.value} duid=$FØDSELSNUMMER operation=PersonQuery",
            Level.INFO
        )
    }

    @Test
    @ResourceLock("auditlogg-lytter")
    fun `får 403-feil ved oppslag av skjermede personer`() {
        opprettVedtaksperiode(opprettPerson(erEgenAnsatt = true))

        val logglytter = Logglytter()
        val pseudoId = opprettPersonPseudoId()

        val body = runQuery("""{ person(personPseudoId: "$pseudoId") { aktorId } }""")

        assertEquals(403, body["errors"].first()["extensions"]["code"].asInt())
        logglytter.assertBleLogget(
            "suid=${SAKSBEHANDLER.ident.value} duid=$FØDSELSNUMMER operation=PersonQuery flexString1=Deny",
            Level.WARN,
        )
    }

    @Test
    @ResourceLock("auditlogg-lytter")
    fun `får 501-feil når oppdatering av snapshot feiler`() {
        every { spleisClient.hentPerson(FØDSELSNUMMER) } throws GraphQLException("Oops")
        opprettVedtaksperiode(opprettPerson())

        val logglytter = Logglytter()
        val pseudoId = opprettPersonPseudoId()

        val body = runQuery("""{ person(personPseudoId: "$pseudoId") { aktorId } }""")

        assertEquals(500, body["errors"].first()["extensions"]["code"].asInt())
        logglytter.assertBleLogget(
            "suid=${SAKSBEHANDLER.ident.value} duid=$FØDSELSNUMMER operation=PersonQuery msg=Feil ved henting av snapshot for person",
            Level.WARN,
        )
    }

    @Test
    fun `Uberegnet periode tilstøtende en periode med oppgave tar med seg varsler`() {
        val personRef = opprettPerson()
        opprettArbeidsgiver()
        val uberegnetPeriode = 2 jan 2023 til (3 jan 2023)
        val periodeMedOppgave = 4 jan 2023 til (5 jan 2023)
        opprettVedtaksperiode(personRef, periode = periodeMedOppgave, skjæringstidspunkt = 2 jan 2018)
        opprettVedtak(personId = personRef, periode = uberegnetPeriode, skjæringstidspunkt = 2 jan 2018)
        val behandlingId = UUID.randomUUID()
        val behandlingRef =
            nyBehandling(
                vedtaksperiodeId = uberegnetPeriode.id,
                behandlingId = behandlingId,
                periode = uberegnetPeriode,
                skjæringstidspunkt = 2 jan 2018,
            )
        val graphQLUberegnetPeriode = opprettUberegnetPeriode(2 jan 2023, 3 jan 2023, uberegnetPeriode.id)
        val graphQLperiodeMedOppgave = opprettBeregnetPeriode(4 jan 2023, 5 jan 2023, periodeMedOppgave.id)
        val snapshotBehandling = opprettSnapshotGenerasjon(listOf(graphQLUberegnetPeriode, graphQLperiodeMedOppgave))
        val arbeidsgiver = opprettSnapshotArbeidsgiver(ORGANISASJONSNUMMER, listOf(snapshotBehandling))
        mockSnapshot(arbeidsgivere = listOf(arbeidsgiver))
        opprettVarseldefinisjon()
        nyttVarsel(vedtaksperiodeId = uberegnetPeriode.id, behandlingRef = behandlingRef, status = "AKTIV")
        val periode = runPersonQuery().plukkUtPeriodeMed(uberegnetPeriode.id)
        val forventetVarsel = setOf(PersonQueryTestVarsel(behandlingId, "EN_KODE"))

        assertEquals(forventetVarsel, periode.varsler)
    }

    @Test
    fun `Uberegnet periode som ikke er tilstøtende en periode med oppgave tar ikke med seg varsler`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val behandlingRef = nyBehandling(vedtaksperiodeId = vedtaksperiodeId)
        val uberegnetPeriode = opprettUberegnetPeriode(2 jan 2023, 3 jan 2023, vedtaksperiodeId)
        val beregnetPeriode = opprettBeregnetPeriode(5 jan 2023, 6 jan 2023, PERIODE.id)
        val snapshotBehandling = opprettSnapshotGenerasjon(listOf(beregnetPeriode, uberegnetPeriode))
        val arbeidsgiver = opprettSnapshotArbeidsgiver(ORGANISASJONSNUMMER, listOf(snapshotBehandling))
        mockSnapshot(arbeidsgivere = listOf(arbeidsgiver))
        opprettVedtaksperiode(opprettPerson())
        opprettVarseldefinisjon()
        nyttVarsel(vedtaksperiodeId = vedtaksperiodeId, behandlingRef = behandlingRef, status = "AKTIV")

        val periode = runPersonQuery().plukkUtPeriodeMed(vedtaksperiodeId)

        assertTrue(periode.varsler.isEmpty())
    }

    @Test
    fun `Bare siste behandling for en uberegnet periode skal ta med seg aktive varsler`() {
        val snapshotBehandlingId1 = UUID.randomUUID()
        val snapshotBehandlingId2 = UUID.randomUUID()
        val personRef = opprettPerson()
        opprettArbeidsgiver()
        val uberegnetPeriode = 2 jan 2023 til (3 jan 2023)
        val periodeMedOppgave = 4 jan 2023 til (5 jan 2023)
        opprettVedtaksperiode(personRef, periode = periodeMedOppgave, skjæringstidspunkt = 2 jan 2018)
        opprettVedtak(personId = personRef, periode = uberegnetPeriode, skjæringstidspunkt = 2 jan 2018)
        val behandlingId1 = UUID.randomUUID()
        val behandlingId2 = UUID.randomUUID()
        val behandlingRef1 =
            nyBehandling(
                vedtaksperiodeId = uberegnetPeriode.id,
                behandlingId = behandlingId1,
                periode = uberegnetPeriode,
                skjæringstidspunkt = 2 jan 2018,
            )
        val behandlingRef2 =
            nyBehandling(
                vedtaksperiodeId = uberegnetPeriode.id,
                behandlingId = behandlingId2,
                periode = uberegnetPeriode,
                skjæringstidspunkt = 2 jan 2018,
            )
        val graphQLUberegnetPeriode = opprettUberegnetPeriode(2 jan 2023, 3 jan 2023, uberegnetPeriode.id)
        val graphQLperiodeMedOppgave = opprettBeregnetPeriode(4 jan 2023, 5 jan 2023, periodeMedOppgave.id)
        val snapshotBehandling1 =
            opprettSnapshotGenerasjon(listOf(graphQLUberegnetPeriode, graphQLperiodeMedOppgave), snapshotBehandlingId1)
        val snapshotBehandling2 =
            opprettSnapshotGenerasjon(listOf(graphQLUberegnetPeriode, graphQLperiodeMedOppgave), snapshotBehandlingId2)
        val arbeidsgiver =
            opprettSnapshotArbeidsgiver(ORGANISASJONSNUMMER, listOf(snapshotBehandling1, snapshotBehandling2))
        mockSnapshot(arbeidsgivere = listOf(arbeidsgiver))
        opprettVarseldefinisjon()
        nyttVarsel(vedtaksperiodeId = uberegnetPeriode.id, behandlingRef = behandlingRef1, status = "AKTIV")
        nyttVarsel(vedtaksperiodeId = uberegnetPeriode.id, behandlingRef = behandlingRef2, status = "AKTIV")
        val førsteBehandlingForPeriode = runPersonQuery().plukkUtPeriodeMed(uberegnetPeriode.id, snapshotBehandlingId2)
        val sisteBehandlingForPeriode = runPersonQuery().plukkUtPeriodeMed(uberegnetPeriode.id, snapshotBehandlingId1)
        val forventedeVarsler =
            setOf(PersonQueryTestVarsel(behandlingId1, "EN_KODE"), PersonQueryTestVarsel(behandlingId2, "EN_KODE"))

        assertTrue(førsteBehandlingForPeriode.varsler.isEmpty())
        assertEquals(forventedeVarsler, sisteBehandlingForPeriode.varsler)
    }

    @Test
    fun `inkluderer handlinger`() {
        val personRef = opprettPerson()
        opprettArbeidsgiver()
        val periode1 = 1 jan 2023 til (23 jan 2023)
        val periode2 = 24 jan 2023 til (31 jan 2023)
        val vedtakId = opprettVedtaksperiode(personRef, periode = periode1)
        ferdigstillOppgave(vedtakId)
        opprettVedtaksperiode(personRef, periode = periode2)
        val førstePeriode = periode1.tilBeregnetPeriode().copy(periodetilstand = GraphQLPeriodetilstand.UTBETALT)
        val andrePeriode = periode2.tilBeregnetPeriode()
        val snapshotBehandling = opprettSnapshotGenerasjon(listOf(førstePeriode, andrePeriode))
        val arbeidsgiver = opprettSnapshotArbeidsgiver(ORGANISASJONSNUMMER, listOf(snapshotBehandling))
        mockSnapshot(arbeidsgivere = listOf(arbeidsgiver))

        val body = runPersonQuery()

        val perioder = body["data"]["person"]["arbeidsgivere"].first()["behandlinger"].first()["perioder"]
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
        opprettArbeidsgiver()
        opprettVedtaksperiode(personRef)
        val (id, fom, tom) = PERIODE
        val graphQLperiodeMedOppgave = opprettBeregnetPeriode(fom, tom, id)
        val snapshotBehandling = opprettSnapshotGenerasjon(listOf(graphQLperiodeMedOppgave))
        val arbeidsgiver = opprettSnapshotArbeidsgiver(ORGANISASJONSNUMMER, listOf(snapshotBehandling))
        mockSnapshot(arbeidsgivere = listOf(arbeidsgiver))

        val body = runPersonQuery()

        val periode = body["data"]["person"]["arbeidsgivere"].first()["behandlinger"].first()["perioder"].first()
        assertFalse(periode["handlinger"].isEmpty)
        assertTrue(periode["handlinger"].first { it["type"].textValue() == ApiPeriodehandling.UTBETALE.name }["tillatt"].booleanValue())
    }

    @Test
    fun `periode med hendelse`() {
        val personRef = opprettPerson()
        opprettArbeidsgiver()
        opprettVedtaksperiode(personRef)
        val (id, fom, tom) = PERIODE
        val eksternDokumentId = UUID.randomUUID()
        val graphQLHendelse = opprettSnapshotHendelse(eksternDokumentId)
        val graphQLperiodeMedOppgave = opprettBeregnetPeriode(fom, tom, id, hendelser = listOf(graphQLHendelse))
        val snapshotBehandling = opprettSnapshotGenerasjon(listOf(graphQLperiodeMedOppgave))
        val arbeidsgiver = opprettSnapshotArbeidsgiver(ORGANISASJONSNUMMER, listOf(snapshotBehandling))
        mockSnapshot(arbeidsgivere = listOf(arbeidsgiver))

        val body = runPersonQuery()

        val hendelse =
            body["data"]["person"]["arbeidsgivere"]
                .first()["behandlinger"]
                .first()["perioder"]
                .first()["hendelser"]
                .first()
        assertNotNull(hendelse)
        assertEquals(eksternDokumentId.toString(), hendelse["eksternDokumentId"].textValue())
    }

    @Test
    fun `periode med avslag`() {
        val avslagsbegrunnelse = "En individuell begrunnelse"
        val person = lagPerson()
            .also { sessionFactory.transactionalSessionScope { transaction -> transaction.personRepository.lagre(it) } }
        val organisasjonsnummer = lagOrganisasjonsnummer()
        val arbeidsgiver1 = Arbeidsgiver.Factory.ny(
            id = ArbeidsgiverIdentifikator.Organisasjonsnummer(organisasjonsnummer),
            navnString = lagOrganisasjonsnavn()
        ).also {
            sessionFactory.transactionalSessionScope { transaction ->
                transaction.arbeidsgiverRepository.lagre(
                    it
                )
            }
        }
        val vedtaksperiode =
            lagVedtaksperiode(identitetsnummer = person.id, organisasjonsnummer = organisasjonsnummer)
                .also {
                    sessionFactory.transactionalSessionScope { transaction ->
                        transaction.vedtaksperiodeRepository.lagre(
                            it
                        )
                    }
                }
        val behandling = lagBehandling(vedtaksperiodeId = vedtaksperiode.id)
            .also { sessionFactory.transactionalSessionScope { transaction -> transaction.behandlingRepository.lagre(it) } }
        val oppgave = lagOppgave(behandling.spleisBehandlingId!!, UUID.randomUUID())
        val saksbehandler = lagSaksbehandler().also {
            sessionFactory.transactionalSessionScope { transaction ->
                transaction.saksbehandlerRepository.lagre(it)
            }
        }
        val vedtakBegrunnelse = VedtakBegrunnelse.ny(
            spleisBehandlingId = behandling.spleisBehandlingId!!,
            tekst = avslagsbegrunnelse,
            utfall = Utfall.AVSLAG,
            saksbehandlerOid = saksbehandler.id
        ).also {
            sessionFactory.transactionalSessionScope { transaction ->
                transaction.vedtakBegrunnelseRepository.lagre(it)
            }
        }

        mockSnapshot(
            fødselsnummer = person.id.value,
            arbeidsgivere = listOf(
                opprettSnapshotArbeidsgiver(
                    organisasjonsnummer = organisasjonsnummer,
                    generasjoner = listOf(
                        opprettSnapshotGenerasjon(
                            perioder = listOf(
                                opprettBeregnetPeriode(
                                    fom = behandling.fom,
                                    tom = behandling.tom,
                                    vedtaksperiodeId = vedtaksperiode.id.value,
                                    utbetalingId = behandling.utbetalingId!!.value
                                )
                            ),
                            id = behandling.spleisBehandlingId!!.value
                        )
                    )
                )
            )
        )

        val body = runPersonQuery(person.id)

        val avslag =
            body["data"]["person"]["arbeidsgivere"]
                .first()["behandlinger"]
                .first()["perioder"]
                .first()["avslag"]
                .first()
        assertNotNull(avslag)
        assertEquals(ApiAvslagstype.AVSLAG, enumValueOf<ApiAvslagstype>(avslag["type"].textValue()))
        assertEquals(avslagsbegrunnelse, avslag["begrunnelse"].textValue())
        assertEquals(saksbehandler.ident.value, avslag["saksbehandlerIdent"].textValue())
        assertFalse(avslag["invalidert"].booleanValue())
    }

    @Test
    fun `kanAvvises-flagget inkluderes i GraphQL-svaret`() {
        opprettVedtaksperiode(opprettPerson(), kanAvvises = false)
        val (id, fom, tom) = PERIODE
        val graphQLperiodeMedOppgave = opprettBeregnetPeriode(fom, tom, id)
        val snapshotBehandling = opprettSnapshotGenerasjon(listOf(graphQLperiodeMedOppgave))
        val arbeidsgiver = opprettSnapshotArbeidsgiver(ORGANISASJONSNUMMER, listOf(snapshotBehandling))
        mockSnapshot(arbeidsgivere = listOf(arbeidsgiver))

        val body = runPersonQuery()

        val periode = body["data"]["person"]["arbeidsgivere"].first()["behandlinger"].first()["perioder"].first()
        val forventedeHandlinger =
            setOf(
                ApiHandling(ApiPeriodehandling.UTBETALE, true, null),
                ApiHandling(ApiPeriodehandling.AVVISE, false, "Spleis støtter ikke å avvise perioden"),
            )
        assertFalse(periode["handlinger"].isEmpty)
        assertEquals(objectMapper.convertValue<Set<ObjectNode>>(forventedeHandlinger), periode["handlinger"].toSet())
    }

    @Test
    fun `andre fødsesnummer inkluderes i GraphQL-svaret`() {
        val annetFødselsnummer = lagFødselsnummer()
        opprettPerson(fødselsnummer = annetFødselsnummer, aktørId = AKTØRID)
        opprettVedtaksperiode(opprettPerson(aktørId = AKTØRID))
        val (id, fom, tom) = PERIODE
        val graphQLperiodeMedOppgave = opprettBeregnetPeriode(fom, tom, id)
        val snapshotBehandling = opprettSnapshotGenerasjon(listOf(graphQLperiodeMedOppgave))
        val arbeidsgiver = opprettSnapshotArbeidsgiver(ORGANISASJONSNUMMER, listOf(snapshotBehandling))
        mockSnapshot(arbeidsgivere = listOf(arbeidsgiver))

        val body = runPersonQuery()

        assertContains(
            body["data"]["person"]["andreFodselsnummer"].map { it["fodselsnummer"].asText() },
            annetFødselsnummer,
        )
    }

    private fun runPersonQuery(identitetsnummer: Identitetsnummer = Identitetsnummer.fraString(FØDSELSNUMMER)) =
        runQuery(
            """
            { 
                person(personPseudoId: "${opprettPersonPseudoId(identitetsnummer)}") { 
                    aktorId
                    andreFodselsnummer {
                        fodselsnummer
                        personPseudoId
                    }
                    arbeidsgivere {
                        behandlinger {
                            id
                            perioder {
                                ... on UberegnetPeriode {
                                    vedtaksperiodeId
                                    varsler {
                                        behandlingId
                                        kode
                                    }                      
                                }
                                ... on BeregnetPeriode {
                                    vedtaksperiodeId
                                    handlinger { type, tillatt, begrunnelse }
                                    varsler {
                                        behandlingId
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
        )

    private fun opprettPersonPseudoId(identitetsnummer: Identitetsnummer = Identitetsnummer.fraString(FØDSELSNUMMER)): UUID =
        sessionFactory.transactionalSessionScope {
            it.personPseudoIdDao.nyPersonPseudoId(identitetsnummer)
        }.value

    private fun JsonNode.plukkUtPeriodeMed(vedtaksperiodeId: UUID): PersonQueryTestPeriode {
        val jsonNode = this["data"]["person"]["arbeidsgivere"].first()["behandlinger"].first()["perioder"]
        val perioder = objectMapper.treeToValue<List<PersonQueryTestPeriode>>(jsonNode)
        return perioder.first { periode -> vedtaksperiodeId == periode.vedtaksperiodeId }
    }

    private fun JsonNode.plukkUtPeriodeMed(
        vedtaksperiodeId: UUID,
        behandlingId: UUID,
    ): PersonQueryTestPeriode {
        val jsonNode = this["data"]["person"]["arbeidsgivere"].first()["behandlinger"]
        val behandlinger = objectMapper.treeToValue<List<PersonQueryTestBehandling>>(jsonNode)
        val behandling = behandlinger.first { behandling -> behandlingId == behandling.id }
        return behandling.perioder.first { periode -> vedtaksperiodeId == periode.vedtaksperiodeId }
    }

    private data class PersonQueryTestBehandling(
        val id: UUID,
        val perioder: List<PersonQueryTestPeriode>,
    )

    private data class PersonQueryTestPeriode(
        val vedtaksperiodeId: UUID,
        val varsler: Set<PersonQueryTestVarsel>,
    )

    private data class PersonQueryTestVarsel(
        val behandlingId: UUID,
        val kode: String,
    )

    private fun Periode.tilBeregnetPeriode(): GraphQLBeregnetPeriode = opprettBeregnetPeriode(fom, tom, id)

    private class Logglytter {
        private val appender =
            ListAppender<ILoggingEvent>().apply {
                (LoggerFactory.getLogger("auditLogger") as Logger).addAppender(this)
                start()
            }

        fun assertBleLogget(
            melding: String,
            level: Level,
        ) = assertTrue(appender.list.count { it.message.contains(melding) && it.level == level } == 1) {
            """
                Forventet at loggen skulle inneholde ett $level-innslag som inneholder teksten:
                    $melding
                Dette ble logget:
                    ${appender.list}}
            """.trimIndent()
        }
    }
}
