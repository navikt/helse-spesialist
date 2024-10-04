package no.nav.helse.modell.utbetaling

import no.nav.helse.db.OpptegnelseRepository
import no.nav.helse.db.UtbetalingRepository
import no.nav.helse.modell.kommando.Command
import no.nav.helse.modell.kommando.CommandContext
import no.nav.helse.modell.utbetaling.Utbetalingsstatus.ANNULLERT
import no.nav.helse.modell.utbetaling.Utbetalingsstatus.GODKJENT_UTEN_UTBETALING
import no.nav.helse.modell.utbetaling.Utbetalingsstatus.OVERFØRT
import no.nav.helse.modell.utbetaling.Utbetalingsstatus.UTBETALING_FEILET
import no.nav.helse.modell.utbetaling.Utbetalingsstatus.UTBETALT
import no.nav.helse.spesialist.api.abonnement.OpptegnelseType
import no.nav.helse.spesialist.api.abonnement.UtbetalingPayload
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.util.UUID

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
    private val utbetalingRepository: UtbetalingRepository,
    private val opptegnelseRepository: OpptegnelseRepository,
) : Command {
    private companion object {
        private val log = LoggerFactory.getLogger(LagreOppdragCommand::class.java)
    }

    internal class Oppdrag(
        private val fagsystemId: String,
        private val mottaker: String,
    ) {
        internal fun lagre(utbetalingRepository: UtbetalingRepository) = utbetalingRepository.nyttOppdrag(fagsystemId, mottaker)
    }

    override fun execute(context: CommandContext): Boolean {
        lagOpptegnelse()
        log.info("lagrer utbetaling $utbetalingId med status $status")
        lagre()
        return true
    }

    private fun lagre() {
        val utbetalingIdRef =
            utbetalingRepository.finnUtbetalingIdRef(utbetalingId)
                ?: run {
                    val arbeidsgiverFagsystemIdRef =
                        requireNotNull(arbeidsgiverOppdrag.lagre(utbetalingRepository)) { "Forventet arbeidsgiver fagsystemId ref" }
                    val personFagsystemIdRef =
                        requireNotNull(personOppdrag.lagre(utbetalingRepository)) { "Forventet person fagsystemId ref" }

                    utbetalingRepository.opprettUtbetalingId(
                        utbetalingId,
                        fødselsnummer,
                        orgnummer,
                        type,
                        opprettet,
                        arbeidsgiverFagsystemIdRef,
                        personFagsystemIdRef,
                        arbeidsgiverbeløp,
                        personbeløp,
                    )
                }

        utbetalingRepository.nyUtbetalingStatus(utbetalingIdRef, status, opprettet, json)
    }

    private fun lagOpptegnelse() {
        val opptegnelseType: OpptegnelseType =
            when {
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

        opptegnelseRepository.opprettOpptegnelse(fødselsnummer, UtbetalingPayload(utbetalingId), opptegnelseType)
    }
}
