package no.nav.helse.modell.vergemal

import no.nav.helse.mediator.meldinger.Vergemålløsning
import no.nav.helse.modell.Toggle
import no.nav.helse.modell.WarningDao
import no.nav.helse.modell.kommando.Command
import no.nav.helse.modell.kommando.CommandContext
import no.nav.helse.modell.vedtak.Warning
import no.nav.helse.modell.vedtak.WarningKilde
import no.nav.helse.warningteller
import org.slf4j.LoggerFactory
import java.util.*

internal class VergemålCommand(
    val vergemålDao: VergemålDao,
    val warningDao: WarningDao,
    val vedtaksperiodeId: UUID
) : Command {

    override fun execute(context: CommandContext): Boolean {
        return if (Toggle.VergemålToggle.enabled) {
            logg.info("Trenger informasjon om vergemål og fullmakter")
            context.behov("Vergemål")
            false
        } else {
            logg.info("Lar være å slå opp på vergemål (togglet av)")
            true
        }
    }

    override fun resume(context: CommandContext): Boolean {
        val løsning = context.get<Vergemålløsning>() ?: return false
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
        warningDao.leggTilWarning(vedtaksperiodeId, Warning(this, WarningKilde.Spesialist))
        warningteller.labels("WARN", this).inc()
    }

    private fun Vergemålløsning.harVergemål() = vergemål.harVergemål
    private fun Vergemålløsning.harFullmakt() = vergemål.harFullmakter || vergemål.harFremtidsfullmakter

    private companion object {
        private val logg = LoggerFactory.getLogger(VergemålCommand::class.java)
    }
}
