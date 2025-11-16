package no.nav.helse.spesialist.application

import no.nav.helse.spesialist.domain.ddd.AggregateRoot

abstract class AbstractInMemoryRepository<IDTYPE, T : AggregateRoot<IDTYPE>>() {
    private val data = mutableListOf<T>()

    protected abstract fun generateId(): IDTYPE

    protected abstract fun deepCopy(original: T): T

    fun finn(id: IDTYPE): T? = data.find { it.id() == id }?.let(::deepCopy)

    fun finnAlle(ider: Set<IDTYPE>): List<T> = data.filter { it.id() in ider }.map(::deepCopy)

    fun alle(): List<T> = data.map(::deepCopy)

    fun lagre(aggregateRoot: T) {
        if (aggregateRoot.harFåttTildeltId()) {
            data.removeIf { it.id() == aggregateRoot.id() }
        } else {
            aggregateRoot.tildelId(generateId())
        }
        data.add(aggregateRoot.let(::deepCopy))
    }

    fun slett(id: IDTYPE) {
        data.removeIf { it.id() == id }
    }
}
