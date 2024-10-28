package no.nav.helse.db

import kotliquery.Session
import no.nav.helse.HelseDao.Companion.asSQL
import no.nav.helse.modell.stoppautomatiskbehandling.StoppknappÅrsak
import java.time.LocalDateTime
import javax.sql.DataSource

class StansAutomatiskBehandlingDao(queryRunner: QueryRunner) :
    StansAutomatiskBehandlingRepository, QueryRunner by queryRunner {
    constructor(session: Session) : this(MedSession(session))
    constructor(dataSource: DataSource) : this(MedDataSource(dataSource))

    override fun lagreFraISyfo(
        fødselsnummer: String,
        status: String,
        årsaker: Set<StoppknappÅrsak>,
        opprettet: LocalDateTime,
        originalMelding: String?,
        kilde: String,
    ) {
        asSQL(
            """
            insert into stans_automatisering (fødselsnummer, status, årsaker, opprettet, kilde, original_melding) 
            values (:fnr, :status, :arsaker, :opprettet, :kilde, cast(:originalMelding as json))
            """.trimIndent(),
            "fnr" to fødselsnummer,
            "status" to status,
            "arsaker" to createArrayOf("varchar", årsaker.map(StoppknappÅrsak::name)),
            "opprettet" to opprettet,
            "kilde" to kilde,
            "originalMelding" to originalMelding,
        ).update()
    }

    override fun lagreFraSpeil(fødselsnummer: String) {
        asSQL(
            """
            insert into stans_automatisering (fødselsnummer, status, årsaker, opprettet, kilde, original_melding) 
            values (:fnr, 'NORMAL', '{}', now(), 'SPEIL', cast(:originalMelding as json))
            """.trimIndent(),
            "fnr" to fødselsnummer,
            "originalMelding" to null,
        ).update()
    }

    override fun hentFor(fødselsnummer: String) =
        asSQL(
            """
            select fødselsnummer, status, årsaker, opprettet, original_melding->>'uuid' as melding_id 
            from stans_automatisering 
            where fødselsnummer = :fnr            
            """.trimIndent(),
            "fnr" to fødselsnummer,
        ).list { row ->
            StansAutomatiskBehandlingFraDatabase(
                fødselsnummer = row.string("fødselsnummer"),
                status = row.string("status"),
                årsaker = row.array<String>("årsaker").map { enumValueOf<StoppknappÅrsak>(it) }.toSet(),
                opprettet = row.localDateTime("opprettet"),
                meldingId = row.stringOrNull("melding_id"),
            )
        }
}
