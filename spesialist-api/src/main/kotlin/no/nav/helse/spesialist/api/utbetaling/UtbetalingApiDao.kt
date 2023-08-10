package no.nav.helse.spesialist.api.utbetaling

import javax.sql.DataSource
import kotliquery.Session
import kotliquery.queryOf
import kotliquery.sessionOf
import org.intellij.lang.annotations.Language

class UtbetalingApiDao(private val dataSource: DataSource) {

    fun findUtbetalinger(fødselsnummer: String): List<UtbetalingApiDto> {
        @Language("PostgreSQL")
        val query = """
SELECT DISTINCT ON (ui.id) *
FROM utbetaling_id ui
         JOIN utbetaling u ON ui.id = u.utbetaling_id_ref
         JOIN person p on ui.person_ref = p.id
         JOIN arbeidsgiver a on ui.arbeidsgiver_ref = a.id
         LEFT JOIN annullert_av_saksbehandler aas on u.annullert_av_saksbehandler_ref = aas.id
         LEFT JOIN saksbehandler s on aas.saksbehandler_ref = s.oid
         WHERE fodselsnummer = :fodselsnummer
ORDER BY ui.id, u.opprettet DESC
        """

        return sessionOf(dataSource).use { session ->
            session.run(
                queryOf(query, mapOf("fodselsnummer" to fødselsnummer.toLong()))
                    .map { row ->
                        val personoppdrag = findOppdrag(session, row.long("person_fagsystem_id_ref"))
                        val arbeidsgiveroppdrag = findOppdrag(session, row.long("arbeidsgiver_fagsystem_id_ref"))

                        UtbetalingApiDto(
                            id = row.uuid("utbetaling_id"),
                            type = row.string("type"),
                            status = Utbetalingsstatus.valueOf(row.string("status")),
                            personoppdrag = personoppdrag,
                            arbeidsgiveroppdrag = arbeidsgiveroppdrag,
                            annullertAvSaksbehandler = row.localDateTimeOrNull("annullert_tidspunkt")?.let {
                                AnnullertAvSaksbehandlerApiDto(
                                    annullertTidspunkt = it,
                                    saksbehandlerNavn = row.string("navn")
                                )
                            },
                            totalbeløp = personoppdrag.totalbeløp() + arbeidsgiveroppdrag.totalbeløp()
                        )
                    }
                    .asList)
        }
    }

    private fun findOppdrag(session: Session, fagsystemIdRef: Long): OppdragApiDto? =
        session.run(
            queryOf(
                "SELECT id,mottaker,fagsystem_id FROM oppdrag WHERE id = :fagsystemIdRef",
                mapOf("fagsystemIdRef" to fagsystemIdRef)
            ).map { row ->
                OppdragApiDto(
                    mottaker = row.string("mottaker"),
                    fagsystemId = row.string("fagsystem_id"),
                    utbetalingslinjer = findUtbetalingslinjer(session, row.long("id"))
                )
            }.asSingle
        )

        private fun findUtbetalingslinjer(session: Session, oppdragId: Long): List<UtbetalingslinjeApiDto> {
        @Language("PostgreSQL")
        val query = "SELECT fom, tom, totalbeløp FROM utbetalingslinje WHERE oppdrag_id=:oppdrag_id;"

        return session.run(queryOf(query, mapOf("oppdrag_id" to oppdragId))
            .map { row ->
                UtbetalingslinjeApiDto(
                    fom = row.localDate("fom"),
                    tom = row.localDate("tom"),
                    totalbeløp = row.intOrNull("totalbeløp")
                )
            }
            .asList)
    }

}

private fun OppdragApiDto?.totalbeløp(): Int {
    return this?.utbetalingslinjer?.sumOf { it.totalbeløp ?: 0 } ?: 0
}
