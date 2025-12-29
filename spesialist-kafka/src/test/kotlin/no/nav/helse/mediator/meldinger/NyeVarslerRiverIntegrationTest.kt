package no.nav.helse.mediator.meldinger

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import no.nav.helse.spesialist.domain.Person
import no.nav.helse.spesialist.domain.Varsel
import no.nav.helse.spesialist.domain.Vedtaksperiode
import no.nav.helse.spesialist.domain.testfixtures.lagBehandling
import no.nav.helse.spesialist.domain.testfixtures.lagVedtaksperiode
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagPerson
import no.nav.helse.spesialist.kafka.IntegrationTestFixture
import no.nav.helse.spesialist.kafka.testfixtures.Testmeldingfabrikk
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

internal class NyeVarslerRiverIntegrationTest {
    private val testRapid = TestRapid()
    private val integrationTestFixture = IntegrationTestFixture(testRapid)
    private val sessionContext = integrationTestFixture.sessionFactory.sessionContext

    @Test
    fun `lagrer varsel ved nye_varsler`() {
        // given
        val person =
            lagPerson()
                .also(sessionContext.personRepository::lagre)
        val vedtaksperiode1 =
            lagVedtaksperiode()
                .also(sessionContext.vedtaksperiodeRepository::lagre)
        val vedtaksperiode2 =
            lagVedtaksperiode()
                .also(sessionContext.vedtaksperiodeRepository::lagre)
        val behandling1 =
            lagBehandling(vedtaksperiodeId = vedtaksperiode1.id)
                .also(sessionContext.behandlingRepository::lagre)
        val behandling2 =
            lagBehandling(vedtaksperiodeId = vedtaksperiode2.id)
                .also(sessionContext.behandlingRepository::lagre)

        // when
        testRapid.sendTestMessage(lagNyeVarslerMelding(person, vedtaksperiode1 to "RV_IV_1", vedtaksperiode2 to "RV_IV_2"))

        // then
        val varslerForBehandling1 = sessionContext.varselRepository.finnVarslerFor(behandling1.id)
        val varslerForBehandling2 = sessionContext.varselRepository.finnVarslerFor(behandling2.id)
        assertEquals(1, varslerForBehandling1.size)
        assertEquals(1, varslerForBehandling2.size)

        assertEquals("RV_IV_1", varslerForBehandling1.single().kode)
        assertEquals("RV_IV_2", varslerForBehandling2.single().kode)
    }

    @Test
    fun `lagrer varsel ved aktivitetslogg_ny_aktivitet`() {
        // given
        val person =
            lagPerson()
                .also(sessionContext.personRepository::lagre)
        val vedtaksperiode =
            lagVedtaksperiode()
                .also(sessionContext.vedtaksperiodeRepository::lagre)
        val behandling =
            lagBehandling(vedtaksperiodeId = vedtaksperiode.id)
                .also(sessionContext.behandlingRepository::lagre)

        // when
        testRapid.sendTestMessage(lagAktivitetsloggNyAktivitetMelding(person, vedtaksperiode to "RV_IV_1"))

        // then
        val varsler = sessionContext.varselRepository.finnVarslerFor(behandling.id)
        assertEquals(1, varsler.size)

        assertEquals("RV_IV_1", varsler.single().kode)
    }

    @Test
    fun `ignorerer varsler for vedtaksperioder vi ikke kjenner til`() {
        val person =
            lagPerson()
                .also(sessionContext.personRepository::lagre)
        // given
        val ikkePersistertVedtaksperiode = lagVedtaksperiode()

        // then
        assertDoesNotThrow {
            // when
            testRapid.sendTestMessage(lagAktivitetsloggNyAktivitetMelding(person, ikkePersistertVedtaksperiode to "RV_IV_1"))
        }
    }

    @Test
    fun `lagrer nye varsler og erstattet eksisterende`() {
        // given
        val person =
            lagPerson()
                .also(sessionContext.personRepository::lagre)
        val vedtaksperiode1 =
            lagVedtaksperiode()
                .also(sessionContext.vedtaksperiodeRepository::lagre)
        val vedtaksperiode2 =
            lagVedtaksperiode()
                .also(sessionContext.vedtaksperiodeRepository::lagre)
        val behandling1 =
            lagBehandling(vedtaksperiodeId = vedtaksperiode1.id)
                .also(sessionContext.behandlingRepository::lagre)
        val behandling2 =
            lagBehandling(vedtaksperiodeId = vedtaksperiode2.id)
                .also(sessionContext.behandlingRepository::lagre)

        testRapid.sendTestMessage(lagNyeVarslerMelding(person, vedtaksperiode1 to "RV_IV_1", vedtaksperiode2 to "RV_IV_2"))
        val varsel1 = sessionContext.varselRepository.finnVarslerFor(behandling1.id).single()
        val varsel2 = sessionContext.varselRepository.finnVarslerFor(behandling2.id).single()
        varsel1.deaktiver()
        sessionContext.varselRepository.lagre(varsel1)

        // when
        testRapid.sendTestMessage(lagNyeVarslerMelding(person, vedtaksperiode1 to "RV_IV_1", vedtaksperiode2 to "RV_IV_2", vedtaksperiode2 to "RV_IV_3"))

        // then
        val varslerForBehandling1 = sessionContext.varselRepository.finnVarslerFor(behandling1.id)
        val varslerForBehandling2 = sessionContext.varselRepository.finnVarslerFor(behandling2.id).sortedBy { it.opprettetTidspunkt }
        assertEquals(1, varslerForBehandling1.size)
        assertEquals(2, varslerForBehandling2.size)

        val funnetVarsel1 = varslerForBehandling1[0]
        val funnetVarsel2 = varslerForBehandling2[0]
        assertEquals("RV_IV_1", funnetVarsel1.kode)
        assertEquals(Varsel.Status.AKTIV, funnetVarsel1.status)
        assertEquals("RV_IV_2", funnetVarsel2.kode)
        assertNotEquals(varsel2.id, funnetVarsel2.id)
        assertEquals("RV_IV_3", varslerForBehandling2[1].kode)
    }

