package no.nav.helse.vedtaksperiode

import com.fasterxml.jackson.module.kotlin.readValue
import kotliquery.Row
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.objectMapper
import no.nav.helse.person.Kjønn
import no.nav.helse.person.PersonDto
import no.nav.helse.person.PersoninfoApiDto
import org.intellij.lang.annotations.Language
import java.util.*
import javax.sql.DataSource

class VedtaksperiodeApiDao(private val dataSource: DataSource) {
    fun findVedtakByFnr(fnr: String) = sessionOf(dataSource).use  { session ->
        @Language("PostgreSQL")
        val query = """
            SELECT * FROM vedtak AS v
                INNER JOIN person AS p ON v.person_ref = p.id
                INNER JOIN person_info as pi ON pi.id=p.info_ref
                INNER JOIN speil_snapshot AS ss ON ss.id = v.speil_snapshot_ref
            WHERE p.fodselsnummer = ? ORDER BY v.id DESC LIMIT 1;
        """
        session.run(queryOf(query, fnr.toLong()).map(::tilVedtaksperiode).asSingle)
    }

    fun findVedtakByAktørId(aktørId: String) = sessionOf(dataSource).use  {
        @Language("PostgreSQL")
        val query = """
            SELECT * FROM vedtak AS v
                INNER JOIN person AS p ON v.person_ref = p.id
                INNER JOIN person_info AS pi ON pi.id=p.info_ref
                INNER JOIN speil_snapshot AS ss ON ss.id = v.speil_snapshot_ref
            WHERE p.aktor_id = ? ORDER BY v.id DESC LIMIT 1;
        """
        it.run(queryOf(query, aktørId.toLong()).map(::tilVedtaksperiode).asSingle)
    }

    fun findVedtakByVedtaksperiodeId(vedtaksperiodeId: UUID) = sessionOf(dataSource).use  { session ->
        @Language("PostgreSQL")
        val query = """
            SELECT * FROM vedtak AS v
                 INNER JOIN person AS p ON v.person_ref = p.id
                 INNER JOIN person_info as pi ON pi.id=p.info_ref
                 INNER JOIN speil_snapshot AS ss ON ss.id = v.speil_snapshot_ref
            WHERE v.vedtaksperiode_id = ? ORDER BY v.id DESC LIMIT 1;
        """
        session.run(queryOf(query, vedtaksperiodeId).map(::tilVedtaksperiode).asSingle)
    }

    private fun tilVedtaksperiode(row: Row): Pair<VedtaksperiodeApiDto, PersonDto> {
        val vedtak = VedtaksperiodeApiDto(
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
        val snapshot = objectMapper.readValue<PersonDto>(row.string("data"))
        return vedtak to snapshot
    }

    private fun Long.toFødselsnummer() = if (this < 10000000000) "0$this" else this.toString()
}
