package no.nav.helse.modell.vergemal

import no.nav.helse.db.SessionContext
import no.nav.helse.db.VergemålDao
import no.nav.helse.db.VergemålOgFremtidsfullmakt
import no.nav.helse.mediator.meldinger.løsninger.Fullmaktløsning
import no.nav.helse.mediator.meldinger.løsninger.Vergemålløsning
import no.nav.helse.modell.kommando.Command
import no.nav.helse.modell.kommando.CommandContext
import no.nav.helse.modell.melding.Behov
import no.nav.helse.modell.person.Sykefraværstilfelle
import no.nav.helse.modell.person.vedtaksperiode.Varselkode.SB_EX_4
import no.nav.helse.spesialist.application.Outbox
import no.nav.helse.spesialist.application.logg.logg
import java.util.UUID

internal class VurderVergemålOgFullmakt(
    private val fødselsnummer: String,
    private val vergemålDao: VergemålDao,
    private val vedtaksperiodeId: UUID,
    private val sykefraværstilfelle: Sykefraværstilfelle,
) : Command {
    override fun execute(
        commandContext: CommandContext,
        sessionContext: SessionContext,
        outbox: Outbox,
    ) = behandle(commandContext)

    override fun resume(
        commandContext: CommandContext,
        sessionContext: SessionContext,
        outbox: Outbox,
    ): Boolean = behandle(commandContext)

    private fun behandle(commandContext: CommandContext): Boolean {
        val vergemålløsning = commandContext.get<Vergemålløsning>()
        val fullmaktløsning = commandContext.get<Fullmaktløsning>()

        if (vergemålløsning == null || fullmaktløsning == null) {
            logg.info("Trenger informasjon om vergemål, fremtidsfullmakter og fullmakt")
            commandContext.behov(Behov.Vergemål)
            commandContext.behov(Behov.Fullmakt)
            return false
        }

        vergemålDao.lagre(
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
            sykefraværstilfelle.håndter(SB_EX_4.nyttVarsel(vedtaksperiodeId))
            return true
        }

        return true
    }

    private fun Vergemålløsning.harVergemål() = vergemålOgFremtidsfullmakt.harVergemål
}
