package no.nav.helse.spesialist.domain.ddd

abstract class AggregateRoot<IDTYPE : ValueObject>(
    id: IDTYPE,
) : Entity<IDTYPE>(id)
