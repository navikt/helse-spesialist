package no.nav.helse.modell.risiko

import no.nav.helse.modell.automatisering.AutomatiseringValidering

class Risikovurdering private constructor(private val kanGodkjennesAutomatisk: Boolean) : AutomatiseringValidering {
    companion object {
        fun restore(kanGodkjennesAutomatisk: Boolean) = Risikovurdering(kanGodkjennesAutomatisk)
    }

    override fun erAautomatiserbar() = kanGodkjennesAutomatisk

    override fun error() = "Vilkårsvurdering for arbeidsuførhet, aktivitetsplikt eller medvirkning er ikke oppfylt"
}
