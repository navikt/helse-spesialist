package no.nav.helse.modell.vergemal

import no.nav.helse.db.SessionContext
import no.nav.helse.db.VergemålOgFremtidsfullmakt
import no.nav.helse.mediator.meldinger.løsninger.Fullmaktløsning
import no.nav.helse.mediator.meldinger.løsninger.Vergemålløsning
import no.nav.helse.modell.kommando.Command
import no.nav.helse.modell.kommando.CommandContext
import no.nav.helse.modell.melding.Behov
import no.nav.helse.modell.person.vedtaksperiode.Varselkode.SB_EX_4
import no.nav.helse.spesialist.application.Outbox
import no.nav.helse.spesialist.application.logg.logg
import no.nav.helse.spesialist.domain.Varsel
import no.nav.helse.spesialist.domain.VarselId
import no.nav.helse.spesialist.domain.VedtaksperiodeId
import java.time.LocalDateTime
import java.util.UUID

internal class VurderVergemålOgFullmakt(
    private val fødselsnummer: String,
    private val vedtaksperiodeId: VedtaksperiodeId,
) : Command {
    override fun execute(
        commandContext: CommandContext,
        sessionContext: SessionContext,
        outbox: Outbox,
    ) = behandle(commandContext, sessionContext)

    override fun resume(
        commandContext: CommandContext,
        sessionContext: SessionContext,
        outbox: Outbox,
    ): Boolean = behandle(commandContext, sessionContext)

    private fun behandle(
        commandContext: CommandContext,
        sessionContext: SessionContext,
    ): Boolean {
        val vergemålløsning = commandContext.get<Vergemålløsning>()
        val fullmaktløsning = commandContext.get<Fullmaktløsning>()

        if (vergemålløsning == null || fullmaktløsning == null) {
            logg.info("Trenger informasjon om vergemål, fremtidsfullmakter og fullmakt")
            commandContext.behov(Behov.Vergemål)
            commandContext.behov(Behov.Fullmakt)
            return false
        }

        sessionContext.vergemålDao.lagre(
            fødselsnummer = fødselsnummer,
            vergemålOgFremtidsfullmakt =
                VergemålOgFremtidsfullmakt(
                    harVergemål = vergemålløsning.vergemålOgFremtidsfullmakt.harVergemål,
                    harFremtidsfullmakter = vergemålløsning.vergemålOgFremtidsfullmakt.harFremtidsfullmakter,
                ),
            fullmakt = fullmaktløsning.harFullmakt,
        )

        if (vergemålløsning.harVergemål()) {
            logg.info("Legger til varsel om vergemål på vedtaksperiode $vedtaksperiodeId")
            val nyesteBehandling =
                sessionContext.behandlingRepository.finnNyesteForVedtaksperiode(vedtaksperiodeId)
                    ?: error("Fant ikke behandling")

            val varsel =
                Varsel.nytt(
                    VarselId(UUID.randomUUID()),
                    behandlingUnikId = nyesteBehandling.id,
                    spleisBehandlingId = nyesteBehandling.spleisBehandlingId,
                    kode = SB_EX_4.name,
                    opprettetTidspunkt = LocalDateTime.now(),
                )
            sessionContext.varselRepository.lagre(varsel)
        }

        return true
    }

    private fun Vergemålløsning.harVergemål() = vergemålOgFremtidsfullmakt.harVergemål
}
