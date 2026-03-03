package no.nav.helse.spesialist.application

import no.nav.helse.db.PeriodehistorikkDao
import no.nav.helse.modell.periodehistorikk.Historikkinnslag
import java.util.UUID

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
        behandlingId: UUID,
    ) {
        behandlingData[behandlingId] = (behandlingData[behandlingId] ?: emptyList()) + historikkinnslag
    }

    fun finnForBehandling(behandlingId: UUID): List<Historikkinnslag> = behandlingData[behandlingId] ?: emptyList()
    fun finnForOppgave(oppgaveId: Long): List<Historikkinnslag> = oppgaveData[oppgaveId] ?: emptyList()

}
