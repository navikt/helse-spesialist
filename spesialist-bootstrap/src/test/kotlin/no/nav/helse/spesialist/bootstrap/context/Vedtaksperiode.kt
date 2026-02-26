package no.nav.helse.spesialist.bootstrap.context

import no.nav.helse.spesialist.domain.testfixtures.jan
import java.time.LocalDate
import java.util.UUID

class Vedtaksperiode(
    val vedtaksperiodeId: UUID = UUID.randomUUID(),
    var spleisBehandlingId: UUID? = null,
    var utbetalingId: UUID? = null,
    var fom: LocalDate = 1 jan 2018,
    var tom: LocalDate = 31 jan 2018,
    var skjæringstidspunkt: LocalDate = fom,
    arbeidsgiver: Arbeidsgiver,
    var sykepengegrunnlagsfakta: Sykepengegrunnlagsfakta =
        Sykepengegrunnlagsfakta(
            skjæringstidspunkt = skjæringstidspunkt,
            fastsatt = Sykepengegrunnlagsfakta.FastsattType.EtterHovedregel,
            arbeidsgivere =
                listOf(
                    Sykepengegrunnlagsfakta.Arbeidsgiver(
                        organisasjonsnummer = arbeidsgiver.organisasjonsnummer,
                        omregnetÅrsinntekt = 600000.0,
                    ),
                ),
        ),
) {
    fun spleisBehandlingIdForÅByggeMelding(meldingsnavn: String): UUID =
        spleisBehandlingId
            ?: error("Feil i testoppsett: Forsøkte å lage en $meldingsnavn-melding før spleisBehandlingId var satt")

    fun utbetalingIdForÅByggeMelding(meldingsnavn: String): UUID =
        utbetalingId
            ?: error("Feil i testoppsett: Forsøkte å lage en $meldingsnavn-melding før utbetalingId var satt")

    fun nyUtbetaling() {
        utbetalingId = UUID.randomUUID()
    }
}
