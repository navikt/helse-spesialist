package no.nav.helse.spesialist.domain

import no.nav.helse.Varselvurdering
import no.nav.helse.spesialist.domain.testfixtures.lagBehandlingUnikId
import no.nav.helse.spesialist.domain.testfixtures.lagSpleisBehandlingId
import no.nav.helse.spesialist.domain.testfixtures.lagVarsel
import no.nav.helse.spesialist.domain.testfixtures.lagVarseldefinisjon
import no.nav.helse.spesialist.domain.testfixtures.lagVarseldefinisjonId
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagSaksbehandler
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.time.LocalDateTime
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

class VarselTest {

    @ParameterizedTest
    @EnumSource(Varsel.Status::class, names = ["AKTIV"], mode = EnumSource.Mode.EXCLUDE)
    fun `varsel kan ikke vurderes hvis det ikke er aktivt`(status: Varsel.Status) {
        // given
        val varsel =
            lagVarsel(behandlingUnikId = lagBehandlingUnikId(), spleisBehandlingId = lagSpleisBehandlingId(), status = status)

        // when
        val kanVurderes = varsel.kanVurderes()

        // then
        assertEquals(false, kanVurderes)
    }

    @Test
    fun `varsel kan vurderes hvis det er aktivt`() {
        // given
        val varsel =
            lagVarsel(behandlingUnikId = lagBehandlingUnikId(), spleisBehandlingId = lagSpleisBehandlingId(), status = Varsel.Status.AKTIV)

        // when
        val kanVurderes = varsel.kanVurderes()

        // then
        assertEquals(true, kanVurderes)
    }

    @Test
    fun `varsel kan vurderes`() {
        // given
        val definisjon = lagVarseldefinisjon()
        val saksbehandler = lagSaksbehandler()
        val varsel = lagVarsel(behandlingUnikId = lagBehandlingUnikId(), spleisBehandlingId = lagSpleisBehandlingId())

        // when
        varsel.vurder(saksbehandler.id, definisjon.id)

        // then
        assertEquals(Varsel.Status.VURDERT, varsel.status)
        assertEquals(saksbehandler.id, varsel.vurdering?.saksbehandlerId)
        assertEquals(definisjon.id, varsel.vurdering?.vurdertDefinisjonId)
    }

    @Test
    fun `varsel kan godkjennes hvis det er vurdert`() {
        // given
        val varsel = Varsel.fraLagring(
            id = VarselId(value = UUID.randomUUID()),
            spleisBehandlingId = SpleisBehandlingId(UUID.randomUUID()),
            behandlingUnikId = BehandlingUnikId(UUID.randomUUID()),
            status = Varsel.Status.VURDERT,
            vurdering = null,
            kode = "RV_IV_2",
            opprettetTidspunkt = LocalDateTime.now(),
        )

        // then
        assertTrue(varsel.kanGodkjennes())
    }

    @Test
    fun `varsel mangler vurdering dersom det er aktivt`() {
        // given
        val varsel = Varsel.fraLagring(
            id = VarselId(value = UUID.randomUUID()),
            spleisBehandlingId = SpleisBehandlingId(UUID.randomUUID()),
            behandlingUnikId = BehandlingUnikId(UUID.randomUUID()),
            status = Varsel.Status.AKTIV,
            vurdering = null,
            kode = "RV_IV_2",
            opprettetTidspunkt = LocalDateTime.now(),
        )

        // then
        assertTrue(varsel.manglerVurdering())
    }

    @Test
    fun `varsel mangler ikke vurdering`() {
        // given
        val varsel = Varsel.fraLagring(
            id = VarselId(value = UUID.randomUUID()),
            spleisBehandlingId = SpleisBehandlingId(UUID.randomUUID()),
            behandlingUnikId = BehandlingUnikId(UUID.randomUUID()),
            status = Varsel.Status.VURDERT,
            vurdering = Varselvurdering(
                lagSaksbehandler().id,
                LocalDateTime.now(),
                lagVarseldefinisjonId()
            ),
            kode = "RV_IV_2",
            opprettetTidspunkt = LocalDateTime.now(),
        )

        // when, then
        assertFalse(varsel.manglerVurdering())
    }

    @ParameterizedTest
    @EnumSource(Varsel.Status::class, names = ["VURDERT"], mode = EnumSource.Mode.EXCLUDE)
    fun `varsel kan ikke godkjennes hvis det har feil status`(status: Varsel.Status) {
        // given
        val varsel = Varsel.fraLagring(
            id = VarselId(value = UUID.randomUUID()),
            spleisBehandlingId = SpleisBehandlingId(UUID.randomUUID()),
            behandlingUnikId = BehandlingUnikId(UUID.randomUUID()),
            status = status,
            vurdering = null,
            kode = "RV_IV_2",
            opprettetTidspunkt = LocalDateTime.now(),
        )

        // then
        assertFalse(varsel.kanGodkjennes())
    }

    @Test
    fun `godkjenn varsel`() {
        // given
        val saksbehandlerSomVurderteVarselet = SaksbehandlerOid(UUID.randomUUID())
        val varsel = Varsel.fraLagring(
            id = VarselId(value = UUID.randomUUID()),
            spleisBehandlingId = SpleisBehandlingId(UUID.randomUUID()),
            behandlingUnikId = BehandlingUnikId(UUID.randomUUID()),
            status = Varsel.Status.VURDERT,
            vurdering = Varselvurdering(
                saksbehandlerId = saksbehandlerSomVurderteVarselet,
                tidspunkt = LocalDateTime.now(),
                vurdertDefinisjonId = VarseldefinisjonId(UUID.randomUUID()),
            ),
            kode = "RV_IV_2",
            opprettetTidspunkt = LocalDateTime.now(),
        )

        // given
        varsel.godkjenn()

        // then
        assertEquals(Varsel.Status.GODKJENT, varsel.status)
        assertEquals(saksbehandlerSomVurderteVarselet, varsel.vurdering?.saksbehandlerId)
    }

    @Test
    fun `kan ikke godkjenne varsel som ikke er vurdert`() {
        // given
        val varsel = Varsel.fraLagring(
            id = VarselId(value = UUID.randomUUID()),
            spleisBehandlingId = SpleisBehandlingId(UUID.randomUUID()),
            behandlingUnikId = BehandlingUnikId(UUID.randomUUID()),
            status = Varsel.Status.AKTIV,
            vurdering = null,
            kode = "RV_IV_2",
            opprettetTidspunkt = LocalDateTime.now(),
        )

        // then
        assertThrows<IllegalStateException> {
            varsel.godkjenn()
        }
    }
}
