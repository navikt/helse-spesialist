package no.nav.helse.migrering.domene

import java.time.LocalDateTime
import java.util.UUID
import net.logstash.logback.argument.StructuredArguments.keyValue
import no.nav.helse.migrering.db.SpesialistDao
import org.postgresql.util.PSQLException
import org.slf4j.LoggerFactory
import kotlin.properties.Delegates

internal class Varsel(
    private val vedtaksperiodeId: UUID,
    private val melding: String,
    private val opprettet: LocalDateTime,
    private val id: UUID,
    private val inaktivFra: LocalDateTime?,
) {
    private var varselkode by Delegates.notNull<String>()
    private var definisjonRef by Delegates.notNull<Long>()

    internal fun definisjon(spesialistDao: SpesialistDao) {
        val (ref, kode) = spesialistDao.finnDefinisjonFor(melding)
        varselkode = kode
        definisjonRef = ref
    }

    internal fun lagre(generasjonId: UUID, ident: String?, statusEndretTidspunkt: LocalDateTime?, status: String, spesialistDao: SpesialistDao) {
        try {
            spesialistDao.lagreVarsel(
                generasjonId = generasjonId,
                definisjonRef = if (status in listOf("AKTIV", "INAKTIV")) null else definisjonRef,
                varselkode = varselkode,
                varselId = id,
                vedtaksperiodeId = vedtaksperiodeId,
                opprettet = opprettet,
                statusEndretIdent = ident,
                statusEndretTidspunkt = statusEndretTidspunkt,
                status = status
            )
        } catch (e: PSQLException) {
            sikkerlogg.info(
                "Kunne ikke lagre varsel: {}, {}, {}, {}",
                keyValue("vedtaksperiodeId", vedtaksperiodeId),
                keyValue("generasjonId", generasjonId),
                keyValue("varselId", id),
                keyValue("varselkode", varselkode),
            )
            e.printStackTrace()
        }

    }

    internal fun erFør(tidspunkt: LocalDateTime) = opprettet <= tidspunkt

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Varsel

        if (vedtaksperiodeId != other.vedtaksperiodeId) return false
        if (melding != other.melding) return false
        if (opprettet != other.opprettet) return false

        return true
    }

    override fun hashCode(): Int {
        var result = vedtaksperiodeId.hashCode()
        result = 31 * result + melding.hashCode()
        result = 31 * result + opprettet.hashCode()
        return result
    }

    internal companion object {
        private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")
        internal fun List<Varsel>.varslerFor(vedtaksperiodeId: UUID): List<Varsel> {
            return filter { it.vedtaksperiodeId == vedtaksperiodeId }
        }

        internal fun List<Varsel>.lagre(generasjonId: UUID, ident: String?, statusEndretTidspunkt: LocalDateTime?, status: String, spesialistDao: SpesialistDao) {
            forEach {
                it.lagre(
                    generasjonId = generasjonId,
                    ident = it.inaktivFra?.let { "Spesialist" } ?: ident,
                    statusEndretTidspunkt = it.inaktivFra ?: statusEndretTidspunkt,
                    status = it.inaktivFra?.let { "INAKTIV" } ?: status,
                    spesialistDao = spesialistDao
                )
            }
        }

        internal fun List<Varsel>.sortert(): List<Varsel> {
            return sortedBy { it.opprettet }
        }

        internal fun MutableList<Varsel>.konsumer(tidspunkt: LocalDateTime): List<Varsel> {
            val konsumerteVarsler = this.takeWhile { it.erFør(tidspunkt) }
            this.removeAll(konsumerteVarsler)
            return konsumerteVarsler
        }

        internal fun List<Varsel>.dedup(): List<Varsel> {
            return sortedByDescending { it.opprettet }
                .distinctBy { it.varselkode }
                .distinctBy { it.id }
        }
    }
}
