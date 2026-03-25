package no.nav.helse.spesialist.application

import no.nav.helse.db.PåVentDao
import java.util.UUID

class DelegatingPåVentDao(
    private val påVentRepository: InMemoryPåVentRepository,
    private val oppgaveRepository: InMemoryOppgaveRepository,
) : PåVentDao {
    override fun erPåVent(vedtaksperiodeId: UUID): Boolean = påVentRepository.alle().any { it.vedtaksperiodeId.value == vedtaksperiodeId }

    override fun slettPåVent(oppgaveId: Long): Int {
        val vedtaksperiodeId = oppgaveRepository.finn(oppgaveId)!!.vedtaksperiodeId
        val påVenter =
            påVentRepository
                .alle()
                .filter { it.vedtaksperiodeId.value == vedtaksperiodeId.value }
                .onEach { påVentRepository.slett(it.id()) }
        return påVenter.size
    }
}
