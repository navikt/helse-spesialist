package no.nav.helse.spesialist.application

import no.nav.helse.db.UtbetalingDao
import no.nav.helse.modell.utbetaling.Utbetaling
import no.nav.helse.modell.utbetaling.Utbetalingsstatus
import no.nav.helse.modell.utbetaling.Utbetalingtype
import java.time.LocalDateTime
import java.util.UUID

class InMemoryUtbetalingDao : UtbetalingDao {
    private data class UtbetalingRecord(
        val utbetalingIdRef: Long,
        val utbetalingId: UUID,
        val fødselsnummer: String,
        val arbeidsgiverIdentifikator: String,
        val type: Utbetalingtype,
        val opprettet: LocalDateTime,
        val arbeidsgiverbeløp: Int,
        val personbeløp: Int,
    )

    private val utbetalinger = mutableMapOf<Long, UtbetalingRecord>()
    private var nesteUtbetalingIdRef = 1L

    val statusHistorikk = mutableMapOf<Long, MutableList<Utbetalingsstatus>>()
    val koblinger = mutableMapOf<UUID, MutableList<UUID>>()

    override fun finnUtbetalingIdRef(utbetalingId: UUID): Long? = utbetalinger.values.find { it.utbetalingId == utbetalingId }?.utbetalingIdRef

    override fun hentUtbetaling(utbetalingId: UUID): Utbetaling {
        val record = requireNotNull(utbetalinger.values.find { it.utbetalingId == utbetalingId }) { "Fant ikke utbetaling med id $utbetalingId" }
        return Utbetaling(
            utbetalingId = record.utbetalingId,
            arbeidsgiverbeløp = record.arbeidsgiverbeløp,
            personbeløp = record.personbeløp,
            type = record.type,
        )
    }

    override fun nyUtbetalingStatus(
        utbetalingIdRef: Long,
        status: Utbetalingsstatus,
        opprettet: LocalDateTime,
        json: String,
    ) {
        statusHistorikk.getOrPut(utbetalingIdRef) { mutableListOf() }.add(status)
    }

    override fun opprettUtbetalingId(
        utbetalingId: UUID,
        fødselsnummer: String,
        arbeidsgiverIdentifikator: String,
        type: Utbetalingtype,
        opprettet: LocalDateTime,
        arbeidsgiverbeløp: Int,
        personbeløp: Int,
    ): Long {
        val ref = nesteUtbetalingIdRef++
        utbetalinger[ref] =
            UtbetalingRecord(
                utbetalingIdRef = ref,
                utbetalingId = utbetalingId,
                fødselsnummer = fødselsnummer,
                arbeidsgiverIdentifikator = arbeidsgiverIdentifikator,
                type = type,
                opprettet = opprettet,
                arbeidsgiverbeløp = arbeidsgiverbeløp,
                personbeløp = personbeløp,
            )
        return ref
    }

    override fun opprettKobling(
        vedtaksperiodeId: UUID,
        utbetalingId: UUID,
    ) {
        koblinger.getOrPut(vedtaksperiodeId) { mutableListOf() }.add(utbetalingId)
    }
}
