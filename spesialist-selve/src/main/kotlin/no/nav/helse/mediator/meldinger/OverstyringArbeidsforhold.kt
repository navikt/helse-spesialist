package no.nav.helse.mediator.meldinger

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import net.logstash.logback.argument.StructuredArguments
import no.nav.helse.mediator.HendelseMediator
import no.nav.helse.mediator.api.OverstyrArbeidsforholdDto
import no.nav.helse.modell.kommando.Command
import no.nav.helse.modell.kommando.InvaliderSaksbehandlerOppgaveCommand
import no.nav.helse.modell.kommando.MacroCommand
import no.nav.helse.modell.kommando.OpprettSaksbehandlerCommand
import no.nav.helse.modell.kommando.PersisterOverstyringArbeidsforholdCommand
import no.nav.helse.modell.kommando.ReserverPersonCommand
import no.nav.helse.modell.overstyring.OverstyringDao
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.helse.rapids_rivers.asLocalDate
import no.nav.helse.rapids_rivers.asLocalDateTime
import no.nav.helse.reservasjon.ReservasjonDao
import no.nav.helse.saksbehandler.SaksbehandlerDao
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import no.nav.helse.modell.kommando.PersisterTotrinnsvurderingArbeidsforholdCommand
import no.nav.helse.oppgave.OppgaveDao
import no.nav.helse.overstyring.OverstyrtVedtaksperiodeDao

internal class OverstyringArbeidsforhold(
    override val id: UUID,
    private val fødselsnummer: String,
    oid: UUID,
    navn: String,
    epost: String,
    ident: String,
    organisasjonsnummer: String,
    overstyrteArbeidsforhold: List<OverstyrArbeidsforholdDto.ArbeidsforholdOverstyrt>,
    skjæringstidspunkt: LocalDate,
    opprettet: LocalDateTime,
    private val json: String,
    reservasjonDao: ReservasjonDao,
    saksbehandlerDao: SaksbehandlerDao,
    overstyringDao: OverstyringDao,
    oppgaveDao: OppgaveDao,
    overstyrtVedtaksperiodeDao: OverstyrtVedtaksperiodeDao,
) : Hendelse, MacroCommand() {
    override val commands: List<Command> = listOf(
        OpprettSaksbehandlerCommand(
            oid = oid,
            navn = navn,
            epost = epost,
            ident = ident,
            saksbehandlerDao = saksbehandlerDao
        ),
        ReserverPersonCommand(oid, fødselsnummer, reservasjonDao),
        PersisterOverstyringArbeidsforholdCommand(
            oid = oid,
            eventId = id,
            fødselsnummer = fødselsnummer,
            overstyrteArbeidsforhold = overstyrteArbeidsforhold,
            skjæringstidspunkt = skjæringstidspunkt,
            overstyringDao = overstyringDao,
            opprettet = opprettet
        ),
        PersisterTotrinnsvurderingArbeidsforholdCommand(
            fødselsnummer = fødselsnummer,
            skjæringstidspunkt = skjæringstidspunkt,
            oppgaveDao = oppgaveDao,
            overstyrtVedtaksperiodeDao = overstyrtVedtaksperiodeDao,
        ),
        InvaliderSaksbehandlerOppgaveCommand(fødselsnummer, organisasjonsnummer, saksbehandlerDao)
    )
    override fun fødselsnummer(): String = fødselsnummer
    override fun toJson(): String = json

    internal class OverstyringArbeidsforholdRiver(
        rapidsConnection: RapidsConnection,
        private val mediator: HendelseMediator
    ): River.PacketListener {
        private val logg = LoggerFactory.getLogger(this::class.java)
        private val sikkerLogg: Logger = LoggerFactory.getLogger("tjenestekall")

        init {
            River(rapidsConnection).apply {
                validate {
                    it.demandValue("@event_name", "overstyr_arbeidsforhold")
                    it.requireKey("aktørId")
                    it.requireKey("fødselsnummer")
                    it.requireKey("organisasjonsnummer")
                    it.requireKey("skjæringstidspunkt")
                    it.requireKey("saksbehandlerIdent")
                    it.requireKey("saksbehandlerOid")
                    it.requireKey("saksbehandlerNavn")
                    it.requireKey("saksbehandlerEpost")
                    it.requireKey("@id")
                    it.requireKey("@opprettet")
                    it.requireArray("overstyrteArbeidsforhold") {
                        requireKey("orgnummer")
                        requireKey("deaktivert")
                        requireKey("begrunnelse")
                        requireKey("forklaring")
                    }
                }
            }.register(this)
        }

        override fun onPacket(packet: JsonMessage, context: MessageContext) {
            val hendelseId = UUID.fromString(packet["@id"].asText())
            logg.info(
                "Mottok overstyring av arbeidsforhold med {}",
                StructuredArguments.keyValue("eventId", hendelseId)
            )
            sikkerLogg.info(
                "Mottok overstyring av arbeidsforhold med {}, {}",
                StructuredArguments.keyValue("hendelseId", hendelseId),
                StructuredArguments.keyValue("hendelse", packet.toJson())
            )

            mediator.overstyringArbeidsforhold(
                id = UUID.fromString(packet["@id"].asText()),
                fødselsnummer = packet["fødselsnummer"].asText(),
                oid = UUID.fromString(packet["saksbehandlerOid"].asText()),
                navn = packet["saksbehandlerNavn"].asText(),
                ident = packet["saksbehandlerIdent"].asText(),
                epost = packet["saksbehandlerEpost"].asText(),
                organisasjonsnummer = packet["organisasjonsnummer"].asText(),
                overstyrteArbeidsforhold = packet["overstyrteArbeidsforhold"].map {
                    OverstyrArbeidsforholdDto.ArbeidsforholdOverstyrt(
                        it["orgnummer"].asText(),
                        it["deaktivert"].asBoolean(),
                        it["begrunnelse"].asText(),
                        it["forklaring"].asText()
                    )
                },
                skjæringstidspunkt = packet["skjæringstidspunkt"].asLocalDate(),
                opprettet = packet["@opprettet"].asLocalDateTime(),
                json = packet.toJson(),
                context = context
            )
        }
    }
}
