package no.nav.helse.spesialist.domain

import no.nav.helse.spesialist.domain.ddd.AggregateRoot
import no.nav.helse.spesialist.domain.ddd.ValueObject
import java.time.LocalDate

@JvmInline
value class PersonId(
    val value: Int,
)

data class Personinfo(
    val fornavn: String,
    val mellomnavn: String?,
    val etternavn: String,
    val fødselsdato: LocalDate?,
    val kjønn: Kjønn?,
    val adressebeskyttelse: Adressebeskyttelse,
) : ValueObject {
    enum class Kjønn {
        Kvinne,
        Mann,
        Ukjent,
    }

    enum class Adressebeskyttelse {
        Ugradert,
        Fortrolig,
        StrengtFortrolig,
        StrengtFortroligUtland,
        Ukjent,
    }
}

class Person private constructor(
    id: PersonId?,
    val fødselsnummer: String,
    val aktørId: String,
    val info: Personinfo?,
    val infoOppdatert: LocalDate?,
    val enhetRef: Int?,
    val enhetRefOppdatert: LocalDate?,
    val infotrygdutbetalingerRef: Int?,
    val infotrygdutbetalingerOppdatert: LocalDate?,
) : AggregateRoot<PersonId>(id) {
    object Factory {
        fun fraLagring(
            id: PersonId?,
            fødselsnummer: String,
            aktørId: String,
            info: Personinfo?,
            infoOppdatert: LocalDate?,
            enhetRef: Int?,
            enhetRefOppdatert: LocalDate?,
            infotrygdutbetalingerRef: Int?,
            infotrygdutbetalingerOppdatert: LocalDate?,
        ) = Person(
            id = id,
            fødselsnummer = fødselsnummer,
            aktørId = aktørId,
            info = info,
            infoOppdatert = infoOppdatert,
            enhetRef = enhetRef,
            enhetRefOppdatert = enhetRefOppdatert,
            infotrygdutbetalingerRef = infotrygdutbetalingerRef,
            infotrygdutbetalingerOppdatert = infotrygdutbetalingerOppdatert,
        )
    }
}
