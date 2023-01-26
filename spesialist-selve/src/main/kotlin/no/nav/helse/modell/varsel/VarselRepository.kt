package no.nav.helse.modell.varsel

import java.time.LocalDateTime
import java.util.UUID
import javax.sql.DataSource
import no.nav.helse.modell.varsel.Varsel.Status.AKTIV
import no.nav.helse.modell.varsel.Varsel.Status.AVVIST
import no.nav.helse.modell.varsel.Varsel.Status.GODKJENT
import no.nav.helse.modell.varsel.Varsel.Status.INAKTIV
import no.nav.helse.tellInaktivtVarsel
import no.nav.helse.tellVarsel

internal interface VarselRepository {
    fun deaktiverFor(vedtaksperiodeId: UUID, generasjonId: UUID, varselkode: String, definisjonId: UUID?)
    fun reaktiverFor(vedtaksperiodeId: UUID, generasjonId: UUID, varselkode: String)
    fun godkjennFor(vedtaksperiodeId: UUID, generasjonId: UUID, varselkode: String, ident: String, definisjonId: UUID?)
    fun avvisFor(vedtaksperiodeId: UUID, generasjonId: UUID, varselkode: String, ident: String, definisjonId: UUID?)
    fun lagreVarsel(id: UUID, generasjonId: UUID, varselkode: String, opprettet: LocalDateTime, vedtaksperiodeId: UUID)
    fun lagreDefinisjon(
        id: UUID,
        varselkode: String,
        tittel: String,
        forklaring: String?,
        handling: String?,
        avviklet: Boolean,
        opprettet: LocalDateTime,
    )
    // Midlertidig - Oppdater opprettet-tidspunkt for eksisterende definisjoner
    fun oppdaterOpprettetTidspunkt(id: UUID, opprettet: LocalDateTime)
}

internal class ActualVarselRepository(dataSource: DataSource) : VarselRepository {

    private val varselDao = VarselDao(dataSource)
    private val definisjonDao = DefinisjonDao(dataSource)

    override fun reaktiverFor(vedtaksperiodeId: UUID, generasjonId: UUID, varselkode: String) {
        varselDao.oppdaterVarsel(vedtaksperiodeId, generasjonId, varselkode, AKTIV, null, null)
        if (varselkode.matches(varselkodeformat.toRegex())) tellVarsel(varselkode)
    }

    override fun deaktiverFor(vedtaksperiodeId: UUID, generasjonId: UUID, varselkode: String, definisjonId: UUID?) {
        val definisjon = definisjonId?.let(definisjonDao::definisjonFor) ?: definisjonDao.sisteDefinisjonFor(varselkode)
        definisjon.oppdaterVarsel(vedtaksperiodeId, generasjonId, INAKTIV, "Spesialist", varselDao::oppdaterVarsel)
        if (varselkode.matches(varselkodeformat.toRegex())) tellInaktivtVarsel(varselkode)
    }

    override fun godkjennFor(vedtaksperiodeId: UUID, generasjonId: UUID, varselkode: String, ident: String, definisjonId: UUID?) {
        val definisjon = definisjonId?.let(definisjonDao::definisjonFor) ?: definisjonDao.sisteDefinisjonFor(varselkode)
        definisjon.oppdaterVarsel(vedtaksperiodeId, generasjonId, GODKJENT, ident, varselDao::oppdaterVarsel)
    }

    override fun avvisFor(vedtaksperiodeId: UUID, generasjonId: UUID, varselkode: String, ident: String, definisjonId: UUID?) {
        val definisjon = definisjonId?.let(definisjonDao::definisjonFor) ?: definisjonDao.sisteDefinisjonFor(varselkode)
        definisjon.oppdaterVarsel(vedtaksperiodeId, generasjonId, AVVIST, ident, varselDao::oppdaterVarsel)
    }

    override fun lagreVarsel(id: UUID, generasjonId: UUID, varselkode: String, opprettet: LocalDateTime, vedtaksperiodeId: UUID) {
        varselDao.lagreVarsel(id, varselkode, opprettet, vedtaksperiodeId, generasjonId)
        if (varselkode.matches(varselkodeformat.toRegex())) tellVarsel(varselkode)
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
        definisjonDao.lagreDefinisjon(id, varselkode, tittel, forklaring, handling, avviklet, opprettet)
    }

    // Midlertidig - Oppdater opprettet-tidspunkt for eksisterende definisjoner
    override fun oppdaterOpprettetTidspunkt(id: UUID, opprettet: LocalDateTime) {
        definisjonDao.oppdaterOpprettetTidspunkt(id, opprettet)
    }

}