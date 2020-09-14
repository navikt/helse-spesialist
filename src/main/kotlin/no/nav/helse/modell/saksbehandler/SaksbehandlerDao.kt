package no.nav.helse.modell.saksbehandler
import kotliquery.Session
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import org.intellij.lang.annotations.Language
import java.util.*
import javax.sql.DataSource

internal class SaksbehandlerDao(private val dataSource: DataSource) {
    internal fun opprettSaksbehandler(
        oid: UUID,
        navn: String,
        epost: String
    ) = using(sessionOf(dataSource)) {
        it.persisterSaksbehandler(oid, navn, epost)
    }

    internal fun finnSaksbehandler(oid: UUID) = using(sessionOf(dataSource)) {
        it.finnSaksbehandler(oid)
    }
}

fun Session.persisterSaksbehandler(
    oid: UUID,
    navn: String,
    epost: String
) {
    @Language("PostgreSQL")
    val opprettSaksbehandlerQuery = """
        INSERT INTO saksbehandler(oid, navn, epost)
        VALUES (:oid,
                :navn,
                :epost)
        ON CONFLICT DO NOTHING
    """

    run(
        queryOf(
            opprettSaksbehandlerQuery,
            mapOf(
                "oid" to oid,
                "navn" to navn,
                "epost" to epost
            )
        ).asUpdate
    )
}

fun Session.finnSaksbehandler(oid: UUID): List<SaksbehandlerDto> {
    @Language("PostgreSQL")
    val finnSaksbehandlerQuery = """
SELECT *
FROM saksbehandler
WHERE oid = ?
    """
    return this.run(queryOf(finnSaksbehandlerQuery, oid).map { saksbehandlerRow ->
        val oid = saksbehandlerRow.string("oid")

        SaksbehandlerDto(
            oid = UUID.fromString(oid),
            navn = saksbehandlerRow.string("navn"),
            epost = saksbehandlerRow.string("epost")
        )
    }.asList)
}


data class SaksbehandlerDto(
    val oid: UUID,
    val navn: String,
    val epost: String
)
