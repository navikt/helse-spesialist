package no.nav.helse.kafka

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import no.nav.helse.spesialist.domain.testfixtures.lagBehandling
import no.nav.helse.spesialist.domain.testfixtures.lagOppgave
import no.nav.helse.spesialist.domain.testfixtures.lagVarsel
import no.nav.helse.spesialist.domain.testfixtures.lagVedtaksperiode
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagPerson
import no.nav.helse.spesialist.kafka.IntegrationTestFixture
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.UUID

internal class MinuttRiverTest {
    private val testRapid = TestRapid()
    private val integrationTestFixture = IntegrationTestFixture(testRapid)
    private val sessionContext = integrationTestFixture.sessionFactory.sessionContext

    @Test
    fun `publiserer gosys_oppgave_endret for oppgaver med aktivt SB_EX_3-varsel`() {
        val person = lagPerson().also(sessionContext.personRepository::lagre)
        val vedtaksperiode =
            lagVedtaksperiode(identitetsnummer = person.id)
                .also(sessionContext.vedtaksperiodeRepository::lagre)
        val behandling =
            lagBehandling(vedtaksperiodeId = vedtaksperiode.id)
                .also(sessionContext.behandlingRepository::lagre)
        lagOppgave(
            behandlingId = behandling.spleisBehandlingId!!,
            godkjenningsbehovId = UUID.randomUUID(),
            vedtaksperiodeId = vedtaksperiode.id,
        ).also(sessionContext.oppgaveRepository::lagre)
        sessionContext.varselRepository.lagre(
            listOf(
                lagVarsel(
                    behandlingUnikId = behandling.id,
                    spleisBehandlingId = behandling.spleisBehandlingId!!,
                    kode = "SB_EX_3",
                ),
            ),
        )

        testRapid.sendTestMessage(minutt())

        val publiserteMeldinger =
            (0 until testRapid.inspektør.size)
                .map { testRapid.inspektør.message(it) }
                .filter { it["@event_name"].asString() == "gosys_oppgave_endret" }
        assertEquals(1, publiserteMeldinger.size)
        assertEquals(person.id.value, publiserteMeldinger.single()["fødselsnummer"].asString())
    }

    @Test
    fun `publiserer ingen meldinger når ingen oppgaver har aktivt SB_EX_3-varsel`() {
        val person = lagPerson().also(sessionContext.personRepository::lagre)
        val vedtaksperiode =
            lagVedtaksperiode(identitetsnummer = person.id)
                .also(sessionContext.vedtaksperiodeRepository::lagre)
        val behandling =
            lagBehandling(vedtaksperiodeId = vedtaksperiode.id)
                .also(sessionContext.behandlingRepository::lagre)
        lagOppgave(
            behandlingId = behandling.spleisBehandlingId!!,
            godkjenningsbehovId = UUID.randomUUID(),
            vedtaksperiodeId = vedtaksperiode.id,
        ).also(sessionContext.oppgaveRepository::lagre)

        testRapid.sendTestMessage(minutt())

        val gosysOppgaveEndretMeldinger =
            (0 until testRapid.inspektør.size)
                .map { testRapid.inspektør.message(it) }
                .filter { it["@event_name"].asString() == "gosys_oppgave_endret" }
        assertEquals(0, gosysOppgaveEndretMeldinger.size)
    }

    @Language("JSON")
    private fun minutt() =
        """
        {
          "@event_name": "minutt",
          "@id": "${UUID.randomUUID()}"
        }
        """.trimIndent()
}
