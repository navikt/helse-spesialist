package no.nav.helse.spesialist.api.rest.personer.saksbilde

import no.nav.helse.spesialist.api.rest.saksbilde.ApiBegrunnelse
import no.nav.helse.spesialist.application.snapshot.SnapshotBegrunnelse
import no.nav.helse.spesialist.application.snapshot.SnapshotDag
import no.nav.helse.spesialist.application.snapshot.SnapshotSykdomsdagkilde
import no.nav.helse.spesialist.application.snapshot.SnapshotSykdomsdagkildetype
import no.nav.helse.spesialist.application.snapshot.SnapshotSykdomsdagtype
import no.nav.helse.spesialist.application.snapshot.SnapshotUtbetalingsdagType
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
            )

        // When:
        val apiDag = dag.tilApiDag()

        // Then:
        assertEquals(listOf(ApiBegrunnelse.OPPHOLDSKRAV), apiDag.begrunnelser)
    }
}
