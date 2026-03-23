package no.nav.helse.spesialist.api.graphql

import no.nav.helse.db.StansAutomatiskBehandlingDao
import no.nav.helse.db.StansAutomatiskBehandlingFraDatabase
import no.nav.helse.modell.stoppautomatiskbehandling.StoppknappÅrsak
import no.nav.helse.spesialist.api.graphql.schema.ApiUnntattFraAutomatiskGodkjenning
import no.nav.helse.spesialist.application.logg.logg

class StansAutomatiskBehandlinghåndtererImpl(
    val stansAutomatiskBehandlingDao: StansAutomatiskBehandlingDao,
) : StansAutomatiskBehandlinghåndterer {
    override fun unntattFraAutomatiskGodkjenning(fødselsnummer: String): ApiUnntattFraAutomatiskGodkjenning =
        stansAutomatiskBehandlingDao
            .hentFor(fødselsnummer)
            .filtrerGjeldendeStopp()
            .tilUnntattFraAutomatiskGodkjenning()

    private fun List<StansAutomatiskBehandlingFraDatabase>.filtrerGjeldendeStopp(): List<StansAutomatiskBehandlingFraDatabase> {
        val gjeldende = mutableListOf<StansAutomatiskBehandlingFraDatabase>()
        sortedWith { a, b ->
            a.opprettet.compareTo(b.opprettet)
        }.forEach {
            when (it.status) {
                "STOPP_AUTOMATIKK" -> gjeldende += it
                "NORMAL" -> gjeldende.clear()
                else -> {
                    logg.error("Ukjent status-type: {}", it.status)
                    gjeldende += it
                }
            }
        }
        return gjeldende
    }

    private fun List<StansAutomatiskBehandlingFraDatabase>.tilUnntattFraAutomatiskGodkjenning() =
        if (isEmpty()) {
            ApiUnntattFraAutomatiskGodkjenning(
                erUnntatt = false,
                arsaker = emptyList(),
                tidspunkt = null,
            )
        } else {
            ApiUnntattFraAutomatiskGodkjenning(
                erUnntatt = true,
                arsaker = flatMap { it.årsaker.map(`StoppknappÅrsak`::name) },
                tidspunkt = last().opprettet,
            )
        }
}
