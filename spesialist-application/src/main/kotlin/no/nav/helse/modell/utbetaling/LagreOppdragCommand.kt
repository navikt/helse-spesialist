package no.nav.helse.modell.utbetaling

import no.nav.helse.db.UtbetalingDao
import no.nav.helse.modell.kommando.Command
import no.nav.helse.modell.kommando.CommandContext
import no.nav.helse.modell.utbetaling.Utbetalingsstatus.ANNULLERT
import no.nav.helse.modell.utbetaling.Utbetalingsstatus.GODKJENT_UTEN_UTBETALING
import no.nav.helse.modell.utbetaling.Utbetalingsstatus.OVERFØRT
import no.nav.helse.modell.utbetaling.Utbetalingsstatus.UTBETALING_FEILET
import no.nav.helse.modell.utbetaling.Utbetalingsstatus.UTBETALT
import no.nav.helse.spesialist.application.OpptegnelseRepository
import no.nav.helse.spesialist.domain.Identitetsnummer
import no.nav.helse.spesialist.domain.Opptegnelse
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.util.UUID

class LagreOppdragCommand(
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
    private val opptegnelseRepository: OpptegnelseRepository,
) : Command {
    private companion object {
        private val log = LoggerFactory.getLogger(LagreOppdragCommand::class.java)
    }

    class Oppdrag(
        private val fagsystemId: String,
        private val mottaker: String,
    ) {
        internal fun lagre(utbetalingDao: UtbetalingDao) = utbetalingDao.nyttOppdrag(fagsystemId, mottaker)
    }

    override fun execute(context: CommandContext): Boolean {
        lagOpptegnelse()
        log.info("lagrer utbetaling $utbetalingId med status $status")
        lagre()
        return true
    }

    private fun lagre() {
        val utbetalingIdRef =
            utbetalingDao.finnUtbetalingIdRef(utbetalingId)
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
                        personbeløp,
                    )
                }

        utbetalingDao.nyUtbetalingStatus(utbetalingIdRef, status, opprettet, json)
    }

    private fun lagOpptegnelse() {
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
                else -> return
            }

        val opptegnelse =
            Opptegnelse.ny(
                identitetsnummer = Identitetsnummer.fraString(fødselsnummer),
                type = opptegnelseType,
            )
        opptegnelseRepository.lagre(opptegnelse)
    }
}
