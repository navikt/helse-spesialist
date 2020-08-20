package no.nav.helse.modell.vedtak.snapshot

import kotliquery.Session
import kotliquery.queryOf
import org.intellij.lang.annotations.Language
import java.util.*

internal fun Session.insertSpeilSnapshot(personBlob: String): Int {
    @Language("PostgreSQL")
    val statement = "INSERT INTO speil_snapshot(data) VALUES(CAST(:personBlob as json));"
    return requireNotNull(
        this.run(
            queryOf(
                statement,
                mapOf("personBlob" to personBlob)
            ).asUpdateAndReturnGeneratedKey
        )?.toInt()
    )
}

internal fun Session.findSpeilSnapshot(id: Int): String? {
    @Language("PostgreSQL")
    val statement = "SELECT data FROM speil_snapshot WHERE id=:id;"
    return this.run(
        queryOf(
            statement,
            mapOf("id" to id)
        ).map { it.string("data") }.asSingle
    )
}

internal fun Session.oppdaterSnapshotForVedtaksperiode(vedtaksperiodeId: UUID, snapshot: String): Int {
    @Language("PostgreSQL")
    val statement = """
        UPDATE speil_snapshot
        SET data=CAST(:snapshot as json), sist_endret=now()
        WHERE id = (SELECT speil_snapshot_ref FROM vedtak WHERE vedtaksperiode_id=:vedtaksperiodeId);
    """
    return requireNotNull(
        this.run(
            queryOf(
                statement,
                mapOf(
                    "snapshot" to snapshot,
                    "vedtaksperiodeId" to vedtaksperiodeId
                )
            ).asUpdate
        )
    )
}
