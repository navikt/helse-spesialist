package no.nav.helse.db

import kotliquery.TransactionalSession
import kotliquery.queryOf
import no.nav.helse.modell.stoppautomatiskbehandling.StoppknappÅrsak
import org.intellij.lang.annotations.Language
import java.time.LocalDateTime

class TransactionalStansAutomatiskBehandlingDao(private val transactionalSession: TransactionalSession) : StansAutomatiskBehandlingRepository {
    override fun hentFor(fødselsnummer: String): List<StansAutomatiskBehandlingFraDatabase> {
        @Language("PostgreSQL")
        val statement =
            """
            select fødselsnummer, status, årsaker, opprettet, original_melding->>'uuid' as melding_id 
            from stans_automatisering 
            where fødselsnummer = :fnr            
            """.trimIndent()
        return transactionalSession.run(
            queryOf(statement, mapOf("fnr" to fødselsnummer))
                .map { row ->
                    StansAutomatiskBehandlingFraDatabase(
                        fødselsnummer = row.string("fødselsnummer"),
                        status = row.string("status"),
                        årsaker = row.array<String>("årsaker").map { enumValueOf<StoppknappÅrsak>(it) }.toSet(),
                        opprettet = row.localDateTime("opprettet"),
                        meldingId = row.stringOrNull("melding_id"),
                    )
                }.asList,
        )
    }

    override fun lagreFraISyfo(
        fødselsnummer: String,
        status: String,
        årsaker: Set<StoppknappÅrsak>,
        opprettet: LocalDateTime,
        originalMelding: String?,
        kilde: String,
    ) {
        @Language("PostgreSQL")
        val statement =
            """
            insert into stans_automatisering (fødselsnummer, status, årsaker, opprettet, kilde, original_melding) 
            values (:fnr, :status, :arsaker::varchar[], :opprettet, :kilde, cast(:originalMelding as json))
            """.trimIndent()
        transactionalSession.run(
            queryOf(
                statement,
                mapOf(
                    "fnr" to fødselsnummer,
                    "status" to status,
                    "arsaker" to årsaker.map(StoppknappÅrsak::name).somDbArray(),
                    "opprettet" to opprettet,
                    "kilde" to kilde,
                    "originalMelding" to originalMelding,
                ),
            ).asUpdate,
        )
    }

    override fun lagreFraSpeil(fødselsnummer: String) {
        @Language("PostgreSQL")
        val statement =
            """
            insert into stans_automatisering (fødselsnummer, status, årsaker, opprettet, kilde, original_melding) 
            values (:fnr, 'NORMAL', '{}', now(), 'SPEIL', cast(:originalMelding as json))
            """.trimIndent()
        transactionalSession.run(
            queryOf(
                statement,
                mapOf(
                    "fnr" to fødselsnummer,
                    "originalMelding" to null,
                ),
            ).asUpdate,
        )
    }
}
