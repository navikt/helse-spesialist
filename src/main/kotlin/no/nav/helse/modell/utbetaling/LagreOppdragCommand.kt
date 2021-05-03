package no.nav.helse.modell.utbetaling

import no.nav.helse.modell.abonnement.OpptegnelseDao
import no.nav.helse.modell.abonnement.OpptegnelseType
import no.nav.helse.modell.abonnement.UtbetalingPayload
import no.nav.helse.modell.kommando.Command
import no.nav.helse.modell.kommando.CommandContext
import no.nav.helse.modell.utbetaling.Utbetalingsstatus.ANNULLERT
import no.nav.helse.modell.utbetaling.Utbetalingsstatus.Companion.godkjenteStatuser
import no.nav.helse.modell.utbetaling.Utbetalingsstatus.UTBETALING_FEILET
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

internal class LagreOppdragCommand(
    private val fødselsnummer: String,
    private val orgnummer: String,
    private val utbetalingId: UUID,
    private val type: String,
    private val status: Utbetalingsstatus,
    private val forrigeStatus: Utbetalingsstatus,
    private val opprettet: LocalDateTime,
    private val arbeidsgiverOppdrag: Oppdrag,
    private val personOppdrag: Oppdrag,
    private val json: String,
    private val utbetalingDao: UtbetalingDao,
    private val opptegnelseDao: OpptegnelseDao
) : Command {

    private companion object {
        private val log = LoggerFactory.getLogger(LagreOppdragCommand::class.java)
    }

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
            private val totalbeløp: Int?,
            private val lønn: Int,
            private val grad: Double,
            private val delytelseId: Int,
            private val refDelytelseId: Int?,
            private val refFagsystemId: String?
        ) {
            internal fun lagre(utbetalingDao: UtbetalingDao, oppdragId: Long) {
                utbetalingDao.nyLinje(
                    oppdragId,
                    endringskode,
                    klassekode,
                    statuskode,
                    datoStatusFom,
                    fom,
                    tom,
                    dagsats,
                    totalbeløp,
                    lønn,
                    grad,
                    delytelseId,
                    refDelytelseId,
                    refFagsystemId
                )
            }
        }
    }

    override fun execute(context: CommandContext): Boolean {
        if (status !in godkjenteStatuser && forrigeStatus !in godkjenteStatuser) return true
        log.info("lagrer utbetaling $utbetalingId med status $status")
        lagre()
        lagOpptegnelse()
        return true
    }

    private fun lagre() {
        val utbetalingIdRef = utbetalingDao.finnUtbetalingIdRef(utbetalingId)
            ?: run {
                val arbeidsgiverFagsystemIdRef =
                    requireNotNull(arbeidsgiverOppdrag.lagre(utbetalingDao)) { "Forventet arbeidsgiver fagsystemId ref" }
                val personFagsystemIdRef =
                    requireNotNull(personOppdrag.lagre(utbetalingDao)) { "Forventet person fagsystemId ref" }

                utbetalingDao.opprettUtbetalingId(
                    utbetalingId,
                    fødselsnummer,
                    orgnummer,
                    type,
                    opprettet,
                    arbeidsgiverFagsystemIdRef,
                    personFagsystemIdRef
                )
            }

        utbetalingDao.nyUtbetalingStatus(utbetalingIdRef, status, opprettet, json)
    }

    private fun lagOpptegnelse() {
        if (type == "ANNULLERING") {
            val opptegnelseType: OpptegnelseType = when (status) {
                UTBETALING_FEILET -> {
                    OpptegnelseType.UTBETALING_ANNULLERING_FEILET
                }
                ANNULLERT -> {
                    OpptegnelseType.UTBETALING_ANNULLERING_OK
                }
                else -> return
            }

            opptegnelseDao.opprettOpptegnelse(fødselsnummer, UtbetalingPayload(utbetalingId), opptegnelseType)
        }
    }

    override fun resume(context: CommandContext) = true

    override fun undo(context: CommandContext) {}
}
