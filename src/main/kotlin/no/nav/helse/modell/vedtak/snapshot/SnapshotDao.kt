package no.nav.helse.modell.vedtak.snapshot

import kotliquery.Session
import kotliquery.queryOf
import java.util.*

internal fun Session.insertSpeilSnapshot(personBlob: String): Int =
    requireNotNull(
        this.run(
            queryOf(
                "INSERT INTO speil_snapshot(data) VALUES(CAST(? as json));",
                personBlob
            ).asUpdateAndReturnGeneratedKey
        )?.toInt()
    )

internal fun Session.findSpeilSnapshot(id: Int): String? =
    this.run(
        queryOf(
            "SELECT data FROM speil_snapshot WHERE id=?;",
            id
        ).map { it.string("data") }.asSingle
    )

internal fun Session.oppdaterSnapshotForVedtaksperiode(vedtaksperiodeId: UUID, snapshot: String): Int =
    requireNotNull(
        this.run(
            queryOf(
                """
                            UPDATE speil_snapshot
                            SET data=CAST(? as json)
                            WHERE id = (SELECT speil_snapshot_ref FROM vedtak WHERE vedtaksperiode_id=?);
                        """,
                snapshot,
                vedtaksperiodeId
            ).asUpdate
        )
    )
