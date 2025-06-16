package no.nav.helse.spesialist.application

import no.nav.helse.db.AvviksvurderingRepository
import no.nav.helse.modell.vilkårsprøving.Avviksvurdering
import java.util.UUID

class InMemoryAvviksvurderingRepository : AvviksvurderingRepository {
    private val avviksvurderinger = mutableMapOf<UUID, Avviksvurdering>()
    private val vilkarsgrunnlagIdAvviksvurderingIderMap = mutableMapOf<UUID, Avviksvurdering>()

    override fun lagre(avviksvurdering: Avviksvurdering) {
        avviksvurderinger[avviksvurdering.unikId] = avviksvurdering
        avviksvurdering.vilkårsgrunnlagId?.let { vilkårsgrunnlagId ->
            opprettKobling(
                avviksvurderingId = avviksvurdering.unikId,
                vilkårsgrunnlagId = vilkårsgrunnlagId
            )
        }
    }

    override fun opprettKobling(avviksvurderingId: UUID, vilkårsgrunnlagId: UUID) {
        avviksvurderinger[avviksvurderingId]?.let { avviksvurdering ->
            vilkarsgrunnlagIdAvviksvurderingIderMap[vilkårsgrunnlagId] = avviksvurdering
        }
    }

    override fun hentAvviksvurdering(vilkårsgrunnlagId: UUID) =
        vilkarsgrunnlagIdAvviksvurderingIderMap[vilkårsgrunnlagId]

    override fun hentAvviksvurderingFor(avviksvurderingId: UUID) =
        avviksvurderinger[avviksvurderingId]

    override fun finnAvviksvurderinger(fødselsnummer: String) =
        avviksvurderinger.values.filter { it.fødselsnummer == fødselsnummer }
}
