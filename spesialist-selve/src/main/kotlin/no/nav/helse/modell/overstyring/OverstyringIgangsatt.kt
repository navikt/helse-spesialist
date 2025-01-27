package no.nav.helse.modell.overstyring

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.db.SessionContext
import no.nav.helse.mediator.Kommandostarter
import no.nav.helse.mediator.meldinger.Personmelding
import no.nav.helse.modell.person.Person
import java.util.UUID

class OverstyringIgangsatt(
    override val id: UUID,
    private val fødselsnummer: String,
    val kilde: UUID,
    val berørteVedtaksperiodeIder: List<UUID>,
    private val json: String,
) : Personmelding {
    constructor(jsonNode: JsonNode) : this(
        id = UUID.fromString(jsonNode["@id"].asText()),
        fødselsnummer = jsonNode["fødselsnummer"].asText(),
        kilde = UUID.fromString(jsonNode["kilde"].asText()),
        berørteVedtaksperiodeIder = jsonNode["berørtePerioder"].map { UUID.fromString(it["vedtaksperiodeId"].asText()) },
        json = jsonNode.toString(),
    )

    override fun behandle(
        person: Person,
        kommandostarter: Kommandostarter,
        sessionContext: SessionContext,
    ) {
        kommandostarter { overstyringIgangsatt(this@OverstyringIgangsatt, sessionContext) }
    }

    override fun fødselsnummer() = fødselsnummer

    override fun toJson() = json
}
