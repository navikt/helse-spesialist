package no.nav.helse.spesialist.domain

import no.nav.helse.Varselvurdering
import no.nav.helse.spesialist.domain.Varsel.Status
import no.nav.helse.spesialist.domain.testfixtures.lagBehandlingUnikId
import no.nav.helse.spesialist.domain.testfixtures.lagSpleisBehandlingId
import no.nav.helse.spesialist.domain.testfixtures.lagVarsel
import no.nav.helse.spesialist.domain.testfixtures.lagVarseldefinisjon
import no.nav.helse.spesialist.domain.testfixtures.lagVarseldefinisjonId
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagSaksbehandler
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.time.LocalDateTime
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class VarselTest {
    @ParameterizedTest
    @EnumSource(Status::class, names = ["AKTIV"], mode = EnumSource.Mode.EXCLUDE)
    fun `varsel kan ikke vurderes hvis det ikke er aktivt`(status: Status) {
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
            lagVarsel(behandlingUnikId = lagBehandlingUnikId(), spleisBehandlingId = lagSpleisBehandlingId(), status = Status.AKTIV)

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
        assertEquals(Status.VURDERT, varsel.status)
        assertEquals(saksbehandler.id, varsel.vurdering?.saksbehandlerId)
        assertEquals(definisjon.id, varsel.vurdering?.vurdertDefinisjonId)
    }

    @Test
    fun `varsel kan godkjennes hvis det er vurdert`() {
        // given
        val varsel =
            Varsel.fraLagring(
                id = VarselId(value = UUID.randomUUID()),
                spleisBehandlingId = SpleisBehandlingId(UUID.randomUUID()),
                behandlingUnikId = BehandlingUnikId(UUID.randomUUID()),
                status = Status.VURDERT,
                vurdering = null,
                kode = "RV_IV_2",
                opprettetTidspunkt = LocalDateTime.now(),
            )

        // then
        assertTrue(varsel.kanGodkjennes())
    }

    @ParameterizedTest
    @EnumSource(Status::class, names = ["AKTIV", "GODKJENT", "VURDERT"])
    fun `varsel trenger vurdering dersom vurdering ikke eksisterer og varselet har tilstand`(status: Status) {
        // given
        val varsel =
            Varsel.fraLagring(
                id = VarselId(value = UUID.randomUUID()),
                spleisBehandlingId = SpleisBehandlingId(UUID.randomUUID()),
                behandlingUnikId = BehandlingUnikId(UUID.randomUUID()),
                status = status,
                vurdering = null,
                kode = "RV_IV_2",
                opprettetTidspunkt = LocalDateTime.now(),
            )

        // then
        assertTrue(varsel.trengerVurdering())
    }

    @ParameterizedTest
    @EnumSource(Status::class, names = ["INAKTIV", "AVVIST", "AVVIKLET"])
    fun `varsel trenger ikke vurdering dersom vurdering ikke eksisterer og varselet har tilstand`(status: Status) {
        // given
        val varsel =
            Varsel.fraLagring(
                id = VarselId(value = UUID.randomUUID()),
                spleisBehandlingId = SpleisBehandlingId(UUID.randomUUID()),
                behandlingUnikId = BehandlingUnikId(UUID.randomUUID()),
                status = status,
                vurdering = null,
                kode = "RV_IV_2",
                opprettetTidspunkt = LocalDateTime.now(),
            )

        // then
        assertFalse(varsel.trengerVurdering())
    }

    @ParameterizedTest
    @EnumSource(Status::class)
    fun `varsel trenger ikke vurdering dersom vurdering eksisterer og varselet har tilstand`(status: Status) {
        // given
        val varsel =
            Varsel.fraLagring(
                id = VarselId(value = UUID.randomUUID()),
                spleisBehandlingId = SpleisBehandlingId(UUID.randomUUID()),
                behandlingUnikId = BehandlingUnikId(UUID.randomUUID()),
                status = status,
                vurdering =
                    Varselvurdering(
                        lagSaksbehandler().id,
                        LocalDateTime.now(),
                        lagVarseldefinisjonId(),
                    ),
                kode = "RV_IV_2",
                opprettetTidspunkt = LocalDateTime.now(),
            )

        // then
        assertFalse(varsel.trengerVurdering())
    }

    @ParameterizedTest
    @EnumSource(Status::class, names = ["VURDERT", "AKTIV"], mode = EnumSource.Mode.EXCLUDE)
    fun `varsel kan ikke avvises dersom varselet har tilstand`(status: Status) {
        // given
        val varsel =
            Varsel.fraLagring(
                id = VarselId(value = UUID.randomUUID()),
                spleisBehandlingId = SpleisBehandlingId(UUID.randomUUID()),
                behandlingUnikId = BehandlingUnikId(UUID.randomUUID()),
                status = status,
                kode = "RV_IV_2",
                opprettetTidspunkt = LocalDateTime.now(),
                vurdering = null,
            )

        // then
        assertFalse(varsel.kanAvvises())
    }

    @ParameterizedTest
    @EnumSource(Status::class, names = ["VURDERT", "AKTIV"], mode = EnumSource.Mode.INCLUDE)
    fun `varsel kan avvises dersom varselet har tilstand`(status: Status) {
        // given
        val varsel =
            Varsel.fraLagring(
                id = VarselId(value = UUID.randomUUID()),
                spleisBehandlingId = SpleisBehandlingId(UUID.randomUUID()),
                behandlingUnikId = BehandlingUnikId(UUID.randomUUID()),
                status = status,
                kode = "RV_IV_2",
                opprettetTidspunkt = LocalDateTime.now(),
                vurdering = null,
            )

        // then
        assertTrue(varsel.kanAvvises())
    }

    @ParameterizedTest
    @EnumSource(Status::class, names = ["VURDERT", "AKTIV"], mode = EnumSource.Mode.EXCLUDE)
    fun `error ved forsøk på å avvise når varselet har tilstand`(status: Status) {
        // given
        val varsel =
            Varsel.fraLagring(
                id = VarselId(value = UUID.randomUUID()),
                spleisBehandlingId = SpleisBehandlingId(UUID.randomUUID()),
                behandlingUnikId = BehandlingUnikId(UUID.randomUUID()),
                status = status,
                kode = "RV_IV_2",
                opprettetTidspunkt = LocalDateTime.now(),
                vurdering = null,
            )

        // then
        assertThrows<IllegalStateException> {
            varsel.avvis()
        }
    }

    @ParameterizedTest
    @EnumSource(Status::class, names = ["VURDERT", "AKTIV"], mode = EnumSource.Mode.INCLUDE)
    fun `ikke error ved forsøk på å avvise når varselet har tilstand`(status: Status) {
        // given
        val varsel =
            Varsel.fraLagring(
                id = VarselId(value = UUID.randomUUID()),
                spleisBehandlingId = SpleisBehandlingId(UUID.randomUUID()),
                behandlingUnikId = BehandlingUnikId(UUID.randomUUID()),
                status = status,
                kode = "RV_IV_2",
                opprettetTidspunkt = LocalDateTime.now(),
                vurdering = null,
            )

        // then
        assertDoesNotThrow {
            varsel.avvis()
        }
    }

    @ParameterizedTest
    @EnumSource(Status::class, names = ["VURDERT"], mode = EnumSource.Mode.EXCLUDE)
    fun `varsel kan ikke godkjennes hvis det har feil status`(status: Status) {
        // given
        val varsel =
            Varsel.fraLagring(
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
    fun `deaktiver aktivt varsel`() {
        // given
        val varsel =
            Varsel.fraLagring(
                id = VarselId(value = UUID.randomUUID()),
                spleisBehandlingId = SpleisBehandlingId(UUID.randomUUID()),
                behandlingUnikId = BehandlingUnikId(UUID.randomUUID()),
                status = Status.AKTIV,
                vurdering = null,
                kode = "RV_IV_2",
                opprettetTidspunkt = LocalDateTime.now(),
            )

        // when
        varsel.deaktiver()

        // then
        assertEquals(Status.INAKTIV, varsel.status)
    }

    @ParameterizedTest
    @EnumSource(Status::class, names = ["AKTIV"], mode = EnumSource.Mode.EXCLUDE)
    fun `kan ikke deaktivere varsel som har status`(status: Status) {
        // given
        val varsel =
            Varsel.fraLagring(
                id = VarselId(value = UUID.randomUUID()),
                spleisBehandlingId = SpleisBehandlingId(UUID.randomUUID()),
                behandlingUnikId = BehandlingUnikId(UUID.randomUUID()),
                status = status,
                vurdering = null,
                kode = "RV_IV_2",
                opprettetTidspunkt = LocalDateTime.now(),
            )

        // then
        assertFailsWith<IllegalStateException> {
            // when
            varsel.deaktiver()
        }
    }

    @Test
    fun `reaktiver inaktivt varsel`() {
        // given
        val varsel =
            Varsel.fraLagring(
                id = VarselId(value = UUID.randomUUID()),
                spleisBehandlingId = SpleisBehandlingId(UUID.randomUUID()),
                behandlingUnikId = BehandlingUnikId(UUID.randomUUID()),
                status = Status.INAKTIV,
                vurdering =
                    Varselvurdering(
                        lagSaksbehandler().id,
                        LocalDateTime.now(),
                        lagVarseldefinisjon(kode = "RV_IV_2").id,
                    ),
                kode = "RV_IV_2",
                opprettetTidspunkt = LocalDateTime.now(),
            )

        // when
        varsel.reaktiver()

        // then
        assertEquals(Status.AKTIV, varsel.status)
        assertEquals(null, varsel.vurdering)
    }

    @ParameterizedTest
    @EnumSource(Status::class, names = ["INAKTIV"], mode = EnumSource.Mode.EXCLUDE)
    fun `kan ikke reaktivere varsel som har status`(status: Status) {
        // given
        val varsel =
            Varsel.fraLagring(
                id = VarselId(value = UUID.randomUUID()),
                spleisBehandlingId = SpleisBehandlingId(UUID.randomUUID()),
                behandlingUnikId = BehandlingUnikId(UUID.randomUUID()),
                status = status,
                vurdering = null,
                kode = "RV_IV_2",
                opprettetTidspunkt = LocalDateTime.now(),
            )

        // then
        assertFailsWith<IllegalStateException> {
            // when
            varsel.reaktiver()
        }
    }

    @Test
    fun `flytt aktivt varsel til ny behandling`() {
        // given
        val nyBehandlingUnikId = lagBehandlingUnikId()
        val nySpleisBehandlingId = lagSpleisBehandlingId()
        val varsel =
            Varsel.fraLagring(
                id = VarselId(value = UUID.randomUUID()),
                spleisBehandlingId = SpleisBehandlingId(UUID.randomUUID()),
                behandlingUnikId = BehandlingUnikId(UUID.randomUUID()),
                status = Status.AKTIV,
                vurdering = null,
                kode = "RV_IV_2",
                opprettetTidspunkt = LocalDateTime.now(),
            )

        // when
        varsel.flyttTil(nyBehandlingUnikId, nySpleisBehandlingId)

        // then
        assertEquals(nyBehandlingUnikId, varsel.behandlingUnikId)
        assertEquals(nySpleisBehandlingId, varsel.spleisBehandlingId)
    }

    @ParameterizedTest
    @EnumSource(Status::class, names = ["AKTIV"], mode = EnumSource.Mode.EXCLUDE)
    fun `kan ikke flytte varsel til ny behandling som har status`(status: Status) {
        // given
        val nyBehandlingUnikId = lagBehandlingUnikId()
        val nySpleisBehandlingId = lagSpleisBehandlingId()
        val varsel =
            Varsel.fraLagring(
                id = VarselId(value = UUID.randomUUID()),
                spleisBehandlingId = SpleisBehandlingId(UUID.randomUUID()),
                behandlingUnikId = BehandlingUnikId(UUID.randomUUID()),
                status = status,
                vurdering = null,
                kode = "RV_IV_2",
                opprettetTidspunkt = LocalDateTime.now(),
            )

        // then
        assertFailsWith<IllegalStateException> {
            // when
            varsel.flyttTil(nyBehandlingUnikId, nySpleisBehandlingId)
        }
    }

    @Test
    fun `godkjenn varsel`() {
        // given
        val saksbehandlerSomVurderteVarselet = SaksbehandlerOid(UUID.randomUUID())
        val varsel =
            Varsel.fraLagring(
                id = VarselId(value = UUID.randomUUID()),
                spleisBehandlingId = SpleisBehandlingId(UUID.randomUUID()),
                behandlingUnikId = BehandlingUnikId(UUID.randomUUID()),
                status = Status.VURDERT,
                vurdering =
                    Varselvurdering(
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
        assertEquals(Status.GODKJENT, varsel.status)
        assertEquals(saksbehandlerSomVurderteVarselet, varsel.vurdering?.saksbehandlerId)
    }

    @Test
    fun `kan ikke godkjenne varsel som ikke er vurdert`() {
        // given
        val varsel =
            Varsel.fraLagring(
                id = VarselId(value = UUID.randomUUID()),
                spleisBehandlingId = SpleisBehandlingId(UUID.randomUUID()),
                behandlingUnikId = BehandlingUnikId(UUID.randomUUID()),
                status = Status.AKTIV,
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
