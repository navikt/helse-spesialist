package no.nav.helse.spesialist.application

import no.nav.helse.spesialist.domain.ddd.AggregateRoot
import no.nav.helse.spesialist.domain.ddd.ValueObject

abstract class AbstractInMemoryRepository<IDTYPE : ValueObject, T : AggregateRoot<IDTYPE>>() {
    private val data = mutableListOf<T>()

    protected abstract fun deepCopy(original: T): T

    fun finn(id: IDTYPE): T? = data.find { it.id == id }?.let(::deepCopy)

    fun finnAlle(ider: Set<IDTYPE>): List<T> = data.filter { it.id in ider }.map(::deepCopy)

    fun alle(): List<T> = data.map(::deepCopy)

    fun lagre(root: T) {
        data.removeIf { it.id == root.id }
        data.add(deepCopy(root))
    }

    fun lagreAlle(roots: Collection<T>) {
        roots.forEach(::lagre)
    }

    fun slett(id: IDTYPE) {
        data.removeIf { it.id == id }
    }
}
