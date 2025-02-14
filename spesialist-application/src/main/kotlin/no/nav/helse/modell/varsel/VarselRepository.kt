package no.nav.helse.modell.varsel

import no.nav.helse.db.DefinisjonDao
import no.nav.helse.db.VarselDao

class VarselRepository(
    private val varselDao: VarselDao,
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

    fun avvikleVarsel(definisjonDto: VarseldefinisjonDto) {
        varselDao.avvikleVarsel(varselkode = definisjonDto.varselkode, definisjonId = definisjonDto.id)
    }
}
