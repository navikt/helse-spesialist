package no.nav.helse.modell.utbetaling

import java.util.UUID
import no.nav.helse.modell.utbetaling.Refusjonstype.DELVIS_REFUSJON
import no.nav.helse.modell.utbetaling.Refusjonstype.FULL_REFUSJON
import no.nav.helse.modell.utbetaling.Refusjonstype.INGEN_REFUSJON
import no.nav.helse.modell.utbetaling.Refusjonstype.INGEN_UTBETALING
import no.nav.helse.modell.utbetaling.Refusjonstype.NEGATIVT_BELØP

class Utbetaling(
    private val utbetalingId: UUID,
    private val arbeidsgiverbeløp: Int,
    private val personbeløp: Int,
) {
    internal fun refusjonstype(): Refusjonstype {
        if (arbeidsgiverbeløp == 0 && personbeløp == 0) return INGEN_UTBETALING
        if (arbeidsgiverbeløp > 0 && personbeløp > 0) return DELVIS_REFUSJON
        if (arbeidsgiverbeløp == 0 && personbeløp > 0) return INGEN_REFUSJON
        if (arbeidsgiverbeløp > 0 && personbeløp == 0) return FULL_REFUSJON
        return NEGATIVT_BELØP
    }
}

internal enum class Refusjonstype {
    FULL_REFUSJON,
    INGEN_REFUSJON,
    DELVIS_REFUSJON,
    INGEN_UTBETALING,
    NEGATIVT_BELØP
}