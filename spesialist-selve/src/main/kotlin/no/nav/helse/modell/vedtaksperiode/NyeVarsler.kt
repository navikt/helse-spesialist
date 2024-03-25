package no.nav.helse.modell.vedtaksperiode

import java.util.UUID
import no.nav.helse.mediator.Kommandofabrikk
import no.nav.helse.mediator.meldinger.Personmelding
import no.nav.helse.modell.person.Person
import no.nav.helse.modell.varsel.Varsel
import no.nav.helse.modell.varsel.Varsel.Companion.varsler
import no.nav.helse.rapids_rivers.JsonMessage

internal class NyeVarsler private constructor(
    override val id: UUID,
    private val fødselsnummer: String,
    internal val varsler: List<Varsel>,
    private val json: String,
) : Personmelding {
    internal constructor(packet: JsonMessage): this(
        id = UUID.fromString(packet["@id"].asText()),
        fødselsnummer = packet["fødselsnummer"].asText(),
        varsler = packet["aktiviteter"].varsler(),
        json = packet.toJson()
    )

    override fun fødselsnummer(): String = fødselsnummer
    override fun toJson(): String = json

    override fun behandle(person: Person, kommandofabrikk: Kommandofabrikk) {
        person.nyeVarsler(this)
    }
}