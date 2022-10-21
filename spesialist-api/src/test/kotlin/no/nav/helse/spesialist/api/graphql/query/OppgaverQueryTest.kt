package no.nav.helse.spesialist.api.graphql.query

import java.time.LocalDateTime
import no.nav.helse.spesialist.api.AbstractGraphQLApiTest
import no.nav.helse.spesialist.api.oppgave.Oppgavestatus
import no.nav.helse.spesialist.api.oppgave.Oppgavetype
import no.nav.helse.spesialist.api.person.Adressebeskyttelse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class OppgaverQueryTest : AbstractGraphQLApiTest() {

    @BeforeAll
    fun setup() {
        setupGraphQLServer()
    }

    @Test
    fun `saksbehandlere som ikke er medlemmer av kode7 får ikke kode7-oppgaver`() {
        opprettVedtaksperiode(opprettPerson(adressebeskyttelse = Adressebeskyttelse.Fortrolig), opprettArbeidsgiver())

        val body = runQuery("""{ alleOppgaver { type } }""")
        val antallOppgaver = body["data"]["alleOppgaver"].size()

        assertEquals(0, antallOppgaver)
    }

    @Test
    fun `saksbehandlere som er medlemmer av kode7 får kode7-oppgaver`() {
        opprettVedtaksperiode(opprettPerson(adressebeskyttelse = Adressebeskyttelse.Fortrolig), opprettArbeidsgiver())

        val body = runQuery("""{ alleOppgaver { type } }""", kode7Saksbehandlergruppe)
        val antallOppgaver = body["data"]["alleOppgaver"].size()

        assertEquals(1, antallOppgaver)
    }

    @Test
    fun `saksbehandlere som ikke er medlemmer av risk-gruppen får ikke risk-oppgaver`() {
        val vedtakRef = opprettVedtaksperiode(opprettPerson(), opprettArbeidsgiver())
        opprettOppgave(Oppgavestatus.AvventerSaksbehandler, Oppgavetype.SØKNAD, vedtakRef)
        opprettOppgave(Oppgavestatus.AvventerSaksbehandler, Oppgavetype.RISK_QA, vedtakRef)

        val body = runQuery("""{ alleOppgaver { type } }""")
        val oppgaver = body["data"]["alleOppgaver"]

        assertEquals(2, oppgaver.size())
        assertTrue(oppgaver.all { Oppgavetype.RISK_QA.name != it["type"].asText() })
    }

    @Test
    fun `saksbehandlere som er medlemmer av risk-gruppen får risk-oppgaver`() {
        val vedtakRef = opprettVedtaksperiode(opprettPerson(), opprettArbeidsgiver())
        opprettOppgave(Oppgavestatus.AvventerSaksbehandler, Oppgavetype.SØKNAD, vedtakRef)
        opprettOppgave(Oppgavestatus.AvventerSaksbehandler, Oppgavetype.RISK_QA, vedtakRef)

        val body = runQuery("""{ alleOppgaver { type } }""", riskSaksbehandlergruppe)
        val oppgaver = body["data"]["alleOppgaver"]

        assertEquals(3, oppgaver.size())
        assertTrue(oppgaver.any { Oppgavetype.RISK_QA.name == it["type"].asText() })
    }

    @Test
    fun `saksbehandlere som ikke er medlemmer av besluttergruppen får ikke beslutteroppgaver`() {
        val vedtakRef = opprettVedtaksperiode(opprettPerson(), opprettArbeidsgiver())
        opprettOppgave(Oppgavestatus.AvventerSaksbehandler, Oppgavetype.SØKNAD, vedtakRef, erBeslutter = true)

        val body = runQuery("""{ alleOppgaver { erBeslutter } }""")
        val oppgaver = body["data"]["alleOppgaver"]

        assertEquals(1, oppgaver.size())
        assertTrue(oppgaver.none { it["erBeslutter"].asBoolean() })
    }

    @Test
    fun `saksbehandlere som er medlemmer av besluttergruppen får beslutteroppgaver`() {
        val vedtakRef = opprettVedtaksperiode(opprettPerson(), opprettArbeidsgiver())
        opprettOppgave(Oppgavestatus.AvventerSaksbehandler, Oppgavetype.SØKNAD, vedtakRef, erBeslutter = true)

        val body = runQuery("""{ alleOppgaver { erBeslutter } }""", beslutterGruppeId)
        val oppgaver = body["data"]["alleOppgaver"]

        assertEquals(2, oppgaver.size())
        assertTrue(oppgaver.any { it["erBeslutter"].asBoolean() })
    }

    @Test
    fun `henter behandlede oppgaver`() {
        opprettSaksbehandler()
        val vedtakRef = opprettVedtaksperiode(opprettPerson(), opprettArbeidsgiver())
        val oppgaveRef = opprettOppgave(vedtakRef = vedtakRef)
        tildelOppgave(oppgaveRef, SAKSBEHANDLER.oid)
        ferdigstillOppgave(vedtakRef)

        val body = runQuery(
            """
            {
                behandledeOppgaver(
                    behandletAvOid: "${SAKSBEHANDLER.oid}", 
                    behandletAvIdent: "${SAKSBEHANDLER.ident}", 
                    fom: "${LocalDateTime.now().minusDays(1)}"
                ) {
                    ferdigstiltAv
                }
            }
        """
        )

        assertEquals("Jan Banan", body["data"]["behandledeOppgaver"].first()["ferdigstiltAv"].asText())
    }

}
