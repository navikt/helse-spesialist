package no.nav.helse.modell.varsel

import no.nav.helse.mediator.builders.GenerasjonBuilder
import no.nav.helse.modell.person.vedtaksperiode.IVedtaksperiodeObserver
import no.nav.helse.modell.varsel.Varsel.Status.AKTIV
import no.nav.helse.modell.varsel.Varsel.Status.GODKJENT
import no.nav.helse.modell.varsel.Varsel.Status.INAKTIV
import no.nav.helse.tellInaktivtVarsel
import no.nav.helse.tellVarsel
import java.time.LocalDateTime
import java.util.UUID
import javax.sql.DataSource

internal class VarselRepository(dataSource: DataSource) : IVedtaksperiodeObserver {
    private val varselDao = VarselDao(dataSource)
    private val definisjonDao = DefinisjonDao(dataSource)

    internal fun byggGenerasjon(
        generasjonId: UUID,
        generasjonBuilder: GenerasjonBuilder,
    ) {
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
        if (varselkode.matches(VARSELKODEFORMAT.toRegex())) tellVarsel(varselkode)
    }

    override fun varselReaktivert(
        varselId: UUID,
        varselkode: String,
        generasjonId: UUID,
        vedtaksperiodeId: UUID,
    ) {
        varselDao.oppdaterStatus(vedtaksperiodeId, generasjonId, varselkode, AKTIV, null, null)
        if (varselkode.matches(VARSELKODEFORMAT.toRegex())) tellVarsel(varselkode)
    }

    override fun varselDeaktivert(
        varselId: UUID,
        varselkode: String,
        generasjonId: UUID,
        vedtaksperiodeId: UUID,
    ) {
        varselDao.oppdaterStatus(vedtaksperiodeId, generasjonId, varselkode, INAKTIV, null, null)
        if (varselkode.matches(VARSELKODEFORMAT.toRegex())) tellInaktivtVarsel(varselkode)
    }

    override fun varselGodkjent(
        varselId: UUID,
        varselkode: String,
        generasjonId: UUID,
        vedtaksperiodeId: UUID,
        statusEndretAv: String,
    ) {
        val definisjon = definisjonDao.sisteDefinisjonFor(varselkode)
        varselDao.oppdaterStatus(
            vedtaksperiodeId,
            generasjonId,
            varselkode,
            status = GODKJENT,
            ident = statusEndretAv,
            definisjon.toDto().id,
        )
    }

    internal fun lagreDefinisjon(definisjonDto: VarseldefinisjonDto) {
        definisjonDao.lagreDefinisjon(
            unikId = definisjonDto.id,
            kode = definisjonDto.varselkode,
            tittel = definisjonDto.tittel,
            forklaring = definisjonDto.forklaring,
            handling = definisjonDto.handling,
            avviklet = definisjonDto.avviklet,
            opprettet = definisjonDto.opprettet,
        )
    }

    internal fun avvikleVarsel(definisjonDto: VarseldefinisjonDto) {
        varselDao.avvikleVarsel(varselkode = definisjonDto.varselkode, definisjonId = definisjonDto.id)
    }
}
