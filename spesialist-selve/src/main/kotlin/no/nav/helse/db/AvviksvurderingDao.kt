package no.nav.helse.db

import javax.sql.DataSource
import no.nav.helse.HelseDao
import no.nav.helse.modell.avviksvurdering.AvviksvurderingDto

class AvviksvurderingDao(dataSource: DataSource) : HelseDao(dataSource) {

    internal fun lagre(avviksvurdering: AvviksvurderingDto) {
        TODO("lagre i ny tabell")
    }
}
