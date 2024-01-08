package no.nav.helse.db

import com.fasterxml.jackson.module.kotlin.readValue
import java.util.UUID
import javax.sql.DataSource
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.HelseDao
import no.nav.helse.modell.avviksvurdering.Avviksvurdering
import no.nav.helse.modell.avviksvurdering.AvviksvurderingDto
import no.nav.helse.modell.avviksvurdering.BeregningsgrunnlagDto
import no.nav.helse.modell.avviksvurdering.SammenligningsgrunnlagDto
import no.nav.helse.objectMapper
import no.nav.helse.spesialist.api.avviksvurdering.Beregningsgrunnlag
import no.nav.helse.spesialist.api.avviksvurdering.Sammenligningsgrunnlag
import org.intellij.lang.annotations.Language
import org.slf4j.LoggerFactory
import no.nav.helse.spesialist.api.avviksvurdering.Avviksvurdering as ApiAvviksvurdering

class AvviksvurderingDao(private val dataSource: DataSource) : HelseDao(dataSource) {

    internal fun lagre(avviksvurdering: AvviksvurderingDto) {
        sessionOf(dataSource, returnGeneratedKey = true).use { session ->
            @Language("PostgreSQL")
            val opprettAvviksvurderingQuery = """
                INSERT INTO avviksvurdering(unik_id, fødselsnummer, skjæringstidspunkt, opprettet, avviksprosent, beregningsgrunnlag, sammenligningsgrunnlag_ref)
                VALUES (:unik_id, :fodselsnummer, :skjaeringstidspunkt, :opprettet, :avviksprosent, CAST(:beregningsgrunnlag as json), :sammenligningsgrunnlag_ref)
                ON CONFLICT (unik_id) DO UPDATE SET opprettet = excluded.opprettet, avviksprosent = excluded.avviksprosent, beregningsgrunnlag = excluded.beregningsgrunnlag, sammenligningsgrunnlag_ref = excluded.sammenligningsgrunnlag_ref;
            """.trimIndent()

            @Language("PostgreSQL")
            val opprettSammenligningsgrunnlagQuery = """
                INSERT INTO sammenligningsgrunnlag(fødselsnummer, skjæringstidspunkt, opprettet, sammenligningsgrunnlag)
                VALUES (:fodselsnummer, :skjaeringstidspunkt, :opprettet, CAST(:sammenligningsgrunnlag as json));
            """.trimIndent()

            @Language("PostgreSQL")
            val opprettKoblingTilVilkårsgrunnlag = """
                INSERT INTO vilkarsgrunnlag_per_avviksvurdering(avviksvurdering_ref, vilkårsgrunnlag_id)
                VALUES (:unik_id, :vilkarsgrunnlag_id) ON CONFLICT DO NOTHING;
            """.trimIndent()

            session.transaction { transactionalSession ->
                val sammenligningsgrunnlagRef = transactionalSession.run(
                    queryOf(
                        opprettSammenligningsgrunnlagQuery,
                        mapOf(
                            "fodselsnummer" to avviksvurdering.fødselsnummer,
                            "skjaeringstidspunkt" to avviksvurdering.skjæringstidspunkt,
                            "opprettet" to avviksvurdering.opprettet,
                            "sammenligningsgrunnlag" to objectMapper.writeValueAsString(avviksvurdering.sammenligningsgrunnlag)
                        )
                    ).asUpdateAndReturnGeneratedKey
                )
                transactionalSession.run(
                    queryOf(
                        opprettAvviksvurderingQuery,
                        mapOf(
                            "unik_id" to avviksvurdering.unikId,
                            "fodselsnummer" to avviksvurdering.fødselsnummer,
                            "skjaeringstidspunkt" to avviksvurdering.skjæringstidspunkt,
                            "opprettet" to avviksvurdering.opprettet,
                            "avviksprosent" to avviksvurdering.avviksprosent,
                            "beregningsgrunnlag" to objectMapper.writeValueAsString(avviksvurdering.beregningsgrunnlag),
                            "sammenligningsgrunnlag_ref" to sammenligningsgrunnlagRef
                        )
                    ).asUpdate
                )
                if (avviksvurdering.vilkårsgrunnlagId != null) {
                    transactionalSession.run(
                        queryOf(
                            opprettKoblingTilVilkårsgrunnlag,
                            mapOf(
                                "unik_id" to avviksvurdering.unikId,
                                "vilkarsgrunnlag_id" to avviksvurdering.vilkårsgrunnlagId,
                            )
                        ).asUpdate
                    )
                }
            }
        }
    }

