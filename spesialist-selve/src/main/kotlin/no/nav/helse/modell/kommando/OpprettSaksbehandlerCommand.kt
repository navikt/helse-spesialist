package no.nav.helse.modell.kommando

import net.logstash.logback.argument.StructuredArguments.keyValue
import no.nav.helse.spesialist.api.saksbehandler.SaksbehandlerDao
import org.slf4j.LoggerFactory
import java.util.*

internal class OpprettSaksbehandlerCommand(
    val oid: UUID,
    val navn: String,
    val epost: String,
    val ident: String,
    val saksbehandlerDao: SaksbehandlerDao
) : Command {
    private companion object {
        private val logg = LoggerFactory.getLogger(OpprettSaksbehandlerCommand::class.java)
    }
    override fun execute(context: CommandContext): Boolean {
        logg.info("Lagrer saksbehandlerinformasjon for saksbehandler med {}", keyValue("oid", oid))
        saksbehandlerDao.opprettSaksbehandler(oid, navn, epost, ident)
        return true
    }
}
