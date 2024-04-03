package no.nav.helse.modell.utbetaling

import no.nav.helse.modell.utbetaling.Refusjonstype.DELVIS_REFUSJON
import no.nav.helse.modell.utbetaling.Refusjonstype.FULL_REFUSJON
import no.nav.helse.modell.utbetaling.Refusjonstype.INGEN_REFUSJON
import no.nav.helse.modell.utbetaling.Refusjonstype.INGEN_UTBETALING
import no.nav.helse.modell.utbetaling.Refusjonstype.NEGATIVT_BELØP
import java.util.UUID

internal class Utbetaling(
    internal val utbetalingId: UUID,
    private val arbeidsgiverbeløp: Int,
    private val personbeløp: Int,
    private val type: Utbetalingtype,
) {
    internal fun refusjonstype(): Refusjonstype {
        if (arbeidsgiverbeløp == 0 && personbeløp == 0) return INGEN_UTBETALING
        if (arbeidsgiverbeløp > 0 && personbeløp > 0) return DELVIS_REFUSJON
        if (arbeidsgiverbeløp == 0 && personbeløp > 0) return INGEN_REFUSJON
        if (arbeidsgiverbeløp > 0 && personbeløp == 0) return FULL_REFUSJON
        return NEGATIVT_BELØP
    }

    internal fun erRevurdering() = type == Utbetalingtype.REVURDERING

    internal fun kunUtbetalingTilArbeidsgiver() = arbeidsgiverbeløp != 0 && personbeløp == 0

    internal fun kunUtbetalingTilSykmeldt() = personbeløp != 0 && arbeidsgiverbeløp == 0

    internal fun delvisRefusjon() = personbeløp != 0 && arbeidsgiverbeløp != 0

    internal fun ingenUtbetaling() = arbeidsgiverbeløp == 0 && personbeløp == 0

    internal fun harEndringIUtbetalingTilSykmeldt() = personbeløp != 0

    override fun equals(other: Any?): Boolean =
        this === other || (
            other is Utbetaling &&
                this.javaClass == other.javaClass &&
                this.utbetalingId == other.utbetalingId &&
                this.arbeidsgiverbeløp == other.arbeidsgiverbeløp &&
                this.personbeløp == other.personbeløp
        )

    override fun hashCode(): Int {
        var result = utbetalingId.hashCode()
        result = 31 * result + arbeidsgiverbeløp
        result = 31 * result + personbeløp
        return result
    }

    override fun toString(): String {
        return "Utbetaling(utbetalingId=$utbetalingId, arbeidsgiverbeløp=$arbeidsgiverbeløp, personbeløp=$personbeløp, type=$type)"
    }
}

internal enum class Refusjonstype {
    FULL_REFUSJON,
    INGEN_REFUSJON,
    DELVIS_REFUSJON,
    INGEN_UTBETALING,
    NEGATIVT_BELØP,
}
