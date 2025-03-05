package no.nav.helse.db

import no.nav.helse.modell.utbetaling.Utbetaling
import no.nav.helse.modell.utbetaling.Utbetalingsstatus
import no.nav.helse.modell.utbetaling.Utbetalingtype
import java.time.LocalDateTime
import java.util.UUID

interface UtbetalingDao {
    fun finnUtbetalingIdRef(utbetalingId: UUID): Long?

    fun hentUtbetaling(utbetalingId: UUID): Utbetaling

    fun nyUtbetalingStatus(
        utbetalingIdRef: Long,
        status: Utbetalingsstatus,
        opprettet: LocalDateTime,
        json: String,
    )

    fun nyttOppdrag(
        fagsystemId: String,
        mottaker: String,
    ): Long?

    fun opprettUtbetalingId(
        utbetalingId: UUID,
        fødselsnummer: String,
        organisasjonsnummer: String,
        type: Utbetalingtype,
        opprettet: LocalDateTime,
        arbeidsgiverFagsystemIdRef: Long,
        personFagsystemIdRef: Long,
        arbeidsgiverbeløp: Int,
        personbeløp: Int,
    ): Long

    fun opprettKobling(
        vedtaksperiodeId: UUID,
        utbetalingId: UUID,
    )

    fun erUtbetalingForkastet(utbetalingId: UUID): Boolean
}
