package no.nav.helse.modell.person.vedtaksperiode

import no.nav.helse.modell.person.vedtaksperiode.LegacyVarsel.Status.AKTIV
import no.nav.helse.modell.person.vedtaksperiode.Varselkode.SB_EX_1
import no.nav.helse.modell.vedtaksperiode.Yrkesaktivitetstype
import no.nav.helse.spesialist.domain.legacy.LegacyBehandling
import no.nav.helse.spesialist.domain.legacy.LegacyBehandling.Companion.finnBehandlingForVedtaksperiode
import no.nav.helse.spesialist.domain.testfixtures.des
import no.nav.helse.spesialist.domain.testfixtures.feb
import no.nav.helse.spesialist.domain.testfixtures.jan
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

internal class LegacyBehandlingTest {
    @Test
    fun `behandling ligger før dato`() {
        val legacyBehandling =
            LegacyBehandling(
                id = UUID.randomUUID(),
                vedtaksperiodeId = UUID.randomUUID(),
                fom = 1 jan 2018,
                tom = 31 jan 2018,
                skjæringstidspunkt = 1 jan 2018,
                yrkesaktivitetstype = Yrkesaktivitetstype.ARBEIDSTAKER,
            )
        assertTrue(legacyBehandling.tilhører(31 jan 2018))
        assertTrue(legacyBehandling.tilhører(1 feb 2018))
        assertFalse(legacyBehandling.tilhører(1 jan 2018))
        assertFalse(legacyBehandling.tilhører(31 des 2017))
    }

