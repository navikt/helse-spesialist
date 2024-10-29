package no.nav.helse.db

import no.nav.helse.modell.vilkårsprøving.AvviksvurderingDto
import java.util.UUID

interface AvviksvurderingDao {
    fun opprettKobling(
        avviksvurderingId: UUID,
        vilkårsgrunnlagId: UUID,
    )

    fun finnAvviksvurderinger(fødselsnummer: String): List<AvviksvurderingDto>
}
