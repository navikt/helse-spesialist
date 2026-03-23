package no.nav.helse.spesialist.domain.ddd

abstract class LateIdAggregateRoot<IDTYPE : ValueObject>(
    id: IDTYPE?,
) : LateIdEntity<IDTYPE>(id)
