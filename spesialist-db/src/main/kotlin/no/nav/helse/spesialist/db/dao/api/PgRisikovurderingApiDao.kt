package no.nav.helse.spesialist.db.dao.api

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.db.api.RisikovurderingApiDao
import no.nav.helse.spesialist.api.risikovurdering.RisikovurderingApiDto
import no.nav.helse.spesialist.db.HelseDao.Companion.asSQL
import no.nav.helse.spesialist.db.MedDataSource
import no.nav.helse.spesialist.db.QueryRunner
import no.nav.helse.spesialist.db.objectMapper
import java.util.UUID
import javax.sql.DataSource

class PgRisikovurderingApiDao internal constructor(dataSource: DataSource) :
    QueryRunner by MedDataSource(dataSource),
    RisikovurderingApiDao {
        override fun finnRisikovurderinger(fødselsnummer: String): Map<UUID, RisikovurderingApiDto> =
            asSQL(
                """
                with vedtaksperioder_for_person as (
                    select vedtaksperiode_id
                    from vedtak v
                    join person p on v.person_ref = p.id
                    where p.fødselsnummer = :foedselsnummer
                )
                select distinct on (r.vedtaksperiode_id) r.vedtaksperiode_id, data
                from risikovurdering_2021 r
                join vedtaksperioder_for_person vfp on r.vedtaksperiode_id = vfp.vedtaksperiode_id
                order by r.vedtaksperiode_id, id desc
                """.trimIndent(),
                "foedselsnummer" to fødselsnummer,
            ).list {
                it.uuid("vedtaksperiode_id") to toDto(it.string("data"))
            }.toMap()

        private fun toDto(data: String) =
            objectMapper.readTree(data).let {
                RisikovurderingApiDto(
                    funn = it["funn"].toList(),
                    kontrollertOk = it["kontrollertOk"].toList(),
                )
            }

        private fun JsonNode.toList(): List<JsonNode> =
            objectMapper.readValue(
                traverse(),
                object : TypeReference<List<JsonNode>>() {},
            )
    }
