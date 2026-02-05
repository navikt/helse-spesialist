package no.nav.helse.modell.oppgave

enum class Mottaker {
    UtbetalingTilSykmeldt,
    DelvisRefusjon,
    UtbetalingTilArbeidsgiver,
    IngenUtbetaling,
    ;

    fun dbVerdi() =
        when (this) {
            UtbetalingTilSykmeldt -> "UtbetalingTilArbeidsgiver"
            UtbetalingTilArbeidsgiver -> "UtbetalingTilArbeidsgiver"
            IngenUtbetaling -> "IngenUtbetaling"
            DelvisRefusjon -> "DelvisRefusjon"
        }
}
