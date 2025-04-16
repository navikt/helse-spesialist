package no.nav.helse.spesialist.db.dao

import kotliquery.Session
import no.nav.helse.db.StansAutomatiskBehandlingSaksbehandlerDao
import no.nav.helse.spesialist.db.HelseDao.Companion.asSQL
import no.nav.helse.spesialist.db.MedDataSource
import no.nav.helse.spesialist.db.MedSession
import no.nav.helse.spesialist.db.QueryRunner
import java.time.LocalDateTime
import javax.sql.DataSource

class PgStansAutomatiskBehandlingSaksbehandlerDao private constructor(queryRunner: QueryRunner) :
    StansAutomatiskBehandlingSaksbehandlerDao, QueryRunner by queryRunner {
        internal constructor(session: Session) : this(MedSession(session))
        internal constructor(dataSource: DataSource) : this(MedDataSource(dataSource))

        override fun lagreStans(fødselsnummer: String) {
            asSQL(
                """
                insert into stans_automatisk_behandling_saksbehandler (fødselsnummer, opprettet)
                values (:fodselsnummer, :opprettet)
                on conflict do nothing 
                """.trimIndent(),
                "fodselsnummer" to fødselsnummer,
                "opprettet" to LocalDateTime.now(),
            ).update()
        }

        override fun opphevStans(fødselsnummer: String) {
            asSQL(
                """
                delete from stans_automatisk_behandling_saksbehandler
                where fødselsnummer = :fodselsnummer
                """.trimIndent(),
                "fodselsnummer" to fødselsnummer,
            ).update()
        }

        override fun erStanset(fødselsnummer: String): Boolean =
            asSQL(
                """
                select exists (
                    select 1 from stans_automatisk_behandling_saksbehandler
                    where fødselsnummer = :fodselsnummer
                )
                """.trimIndent(),
                "fodselsnummer" to fødselsnummer,
            ).single { it.boolean(1) }
    }
