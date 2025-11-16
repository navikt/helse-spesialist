package no.nav.helse.spesialist.domain

import no.nav.helse.spesialist.domain.ddd.LateIdAggregateRoot
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
    val identitetsnummer: Identitetsnummer,
    val aktørId: String,
    info: Personinfo?,
    infoOppdatert: LocalDate?,
    val enhetRef: Int?,
    val enhetRefOppdatert: LocalDate?,
    val infotrygdutbetalingerRef: Int?,
    val infotrygdutbetalingerOppdatert: LocalDate?,
) : LateIdAggregateRoot<PersonId>(id) {
    var info: Personinfo? = info
        private set
    var infoOppdatert: LocalDate? = infoOppdatert
        private set

    fun oppdaterInfo(personinfo: Personinfo) {
        this.info = personinfo
        this.infoOppdatert = LocalDate.now()
    }

    object Factory {
        fun ny(
            identitetsnummer: Identitetsnummer,
            aktørId: String,
            info: Personinfo?,
        ) = Person(
            id = null,
            identitetsnummer = identitetsnummer,
            aktørId = aktørId,
            info = info,
            infoOppdatert = info?.let { LocalDate.now() },
            enhetRef = null,
            enhetRefOppdatert = null,
            infotrygdutbetalingerRef = null,
            infotrygdutbetalingerOppdatert = null,
        )

        fun fraLagring(
            id: PersonId?,
            identitetsnummer: Identitetsnummer,
            aktørId: String,
            info: Personinfo?,
            infoOppdatert: LocalDate?,
            enhetRef: Int?,
            enhetRefOppdatert: LocalDate?,
            infotrygdutbetalingerRef: Int?,
            infotrygdutbetalingerOppdatert: LocalDate?,
        ) = Person(
            id = id,
            identitetsnummer = identitetsnummer,
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
