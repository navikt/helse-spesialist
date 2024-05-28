package no.nav.helse.modell.stoppautomatiskbehandling

import no.nav.helse.db.StansAutomatiskBehandlingDao
import no.nav.helse.db.StansAutomatiskBehandlingFraDatabase
import no.nav.helse.mediator.oppgave.OppgaveDao
import no.nav.helse.modell.saksbehandler.Saksbehandler
import no.nav.helse.modell.saksbehandler.handlinger.Personhandling
import no.nav.helse.modell.utbetaling.UtbetalingDao
import no.nav.helse.spesialist.api.StansAutomatiskBehandlinghåndterer
import no.nav.helse.spesialist.api.graphql.schema.NotatType
import no.nav.helse.spesialist.api.graphql.schema.UnntattFraAutomatiskGodkjenning
import no.nav.helse.spesialist.api.notat.NotatMediator
import no.nav.helse.spesialist.api.periodehistorikk.PeriodehistorikkDao
import no.nav.helse.spesialist.api.periodehistorikk.PeriodehistorikkType.STANS_AUTOMATISK_BEHANDLING
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

class StansAutomatiskBehandlingMediator(
    private val stansAutomatiskBehandlingDao: StansAutomatiskBehandlingDao,
    private val periodehistorikkDao: PeriodehistorikkDao,
    private val oppgaveDao: OppgaveDao,
    private val utbetalingDao: UtbetalingDao,
    private val notatMediator: NotatMediator,
) : StansAutomatiskBehandlinghåndterer {
    private val logg = LoggerFactory.getLogger(this::class.java)
    private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")

    internal fun håndter(
        handling: Personhandling,
        saksbehandler: Saksbehandler,
    ) {
        lagre(
            fødselsnummer = handling.gjelderFødselsnummer(),
            status = "NORMAL",
            årsaker = emptySet(),
            opprettet = LocalDateTime.now(),
            originalMelding = null,
            kilde = "SPEIL",
        )
        lagreNotat(handling.gjelderFødselsnummer(), handling.begrunnelse(), saksbehandler.oid())
    }

    internal fun håndter(
        fødselsnummer: String,
        status: String,
        årsaker: Set<String>,
        opprettet: LocalDateTime,
        originalMelding: String,
        kilde: String,
    ) {
        lagre(
            fødselsnummer = fødselsnummer,
            status = status,
            årsaker = årsaker,
            opprettet = opprettet,
            originalMelding = originalMelding,
            kilde = kilde,
        )
        lagrePeriodehistorikk(fødselsnummer)
    }

    override fun unntattFraAutomatiskGodkjenning(fødselsnummer: String): UnntattFraAutomatiskGodkjenning =
        stansAutomatiskBehandlingDao.hent(fødselsnummer).filtrerGjeldendeStopp().tilUnntattFraAutomatiskGodkjenning()

    override fun erUnntatt(fødselsnummer: String) = stansAutomatiskBehandlingDao.hent(fødselsnummer).filtrerGjeldendeStopp().isNotEmpty()

    private fun lagre(
        fødselsnummer: String,
        status: String,
        årsaker: Set<String>,
        opprettet: LocalDateTime,
        originalMelding: String?,
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

    private fun lagrePeriodehistorikk(fødselsnummer: String) {
        val utbetalingId =
            oppgaveDao.finnOppgaveId(fødselsnummer)?.let { oppgaveDao.finnUtbetalingId(it) }
                ?: utbetalingDao.sisteUtbetalingIdFor(fødselsnummer)
        if (utbetalingId != null) {
            periodehistorikkDao.lagre(STANS_AUTOMATISK_BEHANDLING, null, utbetalingId, null)
        } else {
            sikkerlogg.error("Fant ikke oppgave for $fødselsnummer. Fikk ikke lagret historikkinnslag om stans av automatisk behandling")
        }
    }

    private fun lagreNotat(
        fødselsnummer: String,
        begrunnelse: String,
        saksbehandlerOid: UUID,
    ) = try {
        val oppgaveId = fødselsnummer.finnOppgaveId()
        notatMediator.lagreForOppgaveId(oppgaveId, begrunnelse, saksbehandlerOid, NotatType.OpphevStans)
    } catch (e: Exception) {
        sikkerlogg.error("Fant ikke oppgave for $fødselsnummer. Fikk ikke lagret notat om oppheving av stans")
    }

    private fun String.finnOppgaveId() = oppgaveDao.finnOppgaveId(this) ?: oppgaveDao.finnOppgaveIdUansettStatus(this)

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
