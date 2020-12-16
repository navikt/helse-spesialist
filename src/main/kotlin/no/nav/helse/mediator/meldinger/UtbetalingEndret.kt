package no.nav.helse.mediator.meldinger

import com.fasterxml.jackson.databind.JsonNode
import net.logstash.logback.argument.StructuredArguments.keyValue
import no.nav.helse.mediator.IHendelseMediator
import no.nav.helse.modell.abonnement.OpptegnelseDao
import no.nav.helse.modell.abonnement.OpptegnelseType
import no.nav.helse.modell.abonnement.UtbetalingPayload
import no.nav.helse.modell.kommando.CommandContext
import no.nav.helse.modell.utbetaling.UtbetalingDao
import no.nav.helse.rapids_rivers.*
import no.nav.helse.rapids_rivers.River.PacketListener
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

internal class UtbetalingEndret(
    override val id: UUID,
    private val fødselsnummer: String,
    private val orgnummer: String,
    private val utbetalingId: UUID,
    private val type: String,
    private val status: String,
    private val opprettet: LocalDateTime,
    private val arbeidsgiverOppdrag: Oppdrag,
    private val personOppdrag: Oppdrag,
    private val json: String,
    private val utbetalingDao: UtbetalingDao,
    private val opptegnelseDao: OpptegnelseDao
) : Hendelse {
    internal class Oppdrag(
        private val fagsystemId: String,
        private val mottaker: String,
        private val fagområde: String,
        private val endringskode: String,
        private val sisteArbeidsgiverdag: LocalDate?,
        private val linjer: List<Utbetalingslinje>
    ) {
        internal fun lagre(utbetalingDao: UtbetalingDao) =
            utbetalingDao.nyttOppdrag(fagsystemId, mottaker, fagområde, endringskode, sisteArbeidsgiverdag)?.also {
                lagreLinjer(utbetalingDao, it)
            }

        private fun lagreLinjer(utbetalingDao: UtbetalingDao, oppdragId: Long) {
            linjer.forEach { it.lagre(utbetalingDao, oppdragId) }
        }

        internal class Utbetalingslinje(
            private val endringskode: String,
            private val klassekode: String,
            private val statuskode: String?,
            private val datoStatusFom: LocalDate?,
            private val fom: LocalDate,
            private val tom: LocalDate,
            private val dagsats: Int,
            private val lønn: Int,
            private val grad: Double,
            private val delytelseId: Int,
            private val refDelytelseId: Int?,
            private val refFagsystemId: String?
        ) {
            internal fun lagre(utbetalingDao: UtbetalingDao, oppdragId: Long) {
                utbetalingDao.nyLinje(oppdragId, endringskode, klassekode, statuskode, datoStatusFom, fom, tom, dagsats, lønn, grad, delytelseId, refDelytelseId, refFagsystemId)
            }
        }
    }

    private fun lagre() {
        val utbetalingIdRef = utbetalingDao.finnUtbetalingIdRef(utbetalingId)
            ?: run {
                val arbeidsgiverFagsystemIdRef = requireNotNull(arbeidsgiverOppdrag.lagre(utbetalingDao))  { "Forventet arbeidsgiver fagsystemId ref" }
                val personFagsystemIdRef = requireNotNull(personOppdrag.lagre(utbetalingDao)) { "Forventet person fagsystemId ref" }

                utbetalingDao.opprettUtbetalingId(utbetalingId, fødselsnummer, orgnummer, type, opprettet, arbeidsgiverFagsystemIdRef, personFagsystemIdRef)
            }

        utbetalingDao.nyUtbetalingStatus(utbetalingIdRef, status, opprettet, json)
    }

    private fun lagOpptegnelse() {
        if (type == "ANNULLERING") {
            val opptegnelseType: OpptegnelseType = when (status) {
                "UTBETALING_FEILET" -> { OpptegnelseType.UTBETALING_ANNULLERING_FEILET }
                "ANNULERT" -> { OpptegnelseType.UTBETALING_ANNULLERING_OK }
                else -> return
            }

            opptegnelseDao.opprettOpptegnelse(fødselsnummer, UtbetalingPayload(utbetalingId), opptegnelseType)
        }
    }

    private companion object {
        private val log = LoggerFactory.getLogger(UtbetalingEndret::class.java)
    }

    override fun fødselsnummer(): String = fødselsnummer
    override fun vedtaksperiodeId(): UUID? = null
    override fun toJson(): String = json

    override fun execute(context: CommandContext): Boolean {
        log.info("lagrer utbetaling $utbetalingId med status $status")
        lagre()
        lagOpptegnelse()
        return true
    }

    override fun resume(context: CommandContext) = true

    override fun undo(context: CommandContext) {}

    internal class River(
        rapidsConnection: RapidsConnection,
        private val mediator: IHendelseMediator
    ) : PacketListener {
        private val sikkerLogg: Logger = LoggerFactory.getLogger("tjenestekall")
        private val godkjenteStatuser = listOf("GODKJENT", "SENDT", "OVERFØRT", "UTBETALING_FEILET", "UTBETALT", "ANNULLERT")
        private val gyldigeStatuser = listOf("IKKE_UTBETALT", "FORKASTET", "IKKE_GODKJENT", "GODKJENT_UTEN_UTBETALING") + godkjenteStatuser

        init {
            River(rapidsConnection).apply {
                validate {
                    it.demandValue("@event_name", "utbetaling_endret")
                    it.requireKey("@id", "fødselsnummer", "organisasjonsnummer",
                        "utbetalingId", "arbeidsgiverOppdrag.fagsystemId", "personOppdrag.fagsystemId"
                    )
                    it.requireAny("type", listOf("UTBETALING", "ANNULLERING", "ETTERUTBETALING"))
                    it.requireAny("forrigeStatus", gyldigeStatuser)
                    it.requireAny("gjeldendeStatus", gyldigeStatuser)
                    it.require("@opprettet", JsonNode::asLocalDateTime)
                }
            }.register(this)
        }

        override fun onError(problems: MessageProblems, context: RapidsConnection.MessageContext) {
            sikkerLogg.error("Forstod ikke utbetaling_endret:\n${problems.toExtendedReport()}")
        }

        override fun onPacket(packet: JsonMessage, context: RapidsConnection.MessageContext) {
            val forrigeStatus = packet["forrigeStatus"].asText()
            val status = packet["gjeldendeStatus"].asText()
            if (status !in godkjenteStatuser && forrigeStatus !in godkjenteStatuser) return
            val id = UUID.fromString(packet["utbetalingId"].asText())
            val fødselsnummer = packet["fødselsnummer"].asText()
            val orgnummer = packet["organisasjonsnummer"].asText()
            sikkerLogg.info(
                "Mottok utbetalt event for {}, {}",
                keyValue("fødselsnummer", fødselsnummer),
                keyValue("utbetalingId", id)
            )
            mediator.utbetalingEndret(fødselsnummer, orgnummer, packet, context)
        }
    }
}
