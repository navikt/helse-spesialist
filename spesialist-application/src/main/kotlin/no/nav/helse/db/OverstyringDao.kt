package no.nav.helse.db

import java.util.UUID

// Denne DAO'en kan slettes når ny totrinnsløype er tatt i bruk i produksjon
interface OverstyringDao {
    fun finnesEksternHendelseId(eksternHendelseId: UUID): Boolean

    fun kobleOverstyringOgVedtaksperiode(
        vedtaksperiodeIder: List<UUID>,
        overstyringHendelseId: UUID,
    )

    fun harVedtaksperiodePågåendeOverstyring(vedtaksperiodeId: UUID): Boolean
}
