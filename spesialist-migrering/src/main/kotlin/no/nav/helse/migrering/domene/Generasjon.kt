package no.nav.helse.migrering.domene

import java.time.LocalDateTime
import java.util.UUID
import net.logstash.logback.argument.StructuredArguments
import no.nav.helse.migrering.db.SpesialistDao
import no.nav.helse.migrering.domene.Varsel.Companion.dedup
import no.nav.helse.migrering.domene.Varsel.Companion.lagre
import org.slf4j.LoggerFactory

internal class Generasjon(
    val id: UUID,
    val vedtaksperiodeId: UUID,
    val utbetalingId: UUID?,
    val opprettet: LocalDateTime,
    val låstTidspunkt: LocalDateTime?,
    varsler: List<Varsel>,
) {

    private val varsler: MutableList<Varsel> = varsler.toMutableList()

    internal companion object {
        private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")
        fun List<Generasjon>.lagre(spesialistDao: SpesialistDao, hendelseId: UUID) {
            forEach { it.lagre(spesialistDao, hendelseId) }
        }
    }

    internal fun erLåst() = låstTidspunkt != null

    internal fun nyeVarsler(varsler: List<Varsel>) {
        this.varsler.addAll(varsler)
    }

    internal fun lagre(spesialistDao: SpesialistDao, hendelseId: UUID) {
        val insertGenerasjonOk = spesialistDao.lagreGenerasjon(
            id,
            vedtaksperiodeId,
            utbetalingId,
            opprettet,
            hendelseId,
            låstTidspunkt
        )

        if (!insertGenerasjonOk) {
            sikkerlogg.warn(
                "Kunne ikke inserte generasjon for {}, {}, den eksisterer fra før av.",
                StructuredArguments.keyValue("vedtaksperiodeId", vedtaksperiodeId),
                StructuredArguments.keyValue("utbetalingId", utbetalingId)
            )
            return
        }

        val insertVarselOk = varsler.dedup().lagre(id, spesialistDao)
    }
}