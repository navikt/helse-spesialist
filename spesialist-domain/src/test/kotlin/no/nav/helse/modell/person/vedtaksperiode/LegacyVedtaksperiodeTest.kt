package no.nav.helse.modell.person.vedtaksperiode

import no.nav.helse.modell.vedtaksperiode.Yrkesaktivitetstype
import no.nav.helse.spesialist.domain.testfixtures.jan
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.UUID

class LegacyVedtaksperiodeTest {
    @Test
    fun `ny vedtaksperiode opprettes med spleisBehandlingId`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val vedtaksperiode = nyVedtaksperiode(vedtaksperiodeId)
        val dto = vedtaksperiode.toDto()
        assertNotNull(dto.behandlinger.single().spleisBehandlingId)
    }

    @Test
    fun `Kan ikke gjenopprette vedtaksperiode uten behandlinger`() {
        assertThrows<IllegalStateException> {
            LegacyVedtaksperiode.gjenopprett("987654321", UUID.randomUUID(), false, emptyList())
        }
    }

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
                emptyList(),
                Yrkesaktivitetstype.ARBEIDSTAKER,
            ),
        ),
    )
}
