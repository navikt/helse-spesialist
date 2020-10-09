package no.nav.helse.modell.dkif

import no.nav.helse.mediator.meldinger.DigitalKontaktinformasjonløsning
import no.nav.helse.modell.VedtakDao
import no.nav.helse.modell.kommando.Command
import no.nav.helse.modell.kommando.CommandContext
import org.slf4j.LoggerFactory
import java.util.*

internal class DigitalKontaktinformasjonCommand(
    private val digitalKontaktinformasjonDao: DigitalKontaktinformasjonDao,
    private val vedtakDao: VedtakDao,
    private val vedtaksperiodeId: UUID
) : Command {

    private companion object {
        private val logg = LoggerFactory.getLogger(DigitalKontaktinformasjonCommand::class.java)
    }

    override fun execute(context: CommandContext): Boolean {
        logg.info("Trenger reservasjonsinformasjon fra DKIF")
        context.behov("DigitalKontaktinformasjon")
        return false
    }

    override fun resume(context: CommandContext): Boolean {
        val løsning = context.get<DigitalKontaktinformasjonløsning>() ?: return false
        løsning.lagre(digitalKontaktinformasjonDao)
        løsning.evaluer(vedtakDao, vedtaksperiodeId)
        return true
    }
}
