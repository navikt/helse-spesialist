package no.nav.helse.modell.varsel

import no.nav.helse.db.SessionContext
import no.nav.helse.modell.kommando.Command
import no.nav.helse.modell.kommando.CommandContext
import no.nav.helse.modell.person.HentEnhetløsning
import no.nav.helse.modell.person.vedtaksperiode.Varselkode
import no.nav.helse.spesialist.application.Outbox
import no.nav.helse.spesialist.application.logg.logg
import no.nav.helse.spesialist.domain.Varsel
import no.nav.helse.spesialist.domain.VarselId
import no.nav.helse.spesialist.domain.VedtaksperiodeId
import java.time.LocalDateTime
import java.util.*

internal class VurderEnhetUtland(
    private val fødselsnummer: String,
    private val vedtaksperiodeId: VedtaksperiodeId,
) : Command {
    override fun execute(
        commandContext: CommandContext,
        sessionContext: SessionContext,
        outbox: Outbox,
    ): Boolean {
        val tilhørerEnhetUtland = HentEnhetløsning.erEnhetUtland(sessionContext.personDao.finnEnhetId(fødselsnummer))

        if (tilhørerEnhetUtland) {
            logg.info("Håndterer varsel om utland på vedtaksperiode $vedtaksperiodeId")
            val nyesteBehandling =
                sessionContext.behandlingRepository.finnNyesteForVedtaksperiode(vedtaksperiodeId)
                    ?: error("Fant ikke behandling")

            val varsel =
                Varsel.nytt(
                    VarselId(UUID.randomUUID()),
                    behandlingUnikId = nyesteBehandling.id,
                    spleisBehandlingId = nyesteBehandling.spleisBehandlingId,
                    kode = Varselkode.SB_EX_5.name,
                    opprettetTidspunkt = LocalDateTime.now(),
                )
            sessionContext.varselRepository.lagre(varsel)
        }

        return true
    }
}
