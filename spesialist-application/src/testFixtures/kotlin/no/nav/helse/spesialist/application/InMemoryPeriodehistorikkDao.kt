package no.nav.helse.spesialist.application

import no.nav.helse.db.PeriodehistorikkDao
import no.nav.helse.modell.periodehistorikk.Historikkinnslag
import no.nav.helse.spesialist.domain.BehandlingUnikId
import java.util.*

class InMemoryPeriodehistorikkDao : PeriodehistorikkDao {
    val behandlingData = mutableMapOf<UUID, List<Historikkinnslag>>()
    val oppgaveData = mutableMapOf<Long, List<Historikkinnslag>>()

    override fun lagreMedOppgaveId(
        historikkinnslag: Historikkinnslag,
        oppgaveId: Long,
    ) {
        oppgaveData[oppgaveId] = (oppgaveData[oppgaveId] ?: emptyList()) + historikkinnslag
    }

    override fun lagre(
        historikkinnslag: Historikkinnslag,
        behandlingUnikId: UUID,
    ) {
        behandlingData[behandlingUnikId] = (behandlingData[behandlingUnikId] ?: emptyList()) + historikkinnslag
    }

    fun finnForOppgave(oppgaveId: Long): List<Historikkinnslag> = oppgaveData[oppgaveId] ?: emptyList()

    fun finnForBehandling(behandlingId: BehandlingUnikId): List<Historikkinnslag> = behandlingData[behandlingId.value] ?: emptyList()
}
