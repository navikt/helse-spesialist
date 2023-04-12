package no.nav.helse.modell.varsel

import java.time.LocalDateTime
import java.util.UUID
import javax.sql.DataSource
import no.nav.helse.modell.varsel.Varsel.Status.AKTIV
import no.nav.helse.modell.varsel.Varsel.Status.AVVIST
import no.nav.helse.modell.varsel.Varsel.Status.GODKJENT
import no.nav.helse.modell.varsel.Varsel.Status.INAKTIV
import no.nav.helse.modell.vedtaksperiode.IVedtaksperiodeObserver
import no.nav.helse.tellInaktivtVarsel
import no.nav.helse.tellVarsel

internal interface VarselRepository {
    fun godkjennFor(vedtaksperiodeId: UUID, generasjonId: UUID, varselkode: String, ident: String, definisjonId: UUID?)
    fun avvisFor(vedtaksperiodeId: UUID, generasjonId: UUID, varselkode: String, ident: String, definisjonId: UUID?)
    fun lagreDefinisjon(
        id: UUID,
        varselkode: String,
        tittel: String,
        forklaring: String?,
        handling: String?,
        avviklet: Boolean,
        opprettet: LocalDateTime,
    )

    fun oppdaterGenerasjonFor(id: UUID, gammelGenerasjonId: UUID, nyGenerasjonId: UUID)
}

internal class ActualVarselRepository(dataSource: DataSource) : VarselRepository, IVedtaksperiodeObserver {

    private val varselDao = VarselDao(dataSource)
    private val definisjonDao = DefinisjonDao(dataSource)

    internal fun finnVarslerFor(generasjonId: UUID): List<Varsel> {
        return varselDao.varslerFor(generasjonId)
    }

    override fun varselOpprettet(
        vedtaksperiodeId: UUID,
        generasjonId: UUID,
        varselId: UUID,
        varselkode: String,
        opprettet: LocalDateTime,
        ) {
        varselDao.lagreVarsel(varselId, varselkode, opprettet, vedtaksperiodeId, generasjonId)
        if (varselkode.matches(varselkodeformat.toRegex())) tellVarsel(varselkode)
    }

    override fun varselReaktivert(varselId: UUID, varselkode: String, generasjonId: UUID, vedtaksperiodeId: UUID) {
        varselDao.oppdaterStatus(vedtaksperiodeId, generasjonId, varselkode, AKTIV, null, null)
        if (varselkode.matches(varselkodeformat.toRegex())) tellVarsel(varselkode)
    }

    override fun varselDeaktivert(varselId: UUID, varselkode: String, generasjonId: UUID, vedtaksperiodeId: UUID) {
        val definisjon = definisjonDao.sisteDefinisjonFor(varselkode)
        definisjon.oppdaterVarsel(vedtaksperiodeId, generasjonId, INAKTIV, "Spesialist", varselDao::oppdaterStatus)
        if (varselkode.matches(varselkodeformat.toRegex())) tellInaktivtVarsel(varselkode)
    }

    override fun godkjennFor(vedtaksperiodeId: UUID, generasjonId: UUID, varselkode: String, ident: String, definisjonId: UUID?) {
        val definisjon = definisjonId?.let(definisjonDao::definisjonFor) ?: definisjonDao.sisteDefinisjonFor(varselkode)
        definisjon.oppdaterVarsel(vedtaksperiodeId, generasjonId, GODKJENT, ident, varselDao::oppdaterStatus)
    }

    override fun avvisFor(vedtaksperiodeId: UUID, generasjonId: UUID, varselkode: String, ident: String, definisjonId: UUID?) {
        val definisjon = definisjonId?.let(definisjonDao::definisjonFor) ?: definisjonDao.sisteDefinisjonFor(varselkode)
        definisjon.oppdaterVarsel(vedtaksperiodeId, generasjonId, AVVIST, ident, varselDao::oppdaterStatus)
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

    override fun oppdaterGenerasjonFor(id: UUID, gammelGenerasjonId: UUID, nyGenerasjonId: UUID) {
        varselDao.oppdaterGenerasjon(id, gammelGenerasjonId, nyGenerasjonId)
    }
}