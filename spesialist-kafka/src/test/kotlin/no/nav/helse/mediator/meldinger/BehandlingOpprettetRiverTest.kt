package no.nav.helse.mediator.meldinger

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import no.nav.helse.kafka.BehandlingOpprettetRiver
import no.nav.helse.spesialist.application.InMemoryRepositoriesAndDaos
import no.nav.helse.spesialist.domain.SpleisBehandlingId
import no.nav.helse.spesialist.domain.VedtaksperiodeId
import no.nav.helse.spesialist.kafka.medTransaksjonelleRivers
import no.nav.helse.spesialist.kafka.testfixtures.Testmeldingfabrikk
import no.nav.helse.spesialist.test.TestPerson
import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.test.assertNotNull
import kotlin.test.assertNull

internal class BehandlingOpprettetRiverTest {
    private val inMemoryRepositoriesAndDaos = InMemoryRepositoriesAndDaos()
    private val rapid = TestRapid().medTransaksjonelleRivers(inMemoryRepositoriesAndDaos, BehandlingOpprettetRiver())
    private val testperson = TestPerson()

    @Test
    fun `behandler behandling_opprettet`() {
        val vedtaksperiodeId = VedtaksperiodeId(UUID.randomUUID())
        val spleisBehandlingId = SpleisBehandlingId(UUID.randomUUID())
        rapid.sendTestMessage(
            Testmeldingfabrikk.lagBehandlingOpprettet(
                aktørId = testperson.aktørId,
                fødselsnummer = testperson.fødselsnummer,
                organisasjonsnummer = testperson.orgnummer,
                vedtaksperiodeId = vedtaksperiodeId.value,
                spleisBehandlingId = spleisBehandlingId.value,
            ),
        )
        inMemoryRepositoriesAndDaos.sessionFactory.transactionalSessionScope {
            assertNotNull(it.vedtaksperiodeRepository.finn(vedtaksperiodeId))
            assertNotNull(it.behandlingRepository.finn(spleisBehandlingId))
        }
    }

    @Test
    fun `ignorerer organisasjonsnummer ARBEIDSLEDIG uten å tryne`() {
        val vedtaksperiodeId = VedtaksperiodeId(UUID.randomUUID())
        val spleisBehandlingId = SpleisBehandlingId(UUID.randomUUID())
        rapid.sendTestMessage(
            Testmeldingfabrikk.lagBehandlingOpprettet(
                aktørId = testperson.aktørId,
                fødselsnummer = testperson.fødselsnummer,
                organisasjonsnummer = "ARBEIDSLEDIG",
                vedtaksperiodeId = vedtaksperiodeId.value,
                spleisBehandlingId = spleisBehandlingId.value,
            ),
        )
        inMemoryRepositoriesAndDaos.sessionFactory.transactionalSessionScope {
            assertNull(it.vedtaksperiodeRepository.finn(vedtaksperiodeId))
            assertNull(it.behandlingRepository.finn(spleisBehandlingId))
        }
    }

    @Test
    fun `ignorerer organisasjonsnummer FRILANS uten å tryne`() {
        val vedtaksperiodeId = VedtaksperiodeId(UUID.randomUUID())
        val spleisBehandlingId = SpleisBehandlingId(UUID.randomUUID())
        rapid.sendTestMessage(
            Testmeldingfabrikk.lagBehandlingOpprettet(
                aktørId = testperson.aktørId,
                fødselsnummer = testperson.fødselsnummer,
                organisasjonsnummer = "FRILANS",
                vedtaksperiodeId = vedtaksperiodeId.value,
                spleisBehandlingId = spleisBehandlingId.value,
            ),
        )
        inMemoryRepositoriesAndDaos.sessionFactory.transactionalSessionScope {
            assertNull(it.vedtaksperiodeRepository.finn(vedtaksperiodeId))
            assertNull(it.behandlingRepository.finn(spleisBehandlingId))
        }
    }
}
