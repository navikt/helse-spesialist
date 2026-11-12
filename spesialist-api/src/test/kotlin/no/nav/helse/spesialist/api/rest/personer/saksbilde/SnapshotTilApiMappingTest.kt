package no.nav.helse.spesialist.api.rest.personer.saksbilde

import no.nav.helse.spesialist.api.rest.saksbilde.ApiBegrunnelse
import no.nav.helse.spesialist.api.rest.saksbilde.ApiLøpendeVilkår
import no.nav.helse.spesialist.api.rest.saksbilde.ApiUtfallAvLøpendeVilkår
import no.nav.helse.spesialist.api.rest.saksbilde.ApiVurderingAvLøpendeVilkår
import no.nav.helse.spesialist.application.snapshot.SnapshotBegrunnelse
import no.nav.helse.spesialist.application.snapshot.SnapshotDag
import no.nav.helse.spesialist.application.snapshot.SnapshotLøpendeVilkår
import no.nav.helse.spesialist.application.snapshot.SnapshotSykdomsdagkilde
import no.nav.helse.spesialist.application.snapshot.SnapshotSykdomsdagkildetype
import no.nav.helse.spesialist.application.snapshot.SnapshotSykdomsdagtype
import no.nav.helse.spesialist.application.snapshot.SnapshotUtbetalingsdagType
import no.nav.helse.spesialist.application.snapshot.SnapshotUtfallAvLøpendeVilkår
import no.nav.helse.spesialist.application.snapshot.SnapshotVurderingAvLøpendeVilkår
import java.time.LocalDate
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

class SnapshotTilApiMappingTest {
    @Test
    fun `avvist dag med begrunnelsen Oppholdskrav får OPPHOLDSKRAV`() {
        // Given:
        val dag =
            SnapshotDag(
                begrunnelser = listOf(SnapshotBegrunnelse.OPPHOLDSKRAV),
                dato = LocalDate.parse("2026-01-05"),
                grad = 100.0,
                kilde = SnapshotSykdomsdagkilde(id = UUID.randomUUID(), type = SnapshotSykdomsdagkildetype.SOKNAD),
                sykdomsdagtype = SnapshotSykdomsdagtype.SYKEDAG,
                utbetalingsdagtype = SnapshotUtbetalingsdagType.AVVISTDAG,
                utbetalingsinfo = null,
                vurderingerAvLøpendeVilkår = emptyList(),
            )

        // When:
        val apiDag = dag.tilApiDag()

        // Then:
        assertEquals(listOf(ApiBegrunnelse.OPPHOLDSKRAV), apiDag.begrunnelser)
    }

    @Test
    fun `vurderinger av løpende vilkår mappes til api-dagen`() {
        // Given:
        val oppfylt = UUID.randomUUID()
        val ikkeOppfylt = UUID.randomUUID()
        val dag =
            SnapshotDag(
                begrunnelser = null,
                dato = LocalDate.parse("2026-01-05"),
                grad = 100.0,
                kilde = SnapshotSykdomsdagkilde(id = UUID.randomUUID(), type = SnapshotSykdomsdagkildetype.SOKNAD),
                sykdomsdagtype = SnapshotSykdomsdagtype.SYKEDAG,
                utbetalingsdagtype = SnapshotUtbetalingsdagType.NAVDAG,
                utbetalingsinfo = null,
                vurderingerAvLøpendeVilkår =
                    listOf(
                        SnapshotVurderingAvLøpendeVilkår(SnapshotLøpendeVilkår.OPPHOLDSKRAV, oppfylt, SnapshotUtfallAvLøpendeVilkår.OPPFYLT),
                        SnapshotVurderingAvLøpendeVilkår(SnapshotLøpendeVilkår.OPPHOLDSKRAV, ikkeOppfylt, SnapshotUtfallAvLøpendeVilkår.IKKE_OPPFYLT),
                    ),
            )

        // When:
        val apiDag = dag.tilApiDag()

        // Then:
        assertEquals(
            listOf(
                ApiVurderingAvLøpendeVilkår(ApiLøpendeVilkår.OPPHOLDSKRAV, oppfylt, ApiUtfallAvLøpendeVilkår.OPPFYLT),
                ApiVurderingAvLøpendeVilkår(ApiLøpendeVilkår.OPPHOLDSKRAV, ikkeOppfylt, ApiUtfallAvLøpendeVilkår.IKKE_OPPFYLT),
            ),
            apiDag.vurderingerAvLøpendeVilkår,
        )
    }
}
