package no.nav.helse.modell.utbetaling

import no.nav.helse.db.SessionContext
import no.nav.helse.modell.kommando.Command
import no.nav.helse.modell.kommando.CommandContext
import no.nav.helse.modell.utbetaling.Utbetalingsstatus.ANNULLERT
import no.nav.helse.modell.utbetaling.Utbetalingsstatus.GODKJENT_UTEN_UTBETALING
import no.nav.helse.modell.utbetaling.Utbetalingsstatus.OVERFØRT
import no.nav.helse.modell.utbetaling.Utbetalingsstatus.UTBETALING_FEILET
import no.nav.helse.modell.utbetaling.Utbetalingsstatus.UTBETALT
import no.nav.helse.spesialist.application.Outbox
import no.nav.helse.spesialist.domain.Identitetsnummer
import no.nav.helse.spesialist.domain.Opptegnelse
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.util.UUID

class LagreUtbetalingCommand(
    private val fødselsnummer: String,
    private val orgnummer: String,
    private val utbetalingId: UUID,
    private val type: Utbetalingtype,
    private val status: Utbetalingsstatus,
    private val opprettet: LocalDateTime,
    private val arbeidsgiverbeløp: Int,
    private val personbeløp: Int,
    private val json: String,
) : Command {
    private companion object {
        private val log = LoggerFactory.getLogger(LagreUtbetalingCommand::class.java)
    }

    override fun execute(
        commandContext: CommandContext,
        sessionContext: SessionContext,
        outbox: Outbox,
    ): Boolean {
        lagOpptegnelse(sessionContext)
        log.info("lagrer utbetaling $utbetalingId med status $status")
        lagre(sessionContext)
        return true
    }

    private fun lagre(sessionContext: SessionContext) {
        val utbetalingDao = sessionContext.utbetalingDao
        val utbetalingIdRef =
            utbetalingDao.finnUtbetalingIdRef(utbetalingId)
                ?: utbetalingDao.opprettUtbetalingId(
                    utbetalingId,
                    fødselsnummer,
                    orgnummer,
                    type,
                    opprettet,
                    arbeidsgiverbeløp,
                    personbeløp,
                )

        utbetalingDao.nyUtbetalingStatus(utbetalingIdRef, status, opprettet, json)
    }

    private fun lagOpptegnelse(sessionContext: SessionContext) {
        val opptegnelseType: Opptegnelse.Type =
            when (type) {
                Utbetalingtype.ANNULLERING if status == UTBETALING_FEILET -> {
                    Opptegnelse.Type.UTBETALING_ANNULLERING_FEILET
                }

                Utbetalingtype.ANNULLERING if status == ANNULLERT -> {
                    Opptegnelse.Type.UTBETALING_ANNULLERING_OK
                }

                Utbetalingtype.REVURDERING if status in
                    listOf(
                        UTBETALT,
                        GODKJENT_UTEN_UTBETALING,
                        OVERFØRT,
                    )
                -> {
                    Opptegnelse.Type.REVURDERING_FERDIGBEHANDLET
                }

                else -> {
                    return
                }
            }

        val opptegnelse =
            Opptegnelse.ny(
                identitetsnummer = Identitetsnummer.fraString(fødselsnummer),
                type = opptegnelseType,
            )
        sessionContext.opptegnelseRepository.lagre(opptegnelse)
    }
}
