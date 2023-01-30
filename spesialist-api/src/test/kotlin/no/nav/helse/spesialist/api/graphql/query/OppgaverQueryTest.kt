package no.nav.helse.spesialist.api.graphql.query

import java.time.LocalDateTime
import no.nav.helse.spesialist.api.AbstractGraphQLApiTest
import no.nav.helse.spesialist.api.oppgave.Oppgavestatus
import no.nav.helse.spesialist.api.oppgave.Oppgavetype
import no.nav.helse.spesialist.api.person.Adressebeskyttelse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class OppgaverQueryTest : AbstractGraphQLApiTest() {

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

    @Test
    fun `henter paginerte oppgaver`() {
        val personId = opprettPerson()
        val arbeidsgiverId = opprettArbeidsgiver()
        val vedtakRef = opprettVedtaksperiode(personId, arbeidsgiverId)
        opprettOppgave(vedtakRef = vedtakRef)

        val body = runQuery(
            """
            {
                oppgaver(antall: 20, side: 1) {
                    oppgaver {
                        id
                    }
                    paginering {
                        side
                        antallSider
                        elementerPerSide
                    }
                }
            }
        """
        )

        val oppgaver = body["data"]["oppgaver"]["oppgaver"]
        val paginering = body["data"]["oppgaver"]["paginering"]

        assertEquals(2, oppgaver.size())
        assertEquals(1, paginering["side"].asInt())
        assertEquals(1, paginering["antallSider"].asInt())
        assertEquals(20, paginering["elementerPerSide"].asInt())
    }

    @Test
    fun `henter den første oppgaven`() {
        val personId = opprettPerson()
        val arbeidsgiverId = opprettArbeidsgiver()
        val vedtakRef = opprettVedtaksperiode(personId, arbeidsgiverId, oppgavetype = Oppgavetype.SØKNAD)
        opprettOppgave(oppgavetype = Oppgavetype.REVURDERING, vedtakRef = vedtakRef)

        val body = runQuery(
            """
            {
                oppgaver(antall: 1, side: 1) {
                    oppgaver {
                        type
                    }
                    paginering {
                        side
                        antallSider
                        elementerPerSide
                    }
                }
            }
        """
        )

        val oppgaver = body["data"]["oppgaver"]["oppgaver"]
        val paginering = body["data"]["oppgaver"]["paginering"]

        assertEquals(1, oppgaver.size())
        assertEquals(1, paginering["side"].asInt())
        assertEquals(2, paginering["antallSider"].asInt())
        assertEquals(1, paginering["elementerPerSide"].asInt())
        assertEquals("SOKNAD", oppgaver.first()["type"].asText())
    }

    @Test
    fun `henter den andre oppgaven`() {
        val personId = opprettPerson()
        val arbeidsgiverId = opprettArbeidsgiver()
        val vedtakRef = opprettVedtaksperiode(personId, arbeidsgiverId, oppgavetype = Oppgavetype.SØKNAD)
        opprettOppgave(oppgavetype = Oppgavetype.REVURDERING, vedtakRef = vedtakRef)

        val body = runQuery(
            """
            {
                oppgaver(antall: 1, side: 2) {
                    oppgaver {
                        type
                    }
                    paginering {
                        side
                        antallSider
                        elementerPerSide
                    }
                }
            }
        """
        )

        val oppgaver = body["data"]["oppgaver"]["oppgaver"]
        val paginering = body["data"]["oppgaver"]["paginering"]

        assertEquals(1, oppgaver.size())
        assertEquals(2, paginering["side"].asInt())
        assertEquals(2, paginering["antallSider"].asInt())
        assertEquals(1, paginering["elementerPerSide"].asInt())
        assertEquals("REVURDERING", oppgaver.first()["type"].asText())
    }

    @Test
    fun `henter side 3 av 5`() {
        val personId = opprettPerson()
        val arbeidsgiverId = opprettArbeidsgiver()
        val vedtakRef = opprettVedtaksperiode(personId, arbeidsgiverId, oppgavetype = Oppgavetype.SØKNAD)

        repeat(9) {
            opprettOppgave(vedtakRef = vedtakRef)
        }

        val body = runQuery(
            """
            {
                oppgaver(antall: 2, side: 3) {
                    oppgaver {
                        type
                    }
                    paginering {
                        side
                        antallSider
                        elementerPerSide
                    }
                }
            }
        """
        )

        val oppgaver = body["data"]["oppgaver"]["oppgaver"]
        val paginering = body["data"]["oppgaver"]["paginering"]

        assertEquals(2, oppgaver.size())
        assertEquals(3, paginering["side"].asInt())
        assertEquals(5, paginering["antallSider"].asInt())
        assertEquals(2, paginering["elementerPerSide"].asInt())
    }

    @Test
    fun `får 0 elementer når man henter en side som ikke finnes`() {
        val personId = opprettPerson()
        val arbeidsgiverId = opprettArbeidsgiver()
        val vedtakRef = opprettVedtaksperiode(personId, arbeidsgiverId, oppgavetype = Oppgavetype.SØKNAD)

        repeat(9) {
            opprettOppgave(vedtakRef = vedtakRef)
        }

        val body = runQuery(
            """
            {
                oppgaver(antall: 2, side: 6) {
                    oppgaver {
                        type
                    }
                    paginering {
                        side
                        antallSider
                        elementerPerSide
                    }
                }
            }
        """
        )

        val oppgaver = body["data"]["oppgaver"]["oppgaver"]
        val paginering = body["data"]["oppgaver"]["paginering"]

        assertEquals(0, oppgaver.size())
        assertEquals(6, paginering["side"].asInt())
        assertEquals(5, paginering["antallSider"].asInt())
        assertEquals(2, paginering["elementerPerSide"].asInt())
    }

    @Disabled
    @Test
    fun `sorterer ascending`() {
        val personId = opprettPerson()
        val arbeidsgiverId = opprettArbeidsgiver()
        val vedtakRef = opprettVedtaksperiode(personId, arbeidsgiverId, oppgavetype = Oppgavetype.STIKKPRØVE)

        opprettOppgave(
            oppgavetype = Oppgavetype.REVURDERING,
            vedtakRef = vedtakRef,
            opprettet = LocalDateTime.now().minusDays(1)
        )
        opprettOppgave(
            oppgavetype = Oppgavetype.SØKNAD,
            vedtakRef = vedtakRef,
            opprettet = LocalDateTime.now().minusDays(10)
        )
        opprettOppgave(
            oppgavetype = Oppgavetype.UTBETALING_TIL_SYKMELDT,
            vedtakRef = vedtakRef,
            opprettet = LocalDateTime.now().minusDays(4)
        )
        opprettOppgave(
            oppgavetype = Oppgavetype.DELVIS_REFUSJON,
            vedtakRef = vedtakRef,
            opprettet = LocalDateTime.now().minusDays(7)
        )

        val body = runQuery(
            """
            {
                oppgaver(antall: 2, side: 2, sortering: { opprettet: asc }) {
                    oppgaver {
                        type
                    }
                    paginering {
                        side
                        antallSider
                        elementerPerSide
                    }
                }
            }
        """
        )

        val oppgaver = body["data"]["oppgaver"]["oppgaver"]
        val paginering = body["data"]["oppgaver"]["paginering"]

        assertEquals(2, oppgaver.size())
        assertEquals(2, paginering["side"].asInt())
        assertEquals(2, paginering["antallSider"].asInt())
        assertEquals(2, paginering["elementerPerSide"].asInt())
        assertEquals("REVURDERING", oppgaver[0]["type"].asText())
        assertEquals("UTBETALING_TIL_SYKMELDT", oppgaver[1]["type"].asText())
    }

}
