package no.nav.helse.mediator.meldinger

import no.nav.helse.modell.kommando.Command
import java.util.*

internal interface Hendelse : Command {
    val id: UUID

    fun fødselsnummer(): String
    fun vedtaksperiodeId(): UUID? = null

    fun toJson(): String
}

