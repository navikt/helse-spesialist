package no.nav.helse.modell.varsel

import java.time.LocalDateTime
import java.util.UUID
import javax.sql.DataSource

internal interface VarselRepository {
    fun finnVarslerFor(vedtaksperiodeId: UUID): List<Pair<String, String>>
    fun erAktivFor(vedtaksperiodeId: UUID, varselkode: String): Boolean
    fun deaktiverFor(vedtaksperiodeId: UUID, varselkode: String)
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

    private val varselDao = VarselDao(dataSource)

    override fun finnVarslerFor(vedtaksperiodeId: UUID): List<Pair<String, String>> {
        return varselDao.alleVarslerFor(vedtaksperiodeId)
    }

    override fun erAktivFor(vedtaksperiodeId: UUID, varselkode: String): Boolean {
        TODO("Not yet implemented")
    }

    override fun deaktiverFor(vedtaksperiodeId: UUID, varselkode: String) {
        TODO("Not yet implemented")
    }

    override fun lagreVarsel(id: UUID, varselkode: String, opprettet: LocalDateTime, vedtaksperiodeId: UUID) {
        varselDao.lagreVarsel(id, varselkode, opprettet, vedtaksperiodeId)
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

}