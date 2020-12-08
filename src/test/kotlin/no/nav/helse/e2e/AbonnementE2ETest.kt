package no.nav.helse.e2e

import AbstractE2ETest
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import no.nav.helse.mediator.AbonnementMediator
import no.nav.helse.mediator.api.AbstractApiTest
import no.nav.helse.mediator.api.AbstractApiTest.Companion.authentication
import no.nav.helse.mediator.api.abonnementApi
import no.nav.helse.mediator.meldinger.Kjønn
import no.nav.helse.modell.abonnement.OpptegnelseDto
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageProblems
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.*

private class AbonnementE2ETest : AbstractE2ETest() {
    private val SAKSBEHANDLER_ID = UUID.randomUUID()

    @Test
    fun `Ved abonnering får du et nytt abonnement`() {
        setupPerson()
        setupArbeidsgiver()

        val respons =
            AbstractApiTest.TestServer { abonnementApi(AbonnementMediator(opptegnelseDao)) }
                .withAuthenticatedServer {
                    it.post<HttpResponse>("/api/abonner/$AKTØR") {
                        contentType(ContentType.Application.Json)
                        accept(ContentType.Application.Json)
                        authentication(SAKSBEHANDLER_ID)
                    }
                }

        assertEquals(HttpStatusCode.OK, respons.status)

        val abonnementer = opptegnelseDao.finnAbonnement(SAKSBEHANDLER_ID)
        assertEquals(1, abonnementer.size)

        testRapid.sendTestMessage(meldingsfabrikk.lagUtbelingEndret())

        val alle = opptegnelseDao.alleOpptegnelser()
        val SISTE_SEKVENSID = 0
        val oppdateringer =
            AbstractApiTest.TestServer { abonnementApi(AbonnementMediator(opptegnelseDao)) }
                .withAuthenticatedServer {
                    it.get<HttpResponse>("/api/oppdatering/${SISTE_SEKVENSID}") {
                        contentType(ContentType.Application.Json)
                        accept(ContentType.Application.Json)
                        authentication(SAKSBEHANDLER_ID)
                    }.call.receive<List<OpptegnelseDto>>()
                }

        // opptegnelseDao.finnOpptegnelser(AKTØR.toLong())
        assertEquals(1, oppdateringer.size)
    }

    @Test
    fun `saksoppdatering blir bare sendt en gang, blir slettet når klient rapporterer at event er sett`() {
    }

    private fun setupPerson() {
        val navnId = personDao.insertPersoninfo(
            "Harald",
            "Mellomnavn",
            "Rex",
            LocalDate.now().minusYears(20),
            Kjønn.Ukjent
        )
        val string = """{ "node": "1234" }"""
        val json = JsonMessage(string, MessageProblems(string))
        json["key"] = "string"
        val enhetId = 1219
        val infoTrygdutbetalingerId = personDao.insertInfotrygdutbetalinger(json["key"])
        personDao.insertPerson(
            UNG_PERSON_FNR_2018,
            AKTØR,
            navnId,
            enhetId,
            infoTrygdutbetalingerId
        )
    }

    private fun setupArbeidsgiver() {
        arbeidsgiverDao.insertArbeidsgiver(
            "123456789",
            "Bedrift AS",
            "BEDRIFTSGREIER OG STÆSJ"
        )
    }
}
