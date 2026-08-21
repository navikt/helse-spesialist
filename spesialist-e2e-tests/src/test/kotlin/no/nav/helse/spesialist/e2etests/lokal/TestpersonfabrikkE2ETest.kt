package no.nav.helse.spesialist.e2etests.lokal

import kotliquery.sessionOf
import no.nav.helse.spesialist.db.HelseDao.Companion.asSQL
import no.nav.helse.spesialist.domain.Saksbehandler
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagSaksbehandler
import no.nav.helse.spesialist.domain.tilgangskontroll.Brukerrolle
import no.nav.helse.spesialist.domain.tilgangskontroll.Tilgang
import no.nav.helse.spesialist.e2etests.E2ETestApplikasjon
import no.nav.helse.spesialist.e2etests.GraphQL
import no.nav.helse.spesialist.e2etests.REST
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.UUID

class TestpersonfabrikkE2ETest {
    private val testpersonfabrikk = Testpersonfabrikk(E2ETestApplikasjon)

    @Test
    fun `oppretter testperson med oppgave til godkjenning`() {
        val testperson = testpersonfabrikk.opprettTestperson(Testpersonspesifikasjon())

        assertEquals("AvventerSaksbehandler", gjeldendeOppgavestatus(testperson.vedtaksperiodeId))
        assertTrue(testperson in testpersonfabrikk.opprettedeTestpersoner())
    }

    @Test
    fun `testpersonen kan slås opp slik Speil gjør det`() {
        val testperson = testpersonfabrikk.opprettTestperson(Testpersonspesifikasjon())

        val respons =
            REST.post(
                relativeUrl = "api/personer/sok",
                saksbehandler = lagSaksbehandler(),
                tilganger = setOf(Tilgang.Les, Tilgang.Skriv),
                brukerroller = emptySet(),
                request = mapOf("identitetsnummer" to testperson.fødselsnummer),
            )

        assertNotNull(respons?.get("personPseudoId"))
    }

    @Test
    fun `oppretter testperson uten oppgave når saken behandles automatisk`() {
        val testperson =
            testpersonfabrikk.opprettTestperson(
                Testpersonspesifikasjon(varselkoder = emptyList(), kanGodkjennesAutomatisk = true),
            )

        assertNull(gjeldendeOppgavestatus(testperson.vedtaksperiodeId))
    }

    private fun gjeldendeOppgavestatus(vedtaksperiodeId: UUID?): String? =
        sessionOf(E2ETestApplikasjon.dbModule.dataSource, strict = true).use { session ->
            session.run(
                asSQL(
                    """
                    SELECT o.status
                    FROM oppgave o, vedtaksperiode v
                    WHERE o.vedtak_ref = v.id
                    AND v.vedtaksperiode_id = :vedtaksperiode_id
                    """.trimIndent(),
                    "vedtaksperiode_id" to vedtaksperiodeId,
                ).map { it.string("status") }.asSingle,
            )
        }

    @Test
    fun `arbeidsgiver uten vedtaksperioder blir en ghost-arbeidsgiver`() {
        val testperson =
            testpersonfabrikk.opprettTestperson(
                Testpersonspesifikasjon(
                    arbeidsgivere =
                        listOf(
                            Arbeidsgiverspesifikasjon(),
                            Arbeidsgiverspesifikasjon(vedtaksperioder = emptyList()),
                        ),
                ),
            )

        assertEquals(2, testperson.arbeidsgivere.size)
        val ekteArbeidsgiver = testperson.arbeidsgivere.first()
        val ghost = testperson.arbeidsgivere.last()
        assertEquals(1, ekteArbeidsgiver.vedtaksperiodeIder.size)
        assertTrue(ghost.vedtaksperiodeIder.isEmpty())
        assertEquals("AvventerSaksbehandler", gjeldendeOppgavestatus(ekteArbeidsgiver.vedtaksperiodeIder.single()))
    }

