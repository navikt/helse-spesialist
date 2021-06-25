package no.nav.helse.overstyring

import kotliquery.queryOf
import kotliquery.sessionOf
import org.intellij.lang.annotations.Language
import java.util.*
import javax.sql.DataSource

class OverstyringApiDao(private val dataSource: DataSource) {
    fun finnOverstyring(fødselsnummer: String, organisasjonsnummer: String) = sessionOf(dataSource).use {
        @Language("PostgreSQL")
        val finnOverstyringQuery = """
            SELECT o.*, p.fodselsnummer, a.orgnummer, s.navn, s.ident FROM overstyring o
                INNER JOIN person p ON p.id = o.person_ref
                INNER JOIN arbeidsgiver a on a.id = o.arbeidsgiver_ref
                INNER JOIN saksbehandler s ON s.oid = o.saksbehandler_ref
            WHERE p.fodselsnummer = ?AND a.orgnummer = ?
        """
        it.run(
            queryOf(finnOverstyringQuery, fødselsnummer.toLong(), organisasjonsnummer.toLong())
                .map { overstyringRow ->
                val id = overstyringRow.long("id")
                OverstyringDto(
                    hendelseId = UUID.fromString(overstyringRow.string("hendelse_id")),
                    fødselsnummer = overstyringRow.long("fodselsnummer").toFødselsnummer(),
                    organisasjonsnummer = overstyringRow.int("orgnummer").toString(),
                    begrunnelse = overstyringRow.string("begrunnelse"),
                    timestamp = overstyringRow.localDateTime("tidspunkt"),
                    saksbehandlerNavn = overstyringRow.string("navn"),
                    saksbehandlerIdent = overstyringRow.stringOrNull("ident"),
                    overstyrteDager = it.run(
                        queryOf(
                            "SELECT * FROM overstyrtdag WHERE overstyring_ref = ?", id
                        ).map { overstyringDagRow ->
                            OverstyringDagDto(
                                dato = overstyringDagRow.localDate("dato"),
                                type = enumValueOf(overstyringDagRow.string("dagtype")),
                                grad = overstyringDagRow.intOrNull("grad")
                            )
                        }.asList
                    )
                )
            }.asList
        )
    }

    private fun Long.toFødselsnummer() = if (this < 10000000000) "0$this" else this.toString()
}
