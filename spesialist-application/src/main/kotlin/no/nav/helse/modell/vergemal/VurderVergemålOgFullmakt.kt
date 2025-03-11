package no.nav.helse.modell.vergemal

import no.nav.helse.db.VergemålDao
import no.nav.helse.db.VergemålOgFremtidsfullmakt
import no.nav.helse.mediator.meldinger.løsninger.Fullmaktløsning
import no.nav.helse.mediator.meldinger.løsninger.Vergemålløsning
import no.nav.helse.modell.kommando.Command
import no.nav.helse.modell.kommando.CommandContext
import no.nav.helse.modell.melding.Behov
import no.nav.helse.modell.person.Sykefraværstilfelle
import no.nav.helse.modell.person.vedtaksperiode.Varselkode.SB_EX_4
import org.slf4j.LoggerFactory
import java.util.UUID

internal class VurderVergemålOgFullmakt(
    private val fødselsnummer: String,
    private val vergemålDao: VergemålDao,
    private val vedtaksperiodeId: UUID,
    private val sykefraværstilfelle: Sykefraværstilfelle,
) : Command() {
    override fun execute(context: CommandContext) = behandle(context)

    override fun resume(context: CommandContext) = behandle(context)

    private fun behandle(context: CommandContext): Boolean {
        val vergemålløsning = context.get<Vergemålløsning>()
        val fullmaktløsning = context.get<Fullmaktløsning>()

        if (vergemålløsning == null || fullmaktløsning == null) {
            logg.info("Trenger informasjon om vergemål, fremtidsfullmakter og fullmakt")
            context.behov(Behov.Vergemål)
            context.behov(Behov.Fullmakt)
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

    private companion object {
        private val logg = LoggerFactory.getLogger(VurderVergemålOgFullmakt::class.java)
    }
}
