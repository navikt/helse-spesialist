package no.nav.helse.modell.sykefraværstilfelle

import java.util.UUID
import no.nav.helse.mediator.meldinger.Kommandohendelse
import no.nav.helse.modell.kommando.Command
import no.nav.helse.modell.kommando.MacroCommand
import no.nav.helse.modell.vedtaksperiode.Generasjon
import no.nav.helse.modell.vedtaksperiode.OppdaterSykefraværstilfellerCommand
import no.nav.helse.modell.vedtaksperiode.VedtaksperiodeOppdatering

internal class Sykefraværstilfeller(
    override val id: UUID,
    private val fødselsnummer: String,
    aktørId: String,
    vedtaksperiodeOppdateringer: List<VedtaksperiodeOppdatering>,
    gjeldendeGenerasjoner: List<Generasjon>,
    private val json: String,
) : Kommandohendelse, MacroCommand() {
    override val commands: List<Command> = listOf(
        OppdaterSykefraværstilfellerCommand(
            fødselsnummer = fødselsnummer,
            aktørId = aktørId,
            vedtaksperiodeoppdateringer = vedtaksperiodeOppdateringer,
            generasjoner = gjeldendeGenerasjoner,
            hendelseId = id
        )
    )
    override fun fødselsnummer() = fødselsnummer
    override fun toJson() = json
}