package no.nav.helse.modell.varsel

import no.nav.helse.db.DefinisjonDao

class VarselRepository(
    private val varselDao: PgVarselDao,
    private val definisjonDao: DefinisjonDao,
) {
    fun lagreDefinisjon(definisjonDto: VarseldefinisjonDto) {
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
