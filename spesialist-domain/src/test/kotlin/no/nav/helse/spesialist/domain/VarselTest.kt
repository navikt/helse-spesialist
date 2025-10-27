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
    fun `varsel kan godkjennes`() {
        // given
        val varsel = Varsel.fraLagring(
            id = VarselId(value = UUID.randomUUID()),
            spleisBehandlingId = SpleisBehandlingId(UUID.randomUUID()),
            status = Varsel.Status.VURDERT
        )

        // then
        assertTrue(varsel.kanGodkjennes())
    }

    @ParameterizedTest
    @EnumSource(Varsel.Status::class, names = ["VURDERT"], mode = EnumSource.Mode.EXCLUDE)
    fun `varsel kan ikke godkjennes`(status: Varsel.Status) {
        // given
        val varsel = Varsel.fraLagring(
            id = VarselId(value = UUID.randomUUID()),
            spleisBehandlingId = SpleisBehandlingId(UUID.randomUUID()),
            status = status
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
            status = Varsel.Status.VURDERT
        )
        // given
        varsel.godkjenn()

        // then
        assertEquals(Varsel.Status.GODKJENT, varsel.status)
    }

    @Test
    fun `kan ikke godkjenne varsel som ikke er vurdert`() {
        // given
        val varsel = Varsel.fraLagring(
            id = VarselId(value = UUID.randomUUID()),
            spleisBehandlingId = SpleisBehandlingId(UUID.randomUUID()),
            status = Varsel.Status.AKTIV
        )

        // then
        assertThrows<IllegalStateException> {
            varsel.godkjenn()
        }
    }

}