    @Test
    fun `erstatter varsel om avvik hvis det finnes fra før av`() {
        // given
        val person =
            lagPerson()
                .also(sessionContext.personRepository::lagre)
        val vedtaksperiode1 =
            lagVedtaksperiode()
                .also(sessionContext.vedtaksperiodeRepository::lagre)
        val behandling1 =
            lagBehandling(vedtaksperiodeId = vedtaksperiode1.id)
                .also(sessionContext.behandlingRepository::lagre)
        testRapid.sendTestMessage(lagNyeVarslerMelding(person, vedtaksperiode1 to "RV_IV_2"))
        val eksisterendeVarsel = sessionContext.varselRepository.finnVarslerFor(behandling1.id).single()

        // when
        testRapid.sendTestMessage(lagNyeVarslerMelding(person, vedtaksperiode1 to "RV_IV_2"))

        // then
        val varslerForBehandling1 = sessionContext.varselRepository.finnVarslerFor(behandling1.id)
        val varselet = varslerForBehandling1.single()
        assertEquals(1, varslerForBehandling1.size)

        assertNotEquals(eksisterendeVarsel.id, varselet.id)
        assertEquals(eksisterendeVarsel.spleisBehandlingId, varselet.spleisBehandlingId)
        assertEquals(eksisterendeVarsel.behandlingUnikId, varselet.behandlingUnikId)
        assertEquals("RV_IV_2", varselet.kode)
    }

    @Test
    fun `reaktiverer inaktivt varsel når det dukker opp på nytt`() {
        // given
        val person =
            lagPerson()
                .also(sessionContext.personRepository::lagre)
        val vedtaksperiode1 =
            lagVedtaksperiode()
                .also(sessionContext.vedtaksperiodeRepository::lagre)
        val behandling1 =
            lagBehandling(vedtaksperiodeId = vedtaksperiode1.id)
                .also(sessionContext.behandlingRepository::lagre)
        testRapid.sendTestMessage(lagNyeVarslerMelding(person, vedtaksperiode1 to "RV_IV_1"))
        val eksisterendeVarsel = sessionContext.varselRepository.finnVarslerFor(behandling1.id).single()
        eksisterendeVarsel.deaktiver()
        sessionContext.varselRepository.lagre(eksisterendeVarsel)

        // when
        testRapid.sendTestMessage(lagNyeVarslerMelding(person, vedtaksperiode1 to "RV_IV_1"))

        // then
        val varslerForBehandling1 = sessionContext.varselRepository.finnVarslerFor(behandling1.id)
        val varselet = varslerForBehandling1.single()
        assertEquals(1, varslerForBehandling1.size)

        assertEquals(eksisterendeVarsel.id, varselet.id)
        assertNotEquals(eksisterendeVarsel.status, varselet.status)
        assertEquals(eksisterendeVarsel.spleisBehandlingId, varselet.spleisBehandlingId)
        assertEquals(eksisterendeVarsel.behandlingUnikId, varselet.behandlingUnikId)
        assertEquals("RV_IV_1", varselet.kode)
        assertEquals(Varsel.Status.AKTIV, varselet.status)
    }

    private fun lagNyeVarslerMelding(
        person: Person,
        vararg varselkoder: Pair<Vedtaksperiode, String>,
    ) = Testmeldingfabrikk.lagNyeVarsler(
        fødselsnummer = person.id.value,
        varselkoder = varselkoder.groupBy({ it.first }) { it.second },
    )

    private fun lagAktivitetsloggNyAktivitetMelding(
        person: Person,
        vararg varselkoder: Pair<Vedtaksperiode, String>,
    ) = Testmeldingfabrikk.lagAktivitetsloggNyAktivitet(
        fødselsnummer = person.id.value,
        varselkoder = varselkoder.groupBy({ it.first }) { it.second },
    )
}
