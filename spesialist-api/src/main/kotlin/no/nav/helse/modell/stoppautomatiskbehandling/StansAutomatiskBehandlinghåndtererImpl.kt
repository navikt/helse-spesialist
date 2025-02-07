package no.nav.helse.modell.stoppautomatiskbehandling

import no.nav.helse.db.DialogDao
import no.nav.helse.db.NotatDao
import no.nav.helse.db.OppgaveDao
import no.nav.helse.db.StansAutomatiskBehandlingDao
import no.nav.helse.db.StansAutomatiskBehandlingFraDatabase
import no.nav.helse.modell.saksbehandler.Saksbehandler
import no.nav.helse.modell.saksbehandler.handlinger.OpphevStans
import no.nav.helse.spesialist.api.StansAutomatiskBehandlinghåndterer
import no.nav.helse.spesialist.api.graphql.schema.ApiUnntattFraAutomatiskGodkjenning
import no.nav.helse.spesialist.modell.NotatType
import org.slf4j.LoggerFactory
import java.util.UUID

class StansAutomatiskBehandlinghåndtererImpl(
    private val stansAutomatiskBehandlingDao: StansAutomatiskBehandlingDao,
    private val oppgaveDao: OppgaveDao,
    private val notatDao: NotatDao,
    private val dialogDao: DialogDao,
) : StansAutomatiskBehandlinghåndterer {
    private val logg = LoggerFactory.getLogger(this::class.java)
    private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")

    fun håndter(
        handling: OpphevStans,
        saksbehandler: Saksbehandler,
    ) {
        stansAutomatiskBehandlingDao.lagreFraSpeil(handling.fødselsnummer)
        lagreNotat(handling.fødselsnummer, handling.begrunnelse, saksbehandler.oid())
    }

    override fun unntattFraAutomatiskGodkjenning(fødselsnummer: String): ApiUnntattFraAutomatiskGodkjenning =
        stansAutomatiskBehandlingDao
            .hentFor(fødselsnummer)
            .filtrerGjeldendeStopp()
            .tilUnntattFraAutomatiskGodkjenning()

    private fun lagreNotat(
        fødselsnummer: String,
        begrunnelse: String,
        saksbehandlerOid: UUID,
    ) = try {
        val oppgaveId = fødselsnummer.finnOppgaveId()
        val dialogRef = dialogDao.lagre()
        notatDao.lagreForOppgaveId(oppgaveId, begrunnelse, saksbehandlerOid, NotatType.OpphevStans, dialogRef)
    } catch (e: Exception) {
        sikkerlogg.error("Fant ikke oppgave for $fødselsnummer. Fikk ikke lagret notat om oppheving av stans")
    }

    private fun String.finnOppgaveId() = oppgaveDao.finnOppgaveId(this) ?: oppgaveDao.finnOppgaveIdUansettStatus(this)

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
                arsaker = flatMap { it.årsaker.map(StoppknappÅrsak::name) },
                tidspunkt = last().opprettet,
            )
        }
}
