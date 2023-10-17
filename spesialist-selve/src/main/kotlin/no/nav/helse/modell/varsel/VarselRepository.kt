package no.nav.helse.modell.varsel

import java.time.LocalDateTime
import java.util.UUID
import javax.sql.DataSource
import no.nav.helse.mediator.builders.GenerasjonBuilder
import no.nav.helse.modell.varsel.Varsel.Status.AKTIV
import no.nav.helse.modell.varsel.Varsel.Status.GODKJENT
import no.nav.helse.modell.varsel.Varsel.Status.INAKTIV
import no.nav.helse.modell.vedtaksperiode.IVedtaksperiodeObserver
import no.nav.helse.tellInaktivtVarsel
import no.nav.helse.tellVarsel

internal class ActualVarselRepository(dataSource: DataSource): IVedtaksperiodeObserver {

    private val varselDao = VarselDao(dataSource)
    private val definisjonDao = DefinisjonDao(dataSource)

    internal fun byggGenerasjon(generasjonId: UUID, generasjonBuilder: GenerasjonBuilder) {
        generasjonBuilder.varsler(varselDao.varslerFor(generasjonId))
    }

    override fun varselOpprettet(
        varselId: UUID,
        vedtaksperiodeId: UUID,
        generasjonId: UUID,
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
        varselDao.oppdaterStatus(vedtaksperiodeId, generasjonId, varselkode, INAKTIV, null, null)
        if (varselkode.matches(varselkodeformat.toRegex())) tellInaktivtVarsel(varselkode)
    }

    override fun varselGodkjent(varselId: UUID, varselkode: String, generasjonId: UUID, vedtaksperiodeId: UUID, statusEndretAv: String) {
        val definisjon = definisjonDao.sisteDefinisjonFor(varselkode)
        varselDao.oppdaterStatus(vedtaksperiodeId, generasjonId, varselkode, status = GODKJENT, ident = statusEndretAv, definisjon.toDto().id)
    }

    override fun varselFlyttet(varselId: UUID, gammelGenerasjonId: UUID, nyGenerasjonId: UUID) {
        varselDao.oppdaterGenerasjon(varselId, gammelGenerasjonId, nyGenerasjonId)
    }

    internal fun lagreDefinisjon(definisjonDto: VarseldefinisjonDto) {
        definisjonDao.lagreDefinisjon(
            unikId = definisjonDto.id,
            kode = definisjonDto.varselkode,
            tittel = definisjonDto.tittel,
            forklaring = definisjonDto.forklaring,
            handling = definisjonDto.handling,
            avviklet = definisjonDto.avviklet,
            opprettet = definisjonDto.opprettet
        )
    }
}