package no.nav.helse.spesialist.db.repository

import no.nav.helse.spesialist.db.AbstractDBIntegrationTest
import no.nav.helse.spesialist.domain.Behandling
import no.nav.helse.spesialist.domain.SpleisBehandlingId
import no.nav.helse.spesialist.domain.VedtaksperiodeId
import no.nav.helse.spesialist.domain.testfixtures.jan
import no.nav.helse.spesialist.domain.testfixtures.lagFødselsnummer
import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class PgBehandlingRepositoryTest : AbstractDBIntegrationTest() {
    private val repository = PgBehandlingRepository(session)

    @Test
    fun `finn behandling`() {
        // given
        val spleisBehandlingId = UUID.randomUUID()
        val vedtaksperiodeId = UUID.randomUUID()
        val tags = listOf("FOOBAR")
        val fødselsnummer = lagFødselsnummer()
        val fom = 1.jan(2018)
        val tom = 31.jan(2018)
        opprettPerson(fødselsnummer = fødselsnummer)
        opprettArbeidsgiver()
        opprettBehandling(
            spleisBehandlingId = spleisBehandlingId,
            vedtaksperiodeId = vedtaksperiodeId,
            tags = tags,
            fødselsnummer = fødselsnummer,
            fom = fom,
            tom = tom,
        )

        // when
        val funnet = repository.finn(SpleisBehandlingId(spleisBehandlingId))

        // then
        assertNotNull(funnet)
        assertEquals(spleisBehandlingId, funnet.id.value)
        assertEquals(vedtaksperiodeId, funnet.vedtaksperiodeId.value)
        assertEquals(tags.toSet(), funnet.tags)
        assertEquals(fødselsnummer, funnet.fødselsnummer)
        assertEquals(fom, funnet.fom)
        assertEquals(tom, funnet.tom)
        assertEquals(fom, funnet.skjæringstidspunkt) //ved ny behandling defaultes skjæringstidspunkt til fom
    }

    @Test
    fun `finn behandling med søknad-ider`() {
        // given
        val spleisBehandlingId = UUID.randomUUID()
        val tags = listOf("FOOBAR")
        val fødselsnummer = lagFødselsnummer()
        val fom = 1.jan(2018)
        val tom = 31.jan(2018)
        val søknadId = UUID.randomUUID()
        opprettPerson(fødselsnummer = fødselsnummer)
        opprettArbeidsgiver()
        opprettBehandling(
            spleisBehandlingId = spleisBehandlingId,
            tags = tags,
            fødselsnummer = fødselsnummer,
            fom = fom,
            tom = tom
        )
        repository.finn(SpleisBehandlingId(spleisBehandlingId))
            ?.also {
                it.kobleSøknader(setOf(søknadId))
                repository.lagre(it)
            }

        // when
        val funnet = repository.finn(SpleisBehandlingId(spleisBehandlingId))

        // then
        assertNotNull(funnet)
        assertEquals(setOf(søknadId), funnet.søknadIder())
    }

    @Test
    fun `finn behandlinger med fødselsnummer og skjæringstidspunkt`() {
        // given
        val spleisBehandlingId1 = UUID.randomUUID()
        val spleisBehandlingId2 = UUID.randomUUID()
        val spleisBehandlingId3 = UUID.randomUUID()
        val tags = listOf("FOOBAR")
        val fødselsnummer = lagFødselsnummer()
        opprettPerson(fødselsnummer = fødselsnummer)
        opprettArbeidsgiver()
        opprettBehandling(
            fom = 1.jan(2018),
            spleisBehandlingId = spleisBehandlingId1,
            vedtaksperiodeId = UUID.randomUUID(),
            tags = tags,
            fødselsnummer = fødselsnummer
        )
        opprettBehandling(
            fom = 1.jan(2018),
            spleisBehandlingId = spleisBehandlingId2,
            vedtaksperiodeId = UUID.randomUUID(),
            tags = tags,
            fødselsnummer = fødselsnummer
        )
        opprettBehandling(
            fom = 2.jan(2018),
            spleisBehandlingId = spleisBehandlingId3,
            vedtaksperiodeId = UUID.randomUUID(),
            tags = tags,
            fødselsnummer = fødselsnummer
        )

        // when
        val funnet = repository.finnAndreBehandlingerISykefraværstilfelle(
            Behandling.fraLagring(
                id = SpleisBehandlingId(UUID.randomUUID()),
                tags = emptySet(),
                fødselsnummer = fødselsnummer,
                søknadIder = emptySet(),
                fom = 1.jan(2018),
                tom = 31.jan(2018),
                skjæringstidspunkt = 1.jan(2018),
                varselIder = emptySet(),
                vedtaksperiodeId = VedtaksperiodeId(UUID.randomUUID())
            )
        ).map { it.id.value }

        // then
        assertEquals(2, funnet.size)
        assertContains(funnet, spleisBehandlingId1)
        assertContains(funnet, spleisBehandlingId2)
    }

    @Test
    fun `finn kun siste behandling for en gitt vedtaksperiode`() {
        // given
        val spleisBehandlingId1 = UUID.randomUUID()
        val spleisBehandlingId2 = UUID.randomUUID()
        val tags = listOf("FOOBAR")
        val fødselsnummer = lagFødselsnummer()
        opprettPerson(fødselsnummer = fødselsnummer)
        opprettArbeidsgiver()
        val vedtaksperiodeId = UUID.randomUUID()
        opprettBehandling(
            fom = 2.jan(2018),
            spleisBehandlingId = spleisBehandlingId1,
            vedtaksperiodeId = vedtaksperiodeId,
            tags = tags,
            fødselsnummer = fødselsnummer
        )
        opprettBehandling(
            fom = 2.jan(2018),
            spleisBehandlingId = spleisBehandlingId2,
            vedtaksperiodeId = vedtaksperiodeId,
            tags = tags,
            fødselsnummer = fødselsnummer
        )

        // when
        val funnet = repository.finnAndreBehandlingerISykefraværstilfelle(
            Behandling.fraLagring(
                id = SpleisBehandlingId(UUID.randomUUID()),
                tags = emptySet(),
                fødselsnummer = fødselsnummer,
                søknadIder = emptySet(),
                fom = 1.jan(2018),
                tom = 31.jan(2018),
                skjæringstidspunkt = 2.jan(2018),
                varselIder = emptySet(),
                vedtaksperiodeId = VedtaksperiodeId(UUID.randomUUID())
            ))

        // then
        assertEquals(1, funnet.size)
        assertEquals(spleisBehandlingId2, funnet.first().id.value)
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
        val funnet = repository.finnAndreBehandlingerISykefraværstilfelle(
            Behandling.fraLagring(
                id = SpleisBehandlingId(UUID.randomUUID()),
                tags = emptySet(),
                fødselsnummer = lagFødselsnummer(),
                søknadIder = emptySet(),
                fom = 1.jan(2018),
                tom = 31.jan(2018),
                skjæringstidspunkt = 1.jan(2018),
                varselIder = emptySet(),
                vedtaksperiodeId = VedtaksperiodeId(UUID.randomUUID())
            )
        )

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

    @Test
    fun `finn behandling inkluderer varselIder`() {
        // given
        val spleisBehandlingId = UUID.randomUUID()
        val vedtaksperiodeId = UUID.randomUUID()
        val tags = listOf("FOOBAR")
        val fødselsnummer = lagFødselsnummer()
        opprettPerson(fødselsnummer = fødselsnummer)
        opprettArbeidsgiver()
        opprettBehandling(
            spleisBehandlingId = spleisBehandlingId,
            vedtaksperiodeId = vedtaksperiodeId,
            tags = tags,
            fødselsnummer = fødselsnummer
        )
        val varselId1 = UUID.randomUUID()
        val varselId2 = UUID.randomUUID()
        nyttVarsel(id = varselId1, vedtaksperiodeId = vedtaksperiodeId, spleisBehandlingId = spleisBehandlingId, kode = "EN_KODE_1")
        nyttVarsel(id = varselId2, vedtaksperiodeId = vedtaksperiodeId, spleisBehandlingId = spleisBehandlingId, kode = "EN_KODE_2")

        // when
        val funnet = requireNotNull(repository.finn(SpleisBehandlingId(spleisBehandlingId)))

        // then
        assertEquals(setOf(varselId1, varselId2), funnet.varselIder)
    }

    @Test
    fun `finnAndreBehandlingerISykefraværstilfelle inkluderer varselIder`() {
        // given
        val fødselsnummer = lagFødselsnummer()
        val tags = listOf("FOOBAR")
        val fom = 1.jan(2018)
        val tom = 31.jan(2018)
        opprettPerson(fødselsnummer = fødselsnummer)
        opprettArbeidsgiver()

        val vedtaksperiodeId1 = UUID.randomUUID()
        val vedtaksperiodeId2 = UUID.randomUUID()
        val spleisBehandlingId1 = UUID.randomUUID()
        val spleisBehandlingId2 = UUID.randomUUID()

        opprettBehandling(
            spleisBehandlingId = spleisBehandlingId1,
            vedtaksperiodeId = vedtaksperiodeId1,
            tags = tags,
            fødselsnummer = fødselsnummer,
            fom = fom,
            tom = tom
        )
        opprettBehandling(
            spleisBehandlingId = spleisBehandlingId2,
            vedtaksperiodeId = vedtaksperiodeId2,
            tags = tags,
            fødselsnummer = fødselsnummer,
            fom = fom,
            tom = tom
        )
        val varselId = UUID.randomUUID()
        nyttVarsel(id = varselId, vedtaksperiodeId = vedtaksperiodeId2, spleisBehandlingId = spleisBehandlingId2)

        // when
        val behandling1 = requireNotNull(repository.finn(SpleisBehandlingId(spleisBehandlingId1)))
        val andreBehandlinger = repository.finnAndreBehandlingerISykefraværstilfelle(behandling1)

        // then
        assertEquals(1, andreBehandlinger.size)
        val behandling2 = andreBehandlinger.first()
        assertEquals(spleisBehandlingId2, behandling2.id.value)
        assertEquals(setOf(varselId), behandling2.varselIder)
    }
}
