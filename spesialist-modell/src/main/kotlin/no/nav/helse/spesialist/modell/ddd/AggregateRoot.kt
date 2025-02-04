package no.nav.helse.spesialist.modell.ddd

abstract class AggregateRoot<IDTYPE>(id: IDTYPE?) : Entity<IDTYPE>(id)
