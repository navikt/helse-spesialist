package no.nav.helse.mediator.meldinger.løsninger

import no.nav.helse.db.ÅpneGosysOppgaverRepository
import no.nav.helse.mediator.MeldingMediator
import no.nav.helse.mediator.SpesialistRiver
import no.nav.helse.mediator.asUUID
import no.nav.helse.mediator.oppgave.OppgaveService
import no.nav.helse.modell.gosysoppgaver.ÅpneGosysOppgaverDto
import no.nav.helse.modell.sykefraværstilfelle.Sykefraværstilfelle
import no.nav.helse.modell.varsel.Varselkode.SB_EX_1
import no.nav.helse.modell.varsel.Varselkode.SB_EX_3
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.River
import no.nav.helse.rapids_rivers.asLocalDateTime
import no.nav.helse.rapids_rivers.isMissingOrNull
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.util.UUID

internal class ÅpneGosysOppgaverløsning(
    private val opprettet: LocalDateTime,
    private val fødselsnummer: String,
    private val antall: Int?,
    private val oppslagFeilet: Boolean,
) {
    internal fun lagre(åpneGosysOppgaverRepository: ÅpneGosysOppgaverRepository) {
        åpneGosysOppgaverRepository.persisterÅpneGosysOppgaver(
            ÅpneGosysOppgaverDto(
                fødselsnummer = fødselsnummer,
                antall = antall,
                oppslagFeilet = oppslagFeilet,
                opprettet = opprettet,
            ),
        )
    }

    internal fun evaluer(
        vedtaksperiodeId: UUID,
        sykefraværstilfelle: Sykefraværstilfelle,
        harTildeltOppgave: Boolean,
        oppgaveService: OppgaveService,
    ) {
        varslerForOppslagFeilet(vedtaksperiodeId, sykefraværstilfelle)
        varslerForÅpneGosysOppgaver(vedtaksperiodeId, sykefraværstilfelle, harTildeltOppgave, oppgaveService)
    }

    private fun varslerForOppslagFeilet(
        vedtaksperiodeId: UUID,
        sykefraværstilfelle: Sykefraværstilfelle,
    ) {
        if (oppslagFeilet) {
            sykefraværstilfelle.håndter(SB_EX_3.nyttVarsel(vedtaksperiodeId))
        } else {
            sykefraværstilfelle.deaktiver(SB_EX_3.nyttVarsel(vedtaksperiodeId))
        }
    }

    private fun varslerForÅpneGosysOppgaver(
        vedtaksperiodeId: UUID,
        sykefraværstilfelle: Sykefraværstilfelle,
        harTildeltOppgave: Boolean,
        oppgaveService: OppgaveService,
    ) {
        if (antall == null) return

        when {
            antall > 0 -> {
                sykefraværstilfelle.håndter(SB_EX_1.nyttVarsel(vedtaksperiodeId))
                if (sykefraværstilfelle.harKunGosysvarsel(vedtaksperiodeId)) {
                    oppgaveService.leggTilGosysEgenskap(vedtaksperiodeId)
                }
            }

            antall == 0 && !harTildeltOppgave -> {
                oppgaveService.fjernGosysEgenskap(vedtaksperiodeId)
                sykefraværstilfelle.deaktiver(SB_EX_1.nyttVarsel(vedtaksperiodeId))
            }
        }
    }

    internal class ÅpneGosysOppgaverRiver(
        private val meldingMediator: MeldingMediator,
    ) : SpesialistRiver {
        private val sikkerLogg = LoggerFactory.getLogger("tjenestekall")

        override fun validations() =
            River.PacketValidation {
                it.demandValue("@event_name", "behov")
                it.demandValue("@final", true)
                it.demandAll("@behov", listOf("ÅpneOppgaver"))
                it.require("@opprettet") { message -> message.asLocalDateTime() }
                it.requireKey("@id", "contextId", "hendelseId", "fødselsnummer")
                it.require("@løsning.ÅpneOppgaver.antall") {}
                it.requireKey("@løsning.ÅpneOppgaver.oppslagFeilet")
            }

        override fun onPacket(
            packet: JsonMessage,
            context: MessageContext,
        ) {
            sikkerLogg.info("Mottok melding ÅpneOppgaverMessage:\n{}", packet.toJson())
            val opprettet = packet["@opprettet"].asLocalDateTime()
            val contextId = packet["contextId"].asUUID()
            val hendelseId = packet["hendelseId"].asUUID()
            val fødselsnummer = packet["fødselsnummer"].asText()

            val antall = packet["@løsning.ÅpneOppgaver.antall"].takeUnless { it.isMissingOrNull() }?.asInt()
            val oppslagFeilet = packet["@løsning.ÅpneOppgaver.oppslagFeilet"].asBoolean()

            val åpneGosysOppgaver =
                ÅpneGosysOppgaverløsning(
                    opprettet = opprettet,
                    fødselsnummer = fødselsnummer,
                    antall = antall,
                    oppslagFeilet = oppslagFeilet,
                )

            meldingMediator.løsning(
                hendelseId = hendelseId,
                contextId = contextId,
                behovId = packet["@id"].asUUID(),
                løsning = åpneGosysOppgaver,
                context = context,
            )
        }
    }
}
