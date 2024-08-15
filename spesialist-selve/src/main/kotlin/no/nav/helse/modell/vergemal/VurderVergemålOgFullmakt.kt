package no.nav.helse.modell.vergemal

import no.nav.helse.mediator.meldinger.løsninger.Vergemålløsning
import no.nav.helse.modell.kommando.Command
import no.nav.helse.modell.kommando.CommandContext
import no.nav.helse.modell.sykefraværstilfelle.Sykefraværstilfelle
import no.nav.helse.modell.varsel.Varselkode.SB_EX_4
import no.nav.helse.modell.varsel.Varselkode.SB_IK_1
import org.slf4j.LoggerFactory
import java.util.UUID

internal class VurderVergemålOgFullmakt(
    private val hendelseId: UUID,
    private val fødselsnummer: String,
    private val vergemålDao: VergemålDao,
    private val vedtaksperiodeId: UUID,
    private val sykefraværstilfelle: Sykefraværstilfelle,
) : Command {
    override fun execute(context: CommandContext) = behandle(context)

    override fun resume(context: CommandContext) = behandle(context)

    private fun behandle(context: CommandContext): Boolean {
        val løsning = context.get<Vergemålløsning>()
        if (løsning == null) {
            logg.info("Trenger informasjon om vergemål og fullmakter")
            context.behov("Vergemål")
            return false
        }

        vergemålDao.lagre(fødselsnummer, løsning.vergemål)

        if (løsning.harVergemål()) {
            logg.info("Legger til varsel om vergemål på vedtaksperiode $vedtaksperiodeId")
            sykefraværstilfelle.håndter(SB_EX_4.nyttVarsel(vedtaksperiodeId), hendelseId)
            return true
        }
        if (løsning.harFullmakt()) {
            sykefraværstilfelle.håndter(SB_IK_1.nyttVarsel(vedtaksperiodeId), hendelseId)
        }

        return true
    }

    private fun Vergemålløsning.harVergemål() = vergemål.harVergemål

    private fun Vergemålløsning.harFullmakt() = vergemål.harFullmakter || vergemål.harFremtidsfullmakter

    private companion object {
        private val logg = LoggerFactory.getLogger(VurderVergemålOgFullmakt::class.java)
    }
}
