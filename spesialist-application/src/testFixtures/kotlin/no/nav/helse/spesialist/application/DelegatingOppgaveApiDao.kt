package no.nav.helse.spesialist.application

import no.nav.helse.db.api.OppgaveApiDao
import no.nav.helse.spesialist.api.oppgave.OppgaveForPeriodevisningDto
import no.nav.helse.spesialist.domain.VedtaksperiodeId
import java.util.UUID

class DelegatingOppgaveApiDao(
    private val oppgaveRepository: InMemoryOppgaveRepository,
    private val vedtaksperiodeRepository: InMemoryVedtaksperiodeRepository
) : OppgaveApiDao {
    override fun finnOppgaveId(fødselsnummer: String): Long? {
        TODO("Not yet implemented")
    }

    override fun finnPeriodeoppgave(vedtaksperiodeId: UUID): OppgaveForPeriodevisningDto? {
        TODO("Not yet implemented")
    }

    override fun finnFødselsnummer(oppgaveId: Long): String {
        val vedtaksperiodeId = oppgaveRepository.finn(oppgaveId)!!.vedtaksperiodeId
        return vedtaksperiodeRepository.finn(VedtaksperiodeId(vedtaksperiodeId))!!.fødselsnummer
    }
}
