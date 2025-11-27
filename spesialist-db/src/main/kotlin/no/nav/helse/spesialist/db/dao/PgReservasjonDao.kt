package no.nav.helse.spesialist.db.dao

import kotliquery.Session
import no.nav.helse.db.Reservasjon
import no.nav.helse.db.ReservasjonDao
import no.nav.helse.spesialist.application.logg.sikkerlogg
import no.nav.helse.spesialist.db.HelseDao
import no.nav.helse.spesialist.db.MedDataSource
import no.nav.helse.spesialist.db.MedSession
import no.nav.helse.spesialist.db.QueryRunner
import no.nav.helse.spesialist.domain.Saksbehandler
import no.nav.helse.spesialist.domain.SaksbehandlerOid
import java.util.UUID
import javax.sql.DataSource

class PgReservasjonDao private constructor(
    queryRunner: QueryRunner,
) : ReservasjonDao,
    QueryRunner by queryRunner {
    internal constructor(session: Session) : this(MedSession(session))
    internal constructor(dataSource: DataSource) : this(MedDataSource(dataSource))

    override fun reserverPerson(
        saksbehandlerOid: UUID,
        fødselsnummer: String,
    ) {
        HelseDao
            .asSQL(
                """
                INSERT INTO reserver_person(saksbehandler_ref, person_ref)
                SELECT :saksbehandler_ref, person.id
                FROM person
                WHERE person.fødselsnummer = :foedselsnummer
                ON CONFLICT (person_ref)
                    DO UPDATE SET gyldig_til = current_date + time '23:59:59',
                                  saksbehandler_ref = :saksbehandler_ref;
                """.trimIndent(),
                "saksbehandler_ref" to saksbehandlerOid,
                "foedselsnummer" to fødselsnummer,
            ).update()
    }

    override fun hentReservasjonFor(fødselsnummer: String): Reservasjon? =
        HelseDao
            .asSQL(
                """
                SELECT r.*, s.*, now() as detteErNow FROM reserver_person r
                JOIN person p ON p.id = r.person_ref
                JOIN saksbehandler s ON r.saksbehandler_ref = s.oid
                WHERE p.fødselsnummer = :foedselsnummer AND r.gyldig_til > now();
                """.trimIndent(),
                "foedselsnummer" to fødselsnummer,
            ).singleOrNull { row ->
                val personRef = row.long("person_ref")
                val gyldigTil = row.instant("gyldig_til")
                val detteErNow = row.instant("detteErNow")
                val reservasjon =
                    Reservasjon(
                        Saksbehandler(
                            id = SaksbehandlerOid(row.uuid("oid")),
                            navn = row.string("navn"),
                            epost = row.string("epost"),
                            ident = row.string("ident"),
                        ),
                    )
                sikkerlogg.info("Fant reservasjon for fødselsnummer $fødselsnummer med person_ref $personRef, gyldig til: $gyldigTil databasen mener at now er $detteErNow: $reservasjon")
                reservasjon
            }
}
