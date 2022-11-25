package no.nav.helse.modell.varsel

import java.time.LocalDateTime
import java.util.UUID
import javax.sql.DataSource
import net.logstash.logback.argument.StructuredArguments.keyValue
import no.nav.helse.modell.varsel.Varsel.Status.AKTIV
import no.nav.helse.modell.varsel.Varsel.Status.GODKJENT
import no.nav.helse.modell.varsel.Varsel.Status.INAKTIV
import no.nav.helse.modell.vedtaksperiode.GenerasjonDao
import no.nav.helse.tellInaktivtVarsel
import org.slf4j.LoggerFactory

internal interface VarselRepository {
    fun finnVarslerFor(vedtaksperiodeId: UUID): List<Varsel>
    fun deaktiverFor(vedtaksperiodeId: UUID, varselkode: String)
    fun godkjennFor(vedtaksperiodeId: UUID, varselkode: String, ident: String)
    fun lagreVarsel(id: UUID, varselkode: String, opprettet: LocalDateTime, vedtaksperiodeId: UUID)
    fun lagreDefinisjon(
        id: UUID,
        varselkode: String,
        tittel: String,
        forklaring: String?,
        handling: String?,
        avviklet: Boolean,
        opprettet: LocalDateTime,
    )
}

internal class ActualVarselRepository(dataSource: DataSource) : VarselRepository {

    private companion object {
        private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")
    }

    private val varselDao = VarselDao(dataSource)
    private val generasjonDao = GenerasjonDao(dataSource)

    override fun finnVarslerFor(vedtaksperiodeId: UUID): List<Varsel> {
        return varselDao.alleVarslerFor(vedtaksperiodeId)
    }

    override fun deaktiverFor(vedtaksperiodeId: UUID, varselkode: String) {
        if (!erAktivFor(vedtaksperiodeId, varselkode)) return
        varselDao.oppdaterStatus(vedtaksperiodeId, varselkode, INAKTIV, "Spesialist")
        if (varselkode.matches(varselkodeformat.toRegex())) tellInaktivtVarsel(varselkode)
    }

    override fun godkjennFor(vedtaksperiodeId: UUID, varselkode: String, ident: String) {
        if (!erAktivFor(vedtaksperiodeId, varselkode)) return
        varselDao.oppdaterStatus(vedtaksperiodeId, varselkode, GODKJENT, ident)
    }

    override fun lagreVarsel(id: UUID, varselkode: String, opprettet: LocalDateTime, vedtaksperiodeId: UUID) {
        generasjonDao.finnSisteFor(vedtaksperiodeId)
            ?.run {
                if (erAktivFor(vedtaksperiodeId, varselkode)) return
                this.lagreVarsel(id, varselkode, opprettet, varselDao::lagreVarsel)
            }
            ?: sikkerlogg.info(
                "Lagrer ikke {} for {} fordi det ikke finnes noen generasjon for perioden.",
                keyValue("varselkode", varselkode),
                keyValue("vedtaksperiodeId", vedtaksperiodeId)
            )
    }

    override fun lagreDefinisjon(
        id: UUID,
        varselkode: String,
        tittel: String,
        forklaring: String?,
        handling: String?,
        avviklet: Boolean,
        opprettet: LocalDateTime,
    ) {
        varselDao.lagreDefinisjon(id, varselkode, tittel, forklaring, handling, avviklet, opprettet)
    }

    private fun erAktivFor(vedtaksperiodeId: UUID, varselkode: String): Boolean {
        return varselDao.finnVarselstatus(vedtaksperiodeId, varselkode) == AKTIV
    }
}