package no.nav.helse.spesialist.api.rest.andreYtelser

import io.ktor.http.*
import no.nav.helse.mediator.asUUID
import no.nav.helse.modell.melding.GraderteAndreYtelserEndringerEvent
import no.nav.helse.modell.vedtaksperiode.Yrkesaktivitetstype
import no.nav.helse.spesialist.api.IntegrationTestFixture
import no.nav.helse.spesialist.application.InMemoryMeldingPubliserer
import no.nav.helse.spesialist.domain.Person
import no.nav.helse.spesialist.domain.Saksbehandler
import no.nav.helse.spesialist.domain.SpleisBehandlingId
import no.nav.helse.spesialist.domain.andreytelser.GraderteAndreYtelserDag
import no.nav.helse.spesialist.domain.andreytelser.GraderteAndreYtelserId
import no.nav.helse.spesialist.domain.andreytelser.GraderteAndreYtelserType
import no.nav.helse.spesialist.domain.saksbehandlerhandling.HandlingType
import no.nav.helse.spesialist.domain.testfixtures.sep
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagPerson
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagSaksbehandler
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PostGraderteAndreYtelserIntegrationTest {
    private val integrationTestFixture = IntegrationTestFixture()
    private val sessionContext = integrationTestFixture.sessionFactory.sessionContext

    @Test
    fun `første post oppretter streamen og sender endringsevent`() {
        val person = lagPerson().also(sessionContext.personRepository::lagre)
        val saksbehandler = lagSaksbehandler().also(sessionContext.saksbehandlerRepository::lagre)

        val behandlingId = UUID.randomUUID()

        val response =
            postGraderteAndreYtelser(
                person = person,
                saksbehandler = saksbehandler,
                dagerJson = dagJson(dato = "2024-09-23", andreYtelserGrader = """{ "SVANGERSKAPSPENGER": 60 }""", sykdomsgrad = 40, arbeidsgrad = 0),
                yrkesaktivitet = "ARBEIDSTAKER",
                organisasjonsnummer = "123456789",
                behandlingId = behandlingId,
            )

        assertEquals(HttpStatusCode.OK.value, response.status)
        val lagret = sessionContext.graderteAndreYtelserRepository.finnOrNull(lagId(person, Yrkesaktivitetstype.ARBEIDSTAKER, "123456789"))
        assertNotNull(lagret)
        assertEquals(lagret.versjon, response.bodyAsJsonNode?.get("versjon")?.asUUID())
        assertEquals(
            listOf(
                GraderteAndreYtelserDag(
                    dato = 23 sep 2024,
                    andreYtelserGrader = mapOf(GraderteAndreYtelserType.SVANGERSKAPSPENGER to 60),
                    sykdomsgrad = 40,
                    arbeidsgrad = 0,
                ),
            ),
            lagret.dager,
        )
        val handling = sessionContext.saksbehandlerhandlingRepository.finnAlle(person.id).single()
        assertEquals(lagret.versjon, handling.handlingId)
        assertEquals(HandlingType.GRADERTE_ANDRE_YTELSER_ENDRET, handling.handlingType)
        assertEquals(saksbehandler.ident.value, handling.saksbehandlerIdent)
        assertEquals(SpleisBehandlingId(behandlingId), handling.behandlingId)
        assertNull(handling.skjæringstidspunkt)
        assertEquals(
            sessionContext.totrinnsvurderingRepository.finnAktivForPersonOrNull(person.id.value)?.id(),
            handling.totrinnsvurderingId,
        )
        integrationTestFixture.assertPubliserteUtgåendeHendelser(
            InMemoryMeldingPubliserer.PublisertUtgåendeHendelse(
                fødselsnummer = person.id.value,
                hendelse = GraderteAndreYtelserEndringerEvent(fødselsnummer = person.id, fom = 23 sep 2024),
                årsak = "graderte andre ytelser endret",
            ),
        )
    }

    @Test
    fun `andre post endrer bare dagene den inneholder og gir ny versjon`() {
        val person = lagPerson().also(sessionContext.personRepository::lagre)
        val saksbehandler = lagSaksbehandler().also(sessionContext.saksbehandlerRepository::lagre)
        val første =
            postGraderteAndreYtelser(
                person = person,
                saksbehandler = saksbehandler,
                dagerJson =
                    listOf(
                        dagJson(dato = "2024-09-23", andreYtelserGrader = """{ "SVANGERSKAPSPENGER": 50 }""", sykdomsgrad = 50, arbeidsgrad = 0),
                        dagJson(dato = "2024-09-24", andreYtelserGrader = """{ "SVANGERSKAPSPENGER": 50 }""", sykdomsgrad = 50, arbeidsgrad = 0),
                    ).joinToString(","),
            )

        val andre =
            postGraderteAndreYtelser(
                person = person,
                saksbehandler = saksbehandler,
                versjon = første.bodyAsJsonNode?.get("versjon")?.asUUID(),
                dagerJson = dagJson(dato = "2024-09-24", andreYtelserGrader = """{ "SVANGERSKAPSPENGER": 30 }""", sykdomsgrad = 70, arbeidsgrad = 0),
            )

        assertEquals(HttpStatusCode.OK.value, andre.status)
        val førsteVersjon = første.bodyAsJsonNode?.get("versjon")?.asUUID()
        val andreVersjon = andre.bodyAsJsonNode?.get("versjon")?.asUUID()
        assertNotEquals(førsteVersjon, andreVersjon)

        val lagret = sessionContext.graderteAndreYtelserRepository.finnOrNull(lagId(person, Yrkesaktivitetstype.FRILANS, null))
        assertNotNull(lagret)
        assertEquals(andreVersjon, lagret.versjon)
        assertEquals(2, lagret.events.size)
        assertEquals(
            listOf(
                GraderteAndreYtelserDag(23 sep 2024, mapOf(GraderteAndreYtelserType.SVANGERSKAPSPENGER to 50), 50, 0),
                GraderteAndreYtelserDag(24 sep 2024, mapOf(GraderteAndreYtelserType.SVANGERSKAPSPENGER to 30), 70, 0),
            ),
            lagret.dager,
        )
        val handlinger = sessionContext.saksbehandlerhandlingRepository.finnAlle(person.id)
        assertEquals(setOf(førsteVersjon, andreVersjon), handlinger.map { it.handlingId }.toSet())
        assertEquals(1, handlinger.map { it.totrinnsvurderingId }.distinct().size)
        integrationTestFixture.assertPubliserteUtgåendeHendelser(
            { assertEquals(GraderteAndreYtelserEndringerEvent(person.id, 23 sep 2024), it.hendelse) },
            { assertEquals(GraderteAndreYtelserEndringerEvent(person.id, 24 sep 2024), it.hendelse) },
        )
    }

    @Test
    fun `post uten faktiske endringer lagrer ingenting og returnerer eksisterende versjon`() {
        val person = lagPerson().also(sessionContext.personRepository::lagre)
        val saksbehandler = lagSaksbehandler().also(sessionContext.saksbehandlerRepository::lagre)
        val dag = dagJson(dato = "2024-09-23", andreYtelserGrader = """{ "SVANGERSKAPSPENGER": 50 }""", sykdomsgrad = 50, arbeidsgrad = 0)
        val første = postGraderteAndreYtelser(person = person, saksbehandler = saksbehandler, dagerJson = dag)

        val andre =
            postGraderteAndreYtelser(
                person = person,
                saksbehandler = saksbehandler,
                versjon = første.bodyAsJsonNode?.get("versjon")?.asUUID(),
                dagerJson = dag,
            )

        assertEquals(HttpStatusCode.OK.value, andre.status)
        assertEquals(første.bodyAsJsonNode?.get("versjon")?.asUUID(), andre.bodyAsJsonNode?.get("versjon")?.asUUID())
        val lagret = sessionContext.graderteAndreYtelserRepository.finnOrNull(lagId(person, Yrkesaktivitetstype.FRILANS, null))
        assertEquals(1, lagret?.events?.size)
        assertEquals(1, sessionContext.saksbehandlerhandlingRepository.finnAlle(person.id).size)
        integrationTestFixture.assertPubliserteUtgåendeHendelser(
            { assertEquals(GraderteAndreYtelserEndringerEvent(person.id, 23 sep 2024), it.hendelse) },
        )
    }

    @Test
    fun `avviser endring med utdatert versjon`() {
        val person = lagPerson().also(sessionContext.personRepository::lagre)
        val saksbehandler = lagSaksbehandler().also(sessionContext.saksbehandlerRepository::lagre)
        val første =
            postGraderteAndreYtelser(
                person = person,
                saksbehandler = saksbehandler,
                dagerJson = dagJson(dato = "2024-09-23", andreYtelserGrader = """{ "SVANGERSKAPSPENGER": 50 }""", sykdomsgrad = 50, arbeidsgrad = 0),
            )
        val førsteVersjon = første.bodyAsJsonNode?.get("versjon")?.asUUID()
        postGraderteAndreYtelser(
            person = person,
            saksbehandler = saksbehandler,
            versjon = førsteVersjon,
            dagerJson = dagJson(dato = "2024-09-24", andreYtelserGrader = """{ "SVANGERSKAPSPENGER": 50 }""", sykdomsgrad = 50, arbeidsgrad = 0),
        )

        val response =
            postGraderteAndreYtelser(
                person = person,
                saksbehandler = saksbehandler,
                versjon = førsteVersjon,
                dagerJson = dagJson(dato = "2024-09-25", andreYtelserGrader = """{ "SVANGERSKAPSPENGER": 50 }""", sykdomsgrad = 50, arbeidsgrad = 0),
            )

        assertUtdatertTilstand(response, person, forventetAntallEvents = 2)
    }

    @Test
    fun `avviser endring uten versjon når graderte andre ytelser finnes`() {
        val person = lagPerson().also(sessionContext.personRepository::lagre)
        val saksbehandler = lagSaksbehandler().also(sessionContext.saksbehandlerRepository::lagre)
        postGraderteAndreYtelser(
            person = person,
            saksbehandler = saksbehandler,
            dagerJson = dagJson(dato = "2024-09-23", andreYtelserGrader = """{ "SVANGERSKAPSPENGER": 50 }""", sykdomsgrad = 50, arbeidsgrad = 0),
        )

        val response =
            postGraderteAndreYtelser(
                person = person,
                saksbehandler = saksbehandler,
                versjon = null,
                dagerJson = dagJson(dato = "2024-09-24", andreYtelserGrader = """{ "SVANGERSKAPSPENGER": 50 }""", sykdomsgrad = 50, arbeidsgrad = 0),
            )

        assertUtdatertTilstand(response, person, forventetAntallEvents = 1)
    }

    @Test
    fun `avviser versjon når graderte andre ytelser ikke finnes`() {
        val person = lagPerson().also(sessionContext.personRepository::lagre)
        val saksbehandler = lagSaksbehandler().also(sessionContext.saksbehandlerRepository::lagre)

        val response =
            postGraderteAndreYtelser(
                person = person,
                saksbehandler = saksbehandler,
                versjon = UUID.randomUUID(),
                dagerJson = dagJson(dato = "2024-09-23", andreYtelserGrader = """{ "SVANGERSKAPSPENGER": 50 }""", sykdomsgrad = 50, arbeidsgrad = 0),
            )

        assertFeil(response, person, "UTDATERT_TILSTAND", HttpStatusCode.Conflict)
    }

    @Test
    fun `ulike organisasjonsnumre gir hver sin stream`() {
        val person = lagPerson().also(sessionContext.personRepository::lagre)
        val saksbehandler = lagSaksbehandler().also(sessionContext.saksbehandlerRepository::lagre)
        val dag = dagJson(dato = "2024-09-23", andreYtelserGrader = """{ "SVANGERSKAPSPENGER": 50 }""", sykdomsgrad = 50, arbeidsgrad = 0)

        postGraderteAndreYtelser(person, saksbehandler, dag, yrkesaktivitet = "ARBEIDSTAKER", organisasjonsnummer = "111111111")
        postGraderteAndreYtelser(person, saksbehandler, dag, yrkesaktivitet = "ARBEIDSTAKER", organisasjonsnummer = "222222222")

        assertEquals(
            setOf(
                lagId(person, Yrkesaktivitetstype.ARBEIDSTAKER, "111111111"),
                lagId(person, Yrkesaktivitetstype.ARBEIDSTAKER, "222222222"),
            ),
            sessionContext.graderteAndreYtelserRepository
                .finnAlleForIdentitetsnummer(person.id)
                .map { it.id }
                .toSet(),
        )
    }

    @Test
    fun `avviser dag der total grad overstiger 100`() {
        val person = lagPerson().also(sessionContext.personRepository::lagre)
        val saksbehandler = lagSaksbehandler().also(sessionContext.saksbehandlerRepository::lagre)

        val response =
            postGraderteAndreYtelser(
                person = person,
                saksbehandler = saksbehandler,
                dagerJson = dagJson(dato = "2024-09-23", andreYtelserGrader = """{ "SVANGERSKAPSPENGER": 60, "OMSORGSPENGER": 20 }""", sykdomsgrad = 30, arbeidsgrad = 0),
            )

        assertFeil(response, person, "TOTAL_GRAD_OVER_100")
    }

    @Test
    fun `godtar dag der total grad er nøyaktig 100`() {
        val person = lagPerson().also(sessionContext.personRepository::lagre)
        val saksbehandler = lagSaksbehandler().also(sessionContext.saksbehandlerRepository::lagre)

        val response =
            postGraderteAndreYtelser(
                person = person,
                saksbehandler = saksbehandler,
                dagerJson = dagJson(dato = "2024-09-23", andreYtelserGrader = """{ "SVANGERSKAPSPENGER": 50, "OMSORGSPENGER": 20 }""", sykdomsgrad = 20, arbeidsgrad = 10),
            )

        assertEquals(HttpStatusCode.OK.value, response.status)
        assertEquals(1, sessionContext.graderteAndreYtelserRepository.finnAlleForIdentitetsnummer(person.id).size)
    }

    @Test
    fun `avviser forespørsel uten dager`() {
        val person = lagPerson().also(sessionContext.personRepository::lagre)
        val saksbehandler = lagSaksbehandler().also(sessionContext.saksbehandlerRepository::lagre)

        val response = postGraderteAndreYtelser(person = person, saksbehandler = saksbehandler, dagerJson = "")

        assertFeil(response, person, "MANGLER_ENDRINGER")
    }

    @Test
    fun `avviser dag med negativ grad`() {
        val person = lagPerson().also(sessionContext.personRepository::lagre)
        val saksbehandler = lagSaksbehandler().also(sessionContext.saksbehandlerRepository::lagre)

        val response =
            postGraderteAndreYtelser(
                person = person,
                saksbehandler = saksbehandler,
                dagerJson = dagJson(dato = "2024-09-23", andreYtelserGrader = """{ "SVANGERSKAPSPENGER": 80 }""", sykdomsgrad = 50, arbeidsgrad = -30),
            )

        assertFeil(response, person, "NEGATIV_GRAD")
    }

    private fun lagId(
        person: Person,
        yrkesaktivitet: Yrkesaktivitetstype,
        organisasjonsnummer: String?,
    ) = GraderteAndreYtelserId(person.id, yrkesaktivitet, organisasjonsnummer)

    private fun postGraderteAndreYtelser(
        person: Person,
        saksbehandler: Saksbehandler,
        dagerJson: String,
        versjon: UUID? = null,
        yrkesaktivitet: String = "FRILANS",
        organisasjonsnummer: String? = null,
        behandlingId: UUID = UUID.randomUUID(),
    ): IntegrationTestFixture.Response {
        val personPseudoId = integrationTestFixture.personPseudoIdProvider.nyPersonPseudoId(person.id)
        return integrationTestFixture.post(
            url = "/api/personer/${personPseudoId.value}/graderte-andre-ytelser",
            saksbehandler = saksbehandler,
            body =
                """
                {
                  "yrkesaktivitet": "$yrkesaktivitet",
                  "organisasjonsnummer": ${organisasjonsnummer?.let { "\"$it\"" }},
                  "versjon": ${versjon?.let { "\"$it\"" }},
                  "behandlingId": "$behandlingId",
                  "dager": [$dagerJson],
                  "notatTilBeslutter": "notat om en gradert annen ytelse"
                }
                """.trimIndent(),
        )
    }

    private fun dagJson(
        dato: String,
        andreYtelserGrader: String,
        sykdomsgrad: Int,
        arbeidsgrad: Int,
    ) = """
        {
          "dato": "$dato",
          "andreYtelserGrader": $andreYtelserGrader,
          "sykdomsgrad": $sykdomsgrad,
          "arbeidsgrad": $arbeidsgrad
        }
        """.trimIndent()

    private fun assertUtdatertTilstand(
        response: IntegrationTestFixture.Response,
        person: Person,
        forventetAntallEvents: Int,
    ) {
        assertEquals(HttpStatusCode.Conflict.value, response.status)
        assertEquals("UTDATERT_TILSTAND", response.bodyAsJsonNode?.get("code")?.asString())
        val lagret = sessionContext.graderteAndreYtelserRepository.finnOrNull(lagId(person, Yrkesaktivitetstype.FRILANS, null))
        assertEquals(forventetAntallEvents, lagret?.events?.size)
        assertEquals(forventetAntallEvents, sessionContext.saksbehandlerhandlingRepository.finnAlle(person.id).size)
        assertEquals(forventetAntallEvents, integrationTestFixture.meldingPubliserer.publiserteUtgåendeHendelser.size)
    }

    private fun assertFeil(
        response: IntegrationTestFixture.Response,
        person: Person,
        forventetKode: String,
        forventetStatus: HttpStatusCode = HttpStatusCode.BadRequest,
    ) {
        assertEquals(forventetStatus.value, response.status)
        assertEquals(forventetKode, response.bodyAsJsonNode?.get("code")?.asString())
        assertTrue(sessionContext.graderteAndreYtelserRepository.finnAlleForIdentitetsnummer(person.id).isEmpty())
        assertTrue(sessionContext.saksbehandlerhandlingRepository.finnAlle(person.id).isEmpty())
        integrationTestFixture.assertIngenPubliserteUtgåendeHendelser()
    }
}
