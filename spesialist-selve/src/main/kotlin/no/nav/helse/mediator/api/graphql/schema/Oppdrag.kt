package no.nav.helse.mediator.api.graphql.schema

import no.nav.helse.mediator.graphql.LocalDate
import no.nav.helse.mediator.graphql.LocalDateTime
import no.nav.helse.modell.utbetaling.UtbetalingDao
import no.nav.helse.modell.utbetaling.Utbetalingsstatus
import java.time.format.DateTimeFormatter

enum class Oppdragsstatus {
    GODKJENT,
    SENDT,
    OVERFORT,
    UTBETALING_FEILET,
    UTBETALT,
    ANNULLERT,
    IKKE_UTBETALT,
    FORKASTET,
    IKKE_GODKJENT,
    GODKJENT_UTEN_UTBETALING,
}

data class Utbetalingslinje(
    val fom: LocalDate,
    val tom: LocalDate,
    val totalbelop: Int
)

interface Spennoppdrag {
    val fagsystemId: String
    val linjer: List<Utbetalingslinje>
}

data class Personoppdrag(
    override val fagsystemId: String,
    override val linjer: List<Utbetalingslinje>,
    val fodselsnummer: String
) : Spennoppdrag

data class Arbeidsgiveroppdrag(
    override val fagsystemId: String,
    override val linjer: List<Utbetalingslinje>,
    val organisasjonsnummer: String
) : Spennoppdrag

data class Annullering(
    val tidspunkt: LocalDateTime,
    val saksbehandler: String
)

data class Oppdrag(private val utbetaling: UtbetalingDao.UtbetalingDto) {
    fun type() = utbetaling.type

    fun status() = utbetaling.status.tilOppdragsstatus()

    fun arbeidsgiveroppdrag() = utbetaling.arbeidsgiveroppdrag?.let {
        Arbeidsgiveroppdrag(
            fagsystemId = it.fagsystemId,
            linjer = it.linjer.tilUtbetalingslinjer(),
            organisasjonsnummer = it.mottaker,
        )
    }

    fun personoppdrag() = utbetaling.personoppdrag?.let {
        Personoppdrag(
            fagsystemId = it.fagsystemId,
            linjer = it.linjer.tilUtbetalingslinjer(),
            fodselsnummer = it.mottaker,
        )
    }

    fun annullering() = utbetaling.annullertAvSaksbehandler?.let {
        Annullering(
            tidspunkt = it.annullertTidspunkt.format(DateTimeFormatter.ISO_DATE_TIME),
            saksbehandler = it.saksbehandlerNavn
        )
    }

    fun totalbelop() = utbetaling.totalbeløp

    private fun List<UtbetalingDao.UtbetalingDto.OppdragDto.UtbetalingLinje>.tilUtbetalingslinjer() =
        map {
            Utbetalingslinje(
                fom = it.fom.format(DateTimeFormatter.ISO_DATE),
                tom = it.tom.format(DateTimeFormatter.ISO_DATE),
                totalbelop = it.totalbeløp ?: 0
            )
        }

    private fun Utbetalingsstatus.tilOppdragsstatus(): Oppdragsstatus =
        when (this) {
            Utbetalingsstatus.GODKJENT -> Oppdragsstatus.GODKJENT
            Utbetalingsstatus.SENDT -> Oppdragsstatus.SENDT
            Utbetalingsstatus.OVERFØRT -> Oppdragsstatus.OVERFORT
            Utbetalingsstatus.UTBETALING_FEILET -> Oppdragsstatus.UTBETALING_FEILET
            Utbetalingsstatus.UTBETALT -> Oppdragsstatus.UTBETALT
            Utbetalingsstatus.ANNULLERT -> Oppdragsstatus.ANNULLERT
            Utbetalingsstatus.IKKE_UTBETALT -> Oppdragsstatus.IKKE_UTBETALT
            Utbetalingsstatus.FORKASTET -> Oppdragsstatus.FORKASTET
            Utbetalingsstatus.IKKE_GODKJENT -> Oppdragsstatus.IKKE_GODKJENT
            Utbetalingsstatus.GODKJENT_UTEN_UTBETALING -> Oppdragsstatus.GODKJENT_UTEN_UTBETALING
        }
}