    @Test
    fun `deaktiverer enkelt varsel basert på varselkode`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val behandling = behandling(vedtaksperiodeId = vedtaksperiodeId)
        val varsel = LegacyVarsel(UUID.randomUUID(), "SB_EX_1", LocalDateTime.now(), vedtaksperiodeId)
        behandling.håndterNyttVarsel(varsel)
        behandling.deaktiverVarsel("SB_EX_1")
        behandling.assertVarsler(0, VarselStatusDto.AKTIV, SB_EX_1)
        behandling.assertVarsler(1, VarselStatusDto.INAKTIV, SB_EX_1)
    }

    @Test
    fun `sletter varsel om avvik og legger det til på nytt hvis det finnes fra før`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val varselId = UUID.randomUUID()
        val behandling = behandling(vedtaksperiodeId = vedtaksperiodeId)
        behandling.håndterNyttVarsel(LegacyVarsel(UUID.randomUUID(), "RV_IV_2", LocalDateTime.now(), vedtaksperiodeId))
        behandling.håndterNyttVarsel(LegacyVarsel(varselId, "RV_IV_2", LocalDateTime.now(), vedtaksperiodeId))
        behandling.assertVarsler(1, VarselStatusDto.AKTIV, "RV_IV_2")
    }

    @Test
    fun `Lagrer kun én utgave av et aktivt varsel`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val behandling = behandling(vedtaksperiodeId = vedtaksperiodeId)
        behandling.håndterNyttVarsel(LegacyVarsel(UUID.randomUUID(), "SB_EX_1", LocalDateTime.now(), vedtaksperiodeId))
        behandling.håndterNyttVarsel(LegacyVarsel(UUID.randomUUID(), "SB_EX_1", LocalDateTime.now(), vedtaksperiodeId))

        behandling.assertVarsler(1, VarselStatusDto.AKTIV, SB_EX_1)
    }

    @Test
    fun `behandling kan motta ny utbetalingId`() {
        val behandling = behandling()
        val utbetalingId = UUID.randomUUID()
        behandling.håndterNyUtbetaling(utbetalingId)
    }

    @Test
    fun `behandling kan motta ny utbetalingId så lenge behandlingen ikke er ferdig behandlet`() {
        val behandling = behandling()
        val gammelUtbetalingId = UUID.randomUUID()
        val nyUtbetalingId = UUID.randomUUID()
        behandling.håndterNyUtbetaling(gammelUtbetalingId)
        behandling.håndterForkastetUtbetaling(gammelUtbetalingId)
        behandling.håndterNyUtbetaling(nyUtbetalingId)

        assertEquals(nyUtbetalingId, behandling.toDto().utbetalingId)
    }

    @Test
    fun `Lagrer varsel på behandling selvom den er ferdig behandlet`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val behandlingId = UUID.randomUUID()
        val behandling = behandling(vedtaksperiodeId = vedtaksperiodeId, behandlingId = behandlingId)
        behandling.håndterVedtakFattet()
        val varsel = LegacyVarsel(UUID.randomUUID(), "RV_IM_1", LocalDateTime.now(), vedtaksperiodeId)
        behandling.håndterNyttVarsel(varsel)
        behandling.assertVarsler(1, VarselStatusDto.AKTIV, "RV_IM_1")
    }

    @Test
    fun `Skal kunne opprette varsel på behandling`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val behandling = behandling(vedtaksperiodeId = vedtaksperiodeId)
        behandling.håndterNyttVarsel(LegacyVarsel(UUID.randomUUID(), SB_EX_1.name, LocalDateTime.now(), vedtaksperiodeId))
        behandling.assertVarsler(1, VarselStatusDto.AKTIV, SB_EX_1)
    }

    @Test
    fun `kan ikke knytte utbetalingId til ferdig behandlet behandling som har utbetalingId fra før`() {
        val behandling = behandling()
        val gammelUtbetalingId = UUID.randomUUID()
        val nyUtbetalingId = UUID.randomUUID()
        behandling.håndterNyUtbetaling(gammelUtbetalingId)
        behandling.håndterVedtakFattet()
        behandling.håndterNyUtbetaling(nyUtbetalingId)

        assertEquals(gammelUtbetalingId, behandling.toDto().utbetalingId)
    }

    @Test
    fun `kan ikke knytte utbetalingId til ferdig behandlet behandling som ikke har utbetalingId fra før`() {
        val utbetalingId = UUID.randomUUID()
        val nyUtbetalingId = UUID.randomUUID()

        val behandling = behandling()
        behandling.håndterNyUtbetaling(utbetalingId)

        behandling.håndterVedtakFattet()
        behandling.håndterNyUtbetaling(nyUtbetalingId)
        behandling.assertUtbetalingId(utbetalingId)
    }

    @Test
    fun `kan fjerne utbetalingId fra ubehandlet behandling`() {
        val behandling = behandling()
        val utbetalingId = UUID.randomUUID()
        behandling.håndterNyUtbetaling(utbetalingId)
        behandling.assertUtbetalingId(utbetalingId)
        behandling.håndterForkastetUtbetaling(utbetalingId)
        behandling.assertUtbetalingId(null)
    }

    @Test
    fun `kan ikke fjerne utbetalingId fra ferdig behandlet behandling`() {
        val behandling = behandling()
        val utbetalingId = UUID.randomUUID()
        behandling.håndterNyUtbetaling(utbetalingId)
        behandling.håndterVedtakFattet()
        behandling.håndterForkastetUtbetaling(utbetalingId)
        behandling.assertUtbetalingId(utbetalingId)
    }

    @Test
    fun `finn behandling med vedtaksperiodeId`() {
        val vedtaksperiodeId1 = UUID.randomUUID()
        val vedtaksperiodeId2 = UUID.randomUUID()
        val behandlingV1 = behandling(vedtaksperiodeId = vedtaksperiodeId1)
        val behandlingV2 = behandling(vedtaksperiodeId = vedtaksperiodeId2)

        assertNotNull(listOf(behandlingV1, behandlingV2).finnBehandlingForVedtaksperiode(vedtaksperiodeId1))
    }

    @Test
    fun `finner ikke behandling`() {
        val behandlingV1 = behandling()
        assertNull(listOf(behandlingV1).finnBehandlingForVedtaksperiode(UUID.randomUUID()))
    }

    @Test
    fun `oppdaterer fom, tom, skjæringstidspunkt, behandlingId`() {
        val behandlingId = UUID.randomUUID()
        val behandling = behandling(fom = 1 jan 2018, tom = 31 jan 2018, skjæringstidspunkt = 1 jan 2018)
        behandling.håndter(
            SpleisVedtaksperiode(UUID.randomUUID(), behandlingId, 2 jan 2018, 30 jan 2018, 2 jan 2018),
        )
        val dto = behandling.toDto()

        assertEquals(2 jan 2018, dto.fom)
        assertEquals(30 jan 2018, dto.tom)
        assertEquals(2 jan 2018, dto.skjæringstidspunkt)
        assertEquals(behandlingId, dto.spleisBehandlingId)
    }

    @Test
    fun `referential equals`() {
        val behandling = behandling(UUID.randomUUID(), UUID.randomUUID())
        assertEquals(behandling, behandling)
        assertEquals(behandling.hashCode(), behandling.hashCode())
    }

    @Test
    fun `structural equals`() {
        val behandlingId = UUID.randomUUID()
        val vedtaksperiodeId = UUID.randomUUID()
        val spleisBehandlingId = UUID.randomUUID()
        val behandling1 = behandling(behandlingId, vedtaksperiodeId)
        val utbetalingId = UUID.randomUUID()
        behandling1.håndterNyUtbetaling(utbetalingId)
        behandling1.oppdaterBehandlingsinformasjon(emptyList(), spleisBehandlingId, utbetalingId)
        behandling1.håndterVedtakFattet()
        val behandling2 = behandling(behandlingId, vedtaksperiodeId)
        behandling2.håndterNyUtbetaling(utbetalingId)
        behandling2.oppdaterBehandlingsinformasjon(emptyList(), spleisBehandlingId, utbetalingId)
        behandling2.håndterVedtakFattet()
        assertEquals(behandling1, behandling2)
        assertEquals(behandling1.hashCode(), behandling2.hashCode())
    }

    @Test
    fun `forskjellig behandlingIder`() {
        val behandlingId1 = UUID.randomUUID()
        val behandlingId2 = UUID.randomUUID()
        val vedtaksperiodeId = UUID.randomUUID()
        val behandling1 = behandling(behandlingId1, vedtaksperiodeId)
        val behandling2 = behandling(behandlingId2, vedtaksperiodeId)
        assertNotEquals(behandling1, behandling2)
        assertNotEquals(behandling1.hashCode(), behandling2.hashCode())
    }

    @Test
    fun `forskjellige vedtaksperiodeIder`() {
        val behandlingId = UUID.randomUUID()
        val vedtaksperiodeId1 = UUID.randomUUID()
        val vedtaksperiodeId2 = UUID.randomUUID()
        val behandling1 = behandling(behandlingId, vedtaksperiodeId1)
        val behandling2 = behandling(behandlingId, vedtaksperiodeId2)
        assertNotEquals(behandling1, behandling2)
        assertNotEquals(behandling1.hashCode(), behandling2.hashCode())
    }

    @Test
    fun `forskjellig spleisBehandlingId og tags`() {
        val behandlingId = UUID.randomUUID()
        val vedtaksperiodeId = UUID.randomUUID()
        val behandling1 = behandling(behandlingId, vedtaksperiodeId)
        val spleisBehandlingId = UUID.randomUUID()
        val utbetalingId1 = UUID.randomUUID()
        behandling1.håndterNyUtbetaling(utbetalingId1)
        behandling1.oppdaterBehandlingsinformasjon(listOf("hei"), spleisBehandlingId, utbetalingId1)
        val behandling2 = behandling(behandlingId, vedtaksperiodeId)
        val spleisBehandlingId2 = UUID.randomUUID()
        val utbetalingId2 = UUID.randomUUID()
        behandling2.håndterNyUtbetaling(utbetalingId2)
        behandling2.oppdaterBehandlingsinformasjon(listOf("hallo"), spleisBehandlingId2, utbetalingId2)
        assertNotEquals(behandling1, behandling2)
        assertNotEquals(behandling1.hashCode(), behandling2.hashCode())
    }

    @Test
    fun `forskjellig utbetalingId`() {
        val behandlingId = UUID.randomUUID()
        val vedtaksperiodeId = UUID.randomUUID()
        val behandling1 = behandling(behandlingId, vedtaksperiodeId)
        behandling1.håndterNyUtbetaling(UUID.randomUUID())
        val behandling2 = behandling(behandlingId, vedtaksperiodeId)
        behandling2.håndterNyUtbetaling(UUID.randomUUID())
        assertNotEquals(behandling1, behandling2)
        assertNotEquals(behandling1.hashCode(), behandling2.hashCode())
    }

    @Test
    fun `forskjellig utbetalingId der én behandling har null`() {
        val behandlingId = UUID.randomUUID()
        val vedtaksperiodeId = UUID.randomUUID()
        val behandling1 = behandling(behandlingId, vedtaksperiodeId)
        behandling1.håndterNyUtbetaling(UUID.randomUUID())
        val behandling2 = behandling(behandlingId, vedtaksperiodeId)
        assertNotEquals(behandling1, behandling2)
        assertNotEquals(behandling1.hashCode(), behandling2.hashCode())
    }

    @Test
    fun `forskjellig periode`() {
        val behandlingId = UUID.randomUUID()
        val vedtaksperiodeId = UUID.randomUUID()
        val behandling1 = behandling(behandlingId, vedtaksperiodeId, fom = 1 jan 2018, tom = 31 jan 2018)
        val behandling2 = behandling(behandlingId, vedtaksperiodeId, fom = 1 feb 2018, tom = 28 feb 2018)
        assertNotEquals(behandling1, behandling2)
        assertNotEquals(behandling1.hashCode(), behandling2.hashCode())
    }

    @Test
    fun `forskjellig skjæringstidspunkt`() {
        val behandlingId = UUID.randomUUID()
        val vedtaksperiodeId = UUID.randomUUID()
        val behandling1 = behandling(behandlingId, vedtaksperiodeId, skjæringstidspunkt = 1 jan 2018)
        val behandling2 = behandling(behandlingId, vedtaksperiodeId, skjæringstidspunkt = 1 feb 2018)
        assertNotEquals(behandling1, behandling2)
        assertNotEquals(behandling1.hashCode(), behandling2.hashCode())
    }

    @Test
    fun `forskjellig tilstand`() {
        val behandlingId = UUID.randomUUID()
        val vedtaksperiodeId = UUID.randomUUID()
        val behandling1 = behandling(behandlingId, vedtaksperiodeId, skjæringstidspunkt = 1 jan 2018)
        behandling1.håndterNyUtbetaling(UUID.randomUUID())
        val behandling2 = behandling(behandlingId, vedtaksperiodeId, skjæringstidspunkt = 1 jan 2018)
        assertNotEquals(behandling1, behandling2)
        assertNotEquals(behandling1.hashCode(), behandling2.hashCode())
    }

    @Test
    fun `behandling toDto`() {
        val behandlingId = UUID.randomUUID()
        val vedtaksperiodeId = UUID.randomUUID()
        val fom = 1 jan 2018
        val tom = 31 jan 2018
        val skjæringstidspunkt = 1 jan 2018
        val utbetalingId = UUID.randomUUID()
        val spleisBehandlingId = UUID.randomUUID()
        val legacyBehandling =
            LegacyBehandling(
                id = behandlingId,
                vedtaksperiodeId = vedtaksperiodeId,
                fom = fom,
                tom = tom,
                skjæringstidspunkt = skjæringstidspunkt,
                yrkesaktivitetstype = Yrkesaktivitetstype.ARBEIDSTAKER,
            )
        legacyBehandling.håndterNyUtbetaling(utbetalingId)
        val tags = listOf("tag 1")
        legacyBehandling.oppdaterBehandlingsinformasjon(tags, spleisBehandlingId, utbetalingId)

        val varselId = UUID.randomUUID()
        val opprettet = LocalDateTime.now()
        val varsel = LegacyVarsel(varselId, "SB_EX_1", opprettet, vedtaksperiodeId, AKTIV)
        legacyBehandling.håndterNyttVarsel(varsel)
        val dto = legacyBehandling.toDto()

        assertEquals(
            BehandlingDto(
                behandlingId,
                vedtaksperiodeId,
                utbetalingId,
                spleisBehandlingId,
                skjæringstidspunkt,
                fom,
                tom,
                TilstandDto.KlarTilBehandling,
                tags,
                listOf(varsel.toDto()),
                Yrkesaktivitetstype.ARBEIDSTAKER,
            ),
            dto,
        )
    }

    @Test
    fun `behandlingTilstand toDto`() {
        assertEquals(TilstandDto.VedtakFattet, LegacyBehandling.Tilstand.VedtakFattet.toDto())
        assertEquals(TilstandDto.VidereBehandlingAvklares, LegacyBehandling.Tilstand.VidereBehandlingAvklares.toDto())
        assertEquals(TilstandDto.AvsluttetUtenVedtak, LegacyBehandling.Tilstand.AvsluttetUtenVedtak.toDto())
        assertEquals(TilstandDto.AvsluttetUtenVedtakMedVarsler, LegacyBehandling.Tilstand.AvsluttetUtenVedtakMedVarsler.toDto())
    }

    private fun behandling(
        behandlingId: UUID = UUID.randomUUID(),
        vedtaksperiodeId: UUID = UUID.randomUUID(),
        spleisBehandlingId: UUID? = null,
        fom: LocalDate = 1 jan 2018,
        tom: LocalDate = 31 jan 2018,
        skjæringstidspunkt: LocalDate = 1 jan 2018,
    ) = LegacyBehandling(
        id = behandlingId,
        vedtaksperiodeId = vedtaksperiodeId,
        spleisBehandlingId = spleisBehandlingId,
        fom = fom,
        tom = tom,
        skjæringstidspunkt = skjæringstidspunkt,
        yrkesaktivitetstype = Yrkesaktivitetstype.ARBEIDSTAKER,
    )

    private fun LegacyBehandling.assertVarsler(
        forventetAntall: Int,
        status: VarselStatusDto,
        varselkode: Varselkode,
    ) {
        this.assertVarsler(forventetAntall, status, varselkode.name)
    }

    private fun LegacyBehandling.assertVarsler(
        forventetAntall: Int,
        status: VarselStatusDto,
        varselkode: String,
    ) {
        val dto = this.toDto()
        val varsler = dto.varsler
        val varsel = varsler.filter { it.varselkode == varselkode && it.status == status }
        assertEquals(forventetAntall, varsel.size)
    }

    private fun LegacyBehandling.assertUtbetalingId(utbetalingId: UUID?) {
        val dto = this.toDto()
        assertEquals(utbetalingId, dto.utbetalingId)
    }
}
