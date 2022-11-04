package no.nav.helse.mediator.meldinger

import java.time.LocalDate
import java.util.UUID
import no.nav.helse.mediator.HendelseMediator
import no.nav.helse.modell.VedtakDao
import no.nav.helse.modell.arbeidsgiver.ArbeidsgiverDao
import no.nav.helse.modell.kommando.Command
import no.nav.helse.modell.kommando.MacroCommand
import no.nav.helse.modell.kommando.OpprettMinimaltVedtakCommand
import no.nav.helse.modell.person.PersonDao
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.helse.rapids_rivers.River.PacketListener
import no.nav.helse.rapids_rivers.asLocalDate
import org.slf4j.LoggerFactory

internal class VedtaksperiodeOpprettet(
    override val id: UUID,
    private val fødselsnummer: String,
    organisasjonsnummer: String,
    vedtaksperiodeId: UUID,
    fom: LocalDate,
    tom: LocalDate,
    personDao: PersonDao,
    arbeidsgiverDao: ArbeidsgiverDao,
    vedtakDao: VedtakDao,
    private val json: String,
) : Hendelse, MacroCommand() {

    override fun fødselsnummer() = fødselsnummer
    override fun toJson(): String = json

    override val commands: List<Command> = listOf(
        OpprettMinimaltVedtakCommand(fødselsnummer, organisasjonsnummer, vedtaksperiodeId, fom, tom, personDao, arbeidsgiverDao, vedtakDao)
    )

    internal class River(
        rapidsConnection: RapidsConnection,
        private val mediator: HendelseMediator
    ) : PacketListener {
        private val log = LoggerFactory.getLogger(this::class.java)

        init {
            River(rapidsConnection).apply {
                validate {
                    it.demandValue("@event_name", "vedtaksperiode_endret")
                    it.demandValue("forrigeTilstand", "START")
                    it.requireKey(
                        "@id", "fødselsnummer", "organisasjonsnummer", "vedtaksperiodeId", "fom", "tom"
                    )
                }
            }.register(this)
        }

        override fun onPacket(packet: JsonMessage, context: MessageContext) {
            val id = UUID.fromString(packet["@id"].asText())
            val fødselsnummer = packet["fødselsnummer"].asText()
            val organisasjonsnummer = packet["organisasjonsnummer"].asText()
            val vedtaksperiodeId = UUID.fromString(packet["vedtaksperiodeId"].asText())
            val fom = packet["fom"].asLocalDate()
            val tom = packet["fom"].asLocalDate()

            if (!erProd()) {
                log.info("Oppretter person, arbeidsgiver og vedtak på vedtaksperiodeId: ${packet["vedtaksperiodeId"].asText()}")
                mediator.vedtaksperiodeOpprettet(
                    packet,
                    id,
                    fødselsnummer,
                    organisasjonsnummer,
                    vedtaksperiodeId,
                    fom,
                    tom,
                    context
                )
            } else {
                log.info("VedtaksperiodeOpprettet melding er mottatt men videre håndtering er togglet av")
            }
        }
        private fun erProd() = "prod-gcp" == System.getenv("NAIS_CLUSTER_NAME")
    }
}
