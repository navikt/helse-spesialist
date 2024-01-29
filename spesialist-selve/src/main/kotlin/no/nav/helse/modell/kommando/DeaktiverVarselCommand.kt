package no.nav.helse.modell.kommando

import java.util.UUID
import no.nav.helse.modell.sykefraværstilfelle.Sykefraværstilfelle
import org.slf4j.LoggerFactory

internal class DeaktiverVarselCommand(
    private val sykefraværstilfelle: Sykefraværstilfelle,
    private val vedtaksperiodeId: UUID,
    private val varselkode: String,
) : Command {
    companion object {
        private val logg = LoggerFactory.getLogger(DeaktiverVarselCommand::class.java)
    }
    override fun execute(context: CommandContext): Boolean {
        logg.info("Deaktiverer varsel {} for vedtaksperiodeId {}", varselkode, vedtaksperiodeId)
        sykefraværstilfelle.deaktiverVarsel(vedtaksperiodeId, varselkode)
        return true
    }
}