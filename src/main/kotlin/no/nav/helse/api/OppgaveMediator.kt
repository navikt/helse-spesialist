package no.nav.helse.api

import no.nav.helse.modell.dao.OppgaveDao
import no.nav.helse.modell.dao.PersonDao
import no.nav.helse.modell.dao.VedtakDao
import no.nav.helse.modell.dto.SaksbehandleroppgaveDto

internal class OppgaveMediator(
    private val oppgaveDao: OppgaveDao,
    private val vedtakDao: VedtakDao,
    private val personDao: PersonDao
) {
    fun hentOppgaver(): List<SaksbehandleroppgaveDto> {
        val oppgaver = oppgaveDao.findSaksbehandlerOppgaver() ?: listOf()
        return oppgaver
            .map { it to vedtakDao.findVedtak(it.vedtaksref!!) }
            .map { (oppgave, vedtak) ->
                val personDto = personDao.findPerson(vedtak.personRef) ?: return@map null
                SaksbehandleroppgaveDto(
                    spleisbehovId = oppgave.behovId,
                    opprettet = oppgave.opprettet,
                    vedtaksperiodeId = vedtak.vedtaksperiodeId,
                    periodeFom = vedtak.fom,
                    periodeTom = vedtak.tom,
                    navn = personDto.navn,
                    fødselsnummer = personDto.fødselsnummer
                )
            }.filterNotNull()
    }
}
