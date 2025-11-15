package no.nav.helse.spesialist.application

import no.nav.helse.db.DefinisjonDao
import no.nav.helse.modell.varsel.Varseldefinisjon
import java.time.LocalDateTime
import java.util.UUID

class InMemoryDefinisjonDao(private val varseldefinisjonRepository: InMemoryVarseldefinisjonRepository) :
    DefinisjonDao {
    override fun definisjonFor(definisjonId: UUID): Varseldefinisjon {
        TODO("Not yet implemented")
    }

    override fun sisteDefinisjonFor(varselkode: String): Varseldefinisjon {
        TODO("Not yet implemented")
    }

    override fun lagreDefinisjon(
        unikId: UUID,
        kode: String,
        tittel: String,
        forklaring: String?,
        handling: String?,
        avviklet: Boolean,
        opprettet: LocalDateTime
    ) {
        TODO("Not yet implemented")
    }
}
