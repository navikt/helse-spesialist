package no.nav.helse.spesialist.domain.ddd

abstract class AggregateRoot<IDTYPE>(id: IDTYPE?) : Entity<IDTYPE>(id)
