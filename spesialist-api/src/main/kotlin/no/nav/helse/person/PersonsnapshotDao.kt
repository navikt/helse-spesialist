package no.nav.helse.person

import com.fasterxml.jackson.module.kotlin.readValue
import kotliquery.Row
import no.nav.helse.HelseDao
import no.nav.helse.objectMapper
import java.util.*
import javax.sql.DataSource

class PersonsnapshotDao(dataSource: DataSource): HelseDao(dataSource) {
    fun finnPersonByFnr(fnr: String) =
        """ SELECT * FROM person AS p
                INNER JOIN person_info as pi ON pi.id = p.info_ref
                INNER JOIN speil_snapshot AS ss ON ss.person_ref = p.id
            WHERE p.fodselsnummer = :fnr;
        """.single(mapOf("fnr" to fnr.toLong())) { row ->tilPersonsnapshot(row) }

    fun finnFnrByAktørId(aktørId: String) =
        """SELECT fodselsnummer FROM person WHERE aktor_id = :aktorId"""
            .single(mapOf("aktorId" to aktørId.toLong()))  { it.long("fodselsnummer").toFødselsnummer() }

    fun finnFnrByVedtaksperiodeId(vedtaksperiodeId: UUID) =
        """ SELECT fodselsnummer FROM person
                INNER JOIN vedtak v on person.id = v.person_ref
            WHERE v.vedtaksperiode_id = :vedtaksperiodeId
            """.single(mapOf("vedtaksperiodeId" to vedtaksperiodeId)) { it.long("fodselsnummer").toFødselsnummer() }

    private fun tilPersonsnapshot(row: Row): Pair<PersonMetadataApiDto, SnapshotDto> {
        val person = PersonMetadataApiDto(
            fødselsnummer = row.long("fodselsnummer").toFødselsnummer(),
            aktørId = row.long("aktor_id").toString(),
            personinfo = PersoninfoApiDto(
                fornavn = row.string("fornavn"),
                mellomnavn = row.stringOrNull("mellomnavn"),
                etternavn = row.string("etternavn"),
                fødselsdato = row.localDateOrNull("fodselsdato"),
                kjønn = row.stringOrNull("kjonn")?.let(Kjønn::valueOf)
            )
        )
        val snapshot = objectMapper.readValue<SnapshotDto>(row.string("data"))
        return person to snapshot
    }

    private fun Long.toFødselsnummer() = if (this < 10000000000) "0$this" else this.toString()
}
