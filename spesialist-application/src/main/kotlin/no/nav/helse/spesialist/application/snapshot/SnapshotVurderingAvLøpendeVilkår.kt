package no.nav.helse.spesialist.application.snapshot

import java.util.UUID

data class SnapshotVurderingAvLøpendeVilkår(
    val vilkår: SnapshotLøpendeVilkår,
    val vurderingId: UUID,
    val utfall: SnapshotUtfallAvLøpendeVilkår,
)

enum class SnapshotLøpendeVilkår {
    OPPHOLDSKRAV,
}

enum class SnapshotUtfallAvLøpendeVilkår {
    OPPFYLT,
    IKKE_OPPFYLT,
}
