package no.nav.helse.modell.person.vedtaksperiode

import no.nav.helse.modell.vedtaksperiode.Yrkesaktivitetstype
import no.nav.helse.spesialist.domain.testfixtures.feb
import no.nav.helse.spesialist.domain.testfixtures.jan
import no.nav.helse.spesialist.domain.testfixtures.lagOrganisasjonsnummer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDateTime
import java.util.UUID

class LegacyVedtaksperiodeTest {
    @Test
    fun `vedtaksperioden mottar nye varsler`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val vedtaksperiode = nyVedtaksperiode(vedtaksperiodeId)
        vedtaksperiode.nyeVarsler(listOf(nyttVarsel(vedtaksperiodeId)))
        val gjeldendebehandling = vedtaksperiode.toDto().behandlinger.single()
        assertEquals(1, gjeldendebehandling.varsler.size)
    }

    @Test
    fun `vedtaksperioden ignorerer varsler som ikke er relevante for den`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val vedtaksperiode = nyVedtaksperiode(vedtaksperiodeId)
        vedtaksperiode.nyeVarsler(listOf(nyttVarsel(UUID.randomUUID())))
        val gjeldendebehandling = vedtaksperiode.toDto().behandlinger.single()
        assertEquals(0, gjeldendebehandling.varsler.size)
    }

    @Test
    fun `ny vedtaksperiode opprettes med spleisBehandlingId`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val vedtaksperiode = nyVedtaksperiode(vedtaksperiodeId)
        val dto = vedtaksperiode.toDto()
        assertNotNull(dto.behandlinger.single().spleisBehandlingId)
    }

    @Test
    fun `oppdater aktuell behandling dersom behandlingen er klar til behandling ved godkjenningsbehov`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val spleisBehandlingId = UUID.randomUUID()
        val vedtaksperiode =
            nyVedtaksperiode(
                vedtaksperiodeId = vedtaksperiodeId,
                spleisBehandlingId = spleisBehandlingId,
            )
        vedtaksperiode.nyUtbetaling(UUID.randomUUID())
        vedtaksperiode.nyttGodkjenningsbehov(
            listOf(SpleisVedtaksperiode(vedtaksperiodeId, spleisBehandlingId, 1 feb 2018, 28 feb 2018, 1 feb 2018)),
        )
        val dto = vedtaksperiode.toDto()
        assertEquals(1, dto.behandlinger.size)
        assertEquals(1 feb 2018, dto.behandlinger.single().fom)
        assertEquals(28 feb 2018, dto.behandlinger.single().tom)
        assertEquals(1 feb 2018, dto.behandlinger.single().skjæringstidspunkt)
        assertEquals(spleisBehandlingId, dto.behandlinger.single().spleisBehandlingId)
    }

    @Test
    fun `oppdater aktuell behandling dersom behandlingen avventer videre avklaring ved godkjenningsbehov`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val spleisBehandlingId = UUID.randomUUID()
        val vedtaksperiode =
            nyVedtaksperiode(
                vedtaksperiodeId = vedtaksperiodeId,
                spleisBehandlingId = spleisBehandlingId,
            )
        vedtaksperiode.nyttGodkjenningsbehov(
            listOf(SpleisVedtaksperiode(vedtaksperiodeId, spleisBehandlingId, 1 feb 2018, 28 feb 2018, 1 feb 2018)),
        )
        val dto = vedtaksperiode.toDto()
        assertEquals(1, dto.behandlinger.size)
        assertEquals(1 feb 2018, dto.behandlinger.single().fom)
        assertEquals(28 feb 2018, dto.behandlinger.single().tom)
        assertEquals(1 feb 2018, dto.behandlinger.single().skjæringstidspunkt)
        assertEquals(spleisBehandlingId, dto.behandlinger.single().spleisBehandlingId)
    }

    @Test
    fun `oppdaterer ikke andre behandlinger enn den aktuelle`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val enBehandlingId = UUID.randomUUID()
        val enAnnenBehandlingId = UUID.randomUUID()
        val vedtaksperiode =
            nyVedtaksperiode(
                vedtaksperiodeId = vedtaksperiodeId,
                spleisBehandlingId = enBehandlingId,
            )
        vedtaksperiode.nyttGodkjenningsbehov(
            listOf(SpleisVedtaksperiode(vedtaksperiodeId, enAnnenBehandlingId, 1 feb 2018, 28 feb 2018, 1 feb 2018)),
        )
        val dto = vedtaksperiode.toDto()
        assertEquals(1, dto.behandlinger.size)
        assertEquals(1 jan 2018, dto.behandlinger.single().fom)
        assertEquals(31 jan 2018, dto.behandlinger.single().tom)
        assertEquals(1 jan 2018, dto.behandlinger.single().skjæringstidspunkt)
        assertEquals(enBehandlingId, dto.behandlinger.single().spleisBehandlingId)
    }

    @Test
    fun `ikke ny behandling dersom gjeldende behandling er avsluttet med vedtak og godkjenningsbehovet inneholder behandling for perioden som er siste gjeldende i Spesialist`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val spleisBehandlingId = UUID.randomUUID()
        val vedtaksperiode = nyVedtaksperiode(vedtaksperiodeId, spleisBehandlingId)
        vedtaksperiode.nyUtbetaling(UUID.randomUUID())

        vedtaksperiode.nyttGodkjenningsbehov(
            listOf(SpleisVedtaksperiode(vedtaksperiodeId, spleisBehandlingId, 1 jan 2018, 31 jan 2018, 1 jan 2018)),
        )
        val dto = vedtaksperiode.toDto()
        assertEquals(1, dto.behandlinger.size)
    }

    @Test
    fun `ikke ny behandling dersom gjeldende behandling er avsluttet uten vedtak og godkjenningsbehovet inneholder behandling for perioden som er siste gjeldende i Spesialist`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val spleisBehandlingId = UUID.randomUUID()
        val vedtaksperiode =
            LegacyVedtaksperiode.gjenopprett(
                lagOrganisasjonsnummer(),
                vedtaksperiodeId,
                false,
                behandlinger =
                    listOf(
                        BehandlingDto(
                            UUID.randomUUID(),
                            vedtaksperiodeId,
                            null,
                            spleisBehandlingId,
                            1.jan(2018),
                            1.jan(2018),
                            31.jan(2018),
                            tilstand = TilstandDto.AvsluttetUtenVedtak,
                            tags = emptyList(),
                            vedtakBegrunnelse = null,
                            varsler = emptyList(),
                            yrkesaktivitetstype = Yrkesaktivitetstype.ARBEIDSTAKER,
                        ),
                    ),
            )
        vedtaksperiode.nyttGodkjenningsbehov(
            listOf(SpleisVedtaksperiode(vedtaksperiodeId, spleisBehandlingId, 1 jan 2018, 31 jan 2018, 1 jan 2018)),
        )
        val dto = vedtaksperiode.toDto()
        assertEquals(1, dto.behandlinger.size)
    }

    @Test
    fun `ikke ny behandling dersom gjeldende behandling er avsluttet uten vedtak med varsler og godkjenningsbehovet inneholder behandling for perioden som er siste gjeldende i Spesialist`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val spleisBehandlingId = UUID.randomUUID()
        val vedtaksperiode =
            LegacyVedtaksperiode.gjenopprett(
                lagOrganisasjonsnummer(),
                vedtaksperiodeId,
                false,
                behandlinger =
                    listOf(
                        BehandlingDto(
                            UUID.randomUUID(),
                            vedtaksperiodeId,
                            null,
                            spleisBehandlingId,
                            1.jan(2018),
                            1.jan(2018),
                            31.jan(2018),
                            tilstand = TilstandDto.AvsluttetUtenVedtakMedVarsler,
                            tags = emptyList(),
                            vedtakBegrunnelse = null,
                            varsler = listOf(VarselDto(UUID.randomUUID(), "RV_IM_1", LocalDateTime.now(), vedtaksperiodeId, status = VarselStatusDto.AKTIV)),
                            yrkesaktivitetstype = Yrkesaktivitetstype.ARBEIDSTAKER,
                        ),
                    ),
            )

        vedtaksperiode.nyttGodkjenningsbehov(
            listOf(SpleisVedtaksperiode(vedtaksperiodeId, spleisBehandlingId, 1 jan 2018, 31 jan 2018, 1 jan 2018)),
        )
        val dto = vedtaksperiode.toDto()
        assertEquals(1, dto.behandlinger.size)
        assertEquals(TilstandDto.AvsluttetUtenVedtakMedVarsler, dto.behandlinger[0].tilstand)
    }

    @Test
    fun `Kan ikke gjenopprette vedtaksperiode uten behandlinger`() {
        assertThrows<IllegalStateException> {
            LegacyVedtaksperiode.gjenopprett("987654321", UUID.randomUUID(), false, emptyList())
        }
    }

    private fun nyttVarsel(
        vedtaksperiodeId: UUID,
        varselkode: String = "SB_EX_1",
    ) = LegacyVarsel(UUID.randomUUID(), varselkode, LocalDateTime.now(), vedtaksperiodeId)

    private fun nyVedtaksperiode(
        vedtaksperiodeId: UUID,
        spleisBehandlingId: UUID = UUID.randomUUID(),
    ) = LegacyVedtaksperiode.gjenopprett(
        "987654321",
        vedtaksperiodeId,
        false,
        listOf(
            BehandlingDto(
                UUID.randomUUID(),
                vedtaksperiodeId,
                null,
                spleisBehandlingId,
                1 jan 2018,
                1 jan 2018,
                31 jan 2018,
                TilstandDto.VidereBehandlingAvklares,
                emptyList(),
                null,
                emptyList(),
                Yrkesaktivitetstype.ARBEIDSTAKER,
            ),
        ),
    )
}
