package no.nav.helse.mediator.meldinger

import java.util.UUID
import net.logstash.logback.argument.StructuredArguments.keyValue
import no.nav.helse.mediator.HendelseMediator
import no.nav.helse.mediator.Toggle
import no.nav.helse.modell.SnapshotDao
import no.nav.helse.modell.WarningDao
import no.nav.helse.modell.kommando.Command
import no.nav.helse.modell.kommando.KobleVedtaksperiodeTilOverstyring
import no.nav.helse.modell.kommando.MacroCommand
import no.nav.helse.modell.kommando.OppdaterSnapshotCommand
import no.nav.helse.modell.kommando.OppdaterSpeilSnapshotCommand
import no.nav.helse.modell.kommando.VedtaksperiodeGenerasjonCommand
import no.nav.helse.modell.overstyring.OverstyringDao
import no.nav.helse.modell.person.PersonDao
import no.nav.helse.modell.vedtaksperiode.GenerasjonDao
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.MessageProblems
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.helse.spesialist.api.snapshot.SnapshotClient
import org.slf4j.Logger
import org.slf4j.LoggerFactory

internal class VedtaksperiodeEndret(
    override val id: UUID,
    private val vedtaksperiodeId: UUID,
    private val fødselsnummer: String,
    private val forårsaketAvId: UUID,
    private val forrigeTilstand: String,
    private val json: String,
    warningDao: WarningDao,
    snapshotDao: SnapshotDao,
    snapshotClient: SnapshotClient,
    personDao: PersonDao,
    overstyringDao: OverstyringDao,
    generasjonDao: GenerasjonDao,
) : Hendelse, MacroCommand() {
    override val commands: List<Command> = listOf(
        OppdaterSpeilSnapshotCommand(),
        OppdaterSnapshotCommand(
            snapshotClient = snapshotClient,
            snapshotDao = snapshotDao,
            vedtaksperiodeId = vedtaksperiodeId,
            fødselsnummer = fødselsnummer,
            warningDao = warningDao,
            personDao = personDao,
            json = json
        ),
        KobleVedtaksperiodeTilOverstyring(
            vedtaksperiodeId = vedtaksperiodeId,
            forårsaketAvId = forårsaketAvId,
            overstyringDao = overstyringDao,
        )
    ).let {
        if (Toggle.VedtaksperiodeGenerasjoner.enabled) {
            it + VedtaksperiodeGenerasjonCommand(
                vedtaksperiodeId = vedtaksperiodeId,
                vedtaksperiodeEndretHendelseId = forårsaketAvId,
                generasjonDao = generasjonDao,
                forrigeTilstand = forrigeTilstand
            )
        } else it
    }

    override fun fødselsnummer() = fødselsnummer
    override fun vedtaksperiodeId() = vedtaksperiodeId
    override fun toJson() = json

    internal class VedtaksperiodeEndretRiver(
        rapidsConnection: RapidsConnection,
        private val mediator: HendelseMediator
    ) : River.PacketListener {

        private val log = LoggerFactory.getLogger(this::class.java)
        private val sikkerLogg: Logger = LoggerFactory.getLogger("tjenestekall")

        init {
            River(rapidsConnection).apply {
                validate {
                    it.demandValue("@event_name", "vedtaksperiode_endret")
                    it.rejectValue("forrigeTilstand", "START")
                    it.requireKey("vedtaksperiodeId")
                    it.requireKey("fødselsnummer")
                    it.requireKey("@id")
                    it.requireKey("@forårsaket_av", "@forårsaket_av.id")
                    it.interestedIn("forrigeTilstand")
                }
            }.register(this)
        }

        override fun onError(problems: MessageProblems, context: MessageContext) {
            sikkerLogg.error("Forstod ikke vedtaksperiode_endret:\n${problems.toExtendedReport()}")
        }

        override fun onPacket(packet: JsonMessage, context: MessageContext) {
            val vedtaksperiodeId = UUID.fromString(packet["vedtaksperiodeId"].asText())
            val id = UUID.fromString(packet["@id"].asText())
            val forårsaketAvId = UUID.fromString(packet["@forårsaket_av.id"].asText())
            log.info(
                "Mottok vedtaksperiode endret {}, {}, {}",
                keyValue("vedtaksperiodeId", vedtaksperiodeId),
                keyValue("eventId", id),
                keyValue("forårsaketAvId", forårsaketAvId),
            )
            mediator.vedtaksperiodeEndret(
                packet,
                id,
                vedtaksperiodeId,
                packet["fødselsnummer"].asText(),
                forårsaketAvId,
                packet["forrigeTilstand"].asText(),
                context
            )
        }
    }
}
