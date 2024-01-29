package no.nav.helse.mediator.meldinger

import java.util.UUID
import net.logstash.logback.argument.StructuredArguments
import no.nav.helse.mediator.HendelseMediator
import no.nav.helse.mediator.oppgave.OppgaveDao
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.helse.rapids_rivers.asLocalDate
import org.slf4j.Logger
import org.slf4j.LoggerFactory

internal class TilbakedatertRiver(
    rapidsConnection: RapidsConnection,
    private val mediator: HendelseMediator,
    private val oppgaveDao: OppgaveDao,
) : River.PacketListener {
    private val logg = LoggerFactory.getLogger(this::class.java)
    private val sikkerlogg: Logger = LoggerFactory.getLogger("tjenestekall")

    init {
        River(rapidsConnection).apply {
            validate {
                it.demandValue("@event_name", "tilbakedatering_behandlet")
                it.requireKey("@opprettet")
                it.requireKey("@id")
                it.requireKey("fødselsnummer")
                it.requireKey("sykmeldingId")
                it.requireKey("syketilfelleStartDato")
            }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        val hendelseId = UUID.fromString(packet["@id"].asText())
        val sykmeldingId = packet["sykmeldingId"].asText()
        val fødselsnummer = packet["fødselsnummer"].asText()
        val syketilfelleStartDato = packet["syketilfelleStartDato"].asLocalDate()

        logg.info(
            "Mottok tilbakedatering_behandlet med {}",
            StructuredArguments.keyValue("eventId", hendelseId)
        )
        sikkerlogg.info(
            "Mottok tilbakedatering_behandlet med {}, {}, {}",
            StructuredArguments.keyValue("hendelseId", hendelseId),
            StructuredArguments.keyValue("sykmeldingId", sykmeldingId),
            StructuredArguments.keyValue("hendelse", packet.toJson())
        )

        oppgaveDao.finnOppgaveId(fødselsnummer)?.also { oppgaveId ->
            sikkerlogg.info("Fant en oppgave for {}: {}", fødselsnummer, oppgaveId)

            val oppgaveDataForAutomatisering = oppgaveDao.gosysOppgaveEndretCommandData(oppgaveId)
            if (oppgaveDataForAutomatisering == null) {
                sikkerlogg.info("Fant ikke commandData for {} og {}", fødselsnummer, oppgaveId)
                return
            }

            if (oppgaveDataForAutomatisering.periodeFom > syketilfelleStartDato || oppgaveDataForAutomatisering.periodeTom < syketilfelleStartDato) {
                sikkerlogg.info("SyketilfellestartDato er ikke innenfor periodens fom og tom, for tilbakedateringen {} og {}", fødselsnummer, oppgaveId)
                return
            }

            sikkerlogg.info("Har oppgave til_godkjenning og commandData for fnr $fødselsnummer og vedtaksperiodeId ${oppgaveDataForAutomatisering.vedtaksperiodeId}")
            mediator.godkjentTilbakedatertSykmelding(
                id = hendelseId,
                fødselsnummer = fødselsnummer,
                vedtaksperiodeId = oppgaveDataForAutomatisering.vedtaksperiodeId,
                skjæringstidspunkt = oppgaveDataForAutomatisering.skjæringstidspunkt,
                json = packet.toJson(),
                context = context
            )
        } ?: sikkerlogg.info("Ingen åpne oppgaver for {} ifm. godkjent tilbakedatering", fødselsnummer)

    }
}