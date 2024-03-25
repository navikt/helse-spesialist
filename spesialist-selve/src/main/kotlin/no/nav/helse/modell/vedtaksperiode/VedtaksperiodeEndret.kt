package no.nav.helse.modell.vedtaksperiode

import com.fasterxml.jackson.databind.JsonNode
import java.util.UUID
import no.nav.helse.mediator.meldinger.VedtaksperiodemeldingOld
import no.nav.helse.modell.SnapshotDao
import no.nav.helse.modell.kommando.Command
import no.nav.helse.modell.kommando.MacroCommand
import no.nav.helse.modell.kommando.OppdaterSnapshotCommand
import no.nav.helse.modell.person.PersonDao
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.spesialist.api.snapshot.SnapshotClient

internal class VedtaksperiodeEndret(
    override val id: UUID,
    private val vedtaksperiodeId: UUID,
    private val fødselsnummer: String,
    private val json: String,
) : VedtaksperiodemeldingOld {
    internal constructor(packet: JsonMessage): this(
        id = UUID.fromString(packet["@id"].asText()),
        vedtaksperiodeId = UUID.fromString(packet["vedtaksperiodeId"].asText()),
        fødselsnummer = packet["fødselsnummer"].asText(),
        json = packet.toJson()
    )
    internal constructor(jsonNode: JsonNode): this(
        id = UUID.fromString(jsonNode["@id"].asText()),
        vedtaksperiodeId = UUID.fromString(jsonNode["vedtaksperiodeId"].asText()),
        fødselsnummer = jsonNode["fødselsnummer"].asText(),
        json = jsonNode.toString()
    )

    override fun fødselsnummer() = fødselsnummer
    override fun vedtaksperiodeId() = vedtaksperiodeId
    override fun toJson() = json
}

internal class VedtaksperiodeEndretCommand(
    fødselsnummer: String,
    personDao: PersonDao,
    snapshotDao: SnapshotDao,
    snapshotClient: SnapshotClient
) : MacroCommand() {
    override val commands: List<Command> = listOf(
        OppdaterSnapshotCommand(
            snapshotClient = snapshotClient,
            snapshotDao = snapshotDao,
            fødselsnummer = fødselsnummer,
            personDao = personDao,
        )
    )
}
