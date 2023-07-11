package no.nav.helse.modell.overstyring

import java.util.UUID
import no.nav.helse.mediator.meldinger.Hendelse
import no.nav.helse.modell.kommando.Command
import no.nav.helse.modell.kommando.KobleVedtaksperiodeTilOverstyring
import no.nav.helse.modell.kommando.MacroCommand

internal class OverstyringIgangsatt(
    override val id: UUID,
    private val fødselsnummer: String,
    kilde: UUID,
    berørteVedtaksperiodeIder: List<UUID>,
    private val json: String,
    overstyringDao: OverstyringDao,
) : Hendelse, MacroCommand() {
    override val commands: List<Command> = listOf(
        KobleVedtaksperiodeTilOverstyring(
            berørteVedtaksperiodeIder = berørteVedtaksperiodeIder,
            kilde = kilde,
            overstyringDao = overstyringDao,
        )
    )

    override fun fødselsnummer() = fødselsnummer
    override fun vedtaksperiodeId(): UUID? = null
    override fun toJson() = json

}
