package no.nav.helse.modell.kommando

import org.slf4j.LoggerFactory

// Det er ikke lov å ha tom liste med commands i en MacroCommand. Denne kan brukes som en placeholder til man får implementert egne Commands
internal class DummyCommand : Command {

    private companion object {
        private val logg = LoggerFactory.getLogger(DummyCommand::class.java)
    }

    override fun execute(context: CommandContext): Boolean {
        logg.info("Kjører DummyCommand")
        return true
    }
}
