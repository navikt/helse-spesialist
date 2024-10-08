package no.nav.helse.db

import kotliquery.sessionOf
import no.nav.helse.HelseDao
import java.time.LocalDateTime
import java.util.UUID
import javax.sql.DataSource

class SaksbehandlerDao(private val dataSource: DataSource) : HelseDao(dataSource), SaksbehandlerRepository {
    fun opprettEllerOppdater(
        oid: UUID,
        navn: String,
        epost: String,
        ident: String,
    ) = asSQL(
        """ 
        INSERT INTO saksbehandler (oid, navn, epost, ident)
        VALUES (:oid, :navn, :epost, :ident)
        ON CONFLICT (oid)
            DO UPDATE SET navn = :navn, epost = :epost, ident = :ident
            WHERE (saksbehandler.navn, saksbehandler.epost, saksbehandler.ident)
                IS DISTINCT FROM (excluded.navn, excluded.epost, excluded.ident)
        """.trimIndent(),
        mapOf("oid" to oid, "navn" to navn, "epost" to epost, "ident" to ident),
    ).update()

    fun oppdaterSistObservert(
        oid: UUID,
        sisteHandlingUtført: LocalDateTime = LocalDateTime.now(),
    ) = asSQL(
        """ 
        UPDATE saksbehandler
        SET siste_handling_utført_tidspunkt = :siste_handling_utfort_tidspunkt
        WHERE oid = :oid
        """.trimIndent(),
        mapOf("oid" to oid, "siste_handling_utfort_tidspunkt" to sisteHandlingUtført),
    ).update()

    override fun finnSaksbehandler(oid: UUID) =
        sessionOf(dataSource).use { session ->
            session.transaction { transaction ->
                TransactionalSaksbehandlerDao(transaction).finnSaksbehandler(oid)
            }
        }
}
