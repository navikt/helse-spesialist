package no.nav.helse.spesialist.e2etests.lokal

import kotliquery.sessionOf
import no.nav.helse.spesialist.db.HelseDao.Companion.asSQL
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagSaksbehandler
import no.nav.helse.spesialist.domain.tilgangskontroll.Tilgang
import no.nav.helse.spesialist.e2etests.E2ETestApplikasjon
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

    private fun gjeldendeOppgavestatus(vedtaksperiodeId: UUID): String? =
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
}
