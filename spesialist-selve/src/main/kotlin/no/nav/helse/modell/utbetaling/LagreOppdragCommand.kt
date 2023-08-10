package no.nav.helse.modell.utbetaling

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.modell.kommando.Command
import no.nav.helse.modell.kommando.CommandContext
import no.nav.helse.modell.utbetaling.Utbetalingsstatus.ANNULLERT
import no.nav.helse.modell.utbetaling.Utbetalingsstatus.GODKJENT_UTEN_UTBETALING
import no.nav.helse.modell.utbetaling.Utbetalingsstatus.OVERFØRT
import no.nav.helse.modell.utbetaling.Utbetalingsstatus.UTBETALING_FEILET
import no.nav.helse.modell.utbetaling.Utbetalingsstatus.UTBETALT
import no.nav.helse.spesialist.api.abonnement.OpptegnelseDao
import no.nav.helse.spesialist.api.abonnement.OpptegnelseType
import no.nav.helse.spesialist.api.abonnement.UtbetalingPayload
import org.slf4j.LoggerFactory

internal class LagreOppdragCommand(
    private val fødselsnummer: String,
    private val orgnummer: String,
    private val utbetalingId: UUID,
    private val type: Utbetalingtype,
    private val status: Utbetalingsstatus,
    private val opprettet: LocalDateTime,
    private val arbeidsgiverOppdrag: Oppdrag,
    private val personOppdrag: Oppdrag,
    private val arbeidsgiverbeløp: Int,
    private val personbeløp: Int,
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
        private val linjer: List<Utbetalingslinje>
    ) {
        internal fun lagre(utbetalingDao: UtbetalingDao) =
            utbetalingDao.nyttOppdrag(fagsystemId, mottaker)?.also {
                lagreLinjer(utbetalingDao, it)
            }

        private fun lagreLinjer(utbetalingDao: UtbetalingDao, oppdragId: Long) {
            linjer.forEach { it.lagre(utbetalingDao, oppdragId) }
        }

        internal class Utbetalingslinje(
            private val fom: LocalDate,
            private val tom: LocalDate,
            private val totalbeløp: Int?
        ) {
            internal fun lagre(utbetalingDao: UtbetalingDao, oppdragId: Long) {
                utbetalingDao.nyLinje(oppdragId, fom, tom, totalbeløp)
            }
        }
    }

    override fun execute(context: CommandContext): Boolean {
        lagOpptegnelse()
        log.info("lagrer utbetaling $utbetalingId med status $status")
        lagre()
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
                    personFagsystemIdRef,
                    arbeidsgiverbeløp,
                    personbeløp
                )
            }

        utbetalingDao.nyUtbetalingStatus(utbetalingIdRef, status, opprettet, json)
    }

    private fun lagOpptegnelse() {
        val opptegnelseType: OpptegnelseType = when {
            type == Utbetalingtype.ANNULLERING && status == UTBETALING_FEILET -> {
                OpptegnelseType.UTBETALING_ANNULLERING_FEILET
            }
            type == Utbetalingtype.ANNULLERING && status == ANNULLERT -> {
                OpptegnelseType.UTBETALING_ANNULLERING_OK
            }
            type == Utbetalingtype.REVURDERING && status in listOf(UTBETALT, GODKJENT_UTEN_UTBETALING, OVERFØRT) -> {
                OpptegnelseType.REVURDERING_FERDIGBEHANDLET
            }
            else -> return
        }

        opptegnelseDao.opprettOpptegnelse(fødselsnummer, UtbetalingPayload(utbetalingId), opptegnelseType)
    }
}
