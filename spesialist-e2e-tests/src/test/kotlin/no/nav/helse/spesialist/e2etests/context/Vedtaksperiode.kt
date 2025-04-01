package no.nav.helse.spesialist.e2etests.context

import java.util.UUID

data class Vedtaksperiode(
    val vedtaksperiodeId: UUID = UUID.randomUUID(),
    val vilkårsgrunnlagId: UUID = UUID.randomUUID(),
    var spleisBehandlingId: UUID? = null,
    var utbetalingId: UUID? = null,
) {
    fun spleisBehandlingIdForÅByggeMelding(meldingsnavn: String): UUID =
        spleisBehandlingId
            ?: error("Feil i testoppsett: Forsøkte å lage en $meldingsnavn-melding før spleisBehandlingId var satt")

    fun utbetalingIdForÅByggeMelding(meldingsnavn: String): UUID =
        utbetalingId
            ?: error("Feil i testoppsett: Forsøkte å lage en $meldingsnavn-melding før utbetalingId var satt")
}
