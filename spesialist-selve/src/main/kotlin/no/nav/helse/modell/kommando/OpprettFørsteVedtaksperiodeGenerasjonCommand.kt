package no.nav.helse.modell.kommando

import java.util.UUID
import no.nav.helse.modell.vedtaksperiode.Generasjon

internal class OpprettFørsteVedtaksperiodeGenerasjonCommand(
    private val hendelseId: UUID,
    private val generasjon: Generasjon,
) : Command {

    override fun execute(context: CommandContext): Boolean {
        generasjon.håndterVedtaksperiodeOpprettet(hendelseId)
        return true
    }

}