package no.nav.helse.modell.utbetaling

import java.util.EnumSet

enum class Utbetalingsstatus {
    GODKJENT,
    SENDT,
    OVERFØRT,
    UTBETALING_FEILET,
    UTBETALT,
    ANNULLERT,
    IKKE_UTBETALT,
    FORKASTET,
    IKKE_GODKJENT,
    GODKJENT_UTEN_UTBETALING,
    NY,
    ;

    companion object {
        val gyldigeStatuser = EnumSet.allOf(Utbetalingsstatus::class.java)

        fun EnumSet<Utbetalingsstatus>.values() = this.map(Utbetalingsstatus::toString)
    }
}

enum class Utbetalingtype {
    UTBETALING,
    ETTERUTBETALING,
    ANNULLERING,
    REVURDERING,
    FERIEPENGER,
    ;

    companion object {
        val gyldigeTyper = EnumSet.allOf(Utbetalingtype::class.java)

        fun EnumSet<Utbetalingtype>.values() = this.map(Utbetalingtype::toString)
    }
}
