package no.nav.helse.modell.varsel

import java.time.LocalDateTime
import java.util.UUID
import javax.sql.DataSource
import no.nav.helse.modell.varsel.Varsel.Status
import no.nav.helse.modell.varsel.Varsel.Status.AKTIV
import no.nav.helse.modell.varsel.Varsel.Status.GODKJENT
import no.nav.helse.modell.varsel.Varsel.Status.INAKTIV

internal interface VarselRepository {
    fun finnVarslerFor(vedtaksperiodeId: UUID): List<Varsel>
    fun erAktivFor(vedtaksperiodeId: UUID, varselkode: String): Boolean
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

    private val dao = VarselDao(dataSource)

    override fun finnVarslerFor(vedtaksperiodeId: UUID): List<Varsel> {
        return dao.alleVarslerFor(vedtaksperiodeId)
    }

    override fun erAktivFor(vedtaksperiodeId: UUID, varselkode: String): Boolean {
        return dao.finnVarselstatus(vedtaksperiodeId, varselkode) == AKTIV
    }

    override fun deaktiverFor(vedtaksperiodeId: UUID, varselkode: String) {
        endreStatusHvisAktiv(vedtaksperiodeId, varselkode, INAKTIV, "Spesialist")
    }

    override fun godkjennFor(vedtaksperiodeId: UUID, varselkode: String, ident: String) {
        endreStatusHvisAktiv(vedtaksperiodeId, varselkode, GODKJENT, ident)
    }

    override fun lagreVarsel(id: UUID, varselkode: String, opprettet: LocalDateTime, vedtaksperiodeId: UUID) {
        dao.lagreVarsel(id, varselkode, opprettet, vedtaksperiodeId)
    }

    override fun lagreDefinisjon(id: UUID, varselkode: String, tittel: String, forklaring: String?, handling: String?, avviklet: Boolean, opprettet: LocalDateTime) {
        dao.lagreDefinisjon(id, varselkode, tittel, forklaring, handling, avviklet, opprettet)
    }

    private fun endreStatusHvisAktiv(vedtaksperiodeId: UUID, varselkode: String, status: Status, ident: String) {
        if (dao.finnVarselstatus(vedtaksperiodeId, varselkode) != AKTIV) return
        dao.oppdaterStatus(vedtaksperiodeId, varselkode, status, ident)
    }
}