package no.nav.helse.modell.stoppautomatiskbehandling

import no.nav.helse.db.StansAutomatiskBehandlingDao
import no.nav.helse.db.StansAutomatiskBehandlingFraDatabase
import no.nav.helse.spesialist.api.StansAutomatiskBehandlinghåndterer
import no.nav.helse.spesialist.api.graphql.schema.UnntattFraAutomatiskGodkjenning
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class StansAutomatiskBehandlingService(private val stansAutomatiskBehandlingDao: StansAutomatiskBehandlingDao) :
    StansAutomatiskBehandlinghåndterer {
    private val logg = LoggerFactory.getLogger(this::class.java)

    override fun lagre(
        fødselsnummer: String,
        status: String,
        årsaker: Set<String>,
        opprettet: LocalDateTime,
        originalMelding: String,
        kilde: String,
    ) {
        stansAutomatiskBehandlingDao.lagre(
            fødselsnummer = fødselsnummer,
            status = status,
            årsaker = årsaker,
            opprettet = opprettet,
            originalMelding = originalMelding,
            kilde = kilde,
        )
    }

    override fun unntattFraAutomatiskGodkjenning(fødselsnummer: String): UnntattFraAutomatiskGodkjenning =
        stansAutomatiskBehandlingDao.hent(fødselsnummer).filtrerGjeldendeStopp().tilUnntattFraAutomatiskGodkjenning()

    override fun erUnntatt(fødselsnummer: String) = stansAutomatiskBehandlingDao.hent(fødselsnummer).filtrerGjeldendeStopp().isNotEmpty()

    private fun List<StansAutomatiskBehandlingFraDatabase>.filtrerGjeldendeStopp(): List<StansAutomatiskBehandlingFraDatabase> {
        val gjeldende = mutableListOf<StansAutomatiskBehandlingFraDatabase>()
        this.sortedWith { a, b ->
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
                arsaker = this.flatMap { it.årsaker }.toList(),
                tidspunkt = this.last().opprettet.format(DateTimeFormatter.ISO_DATE_TIME),
            )
        }
}
