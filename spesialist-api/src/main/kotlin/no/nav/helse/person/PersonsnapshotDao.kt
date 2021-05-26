package no.nav.helse.person

import com.fasterxml.jackson.module.kotlin.readValue
import kotliquery.Row
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.objectMapper
import org.intellij.lang.annotations.Language
import java.util.*
import javax.sql.DataSource

class PersonsnapshotDao(private val dataSource: DataSource) {
    fun finnPersonByFnr(fnr: String) = sessionOf(dataSource).use  { session ->
        @Language("PostgreSQL")
        val query = """
            SELECT * FROM vedtak AS v
                INNER JOIN person AS p ON v.person_ref = p.id
                INNER JOIN person_info as pi ON pi.id=p.info_ref
                INNER JOIN speil_snapshot AS ss ON ss.id = v.speil_snapshot_ref
            WHERE p.fodselsnummer = ? ORDER BY v.id DESC LIMIT 1;
        """
        session.run(queryOf(query, fnr.toLong()).map(::tilPersonsnapshot).asSingle)
    }

    fun finnPersonByAktørid(aktørId: String) = sessionOf(dataSource).use  {
        @Language("PostgreSQL")
        val query = """
            SELECT * FROM vedtak AS v
                INNER JOIN person AS p ON v.person_ref = p.id
                INNER JOIN person_info AS pi ON pi.id=p.info_ref
                INNER JOIN speil_snapshot AS ss ON ss.id = v.speil_snapshot_ref
            WHERE p.aktor_id = ? ORDER BY v.id DESC LIMIT 1;
        """
        it.run(queryOf(query, aktørId.toLong()).map(::tilPersonsnapshot).asSingle)
    }

    fun finnPersonByVedtaksperiodeid(vedtaksperiodeId: UUID) = sessionOf(dataSource).use  { session ->
        @Language("PostgreSQL")
        val query = """
            SELECT * FROM vedtak AS v
                 INNER JOIN person AS p ON v.person_ref = p.id
                 INNER JOIN person_info as pi ON pi.id=p.info_ref
                 INNER JOIN speil_snapshot AS ss ON ss.id = v.speil_snapshot_ref
            WHERE v.vedtaksperiode_id = ? ORDER BY v.id DESC LIMIT 1;
        """
        session.run(queryOf(query, vedtaksperiodeId).map(::tilPersonsnapshot).asSingle)
    }

    private fun tilPersonsnapshot(row: Row): Pair<PersonMetadataDto, SnapshotDto> {
        val person = PersonMetadataDto(
            fødselsnummer = row.long("fodselsnummer").toFødselsnummer(),
            aktørId = row.long("aktor_id").toString(),
            personinfo = PersoninfoApiDto(
                fornavn = row.string("fornavn"),
                mellomnavn = row.stringOrNull("mellomnavn"),
                etternavn = row.string("etternavn"),
                fødselsdato = row.localDateOrNull("fodselsdato"),
                kjønn = row.stringOrNull("kjonn")?.let(Kjønn::valueOf)
            ),
            arbeidsgiverRef = row.long("arbeidsgiver_ref"),
            speilSnapshotRef = row.int("speil_snapshot_ref"),
            infotrygdutbetalingerRef = row.intOrNull("infotrygdutbetalinger_ref")
        )
        val snapshot = objectMapper.readValue<SnapshotDto>(row.string("data"))
        return person to snapshot
    }

    private fun Long.toFødselsnummer() = if (this < 10000000000) "0$this" else this.toString()
}
