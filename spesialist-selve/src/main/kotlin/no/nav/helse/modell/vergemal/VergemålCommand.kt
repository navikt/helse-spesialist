package no.nav.helse.modell.vergemal

import java.time.LocalDateTime
import no.nav.helse.mediator.meldinger.Vergemålløsning
import no.nav.helse.modell.WarningDao
import no.nav.helse.modell.kommando.Command
import no.nav.helse.modell.kommando.CommandContext
import no.nav.helse.modell.vedtak.Warning
import no.nav.helse.modell.vedtak.WarningKilde
import no.nav.helse.tellWarning
import org.slf4j.LoggerFactory
import java.util.*

internal class VergemålCommand(
    val vergemålDao: VergemålDao,
    val warningDao: WarningDao,
    val vedtaksperiodeId: UUID
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
            "Registert fullmakt på personen.".leggTilSomWarning()
        }

        return true
    }


    private fun String.leggTilSomWarning() {
        warningDao.leggTilWarning(
            vedtaksperiodeId, Warning(
                melding = this,
                kilde = WarningKilde.Spesialist,
                opprettet = LocalDateTime.now(),
            )
        )
        tellWarning(this)
    }

    private fun Vergemålløsning.harVergemål() = vergemål.harVergemål
    private fun Vergemålløsning.harFullmakt() = vergemål.harFullmakter || vergemål.harFremtidsfullmakter

    private companion object {
        private val logg = LoggerFactory.getLogger(VergemålCommand::class.java)
    }
}
