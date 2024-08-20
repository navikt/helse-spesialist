package no.nav.helse.modell.varsel

import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.HelseDao
import no.nav.helse.modell.person.vedtaksperiode.Varsel.Status
import org.intellij.lang.annotations.Language
import java.time.LocalDateTime
import java.util.UUID
import javax.sql.DataSource

internal class VarselDao(private val dataSource: DataSource) : HelseDao(dataSource) {
    internal fun avvikleVarsel(
        varselkode: String,
        definisjonId: UUID,
    ) {
        @Language("PostgreSQL")
        val query =
            """
                UPDATE selve_varsel 
                SET status = :avvikletStatus,
                    status_endret_tidspunkt = :endretTidspunkt,
                    status_endret_ident = :ident, 
                    definisjon_ref = (SELECT id FROM api_varseldefinisjon WHERE unik_id = :definisjonId) 
                WHERE kode = :varselkode AND status = :aktivStatus;
            """

        sessionOf(dataSource).use { session ->
            session.run(
                queryOf(
                    query,
                    mapOf(
                        "avvikletStatus" to Status.AVVIKLET.name,
                        "aktivStatus" to Status.AKTIV.name,
                        "endretTidspunkt" to LocalDateTime.now(),
                        "ident" to "avviklet_fra_speaker",
                        "definisjonId" to definisjonId,
                        "varselkode" to varselkode,
                    ),
                ).asUpdate,
            )
        }
    }
}
