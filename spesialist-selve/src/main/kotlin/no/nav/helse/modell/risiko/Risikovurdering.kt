package no.nav.helse.modell.risiko

import no.nav.helse.modell.automatisering.AutomatiseringValidering

internal class Risikovurdering private constructor(private val kanGodkjennesAutomatisk: Boolean) : AutomatiseringValidering {
    internal companion object {
        internal fun restore(kanGodkjennesAutomatisk: Boolean) = Risikovurdering(kanGodkjennesAutomatisk)
    }

    override fun erAautomatiserbar() = kanGodkjennesAutomatisk
    override fun error() = "Vilkårsvurdering for arbeidsuførhet, aktivitetsplikt eller medvirkning er ikke oppfylt"
}
