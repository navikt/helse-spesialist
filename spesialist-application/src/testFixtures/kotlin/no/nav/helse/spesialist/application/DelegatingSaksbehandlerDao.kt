package no.nav.helse.spesialist.application

import no.nav.helse.db.SaksbehandlerDao
import no.nav.helse.spesialist.domain.Saksbehandler
import no.nav.helse.spesialist.domain.SaksbehandlerOid
import java.time.LocalDateTime
import java.util.UUID

class DelegatingSaksbehandlerDao(private val inMemorySaksbehandlerRepository: InMemorySaksbehandlerRepository) :
    SaksbehandlerDao {
    override fun hent(ident: String): Saksbehandler? =
        inMemorySaksbehandlerRepository.alle().find { it.ident == ident }

    override fun hentAlleAktiveSisteTreMnderEllerHarTildelteOppgaver(): List<Saksbehandler> =
        inMemorySaksbehandlerRepository.alle().toList()

    override fun opprettEllerOppdater(oid: UUID, navn: String, epost: String, ident: String): Int {
        inMemorySaksbehandlerRepository.lagre(
            Saksbehandler(
                id = SaksbehandlerOid(oid),
                navn = navn,
                epost = epost,
                ident = ident
            )
        )
        return 1
    }

    override fun oppdaterSistObservert(oid: UUID, sisteHandlingUtført: LocalDateTime): Int = 1
}
