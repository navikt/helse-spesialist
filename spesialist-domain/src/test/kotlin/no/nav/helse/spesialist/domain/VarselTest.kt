package no.nav.helse.spesialist.domain

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

class VarselTest {

    @Test
    fun `varsel kan godkjennes hvis det er vurdert`() {
        // given
        val varsel = Varsel.fraLagring(
            id = VarselId(value = UUID.randomUUID()),
            spleisBehandlingId = SpleisBehandlingId(UUID.randomUUID()),
            status = Varsel.Status.VURDERT,
            vurdering = null,
            kode = "RV_IV_2"
        )

        // then
        assertTrue(varsel.kanGodkjennes())
    }

    @ParameterizedTest
    @EnumSource(Varsel.Status::class, names = ["VURDERT"], mode = EnumSource.Mode.EXCLUDE)
    fun `varsel kan ikke godkjennes hvis det har feil status`(status: Varsel.Status) {
        // given
        val varsel = Varsel.fraLagring(
            id = VarselId(value = UUID.randomUUID()),
            spleisBehandlingId = SpleisBehandlingId(UUID.randomUUID()),
            status = status,
            vurdering = null,
            kode = "RV_IV_2"
        )

        // then
        assertFalse(varsel.kanGodkjennes())
    }

    @Test
    fun `godkjenn varsel`() {
        // given
        val varsel = Varsel.fraLagring(
            id = VarselId(value = UUID.randomUUID()),
            spleisBehandlingId = SpleisBehandlingId(UUID.randomUUID()),
            status = Varsel.Status.VURDERT,
            vurdering = null,
            kode = "RV_IV_2"
        )
        val saksbehandlerId = SaksbehandlerOid(UUID.randomUUID())

        // given
        varsel.godkjenn(saksbehandlerId)

        // then
        assertEquals(Varsel.Status.GODKJENT, varsel.status)
        assertEquals(saksbehandlerId, varsel.vurdering?.saksbehandlerId)
    }

    @Test
    fun `kan ikke godkjenne varsel som ikke er vurdert`() {
        // given
        val varsel = Varsel.fraLagring(
            id = VarselId(value = UUID.randomUUID()),
            spleisBehandlingId = SpleisBehandlingId(UUID.randomUUID()),
            status = Varsel.Status.AKTIV,
            vurdering = null,
            kode = "RV_IV_2"
        )

        // then
        assertThrows<IllegalStateException> {
            varsel.godkjenn(SaksbehandlerOid(UUID.randomUUID()))
        }
    }
}
