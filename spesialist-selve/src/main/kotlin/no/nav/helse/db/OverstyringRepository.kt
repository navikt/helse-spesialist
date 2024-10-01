package no.nav.helse.db

import no.nav.helse.spesialist.api.overstyring.OverstyringType
import java.util.UUID

interface OverstyringRepository {
    fun finnOverstyringerMedTypeForVedtaksperioder(vedtaksperiodeIder: List<UUID>): List<OverstyringType>

    fun finnOverstyringerMedTypeForVedtaksperiode(vedtaksperiodeId: UUID): List<OverstyringType>

    fun finnesEksternHendelseId(eksternHendelseId: UUID): Boolean

    fun kobleOverstyringOgVedtaksperiode(
        vedtaksperiodeIder: List<UUID>,
        overstyringHendelseId: UUID,
    )
}
