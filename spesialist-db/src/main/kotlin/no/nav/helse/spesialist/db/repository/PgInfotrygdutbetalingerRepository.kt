package no.nav.helse.spesialist.db.repository

import kotliquery.Session
import no.nav.helse.spesialist.application.InfotrygdutbetalingerRepository
import no.nav.helse.spesialist.db.HelseDao.Companion.asSQL
import no.nav.helse.spesialist.db.MedSession
import no.nav.helse.spesialist.db.QueryRunner
import no.nav.helse.spesialist.domain.Identitetsnummer
import no.nav.helse.spesialist.domain.InfotrygdUtbetalinger

internal class PgInfotrygdutbetalingerRepository(
    session: Session,
) : QueryRunner by MedSession(session),
    InfotrygdutbetalingerRepository {
    override fun finn(identitetsnummer: Identitetsnummer): InfotrygdUtbetalinger? =
        asSQL(
            """
            SELECT i.data, p.infotrygdutbetalinger_oppdatert FROM infotrygdutbetalinger i
            INNER JOIN person p ON p.infotrygdutbetalinger_ref = i.id
            WHERE p.fødselsnummer = :fodselsnummer
            """,
            "fodselsnummer" to identitetsnummer.value,
        ).singleOrNull { row ->
            InfotrygdUtbetalinger.Factory.fraLagring(
                id = identitetsnummer,
                data = row.string("data"),
                oppdatert = row.localDate("infotrygdutbetalinger_oppdatert"),
            )
        }

    override fun lagre(infotrygdUtbetalinger: InfotrygdUtbetalinger) {
        val fødselsnummer = infotrygdUtbetalinger.id.value
        val existingInfotrygdutbetalingerId =
            asSQL(
                "SELECT infotrygdutbetalinger_ref FROM person WHERE fødselsnummer = :fodselsnummer",
                "fodselsnummer" to fødselsnummer,
            ).singleOrNull { row -> row.longOrNull("infotrygdutbetalinger_ref") }

        if (existingInfotrygdutbetalingerId == null) {
            val infotrygdutbetalingerId =
                asSQL(
                    "INSERT INTO infotrygdutbetalinger (data) VALUES (CAST(:data as json))",
                    "data" to infotrygdUtbetalinger.data,
                ).updateAndReturnGeneratedKey()
            asSQL(
                """
                UPDATE person SET infotrygdutbetalinger_ref = :ref, infotrygdutbetalinger_oppdatert = :oppdatert
                WHERE fødselsnummer = :fodselsnummer
                """,
                "ref" to infotrygdutbetalingerId,
                "oppdatert" to infotrygdUtbetalinger.oppdatert,
                "fodselsnummer" to fødselsnummer,
            ).update()
        } else {
            asSQL(
                "UPDATE infotrygdutbetalinger SET data = CAST(:data as json) WHERE id = :id",
                "data" to infotrygdUtbetalinger.data,
                "id" to existingInfotrygdutbetalingerId,
            ).update()
            asSQL(
                "UPDATE person SET infotrygdutbetalinger_oppdatert = :oppdatert WHERE fødselsnummer = :fodselsnummer",
                "oppdatert" to infotrygdUtbetalinger.oppdatert,
                "fodselsnummer" to fødselsnummer,
            ).update()
        }
    }
}
