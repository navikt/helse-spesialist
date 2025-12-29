package no.nav.helse.spesialist.db.repository

import no.nav.helse.Varselvurdering
import no.nav.helse.spesialist.db.AbstractDBIntegrationTest
import no.nav.helse.spesialist.domain.Varsel
import no.nav.helse.spesialist.domain.testfixtures.lagVarselId
import no.nav.helse.spesialist.domain.testfixtures.lagVarseldefinisjon
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagSaksbehandler
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.time.LocalDateTime
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class PgVarselRepositoryTest : AbstractDBIntegrationTest() {
    private val person = opprettPerson()
    private val arbeidsgiver = opprettArbeidsgiver()
    private val vedtaksperiode = opprettVedtaksperiode(person, arbeidsgiver)
    private val behandling = opprettBehandling(vedtaksperiode)
    private val saksbehandler = lagSaksbehandler().also { sessionContext.saksbehandlerRepository.lagre(it) }
    private val varseldefinisjon =
        lagVarseldefinisjon(kode = "RV_IV_1").also {
            opprettVarseldefinisjon(it.tittel, it.kode, it.id.value)
        }

    private val repository = sessionContext.varselRepository

    @Test
    fun `finn varsel gitt varselId`() {
        // given
        val varsel = opprettVarsel(behandling, kode = "RV_IV_1")
        varsel.vurder(saksbehandler.id, varseldefinisjon.id)
        sessionContext.varselRepository.lagre(varsel)

        // when
        val funnet = repository.finn(varsel.id)

        // then
        assertNotNull(funnet)
        assertEquals(varsel.id, funnet.id)
        assertEquals(Varsel.Status.VURDERT, funnet.status)
        assertEquals("RV_IV_1", funnet.kode)
        assertEquals(behandling.spleisBehandlingId, funnet.spleisBehandlingId)
        assertEquals(varseldefinisjon.id, funnet.vurdering?.vurdertDefinisjonId)
    }

    @ParameterizedTest
    @EnumSource(value = Varsel.Status::class, names = ["VURDERT", "GODKJENT"])
    fun `map ut vurdering hvis status er VURDERT eller GODKJENT`(status: Varsel.Status) {
        // given
        val varsel =
            Varsel.fraLagring(
                id = lagVarselId(),
                spleisBehandlingId = behandling.spleisBehandlingId,
                behandlingUnikId = behandling.id,
                status = status,
                kode = "RV_IV_1",
                opprettetTidspunkt = LocalDateTime.now(),
                vurdering = Varselvurdering(saksbehandler.id, LocalDateTime.now(), varseldefinisjon.id),
            )
        sessionContext.varselRepository.lagre(varsel)

        // when
        val funnet = repository.finn(varsel.id)

        // then
        assertNotNull(funnet)
        assertEquals(varsel.id, funnet.id)
        assertEquals(status, funnet.status)
        assertEquals("RV_IV_1", funnet.kode)
        assertNotNull(funnet.vurdering)
        assertEquals(behandling.spleisBehandlingId, funnet.spleisBehandlingId)
        assertEquals(varseldefinisjon.id, funnet.vurdering?.vurdertDefinisjonId)
        assertEquals(saksbehandler.id, funnet.vurdering?.saksbehandlerId)
    }

    @ParameterizedTest
    @EnumSource(value = Varsel.Status::class, names = ["VURDERT", "GODKJENT"], mode = EnumSource.Mode.EXCLUDE)
    fun `ikke map ut vurdering hvis status er noe annet enn VURDERT eller GODKJENT`(status: Varsel.Status) {
        // given
        val varsel =
            Varsel.fraLagring(
                id = lagVarselId(),
                spleisBehandlingId = behandling.spleisBehandlingId,
                behandlingUnikId = behandling.id,
                status = status,
                kode = "RV_IV_1",
                opprettetTidspunkt = LocalDateTime.now(),
                vurdering = null,
            )
        sessionContext.varselRepository.lagre(varsel)

        // when
        val funnet = repository.finn(varsel.id)

        // then
        assertNotNull(funnet)
        assertEquals(varsel.id, funnet.id)
        assertEquals(status, funnet.status)
        assertEquals("RV_IV_1", funnet.kode)
        assertEquals(behandling.spleisBehandlingId, funnet.spleisBehandlingId)
        assertNull(funnet.vurdering)
    }

    @Test
    fun `finn varsler for gitt behandling`() {
        // given
        val varsel1 = opprettVarsel(behandling, "RV_IV_1")
        val varsel2 = opprettVarsel(behandling, "RV_IV_2")

        // when
        val funnet =
            repository
                .finnVarsler(listOfNotNull(behandling.spleisBehandlingId))
                .sortedBy { it.opprettetTidspunkt }

        // then
        assertEquals(2, funnet.size)
        assertEquals(varsel1.id, funnet[0].id)
        assertEquals(varsel2.id, funnet[1].id)
        assertEquals(Varsel.Status.AKTIV, funnet[0].status)
        assertEquals(Varsel.Status.AKTIV, funnet[1].status)
    }

    @Test
    fun `finn varsel med vurdering for gitt behandling`() {
        // given
        val varsel =
            Varsel.fraLagring(
                id = lagVarselId(),
                spleisBehandlingId = behandling.spleisBehandlingId,
                behandlingUnikId = behandling.id,
                status = Varsel.Status.VURDERT,
                kode = "RV_IV_1",
                opprettetTidspunkt = LocalDateTime.now(),
                vurdering = Varselvurdering(saksbehandler.id, LocalDateTime.now(), varseldefinisjon.id),
            )
        sessionContext.varselRepository.lagre(varsel)

        // when
        val funnet = repository.finnVarsler(listOfNotNull(behandling.spleisBehandlingId))

        // then
        assertEquals(1, funnet.size)
        assertEquals(varsel.id, funnet.first().id)
        assertEquals(Varsel.Status.VURDERT, funnet.first().status)
        assertEquals(saksbehandler.id, funnet.first().vurdering?.saksbehandlerId)
    }

    @Test
    fun `finn varsel med vurdering for gitt behandling vha behandlingUnikId`() {
        // given
        val varsel =
            Varsel.fraLagring(
                id = lagVarselId(),
                spleisBehandlingId = behandling.spleisBehandlingId,
                behandlingUnikId = behandling.id,
                status = Varsel.Status.VURDERT,
                kode = "RV_IV_1",
                opprettetTidspunkt = LocalDateTime.now(),
                vurdering = Varselvurdering(saksbehandler.id, LocalDateTime.now(), varseldefinisjon.id),
            )
        sessionContext.varselRepository.lagre(varsel)

        // when
        val funnet = repository.finnVarslerFor(behandling.id)

        // then
        assertEquals(1, funnet.size)
        assertEquals(varsel.id, funnet.first().id)
        assertEquals(Varsel.Status.VURDERT, funnet.first().status)
        assertEquals(saksbehandler.id, funnet.first().vurdering?.saksbehandlerId)
    }

    @Test
    fun `finn varsel med vurdering for flere behandlinger vha behandlingUnikId`() {
        // given
        val behandling1 = behandling
        val vedtaksperiode2 = opprettVedtaksperiode(person, arbeidsgiver)
        val behandling2 = opprettBehandling(vedtaksperiode2)

        opprettVarsel(behandling1, "RV_IV_1")
        opprettVarsel(behandling2, "RV_IV_2")

        // when
        val funnet = repository.finnVarslerFor(listOf(behandling1.id, behandling2.id))

        // then
        assertEquals(2, funnet.size)
    }

    @Test
    fun `finn varsel for flere behandlinger`() {
        // given
        val behandling1 = behandling
        val vedtaksperiode2 = opprettVedtaksperiode(person, arbeidsgiver)
        val behandling2 = opprettBehandling(vedtaksperiode2)

        val varsel1 = opprettVarsel(behandling1, "RV_IV_1")
        val varsel2 = opprettVarsel(behandling2, "RV_IV_2")

        // when
        val funnet = repository.finnVarsler(listOfNotNull(behandling1.spleisBehandlingId, behandling2.spleisBehandlingId))

        // then
        assertEquals(2, funnet.size)
        assertContains(funnet.map { it.id }, varsel1.id)
        assertContains(funnet.map { it.id }, varsel2.id)
        assertEquals(Varsel.Status.AKTIV, funnet.find { it.id == varsel1.id }?.status)
        assertEquals(Varsel.Status.AKTIV, funnet.find { it.id == varsel2.id }?.status)
    }

    @Test
    fun `slett varsel`() {
        // given
        val varsel = opprettVarsel(behandling, "RV_IV_1")

        // when
        repository.slett(varsel.id)

        // then
        val funnet = repository.finn(varsel.id)
        assertNull(funnet)
    }
}
