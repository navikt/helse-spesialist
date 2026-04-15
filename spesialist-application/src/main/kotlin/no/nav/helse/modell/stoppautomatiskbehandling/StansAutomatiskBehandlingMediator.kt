package no.nav.helse.modell.stoppautomatiskbehandling

import no.nav.helse.db.OppgaveDao
import no.nav.helse.db.PeriodehistorikkDao
import no.nav.helse.db.SessionContext
import no.nav.helse.modell.objectMapper
import no.nav.helse.modell.periodehistorikk.Historikkinnslag
import no.nav.helse.spesialist.application.VeilederStansRepository
import no.nav.helse.spesialist.application.logg.teamLogs
import no.nav.helse.spesialist.domain.Identitetsnummer
import no.nav.helse.spesialist.domain.VeilederStans
import java.time.ZoneOffset
import java.util.UUID

class StansAutomatiskBehandlingMediator(
    private val periodehistorikkDao: PeriodehistorikkDao,
    private val oppgaveDao: OppgaveDao,
    private val veilederStansRepository: VeilederStansRepository,
) {
    object Factory {
        fun stansAutomatiskBehandlingMediator(
            sessionContext: SessionContext,
        ): StansAutomatiskBehandlingMediator =
            StansAutomatiskBehandlingMediator(
                sessionContext.periodehistorikkDao,
                sessionContext.oppgaveDao,
                sessionContext.veilederStansRepository,
            )
    }

    fun håndter(melding: StansAutomatiskBehandlingMelding) {
        if (melding.status == "STOPP_AUTOMATIKK") {
            veilederStansRepository.lagre(
                VeilederStans.ny(
                    identitetsnummer = Identitetsnummer.fraString(melding.fødselsnummer()),
                    årsaker = melding.årsaker.map { VeilederStans.StansÅrsak.valueOf(it.name) }.toSet(),
                    opprettet = melding.opprettet.toInstant(ZoneOffset.UTC),
                    originalMeldingId = UUID.fromString(objectMapper.readTree(melding.originalMelding).path("uuid").asText()),
                ),
            )
        }
        lagrePeriodehistorikk(melding.fødselsnummer())
    }

    private fun lagrePeriodehistorikk(fødselsnummer: String) {
        val oppgaveId = oppgaveDao.finnOppgaveId(fødselsnummer)
        if (oppgaveId != null) {
            val innslag = Historikkinnslag.automatiskBehandlingStanset()
            periodehistorikkDao.lagreMedOppgaveId(innslag, oppgaveId)
        } else {
            teamLogs.info("Fant ikke oppgave for $fødselsnummer. Fikk ikke lagret historikkinnslag om stans av automatisk behandling")
        }
    }
}
