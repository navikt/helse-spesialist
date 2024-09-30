package no.nav.helse.db

import no.nav.helse.modell.utbetaling.UtbetalingDao.TidligereUtbetalingerForVedtaksperiodeDto
import no.nav.helse.modell.utbetaling.Utbetalingsstatus
import no.nav.helse.modell.utbetaling.Utbetalingtype
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

interface UtbetalingRepository {
    fun finnUtbetalingIdRef(utbetalingId: UUID): Long?

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

    fun nyLinje(
        oppdragId: Long,
        fom: LocalDate,
        tom: LocalDate,
        totalbeløp: Int?,
    )

    fun opprettUtbetalingId(
        utbetalingId: UUID,
        fødselsnummer: String,
        orgnummer: String,
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

    fun fjernKobling(
        vedtaksperiodeId: UUID,
        utbetalingId: UUID,
    )

    fun utbetalingerForVedtaksperiode(vedtaksperiodeId: UUID): List<TidligereUtbetalingerForVedtaksperiodeDto>

    fun erUtbetalingForkastet(utbetalingId: UUID): Boolean
}