    @Test
    fun `to arbeidsgivere med hver sin vedtaksperiode - andre periode venter til første er godkjent`() {
        val testperson =
            testpersonfabrikk.opprettTestperson(
                Testpersonspesifikasjon(
                    arbeidsgivere =
                        listOf(
                            Arbeidsgiverspesifikasjon(),
                            Arbeidsgiverspesifikasjon(),
                        ),
                ),
            )

        assertEquals(2, testperson.arbeidsgivere.size)
        val vedtaksperiodeIder = testperson.arbeidsgivere.flatMap { it.vedtaksperiodeIder }
        assertEquals(2, vedtaksperiodeIder.size)
        val (førstePeriode, andrePeriode) = vedtaksperiodeIder

        // Spesialist tillater ikke mer enn én aktiv oppgave per person samtidig: den andre perioden
        // sendes ikke til godkjenning før den første faktisk er godkjent.
        assertEquals("AvventerSaksbehandler", gjeldendeOppgavestatus(førstePeriode))
        assertNull(gjeldendeOppgavestatus(andrePeriode))

        saksbehandlerFatterVedtak(testperson.fødselsnummer)
        testpersonfabrikk.fortsettMedNestePeriode(testperson.fødselsnummer)

        assertEquals("Ferdigstilt", gjeldendeOppgavestatus(førstePeriode))
        assertEquals("AvventerSaksbehandler", gjeldendeOppgavestatus(andrePeriode))
    }

    @Test
    fun `arbeidsgiver med to vedtaksperioder - andre periode venter til første er godkjent`() {
        val testperson =
            testpersonfabrikk.opprettTestperson(
                Testpersonspesifikasjon(
                    arbeidsgivere =
                        listOf(
                            Arbeidsgiverspesifikasjon(
                                vedtaksperioder =
                                    listOf(
                                        Vedtaksperiodespesifikasjon(),
                                        Vedtaksperiodespesifikasjon(),
                                    ),
                            ),
                        ),
                ),
            )

        assertEquals(1, testperson.arbeidsgivere.size)
        val (førstePeriode, andrePeriode) = testperson.arbeidsgivere.single().vedtaksperiodeIder

        assertEquals("AvventerSaksbehandler", gjeldendeOppgavestatus(førstePeriode))
        assertNull(gjeldendeOppgavestatus(andrePeriode))

        saksbehandlerFatterVedtak(testperson.fødselsnummer)
        testpersonfabrikk.fortsettMedNestePeriode(testperson.fødselsnummer)

        assertEquals("Ferdigstilt", gjeldendeOppgavestatus(førstePeriode))
        assertEquals("AvventerSaksbehandler", gjeldendeOppgavestatus(andrePeriode))
    }

    /**
     * Simulerer at en saksbehandler godkjenner varsler og fatter vedtak på den perioden som for
     * øyeblikket har en aktiv oppgave for personen. Dette gjøres via de samme REST/GraphQL-kallene
     * som en ekte bruker av Speil ville gjort.
     */
    private fun saksbehandlerFatterVedtak(fødselsnummer: String) {
        val saksbehandler: Saksbehandler = lagSaksbehandler()
        val tilganger = setOf(Tilgang.Les, Tilgang.Skriv)
        val brukerroller = emptySet<Brukerrolle>()

        val personPseudoId =
            REST
                .post(
                    relativeUrl = "api/personer/sok",
                    saksbehandler = saksbehandler,
                    tilganger = tilganger,
                    brukerroller = brukerroller,
                    request = mapOf("identitetsnummer" to fødselsnummer),
                )!!["personPseudoId"]
                .asString()

        REST.put(
            relativeUrl = "/api/personer/$personPseudoId/tildeling",
            saksbehandler = saksbehandler,
            tilganger = tilganger,
            brukerroller = brukerroller,
            request = mapOf("navident" to saksbehandler.ident.value),
        )

        val person =
            GraphQL.call(
                operationName = "FetchPerson",
                saksbehandler = saksbehandler,
                tilganger = tilganger,
                brukerroller = brukerroller,
                variables = mapOf("personPseudoId" to personPseudoId),
            )["data"]["person"]

        val perioder =
            person["arbeidsgivere"]
                .flatMap { arbeidsgiver -> arbeidsgiver["behandlinger"] }
                .flatMap { behandling -> behandling["perioder"] }

        perioder
            .flatMap { periode -> periode["varsler"] }
            .forEach { varsel ->
                REST.put(
                    relativeUrl = "api/varsler/${varsel["id"].asString()}/vurdering",
                    saksbehandler = saksbehandler,
                    tilganger = tilganger,
                    brukerroller = brukerroller,
                    request = mapOf("definisjonId" to varsel["definisjonId"].asString()),
                )
            }

        val behandlingId =
            perioder
                .firstOrNull { periode -> periode.get("oppgave")?.isNull == false }
                ?.get("behandlingId")
                ?.asString()
                ?: error("Fant ingen periode med aktiv oppgave for personen")

        REST.post(
            relativeUrl = "api/behandlinger/$behandlingId/vedtak",
            saksbehandler = saksbehandler,
            tilganger = tilganger,
            brukerroller = brukerroller,
            request = mapOf("begrunnelse" to null),
        )
    }
}
