package no.nav.helse.db

import no.nav.helse.modell.vilkårsprøving.Avviksvurdering
import java.util.UUID

interface AvviksvurderingRepository {
    fun lagre(avviksvurdering: Avviksvurdering)

    fun opprettKobling(
        avviksvurderingId: UUID,
        vilkårsgrunnlagId: UUID,
    )

    fun hentAvviksvurdering(vilkårsgrunnlagId: UUID): Avviksvurdering?

    fun finnAvviksvurderinger(fødselsnummer: String): List<Avviksvurdering>
}
