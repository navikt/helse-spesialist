package no.nav.helse.modell.kommando

import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.module.kotlin.convertValue
import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.helse.modell.SnapshotDao
import no.nav.helse.modell.VedtakDao
import no.nav.helse.modell.WarningDao
import no.nav.helse.modell.vedtak.WarningDto
import no.nav.helse.modell.vedtak.WarningKilde
import no.nav.helse.modell.vedtak.snapshot.PersonFraSpleisDto
import no.nav.helse.modell.vedtak.snapshot.SpeilSnapshotRestClient
import no.nav.helse.objectMapper
import org.slf4j.LoggerFactory
import java.util.*

internal class OppdaterSnapshotCommand(
    private val speilSnapshotRestClient: SpeilSnapshotRestClient,
    private val vedtakDao: VedtakDao,
    private val warningDao: WarningDao,
    private val snapshotDao: SnapshotDao,
    private val vedtaksperiodeId: UUID,
    private val fødselsnummer: String
) : Command {

    private companion object {
        private val log = LoggerFactory.getLogger(OppdaterSnapshotCommand::class.java)
    }

    override fun execute(context: CommandContext): Boolean {
        if (null == vedtakDao.finnVedtakId(vedtaksperiodeId)) return ignorer()
        return oppdaterSnapshot()
    }

    override fun resume(context: CommandContext): Boolean {
        return oppdaterSnapshot()
    }

    override fun undo(context: CommandContext) {
        // sletting av snapshot gir ikke mening i denne kontekst
    }

    private fun ignorer(): Boolean {
        log.info("kjenner ikke til vedtaksperiode $vedtaksperiodeId")
        return true
    }

    private fun oppdaterSnapshot(): Boolean {
        log.info("oppdaterer snapshot for $vedtaksperiodeId")
        return speilSnapshotRestClient.hentSpeilSpapshot(fødselsnummer).let {
            val oppdatertSnapshot = snapshotDao.oppdaterSnapshotForVedtaksperiode(vedtaksperiodeId, it) != 0
            if (oppdatertSnapshot) {
                log.info("oppdaterer warnings for $vedtaksperiodeId")
                val warnings = warnings(it)
                warningDao.oppdaterSpleisWarnings(
                    vedtaksperiodeId,
                    warnings.map { w -> WarningDto(w.melding, WarningKilde.Spleis) })
            }
            oppdatertSnapshot
        }
    }

    private fun warnings(json: String) =
        try {
            objectMapper.readValue<PersonFraSpleisDto>(json).arbeidsgivere
                .flatMap { it.vedtaksperioder }
                .filter { UUID.fromString(it["id"].asText()) == vedtaksperiodeId }
                .flatMap { it.findValues("aktivitetslogg") }
                .flatten()
                .map { objectMapper.convertValue<Warning>(it) }
                .filter { it.alvorlighetsgrad == "W" }
        } catch (e: JsonParseException) {
            throw RuntimeException("Feilet ved instansiering av speil-snapshot", e)
        }

    class Warning(
        val alvorlighetsgrad: String,
        val vedtaksperiodeId: String,
        val melding: String
    )
}
