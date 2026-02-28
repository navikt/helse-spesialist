package no.nav.helse.spesialist.application

import no.nav.helse.db.UtbetalingDao
import no.nav.helse.modell.utbetaling.Utbetaling
import no.nav.helse.modell.utbetaling.Utbetalingsstatus
import no.nav.helse.modell.utbetaling.Utbetalingtype
import java.time.LocalDateTime
import java.util.UUID

class UnimplementedUtbetalingDao : UtbetalingDao {
    override fun finnUtbetalingIdRef(utbetalingId: UUID): Long? {
        TODO("Not yet implemented")
    }

    override fun hentUtbetaling(utbetalingId: UUID): Utbetaling {
        TODO("Not yet implemented")
    }

    override fun nyUtbetalingStatus(
        utbetalingIdRef: Long,
        status: Utbetalingsstatus,
        opprettet: LocalDateTime,
        json: String
    ) {
        TODO("Not yet implemented")
    }

    override fun nyttOppdrag(fagsystemId: String, mottaker: String): Long? {
        TODO("Not yet implemented")
    }

    override fun opprettUtbetalingId(
        utbetalingId: UUID,
        fødselsnummer: String,
        arbeidsgiverIdentifikator: String,
        type: Utbetalingtype,
        opprettet: LocalDateTime,
        arbeidsgiverFagsystemIdRef: Long,
        personFagsystemIdRef: Long,
        arbeidsgiverbeløp: Int,
        personbeløp: Int
    ): Long {
        TODO("Not yet implemented")
    }

    override fun opprettKobling(vedtaksperiodeId: UUID, utbetalingId: UUID) {
        TODO("Not yet implemented")
    }

    override fun erUtbetalingForkastet(utbetalingId: UUID): Boolean {
        TODO("Not yet implemented")
    }
}
