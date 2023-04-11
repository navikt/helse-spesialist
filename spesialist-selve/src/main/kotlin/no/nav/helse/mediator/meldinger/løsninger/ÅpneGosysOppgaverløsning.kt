package no.nav.helse.mediator.meldinger.løsninger

import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.mediator.HendelseMediator
import no.nav.helse.modell.WarningDao
import no.nav.helse.modell.gosysoppgaver.ÅpneGosysOppgaverDao
import no.nav.helse.modell.gosysoppgaver.ÅpneGosysOppgaverDto
import no.nav.helse.modell.sykefraværstilfelle.Sykefraværstilfelle
import no.nav.helse.modell.varsel.VarselRepository
import no.nav.helse.modell.varsel.Varselkode
import no.nav.helse.modell.varsel.Varselkode.SB_EX_1
import no.nav.helse.modell.varsel.Varselkode.SB_EX_3
import no.nav.helse.modell.vedtak.Warning
import no.nav.helse.modell.vedtak.WarningKilde
import no.nav.helse.modell.vedtaksperiode.GenerasjonRepository
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.helse.rapids_rivers.asLocalDateTime
import no.nav.helse.rapids_rivers.isMissingOrNull
import no.nav.helse.tellWarning
import no.nav.helse.tellWarningInaktiv
import org.slf4j.LoggerFactory

internal class ÅpneGosysOppgaverløsning(
    private val opprettet: LocalDateTime,
    private val fødselsnummer: String,
    private val antall: Int?,
    private val oppslagFeilet: Boolean,
) {
    internal fun lagre(åpneGosysOppgaverDao: ÅpneGosysOppgaverDao) {
        åpneGosysOppgaverDao.persisterÅpneGosysOppgaver(
            ÅpneGosysOppgaverDto(
                fødselsnummer = fødselsnummer,
                antall = antall,
                oppslagFeilet = oppslagFeilet,
                opprettet = opprettet
            )
        )
    }

    internal fun evaluer(warningDao: WarningDao, varselRepository: VarselRepository, generasjonRepository: GenerasjonRepository, vedtaksperiodeId: UUID, sykefraværstilfelle: Sykefraværstilfelle) {
        warningsForOppslagFeilet(warningDao, varselRepository, generasjonRepository, vedtaksperiodeId, sykefraværstilfelle)
        warningsForÅpneGosysOppgaver(warningDao, varselRepository, generasjonRepository, vedtaksperiodeId, sykefraværstilfelle)
    }

    private fun warningsForOppslagFeilet(warningDao: WarningDao, varselRepository: VarselRepository, generasjonRepository: GenerasjonRepository, vedtaksperiodeId: UUID, sykefraværstilfelle: Sykefraværstilfelle) {
        val melding = "Kunne ikke sjekke åpne oppgaver på sykepenger i Gosys"

        if (oppslagFeilet) {
            sykefraværstilfelle.håndter(SB_EX_3.nyttVarsel(vedtaksperiodeId))
            leggTilWarning(warningDao, vedtaksperiodeId, melding)
        } else {
            setEksisterendeWarningInaktive(warningDao, vedtaksperiodeId, melding)
            deaktiverVarsel(varselRepository, generasjonRepository, vedtaksperiodeId, SB_EX_3)
        }
    }

    private fun warningsForÅpneGosysOppgaver(warningDao: WarningDao, varselRepository: VarselRepository, generasjonRepository: GenerasjonRepository, vedtaksperiodeId: UUID, sykefraværstilfelle: Sykefraværstilfelle) {
        if (antall == null) return
        val melding = "Det finnes åpne oppgaver på sykepenger i Gosys"

        val harAlleredeVarsel = warningDao.finnAktiveWarningsMedMelding(vedtaksperiodeId, melding).isNotEmpty()
        when {
            antall > 0 && !harAlleredeVarsel-> {
                sykefraværstilfelle.håndter(SB_EX_1.nyttVarsel(vedtaksperiodeId))
                leggTilWarning(warningDao, vedtaksperiodeId, melding)
            }
            antall == 0 && harAlleredeVarsel -> {
                setEksisterendeWarningInaktive(warningDao, vedtaksperiodeId, melding)
                deaktiverVarsel(varselRepository, generasjonRepository, vedtaksperiodeId, SB_EX_1)
            }
        }
    }

    private fun leggTilWarning(warningDao: WarningDao, vedtaksperiodeId: UUID, melding: String) {
        warningDao.leggTilWarning(vedtaksperiodeId, Warning(melding, WarningKilde.Spesialist, LocalDateTime.now()))
        tellWarning(melding)
    }

    private fun setEksisterendeWarningInaktive(warningDao: WarningDao, vedtaksperiodeId: UUID, melding: String) {
        warningDao.setWarningMedMeldingInaktiv(vedtaksperiodeId, melding, LocalDateTime.now())
        tellWarningInaktiv(melding)
    }

    private fun deaktiverVarsel(varselRepository: VarselRepository, generasjonRepository: GenerasjonRepository, vedtaksperiodeId: UUID, varselkode: Varselkode) {
        val generasjon = generasjonRepository.sisteFor(vedtaksperiodeId)
        varselkode.deaktiverFor(generasjon, varselRepository)
    }

    internal class ÅpneGosysOppgaverRiver(
        rapidsConnection: RapidsConnection,
        private val hendelseMediator: HendelseMediator,
    ) : River.PacketListener {
        private val sikkerLogg = LoggerFactory.getLogger("tjenestekall")

        init {
            River(rapidsConnection).apply {
                validate {
                    it.demandValue("@event_name", "behov")
                    it.demandValue("@final", true)
                    it.demandAll("@behov", listOf("ÅpneOppgaver"))
                    it.require("@opprettet") { message -> message.asLocalDateTime() }
                    it.requireKey("@id", "contextId", "hendelseId", "fødselsnummer")
                    it.require("@løsning.ÅpneOppgaver.antall") {}
                    it.requireKey("@løsning.ÅpneOppgaver.oppslagFeilet")
                }
            }.register(this)
        }

        override fun onPacket(packet: JsonMessage, context: MessageContext) {
            sikkerLogg.info("Mottok melding ÅpneOppgaverMessage:\n{}", packet.toJson())
            val opprettet = packet["@opprettet"].asLocalDateTime()
            val contextId = UUID.fromString(packet["contextId"].asText())
            val hendelseId = UUID.fromString(packet["hendelseId"].asText())
            val fødselsnummer = packet["fødselsnummer"].asText()

            val antall = packet["@løsning.ÅpneOppgaver.antall"].takeUnless { it.isMissingOrNull() }?.asInt()
            val oppslagFeilet = packet["@løsning.ÅpneOppgaver.oppslagFeilet"].asBoolean()

            val åpneGosysOppgaver = ÅpneGosysOppgaverløsning(
                opprettet = opprettet,
                fødselsnummer = fødselsnummer,
                antall = antall,
                oppslagFeilet = oppslagFeilet
            )

            hendelseMediator.løsning(
                hendelseId = hendelseId,
                contextId = contextId,
                behovId = UUID.fromString(packet["@id"].asText()),
                løsning = åpneGosysOppgaver,
                context = context
            )
        }
    }
}
