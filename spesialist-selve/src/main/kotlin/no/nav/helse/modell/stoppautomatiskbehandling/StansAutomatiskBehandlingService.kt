package no.nav.helse.modell.stoppautomatiskbehandling

import no.nav.helse.db.StansAutomatiskBehandlingDao
import no.nav.helse.db.StansAutomatiskBehandlingFraDatabase
import no.nav.helse.spesialist.api.StansAutomatiskBehandlinghåndterer
import no.nav.helse.spesialist.api.graphql.schema.UnntattFraAutomatiskGodkjenning
import java.time.format.DateTimeFormatter

class StansAutomatiskBehandlingService(private val stansAutomatiskBehandlingDao: StansAutomatiskBehandlingDao) :
    StansAutomatiskBehandlinghåndterer {
    override fun unntattFraAutomatiskGodkjenning(fødselsnummer: String): UnntattFraAutomatiskGodkjenning =
        stansAutomatiskBehandlingDao.hent(fødselsnummer).filtrerGjeldendeStopp().tilUnntattFraAutomatiskGodkjenning()

    override fun erUnntatt(fødselsnummer: String) = stansAutomatiskBehandlingDao.hent(fødselsnummer).filtrerGjeldendeStopp().isNotEmpty()

    private fun List<StansAutomatiskBehandlingFraDatabase>.filtrerGjeldendeStopp(): List<StansAutomatiskBehandlingFraDatabase> {
        val gjeldende = mutableListOf<StansAutomatiskBehandlingFraDatabase>()
        this.sortedWith { a, b ->
            a.opprettet.compareTo(b.opprettet)
        }.forEach {
            when (it.status) {
                Status.STOPP_AUTOMATIKK -> gjeldende += it
                Status.NORMAL -> gjeldende.clear()
            }
        }
        return gjeldende
    }

    private fun List<StansAutomatiskBehandlingFraDatabase>.tilUnntattFraAutomatiskGodkjenning(): UnntattFraAutomatiskGodkjenning =
        if (this.isEmpty()) {
            UnntattFraAutomatiskGodkjenning(
                erUnntatt = false,
                arsaker = emptyList(),
                tidspunkt = null,
            )
        } else {
            UnntattFraAutomatiskGodkjenning(
                erUnntatt = true,
                arsaker = this.flatMap { it.årsaker }.map { it.name }.toList(),
                tidspunkt = this.last().opprettet.format(DateTimeFormatter.ISO_DATE_TIME),
            )
        }
}
