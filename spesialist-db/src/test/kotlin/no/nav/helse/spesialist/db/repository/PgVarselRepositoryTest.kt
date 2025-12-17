package no.nav.helse.spesialist.db.repository

import no.nav.helse.spesialist.db.AbstractDBIntegrationTest
import no.nav.helse.spesialist.domain.SaksbehandlerOid
import no.nav.helse.spesialist.domain.SpleisBehandlingId
import no.nav.helse.spesialist.domain.Varsel
import no.nav.helse.spesialist.domain.VarselId
import no.nav.helse.spesialist.domain.VarseldefinisjonId
import no.nav.helse.spesialist.domain.VedtaksperiodeId
import no.nav.helse.spesialist.domain.testfixtures.lagSpleisBehandlingId
import no.nav.helse.spesialist.domain.testfixtures.lagVarselId
import no.nav.helse.spesialist.domain.testfixtures.lagVarseldefinisjonId
import no.nav.helse.spesialist.domain.testfixtures.lagVedtaksperiodeId
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagSaksbehandler
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.time.LocalDateTime
import java.util.UUID
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class PgVarselRepositoryTest : AbstractDBIntegrationTest() {
    private val repository = sessionContext.varselRepository

    @Test
    fun `finn varsel gitt varselId`() {
        // given
        val vedtaksperiodeId = VedtaksperiodeId(UUID.randomUUID())
        val spleisBehandlingId = SpleisBehandlingId(UUID.randomUUID())
        val varselId = VarselId(UUID.randomUUID())
        val saksbehandler = lagSaksbehandler()
        val varseldefinisjonId = VarseldefinisjonId(UUID.randomUUID())
        opprettSaksbehandler(saksbehandler.id.value, saksbehandler.navn, saksbehandler.epost, saksbehandler.ident)
        val person = opprettPerson()
        opprettArbeidsgiver()
        opprettBehandling(
            vedtaksperiodeId = vedtaksperiodeId.value,
            spleisBehandlingId = spleisBehandlingId.value,
            fødselsnummer = person.id.value,
        )
        nyttVarsel(
            id = varselId.value,
            vedtaksperiodeId = vedtaksperiodeId.value,
            spleisBehandlingId = spleisBehandlingId.value,
            saksbehandlerSomEndretId = saksbehandler.ident,
            endretTidspunkt = LocalDateTime.now(),
            status = "VURDERT",
            kode = "RV_IV_1",
            opprettet = LocalDateTime.now(),
            definisjonRef = opprettVarseldefinisjon(kode = "RV_IV_1", definisjonId = varseldefinisjonId.value),
        )

        // when
        val funnet = repository.finn(varselId)

        // then
        assertNotNull(funnet)
        assertEquals(varselId, funnet.id)
        assertEquals(Varsel.Status.VURDERT, funnet.status)
        assertEquals("RV_IV_1", funnet.kode)
        assertEquals(spleisBehandlingId, funnet.spleisBehandlingId)
        assertEquals(varseldefinisjonId, funnet.vurdering?.vurdertDefinisjonId)
    }

    @ParameterizedTest
    @EnumSource(value = Varsel.Status::class, names = ["VURDERT", "GODKJENT"])
    fun `map ut vurdering hvis status er VURDERT eller GODKJENT`(status: Varsel.Status) {
        // given
        val vedtaksperiodeId = VedtaksperiodeId(UUID.randomUUID())
        val spleisBehandlingId = SpleisBehandlingId(UUID.randomUUID())
        val varselId = VarselId(UUID.randomUUID())
        val fødselsnummer = opprettPerson().id.value
        val saksbehandler = lagSaksbehandler()
        val varseldefinisjonId = VarseldefinisjonId(UUID.randomUUID())
        opprettSaksbehandler(saksbehandler.id.value, saksbehandler.navn, saksbehandler.epost, saksbehandler.ident)
        opprettArbeidsgiver()
        opprettBehandling(
            vedtaksperiodeId = vedtaksperiodeId.value,
            spleisBehandlingId = spleisBehandlingId.value,
            fødselsnummer = fødselsnummer,
        )
        nyttVarsel(
            id = varselId.value,
            vedtaksperiodeId = vedtaksperiodeId.value,
            spleisBehandlingId = spleisBehandlingId.value,
            saksbehandlerSomEndretId = saksbehandler.ident,
            endretTidspunkt = LocalDateTime.now(),
            status = status.name,
            kode = "RV_IV_1",
            opprettet = LocalDateTime.now(),
            definisjonRef = opprettVarseldefinisjon(kode = "RV_IV_1", definisjonId = varseldefinisjonId.value),
        )

        // when
        val funnet = repository.finn(varselId)

        // then
        assertNotNull(funnet)
        assertEquals(varselId, funnet.id)
        assertEquals(status, funnet.status)
        assertEquals("RV_IV_1", funnet.kode)
        assertNotNull(funnet.vurdering)
        assertEquals(spleisBehandlingId, funnet.spleisBehandlingId)
        assertEquals(varseldefinisjonId, funnet.vurdering?.vurdertDefinisjonId)
        assertEquals(saksbehandler.id, funnet.vurdering?.saksbehandlerId)
    }

    @ParameterizedTest
    @EnumSource(value = Varsel.Status::class, names = ["VURDERT", "GODKJENT"], mode = EnumSource.Mode.EXCLUDE)
    fun `ikke map ut vurdering hvis status er noe annet enn VURDERT eller GODKJENT`(status: Varsel.Status) {
        // given
        val vedtaksperiodeId = VedtaksperiodeId(UUID.randomUUID())
        val spleisBehandlingId = SpleisBehandlingId(UUID.randomUUID())
        val varselId = VarselId(UUID.randomUUID())
        val fødselsnummer = opprettPerson().id.value
        val saksbehandler = lagSaksbehandler()
        val varseldefinisjonId = VarseldefinisjonId(UUID.randomUUID())
        opprettSaksbehandler(saksbehandler.id.value, saksbehandler.navn, saksbehandler.epost, saksbehandler.ident)
        opprettArbeidsgiver()
        opprettBehandling(
            vedtaksperiodeId = vedtaksperiodeId.value,
            spleisBehandlingId = spleisBehandlingId.value,
            fødselsnummer = fødselsnummer,
        )
        nyttVarsel(
            id = varselId.value,
            vedtaksperiodeId = vedtaksperiodeId.value,
            spleisBehandlingId = spleisBehandlingId.value,
            saksbehandlerSomEndretId = saksbehandler.ident,
            endretTidspunkt = LocalDateTime.now(),
            status = status.name,
            kode = "RV_IV_1",
            opprettet = LocalDateTime.now(),
            definisjonRef = opprettVarseldefinisjon(kode = "RV_IV_1", definisjonId = varseldefinisjonId.value),
        )

        // when
        val funnet = repository.finn(varselId)

        // then
        assertNotNull(funnet)
        assertEquals(varselId, funnet.id)
        assertEquals(status, funnet.status)
        assertEquals("RV_IV_1", funnet.kode)
        assertEquals(spleisBehandlingId, funnet.spleisBehandlingId)
        assertNull(funnet.vurdering)
    }

    @Test
    fun `finn varsler for gitt behandling`() {
        // given
        val vedtaksperiodeId = VedtaksperiodeId(UUID.randomUUID())
        val spleisBehandlingId = SpleisBehandlingId(UUID.randomUUID())
        val varselId = VarselId(UUID.randomUUID())
        val fødselsnummer = opprettPerson().id.value
        opprettArbeidsgiver()
        opprettBehandling(
            vedtaksperiodeId = vedtaksperiodeId.value,
            spleisBehandlingId = spleisBehandlingId.value,
            fødselsnummer = fødselsnummer,
        )
        nyttVarsel(
            id = varselId.value,
            vedtaksperiodeId = vedtaksperiodeId.value,
            spleisBehandlingId = spleisBehandlingId.value,
            saksbehandlerSomEndretId = null,
            endretTidspunkt = null,
        )

        // when
        val funnet = repository.finnVarsler(listOf(spleisBehandlingId))

        // then
        assertEquals(1, funnet.size)
        assertEquals(varselId, funnet.first().id)
        assertEquals(Varsel.Status.AKTIV, funnet.first().status)
    }

    @Test
    fun `finn varsel med vurdering for gitt behandling`() {
        // given
        val vedtaksperiodeId = VedtaksperiodeId(UUID.randomUUID())
        val spleisBehandlingId = SpleisBehandlingId(UUID.randomUUID())
        val varselId = VarselId(UUID.randomUUID())
        val fødselsnummer = opprettPerson().id.value
        val saksbehandlerIdent = lagSaksbehandler().ident
        val saksbehandlerOid = SaksbehandlerOid(opprettSaksbehandler(ident = saksbehandlerIdent))
        val varseldefinisjonId = VarseldefinisjonId(UUID.randomUUID())

        opprettArbeidsgiver()
        opprettBehandling(
            vedtaksperiodeId = vedtaksperiodeId.value,
            spleisBehandlingId = spleisBehandlingId.value,
            fødselsnummer = fødselsnummer,
        )

        nyttVarsel(
            id = varselId.value,
            vedtaksperiodeId = vedtaksperiodeId.value,
            spleisBehandlingId = spleisBehandlingId.value,
            saksbehandlerSomEndretId = saksbehandlerIdent,
            endretTidspunkt = LocalDateTime.now(),
            status = "VURDERT",
            definisjonRef = opprettVarseldefinisjon(kode = "RV_IV_1", definisjonId = varseldefinisjonId.value),
        )

        // when
        val funnet = repository.finnVarsler(listOf(spleisBehandlingId))

        // then
        assertEquals(1, funnet.size)
        assertEquals(varselId, funnet.first().id)
        assertEquals(Varsel.Status.VURDERT, funnet.first().status)
        assertEquals(saksbehandlerOid, funnet.first().vurdering?.saksbehandlerId)
    }

    @Test
    fun `finn varsel med vurdering for gitt behandling vha behandlingUnikId`() {
        // given
        val vedtaksperiodeId = VedtaksperiodeId(UUID.randomUUID())
        val spleisBehandlingId = SpleisBehandlingId(UUID.randomUUID())
        val varselId = VarselId(UUID.randomUUID())
        val fødselsnummer = opprettPerson().id.value
        val saksbehandlerIdent = lagSaksbehandler().ident
        val saksbehandlerOid = SaksbehandlerOid(opprettSaksbehandler(ident = saksbehandlerIdent))
        val varseldefinisjonId = VarseldefinisjonId(UUID.randomUUID())

        opprettArbeidsgiver()
        opprettBehandling(
            vedtaksperiodeId = vedtaksperiodeId.value,
            spleisBehandlingId = spleisBehandlingId.value,
            fødselsnummer = fødselsnummer,
        )

        val behandling = sessionContext.behandlingRepository.finn(spleisBehandlingId) ?: error("Fant ikke behandling")

        nyttVarsel(
            id = varselId.value,
            vedtaksperiodeId = vedtaksperiodeId.value,
            spleisBehandlingId = spleisBehandlingId.value,
            saksbehandlerSomEndretId = saksbehandlerIdent,
            endretTidspunkt = LocalDateTime.now(),
            status = "VURDERT",
            definisjonRef = opprettVarseldefinisjon(kode = "RV_IV_1", definisjonId = varseldefinisjonId.value),
        )

        // when
        val funnet = repository.finnVarslerFor(behandling.id)

        // then
        assertEquals(1, funnet.size)
        assertEquals(varselId, funnet.first().id)
        assertEquals(Varsel.Status.VURDERT, funnet.first().status)
        assertEquals(saksbehandlerOid, funnet.first().vurdering?.saksbehandlerId)
    }

    @Test
    fun `finn varsel med vurdering for flere behandlinger vha behandlingUnikId`() {
        // given
        val vedtaksperiodeId = VedtaksperiodeId(UUID.randomUUID())
        val spleisBehandlingId1 = SpleisBehandlingId(UUID.randomUUID())
        val spleisBehandlingId2 = SpleisBehandlingId(UUID.randomUUID())
        val fødselsnummer = opprettPerson().id.value
        val saksbehandlerIdent = lagSaksbehandler().ident
        opprettSaksbehandler(ident = saksbehandlerIdent)

        opprettArbeidsgiver()
        opprettBehandling(
            vedtaksperiodeId = vedtaksperiodeId.value,
            spleisBehandlingId = spleisBehandlingId1.value,
            fødselsnummer = fødselsnummer,
        )
        opprettBehandling(
            vedtaksperiodeId = vedtaksperiodeId.value,
            spleisBehandlingId = spleisBehandlingId2.value,
            fødselsnummer = fødselsnummer,
        )

        val behandling1 = sessionContext.behandlingRepository.finn(spleisBehandlingId1) ?: error("Fant ikke behandling")
        val behandling2 = sessionContext.behandlingRepository.finn(spleisBehandlingId2) ?: error("Fant ikke behandling")

        nyttVarsel(
            id = VarselId(UUID.randomUUID()).value,
            vedtaksperiodeId = vedtaksperiodeId.value,
            spleisBehandlingId = spleisBehandlingId1.value,
            saksbehandlerSomEndretId = saksbehandlerIdent,
            endretTidspunkt = LocalDateTime.now(),
            status = "VURDERT",
            definisjonRef = opprettVarseldefinisjon(kode = "RV_IV_1", definisjonId = VarseldefinisjonId(UUID.randomUUID()).value),
        )
        nyttVarsel(
            id = VarselId(UUID.randomUUID()).value,
            vedtaksperiodeId = vedtaksperiodeId.value,
            spleisBehandlingId = spleisBehandlingId2.value,
            saksbehandlerSomEndretId = saksbehandlerIdent,
            endretTidspunkt = LocalDateTime.now(),
            status = "VURDERT",
            definisjonRef = opprettVarseldefinisjon(kode = "RV_IV_2", definisjonId = VarseldefinisjonId(UUID.randomUUID()).value),
        )

        // when
        val funnet = repository.finnVarslerFor(listOf(behandling1.id, behandling2.id))

        // then
        assertEquals(2, funnet.size)
    }

    @Test
    fun `finn varsel for flere behandlinger`() {
        // given
        val vedtaksperiodeId1 = VedtaksperiodeId(UUID.randomUUID())
        val spleisBehandlingId1 = SpleisBehandlingId(UUID.randomUUID())
        val varselId1 = VarselId(UUID.randomUUID())
        val vedtaksperiodeId2 = VedtaksperiodeId(UUID.randomUUID())
        val spleisBehandlingId2 = SpleisBehandlingId(UUID.randomUUID())
        val varselId2 = VarselId(UUID.randomUUID())
        val fødselsnummer = opprettPerson().id.value
        opprettArbeidsgiver()
        opprettBehandling(
            vedtaksperiodeId = vedtaksperiodeId1.value,
            spleisBehandlingId = spleisBehandlingId1.value,
            fødselsnummer = fødselsnummer,
        )
        opprettBehandling(
            vedtaksperiodeId = vedtaksperiodeId2.value,
            spleisBehandlingId = spleisBehandlingId2.value,
            fødselsnummer = fødselsnummer,
        )
        nyttVarsel(id = varselId1.value, vedtaksperiodeId = vedtaksperiodeId1.value, spleisBehandlingId = spleisBehandlingId1.value)
        nyttVarsel(id = varselId2.value, vedtaksperiodeId = vedtaksperiodeId2.value, spleisBehandlingId = spleisBehandlingId2.value)

        // when
        val funnet = repository.finnVarsler(listOf(spleisBehandlingId1, spleisBehandlingId2))

        // then
        assertEquals(2, funnet.size)
        assertContains(funnet.map { it.id }, varselId1)
        assertContains(funnet.map { it.id }, varselId2)
        assertEquals(Varsel.Status.AKTIV, funnet.find { it.id == varselId1 }?.status)
        assertEquals(Varsel.Status.AKTIV, funnet.find { it.id == varselId2 }?.status)
    }

    @Test
    fun `lagre varsel med vurdering`() {
        // given
        val vedtaksperiodeId = VedtaksperiodeId(UUID.randomUUID())
        val spleisBehandlingId = SpleisBehandlingId(UUID.randomUUID())
        val varselId = VarselId(UUID.randomUUID())
        val fødselsnummer = opprettPerson().id.value
        val saksbehandlerIdent = lagSaksbehandler().ident
        val saksbehandlerId = SaksbehandlerOid(opprettSaksbehandler(ident = saksbehandlerIdent))
        val definisjonId = VarseldefinisjonId(UUID.randomUUID())
        opprettArbeidsgiver()
        opprettBehandling(
            vedtaksperiodeId = vedtaksperiodeId.value,
            spleisBehandlingId = spleisBehandlingId.value,
            fødselsnummer = fødselsnummer,
        )
        nyttVarsel(
            id = varselId.value,
            vedtaksperiodeId = vedtaksperiodeId.value,
            spleisBehandlingId = spleisBehandlingId.value,
            saksbehandlerSomEndretId = saksbehandlerIdent,
            status = "AKTIV",
            definisjonRef = null,
        )

        opprettVarseldefinisjon(kode = "RV_IV_1", definisjonId = definisjonId.value)
        // when
        val funnet = repository.finnVarsler(listOf(spleisBehandlingId)).single()
        funnet.vurder(saksbehandlerId, definisjonId)
        repository.lagre(funnet)

        // then
        val oppdatert = repository.finnVarsler(listOf(spleisBehandlingId)).single()
        assertEquals(Varsel.Status.VURDERT, funnet.status)
        assertEquals(saksbehandlerId, oppdatert.vurdering?.saksbehandlerId)
        assertEquals(definisjonId, oppdatert.vurdering?.vurdertDefinisjonId)
    }

    @Test
    fun `lagre varsel uten vurdering`() {
        // given
        val vedtaksperiodeId = lagVedtaksperiodeId()
        val spleisBehandlingId = lagSpleisBehandlingId()
        val varselId = lagVarselId()
        val fødselsnummer = opprettPerson().id.value
        val saksbehandlerIdent = lagSaksbehandler().ident
        val saksbehandlerId = SaksbehandlerOid(opprettSaksbehandler(ident = saksbehandlerIdent))
        val definisjonId = lagVarseldefinisjonId()
        opprettArbeidsgiver()
        opprettBehandling(
            vedtaksperiodeId = vedtaksperiodeId.value,
            spleisBehandlingId = spleisBehandlingId.value,
            fødselsnummer = fødselsnummer,
        )
        nyttVarsel(
            id = varselId.value,
            vedtaksperiodeId = vedtaksperiodeId.value,
            spleisBehandlingId = spleisBehandlingId.value,
            saksbehandlerSomEndretId = saksbehandlerIdent,
            status = "AKTIV",
            definisjonRef = null,
            kode = "RV_IV_1",
        )

        opprettVarseldefinisjon(kode = "RV_IV_1", definisjonId = definisjonId.value)
        // when
        val funnet = repository.finn(varselId) ?: error("Fant ikke varsel")
        funnet.vurder(saksbehandlerId, definisjonId)
        repository.lagre(funnet)
        funnet.slettVurdering()
        repository.lagre(funnet)

        // then
        val oppdatert = repository.finn(funnet.id) ?: error("Fant ikke varsel")
        assertEquals(Varsel.Status.AKTIV, funnet.status)
        assertEquals(null, oppdatert.vurdering)
    }
}
