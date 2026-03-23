package no.nav.helse.spesialist.domain

import no.nav.helse.spesialist.domain.ddd.AggregateRoot
import java.time.LocalDate

class InfotrygdUtbetalinger private constructor(
    id: Identitetsnummer,
    val data: String,
    val oppdatert: LocalDate,
) : AggregateRoot<Identitetsnummer>(id) {
    object Factory {
        fun ny(
            id: Identitetsnummer,
            data: String,
        ): InfotrygdUtbetalinger =
            InfotrygdUtbetalinger(
                id = id,
                data = data,
                oppdatert = LocalDate.now(),
            )

        fun fraLagring(
            id: Identitetsnummer,
            data: String,
            oppdatert: LocalDate,
        ): InfotrygdUtbetalinger =
            InfotrygdUtbetalinger(
                id = id,
                data = data,
                oppdatert = oppdatert,
            )
    }
}
