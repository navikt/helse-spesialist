package no.nav.helse.modell.varsel

import javax.sql.DataSource

internal class VarselRepository(dataSource: DataSource) {
    private val varselDao = VarselDao(dataSource)
    private val definisjonDao = DefinisjonDao(dataSource)

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
