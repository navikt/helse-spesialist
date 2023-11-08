package no.nav.helse.modell.vedtaksperiode

import java.util.UUID
import net.logstash.logback.argument.StructuredArguments.kv
import no.nav.helse.modell.VedtakDao
import no.nav.helse.modell.kommando.Command
import no.nav.helse.modell.kommando.CommandContext
import no.nav.helse.modell.varsel.DefinisjonDao
import no.nav.helse.modell.varsel.Varsel
import no.nav.helse.modell.varsel.Varselmelder
import no.nav.helse.spesialist.api.varsel.Varsel.Varselstatus
import org.slf4j.LoggerFactory

internal class ForkastVedtaksperiodeCommand(
    private val hendelseId: UUID,
    private val vedtaksperiodeId: UUID,
    private val fødselsnummer: String,
    private val generasjon: Generasjon,
    private val vedtakDao: VedtakDao,
    private val definisjonDao: DefinisjonDao,
    private val varselmelder: Varselmelder,
) : Command {
    private companion object {
        private val logg = LoggerFactory.getLogger(ForkastVedtaksperiodeCommand::class.java)
    }

    override fun execute(context: CommandContext): Boolean {
        logg.info("Markerer {} som forkastet", kv("vedtaksperiodeId", vedtaksperiodeId))
        generasjon.registrer(varselAvvistObserver(fødselsnummer, definisjonDao, varselmelder))
        generasjon.avvisVarsler()
        vedtakDao.markerForkastet(vedtaksperiodeId, hendelseId)
        return true
    }
}

private fun varselAvvistObserver(fnr: String, definisjonDao: DefinisjonDao, varselmelder: Varselmelder) =
    object : IVedtaksperiodeObserver {
        override fun varselAvvist(
            varselId: UUID,
            varselkode: String,
            generasjonId: UUID,
            vedtaksperiodeId: UUID,
            forrigeStatus: Varsel.Status,
            gjeldendeStatus: Varsel.Status,
        ) {
            val definisjon = definisjonDao.sisteDefinisjonFor(varselkode)
            val tittel = definisjon.toDto().tittel
            val forrige = Varselstatus.valueOf(forrigeStatus.name)
            val gjeldende = Varselstatus.valueOf(gjeldendeStatus.name)

            varselmelder.meldVarselEndret(fnr, null, vedtaksperiodeId, varselId, tittel, varselkode, forrige, gjeldende)
        }
    }
