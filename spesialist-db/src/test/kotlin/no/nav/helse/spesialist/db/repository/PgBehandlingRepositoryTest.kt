package no.nav.helse.spesialist.db.repository

import no.nav.helse.spesialist.db.AbstractDBIntegrationTest
import no.nav.helse.spesialist.domain.SpleisBehandlingId
import no.nav.helse.spesialist.domain.testfixtures.jan
import no.nav.helse.spesialist.domain.testfixtures.lagFødselsnummer
import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class PgBehandlingRepositoryTest: AbstractDBIntegrationTest() {
    private val repository = PgBehandlingRepository(session)
    @Test
    fun `finn behandling`() {
        // given
        val spleisBehandlingId = UUID.randomUUID()
        val tags = listOf("FOOBAR")
        val fødselsnummer = lagFødselsnummer()
        opprettPerson(fødselsnummer = fødselsnummer)
        opprettArbeidsgiver()
        opprettBehandling(
            spleisBehandlingId = spleisBehandlingId,
            tags = tags,
            fødselsnummer = fødselsnummer
        )

        // when
        val funnet = repository.finn(SpleisBehandlingId(spleisBehandlingId))

        // then
        assertNotNull(funnet)
        assertEquals(spleisBehandlingId, funnet.id.value)
        assertEquals(tags.toSet(), funnet.tags)
        assertEquals(fødselsnummer, funnet.fødselsnummer)
    }

    @Test
    fun `finn behandlinger med fødselsnummer og skjæringstidspunkt`() {
        // given
        val spleisBehandlingId1 = UUID.randomUUID()
        val spleisBehandlingId2 = UUID.randomUUID()
        val tags = listOf("FOOBAR")
        val fødselsnummer = lagFødselsnummer()
        opprettPerson(fødselsnummer = fødselsnummer)
        opprettArbeidsgiver()
        opprettBehandling(
            fom = 1.jan(2018),
            spleisBehandlingId = spleisBehandlingId1,
            tags = tags,
            fødselsnummer = fødselsnummer
        )
        opprettBehandling(
            fom = 1.jan(2018),
            spleisBehandlingId = spleisBehandlingId2,
            tags = tags,
            fødselsnummer = fødselsnummer
        )
        opprettBehandling(
            fom = 2.jan(2018),
            spleisBehandlingId = spleisBehandlingId2,
            tags = tags,
            fødselsnummer = fødselsnummer
        )

        // when
        val funnet = repository.finnBehandlingerISykefraværstilfelle(fødselsnummer, 1.jan(2018))

        // then
        assertEquals(2, funnet.size)
        assertEquals(spleisBehandlingId1, funnet[0].id.value)
        assertEquals(spleisBehandlingId2, funnet[1].id.value)
    }

    @Test
    fun `ikke finn behandlinger som hører til annen person`() {
        // given
        val spleisBehandlingId1 = UUID.randomUUID()
        val tags = listOf("FOOBAR")
        val fødselsnummer = lagFødselsnummer()
        opprettPerson(fødselsnummer = fødselsnummer)
        opprettArbeidsgiver()
        opprettBehandling(
            fom = 1.jan(2018),
            spleisBehandlingId = spleisBehandlingId1,
            tags = tags,
            fødselsnummer = fødselsnummer
        )

        // when
        val funnet = repository.finnBehandlingerISykefraværstilfelle(lagFødselsnummer(), 1.jan(2018))

        // then
        assertEquals(0, funnet.size)
    }

    @Test
    fun `lagre kobling mellom behandling og søknadId`() {
        // given
        val spleisBehandlingId = UUID.randomUUID()
        val tags = listOf("FOOBAR")
        val fødselsnummer = lagFødselsnummer()
        opprettPerson(fødselsnummer = fødselsnummer)
        opprettArbeidsgiver()
        opprettBehandling(
            spleisBehandlingId = spleisBehandlingId,
            tags = tags,
            fødselsnummer = fødselsnummer
        )

        val funnet = requireNotNull(repository.finn(SpleisBehandlingId(spleisBehandlingId)))

        //when
        val søknadId = UUID.randomUUID()
        funnet.kobleSøknader(mutableSetOf(søknadId))
        repository.lagre(funnet)

        val funnetIgjen = requireNotNull(repository.finn(SpleisBehandlingId(spleisBehandlingId)))

        //then
        assertContains(funnetIgjen.søknadIder(), søknadId)
    }

}