    internal fun finnAvviksvurderinger(fødselsnummer: String): List<Avviksvurdering> = asSQL(
        """
            SELECT av.unik_id, vpa.vilkårsgrunnlag_id, av.fødselsnummer, av.skjæringstidspunkt, av.opprettet, avviksprosent, beregningsgrunnlag, sg.sammenligningsgrunnlag FROM avviksvurdering av 
            INNER JOIN sammenligningsgrunnlag sg ON av.sammenligningsgrunnlag_ref = sg.id
            INNER JOIN vilkarsgrunnlag_per_avviksvurdering vpa ON vpa.avviksvurdering_ref = av.unik_id
            WHERE av.fødselsnummer = :fodselsnummer
            AND av.slettet IS NULL;
        """.trimIndent(),
        mapOf(
            "fodselsnummer" to fødselsnummer,
        )
    ).list {
        Avviksvurdering(
            unikId = it.uuid("unik_id"),
            vilkårsgrunnlagId = it.uuid("vilkårsgrunnlag_id"),
            fødselsnummer = it.string("fødselsnummer"),
            skjæringstidspunkt = it.localDate("skjæringstidspunkt"),
            opprettet = it.localDateTime("opprettet"),
            avviksprosent = it.double("avviksprosent"),
            sammenligningsgrunnlag = objectMapper.readValue<SammenligningsgrunnlagDto>(it.string("sammenligningsgrunnlag")),
            beregningsgrunnlag = objectMapper.readValue<BeregningsgrunnlagDto>(it.string("beregningsgrunnlag")),
        )
    }

    internal fun finnAvviksvurdering(vilkårsgrunnlagId: UUID): ApiAvviksvurdering? = asSQL(
        """
            SELECT av.unik_id, vpa.vilkårsgrunnlag_id, av.fødselsnummer, av.skjæringstidspunkt, av.opprettet, avviksprosent, beregningsgrunnlag, sg.sammenligningsgrunnlag 
            FROM avviksvurdering av 
            INNER JOIN sammenligningsgrunnlag sg ON av.sammenligningsgrunnlag_ref = sg.id
            INNER JOIN vilkarsgrunnlag_per_avviksvurdering vpa ON vpa.avviksvurdering_ref = av.unik_id
            WHERE vpa.vilkårsgrunnlag_id = :vilkarsgrunnlagId AND av.slettet IS NULL
            ORDER BY av.opprettet DESC
            LIMIT 1;
        """.trimIndent(),
        mapOf(
            "vilkarsgrunnlagId" to vilkårsgrunnlagId,
        )
    ).single {
        ApiAvviksvurdering(
            unikId = it.uuid("unik_id"),
            vilkårsgrunnlagId = it.uuid("vilkårsgrunnlag_id"),
            fødselsnummer = it.string("fødselsnummer"),
            skjæringstidspunkt = it.localDate("skjæringstidspunkt"),
            opprettet = it.localDateTime("opprettet"),
            avviksprosent = it.double("avviksprosent"),
            sammenligningsgrunnlag = objectMapper.readValue<Sammenligningsgrunnlag>(it.string("sammenligningsgrunnlag")),
            beregningsgrunnlag = objectMapper.readValue<Beregningsgrunnlag>(it.string("beregningsgrunnlag")),
        )
    }

    fun opprettKobling(avviksvurderingId: UUID, vilkårsgrunnlagId: UUID) {
        try {
            asSQL(
                """
                    INSERT INTO vilkarsgrunnlag_per_avviksvurdering(avviksvurdering_ref, vilkårsgrunnlag_id)
                    VALUES (:unik_id, :vilkarsgrunnlag_id) ON CONFLICT DO NOTHING;
                """.trimIndent(),
                mapOf(
                    "unik_id" to avviksvurderingId,
                    "vilkarsgrunnlag_id" to vilkårsgrunnlagId,
                )
            ).update()
        } catch (e: Exception) {
            logg.info("Lagrer IKKE kobling mellom avviksvurdering og vilkårsgrunnlag. NB! Midlertidig kode!")
        }
    }

    companion object {
        private val logg = LoggerFactory.getLogger(AvviksvurderingDao::class.java)
    }
}
