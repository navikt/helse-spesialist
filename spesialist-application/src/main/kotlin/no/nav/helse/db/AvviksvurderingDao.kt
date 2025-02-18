package no.nav.helse.db

import no.nav.helse.modell.vilkårsprøving.AvviksvurderingDto

interface AvviksvurderingDao {
    fun finnAvviksvurderinger(fødselsnummer: String): List<AvviksvurderingDto>
}
