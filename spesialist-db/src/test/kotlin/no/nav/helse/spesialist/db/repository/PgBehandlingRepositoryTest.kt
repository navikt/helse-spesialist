package no.nav.helse.spesialist.db.repository

import no.nav.helse.modell.vedtaksperiode.Yrkesaktivitetstype
import no.nav.helse.spesialist.db.AbstractDBIntegrationTest
import no.nav.helse.spesialist.domain.Behandling
import no.nav.helse.spesialist.domain.SpleisBehandlingId
import no.nav.helse.spesialist.domain.UtbetalingId
import no.nav.helse.spesialist.domain.testfixtures.feb
import no.nav.helse.spesialist.domain.testfixtures.jan
import no.nav.helse.spesialist.domain.testfixtures.lagBehandling
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagFødselsnummer
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
        assertEquals(spleisBehandlingId, funnet.spleisBehandlingId!!.value)
        assertEquals(vedtaksperiodeId, funnet.vedtaksperiodeId.value)
        assertEquals(tags.toSet(), funnet.tags)
        assertEquals(fom, funnet.fom)
        assertEquals(tom, funnet.tom)
        assertEquals(fom, funnet.skjæringstidspunkt) //ved ny behandling defaultes skjæringstidspunkt til fom
    }

    @Test
    fun `finn behandling vha behandlingUnikId`() {
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
        val funnetMedBehandlingId = repository.finn(SpleisBehandlingId(spleisBehandlingId))
        val funnetMedBehandlingUnikId = repository.finn(funnetMedBehandlingId!!.id)

        // then
        assertNotNull(funnetMedBehandlingUnikId)
        assertEquals(spleisBehandlingId, funnetMedBehandlingUnikId.spleisBehandlingId!!.value)
        assertEquals(vedtaksperiodeId, funnetMedBehandlingUnikId.vedtaksperiodeId.value)
        assertEquals(tags.toSet(), funnetMedBehandlingUnikId.tags)
        assertEquals(fom, funnetMedBehandlingUnikId.fom)
        assertEquals(tom, funnetMedBehandlingUnikId.tom)
        assertEquals(fom, funnetMedBehandlingUnikId.skjæringstidspunkt) //ved ny behandling defaultes skjæringstidspunkt til fom
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
            lagBehandling(tags = emptySet(), fom = 1.jan(2018)),
            fødselsnummer = fødselsnummer
        ).map { it.spleisBehandlingId!!.value }

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
            lagBehandling(tags = emptySet(), skjæringstidspunkt = 2 jan 2018),
            fødselsnummer = fødselsnummer
        )

        // then
        assertEquals(1, funnet.size)
        assertEquals(spleisBehandlingId2, funnet.first().spleisBehandlingId!!.value)
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
            lagBehandling(tags = emptySet()),
            fødselsnummer = lagFødselsnummer()
        )

        // then
        assertEquals(0, funnet.size)
    }

    @Test
    fun `lagre behandling`() {
        // given
        val behandling = lagBehandling()

        //when
        repository.lagre(behandling)

        //then
        val funnet = repository.finn(behandling.id)
        assertNotNull(funnet)
        assertEquals(behandling.id, funnet.id)
        assertEquals(behandling.spleisBehandlingId, funnet.spleisBehandlingId)
        assertEquals(behandling.vedtaksperiodeId, funnet.vedtaksperiodeId)
        assertEquals(behandling.utbetalingId, funnet.utbetalingId)
        assertEquals(behandling.fom, funnet.fom)
        assertEquals(behandling.tom, funnet.tom)
        assertEquals(behandling.skjæringstidspunkt, funnet.skjæringstidspunkt)
        assertEquals(behandling.tilstand, funnet.tilstand)
        assertEquals(behandling.tags, funnet.tags)
        assertEquals(behandling.yrkesaktivitetstype, funnet.yrkesaktivitetstype)
    }

    @Test
    fun `oppdater behandling`() {
        // given
        val orginalBehandling = lagBehandling(
            utbetalingId = UtbetalingId(UUID.randomUUID()),
            fom = 1.feb(2018),
            tom = 28.feb(2018),
            skjæringstidspunkt = 1.feb(2018),
            tilstand = Behandling.Tilstand.KlarTilBehandling,
            tags = setOf("DelvisInnvilget"),
            yrkesaktivitetstype = Yrkesaktivitetstype.ARBEIDSTAKER,
        )
        repository.lagre(orginalBehandling)

        //when
        val oppdatertBehandling = lagBehandling(
            id = orginalBehandling.id,
            spleisBehandlingId = orginalBehandling.spleisBehandlingId,
            vedtaksperiodeId = orginalBehandling.vedtaksperiodeId,
            utbetalingId = UtbetalingId(UUID.randomUUID()),
            fom = 1.jan(2018),
            tom = 31.jan(2018),
            skjæringstidspunkt = 1.jan(2018),
            tilstand = Behandling.Tilstand.VedtakFattet,
            tags = setOf("Innvilget"),
            yrkesaktivitetstype = Yrkesaktivitetstype.ARBEIDSLEDIG
        )
        repository.lagre(oppdatertBehandling)

        //then
        val funnet = repository.finn(orginalBehandling.id)
        assertNotNull(funnet)
        assertEquals(orginalBehandling.id, funnet.id)
        assertEquals(orginalBehandling.spleisBehandlingId, funnet.spleisBehandlingId)
        assertEquals(orginalBehandling.vedtaksperiodeId, funnet.vedtaksperiodeId)
        assertEquals(oppdatertBehandling.utbetalingId, funnet.utbetalingId)
        assertEquals(oppdatertBehandling.fom, funnet.fom)
        assertEquals(oppdatertBehandling.tom, funnet.tom)
        assertEquals(oppdatertBehandling.skjæringstidspunkt, funnet.skjæringstidspunkt)
        assertEquals(oppdatertBehandling.tilstand, funnet.tilstand)
        assertEquals(oppdatertBehandling.tags, funnet.tags)
        assertEquals(oppdatertBehandling.yrkesaktivitetstype, funnet.yrkesaktivitetstype)
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
