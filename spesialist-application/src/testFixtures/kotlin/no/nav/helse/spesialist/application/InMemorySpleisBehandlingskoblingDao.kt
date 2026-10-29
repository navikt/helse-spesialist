package no.nav.helse.spesialist.application

import no.nav.helse.db.SpleisBehandlingskoblingDao
import no.nav.helse.db.SpleisBehandlingskoblingDao.SpesialistBehandling
import java.util.UUID

class InMemorySpleisBehandlingskoblingDao : SpleisBehandlingskoblingDao {
    val behandlinger = mutableMapOf<UUID, MutableList<SpesialistBehandling>>()
    val varslerUtenSpleisBehandlingId = mutableMapOf<Long, Int>()
    val koblinger = mutableMapOf<Long, UUID>()

    override fun finnBehandlinger(vedtaksperiodeId: UUID): List<SpesialistBehandling> = behandlinger[vedtaksperiodeId].orEmpty()

    override fun antallVarslerUtenSpleisBehandlingId(behandlingRef: Long): Int = varslerUtenSpleisBehandlingId[behandlingRef] ?: 0

    override fun settSpleisBehandlingIdPåVarsler(
        behandlingRef: Long,
        spleisBehandlingId: UUID,
    ): Int {
        koblinger[behandlingRef] = spleisBehandlingId
        return varslerUtenSpleisBehandlingId.remove(behandlingRef) ?: 0
    }
}
