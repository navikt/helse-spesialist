package no.nav.helse.modell.vedtaksperiode

import java.util.UUID
import no.nav.helse.mediator.meldinger.Vedtaksperiodemelding
import no.nav.helse.modell.SnapshotDao
import no.nav.helse.modell.kommando.Command
import no.nav.helse.modell.kommando.MacroCommand
import no.nav.helse.modell.kommando.OppdaterSnapshotCommand
import no.nav.helse.modell.kommando.VedtaksperiodeGenerasjonCommand
import no.nav.helse.modell.person.PersonDao
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.spesialist.api.snapshot.SnapshotClient

internal class VedtaksperiodeEndret(
    override val id: UUID,
    private val vedtaksperiodeId: UUID,
    private val fødselsnummer: String,
    private val json: String,
    val forårsaketAvId: UUID,
    val forrigeTilstand: String,
    val gjeldendeTilstand: String,
) : Vedtaksperiodemelding {
    internal constructor(packet: JsonMessage): this(
        id = UUID.fromString(packet["@id"].asText()),
        vedtaksperiodeId = UUID.fromString(packet["vedtaksperiodeId"].asText()),
        fødselsnummer = packet["fødselsnummer"].asText(),
        forårsaketAvId = UUID.fromString(packet["@forårsaket_av.id"].asText()),
        forrigeTilstand = packet["forrigeTilstand"].asText(),
        gjeldendeTilstand = packet["gjeldendeTilstand"].asText(),
        json = packet.toJson()
    )

    override fun fødselsnummer() = fødselsnummer
    override fun vedtaksperiodeId() = vedtaksperiodeId
    override fun toJson() = json
}

internal class VedtaksperiodeEndretCommand(
    fødselsnummer: String,
    vedtaksperiodeId: UUID,
    forårsaketAvId: UUID,
    forrigeTilstand: String,
    gjeldendeTilstand: String,
    gjeldendeGenerasjon: Generasjon,
    personDao: PersonDao,
    snapshotDao: SnapshotDao,
    snapshotClient: SnapshotClient
) : MacroCommand() {
    override val commands: List<Command> = listOf(
        OppdaterSnapshotCommand(
            snapshotClient = snapshotClient,
            snapshotDao = snapshotDao,
            vedtaksperiodeId = vedtaksperiodeId,
            fødselsnummer = fødselsnummer,
            personDao = personDao,
        ),
        VedtaksperiodeGenerasjonCommand(
            vedtaksperiodeEndretHendelseId = forårsaketAvId,
            forrigeTilstand = forrigeTilstand,
            gjeldendeTilstand = gjeldendeTilstand,
            gjeldendeGenerasjon = gjeldendeGenerasjon
        )
    )
}
