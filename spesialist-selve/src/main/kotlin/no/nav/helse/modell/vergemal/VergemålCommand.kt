package no.nav.helse.modell.vergemal

import java.util.UUID
import no.nav.helse.mediator.meldinger.løsninger.Vergemålløsning
import no.nav.helse.modell.kommando.Command
import no.nav.helse.modell.kommando.CommandContext
import no.nav.helse.modell.sykefraværstilfelle.Sykefraværstilfelle
import no.nav.helse.modell.varsel.Varselkode.SB_IK_1
import org.slf4j.LoggerFactory

internal class VergemålCommand(
    private val hendelseId: UUID,
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

        løsning.lagre(vergemålDao)

        if (løsning.harVergemål()) {
            // Om personen har vergemål vil vedtaksperioden automatisk avvises
            // og vi trenger ikke leggge på eventuelle warnings for fullmakt
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
        private val logg = LoggerFactory.getLogger(VergemålCommand::class.java)
    }
}
