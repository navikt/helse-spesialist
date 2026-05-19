package no.nav.helse.modell.automatisering.sjekker

import no.nav.helse.modell.automatisering.AutomatiseringValidering

class Risikovurdering private constructor(
    private val kanGodkjennesAutomatisk: Boolean,
) : AutomatiseringValidering {
    companion object {
        fun restore(kanGodkjennesAutomatisk: Boolean) = Risikovurdering(kanGodkjennesAutomatisk)
    }

    override fun erAutomatiserbar() = kanGodkjennesAutomatisk

    override fun årsakTilIkkeAutomatiserbar() = "Vilkårsvurdering for arbeidsuførhet, aktivitetsplikt eller medvirkning er ikke oppfylt"
}
