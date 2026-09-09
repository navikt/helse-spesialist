package no.nav.helse.spesialist.db.repository

import no.nav.helse.modell.vedtaksperiode.Yrkesaktivitetstype
import no.nav.helse.spesialist.db.AbstractDBIntegrationTest
import no.nav.helse.spesialist.domain.Behandling
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
    private val person = opprettPerson()
    private val arbeidsgiver = opprettArbeidsgiver()
    private val repository = PgBehandlingRepository(session)

    @Test
    fun `finn behandling`() {
        // given
        val tags = listOf("FOOBAR")
        val fom = 1.jan(2018)
        val tom = 31.jan(2018)
        val vedtaksperiode = opprettVedtaksperiode(person, arbeidsgiver)
        val behandling = opprettBehandling(vedtaksperiode, tags = tags, fom = fom, tom = tom)

        // when
        val funnet = repository.finnOrNull(behandling.id)

        // then
        assertNotNull(funnet)
        assertEquals(behandling.spleisBehandlingId, funnet.spleisBehandlingId)
        assertEquals(vedtaksperiode.id, funnet.vedtaksperiodeId)
        assertEquals(tags.toSet(), funnet.tags)
        assertEquals(fom, funnet.fom)
        assertEquals(tom, funnet.tom)
        assertEquals(fom, funnet.skjæringstidspunkt) // ved ny behandling defaultes skjæringstidspunkt til fom
    }

    @Test
    fun `finn behandling vha behandlingUnikId`() {
        // given
        val tags = listOf("FOOBAR")
        val fom = 1.jan(2018)
        val tom = 31.jan(2018)
        val vedtaksperiode = opprettVedtaksperiode(person, arbeidsgiver)
        val behandling = opprettBehandling(vedtaksperiode, tags = tags, fom = fom, tom = tom)

        // when
        val funnetMedBehandlingUnikId = repository.finnOrNull(behandling.id)

        // then
        assertNotNull(funnetMedBehandlingUnikId)
        assertEquals(behandling.spleisBehandlingId, funnetMedBehandlingUnikId.spleisBehandlingId)
        assertEquals(vedtaksperiode.id, funnetMedBehandlingUnikId.vedtaksperiodeId)
        assertEquals(tags.toSet(), funnetMedBehandlingUnikId.tags)
        assertEquals(fom, funnetMedBehandlingUnikId.fom)
        assertEquals(tom, funnetMedBehandlingUnikId.tom)
        assertEquals(fom, funnetMedBehandlingUnikId.skjæringstidspunkt) // ved ny behandling defaultes skjæringstidspunkt til fom
    }

    @Test
    fun `finn behandlinger med fødselsnummer og skjæringstidspunkt`() {
        // given
        val vedtaksperiode1 = opprettVedtaksperiode(person, arbeidsgiver)
        val vedtaksperiode2 = opprettVedtaksperiode(person, arbeidsgiver)
        val vedtaksperiode3 = opprettVedtaksperiode(person, arbeidsgiver)
        val vedtaksperiode4 = opprettVedtaksperiode(person, arbeidsgiver)
        val behandling1 = opprettBehandling(vedtaksperiode1, skjæringstidspunkt = 1 jan 2018)
        val behandling2 = opprettBehandling(vedtaksperiode2, skjæringstidspunkt = 1 jan 2018)
        val behandling3 = opprettBehandling(vedtaksperiode3, skjæringstidspunkt = 1 jan 2018)
        val behandling4 = opprettBehandling(vedtaksperiode4, skjæringstidspunkt = 1 jan 2018)

        // when
        val funnet =
            repository
                .finnAndreBehandlingerISykefraværstilfelle(
                    behandling4,
                    fødselsnummer = person.id.value,
                ).map { it.id }

        // then
        assertEquals(3, funnet.size)
        assertContains(funnet, behandling1.id)
        assertContains(funnet, behandling2.id)
        assertContains(funnet, behandling3.id)
    }

    @Test
    fun `finn kun siste behandling for en gitt vedtaksperiode`() {
        // given
        val vedtaksperiode1 = opprettVedtaksperiode(person, arbeidsgiver)
        val vedtaksperiode2 = opprettVedtaksperiode(person, arbeidsgiver)
        val behandling1 = opprettBehandling(vedtaksperiode1)
        val behandling2 = opprettBehandling(vedtaksperiode2)

        // when
        val funnet =
            repository.finnAndreBehandlingerISykefraværstilfelle(
                behandling2,
                fødselsnummer = person.id.value,
            )

        // then
        assertEquals(1, funnet.size)
        assertEquals(behandling1.id, funnet.first().id)
    }

    @Test
    fun `ikke finn behandlinger som hører til annen person`() {
        // given
        val vedtaksperiode = opprettVedtaksperiode(person, arbeidsgiver)
        opprettBehandling(vedtaksperiode)

        // when
        val funnet =
            repository.finnAndreBehandlingerISykefraværstilfelle(
                lagBehandling(tags = emptySet()),
                fødselsnummer = lagFødselsnummer(),
            )

        // then
        assertEquals(0, funnet.size)
    }

    @Test
    fun `finn alle behandlinger for en person`() {
        // given
        val vedtaksperiode1 = opprettVedtaksperiode(person, arbeidsgiver)
        val vedtaksperiode2 = opprettVedtaksperiode(person, arbeidsgiver)
        val behandling1 = opprettBehandling(vedtaksperiode1)
        val behandling2 = opprettBehandling(vedtaksperiode2)

        // when
        val funnet = repository.finnAlle(person.id).map { it.id }

        // then
        assertEquals(2, funnet.size)
        assertContains(funnet, behandling1.id)
        assertContains(funnet, behandling2.id)
    }

    @Test
    fun `finn alle behandlinger returnerer nyeste behandling først`() {
        // given
        val vedtaksperiode = opprettVedtaksperiode(person, arbeidsgiver)
        val førsteBehandling = opprettBehandling(vedtaksperiode)
        val andreBehandling = opprettBehandling(vedtaksperiode)

        // when
        val funnet = repository.finnAlle(person.id)

        // then
        assertEquals(listOf(andreBehandling.id, førsteBehandling.id), funnet.map { it.id })
    }

    @Test
    fun `finn alle behandlinger ekskluderer behandlinger som hører til forkastet vedtaksperiode`() {
        // given
        val aktivVedtaksperiode = opprettVedtaksperiode(person, arbeidsgiver)
        val forkastetVedtaksperiode = opprettVedtaksperiode(person, arbeidsgiver, forkastet = true)
        val aktivBehandling = opprettBehandling(aktivVedtaksperiode)
        opprettBehandling(forkastetVedtaksperiode)

        // when
        val funnet = repository.finnAlle(person.id)

        // then
        assertEquals(listOf(aktivBehandling.id), funnet.map { it.id })
    }

    @Test
    fun `finn alle behandlinger ekskluderer behandlinger som hører til annen person`() {
        // given
        val annenPerson = opprettPerson()
        val vedtaksperiode = opprettVedtaksperiode(person, arbeidsgiver)
        val annenVedtaksperiode = opprettVedtaksperiode(annenPerson, arbeidsgiver)
        val behandling = opprettBehandling(vedtaksperiode)
        opprettBehandling(annenVedtaksperiode)

        // when
        val funnet = repository.finnAlle(person.id)

        // then
        assertEquals(listOf(behandling.id), funnet.map { it.id })
    }

    @Test
    fun `finn alle behandlinger returnerer tom liste når personen ikke har noen behandlinger`() {
        // given
        opprettPerson()

        // when
        val funnet = repository.finnAlle(person.id)

        // then
        assertEquals(0, funnet.size)
    }

    @Test
    fun `lagre behandling`() {
        // given
        val vedtaksperiode = opprettVedtaksperiode(person, arbeidsgiver)
        val behandling = lagBehandling(vedtaksperiodeId = vedtaksperiode.id)

        // when
        repository.lagre(behandling)

        // then
        val funnet = repository.finnOrNull(behandling.id)
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
        val vedtaksperiode = opprettVedtaksperiode(person, arbeidsgiver)

        val orginalBehandling =
            lagBehandling(
                vedtaksperiodeId = vedtaksperiode.id,
                utbetalingId = UtbetalingId(UUID.randomUUID()),
                fom = 1.feb(2018),
                tom = 28.feb(2018),
                skjæringstidspunkt = 1.feb(2018),
                tilstand = Behandling.Tilstand.KlarTilBehandling,
                tags = setOf("DelvisInnvilget"),
                yrkesaktivitetstype = Yrkesaktivitetstype.ARBEIDSTAKER,
            )
        repository.lagre(orginalBehandling)

        // when
        val oppdatertBehandling =
            lagBehandling(
                id = orginalBehandling.id,
                spleisBehandlingId = orginalBehandling.spleisBehandlingId,
                vedtaksperiodeId = orginalBehandling.vedtaksperiodeId,
                utbetalingId = UtbetalingId(UUID.randomUUID()),
                fom = 1.jan(2018),
                tom = 31.jan(2018),
                skjæringstidspunkt = 1.jan(2018),
                tilstand = Behandling.Tilstand.VedtakFattet,
                tags = setOf("Innvilget"),
                yrkesaktivitetstype = Yrkesaktivitetstype.ARBEIDSLEDIG,
            )
        repository.lagre(oppdatertBehandling)

        // then
        val funnet = repository.finnOrNull(orginalBehandling.id)
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
}
