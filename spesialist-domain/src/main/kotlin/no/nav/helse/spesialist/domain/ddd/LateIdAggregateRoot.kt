package no.nav.helse.spesialist.domain.ddd

abstract class LateIdAggregateRoot<IDTYPE>(
    id: IDTYPE?,
) : LateIdEntity<IDTYPE>(id)
