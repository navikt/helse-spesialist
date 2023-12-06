package no.nav.helse.db

import com.fasterxml.jackson.module.kotlin.readValue
import java.time.LocalDate
import javax.sql.DataSource
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.HelseDao
import no.nav.helse.modell.avviksvurdering.Avviksvurdering
import no.nav.helse.modell.avviksvurdering.AvviksvurderingDto
import no.nav.helse.modell.avviksvurdering.BeregningsgrunnlagDto
import no.nav.helse.modell.avviksvurdering.SammenligningsgrunnlagDto
import no.nav.helse.objectMapper
import org.intellij.lang.annotations.Language

class AvviksvurderingDao(private val dataSource: DataSource) : HelseDao(dataSource) {

    internal fun lagre(avviksvurdering: AvviksvurderingDto) {
        sessionOf(dataSource, returnGeneratedKey = true).use { session ->
            @Language("PostgreSQL")
            val opprettAvviksvurderingQuery = """
                INSERT INTO avviksvurdering(unik_id, fødselsnummer, skjæringstidspunkt, opprettet, avviksprosent, beregningsgrunnlag, sammenligningsgrunnlag_ref)
                VALUES (:unik_id, :fodselsnummer, :skjaeringstidspunkt, :opprettet, :avviksprosent, CAST(:beregningsgrunnlag as json), :sammenligningsgrunnlag_ref)
            """.trimIndent()

            @Language("PostgreSQL")
            val opprettSammenligningsgrunnlagQuery = """
                INSERT INTO sammenligningsgrunnlag(unik_id, fødselsnummer, skjæringstidspunkt, opprettet, sammenligningsgrunnlag)
                VALUES (:unik_id, :fodselsnummer, :skjaeringstidspunkt, :opprettet, CAST(:sammenligningsgrunnlag as json))
            """.trimIndent()

            session.transaction { transactionalSession ->
                val sammenligningsgrunnlagRef = transactionalSession.run(
                    queryOf(
                        opprettSammenligningsgrunnlagQuery,
                        mapOf(
                            "unik_id" to avviksvurdering.sammenligningsgrunnlag.unikId,
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
            }
        }
    }

    internal fun finnAvviksvurdering(fødselsnummer: String, skjæringstidpunkt: LocalDate): Avviksvurdering? = asSQL(
        """
            SELECT av.unik_id, av.fødselsnummer, av.skjæringstidspunkt, av.opprettet, avviksprosent, beregningsgrunnlag, sg.sammenligningsgrunnlag FROM avviksvurdering av 
            INNER JOIN sammenligningsgrunnlag sg ON av.sammenligningsgrunnlag_ref = sg.id
            WHERE av.fødselsnummer = :fodselsnummer AND av.skjæringstidspunkt = :skjaeringstidspunkt;
        """.trimIndent(),
        mapOf(
            "fodselsnummer" to fødselsnummer,
            "skjaeringstidspunkt" to skjæringstidpunkt
        )
    ).single {
        Avviksvurdering(
            unikId = it.uuid("unik_id"),
            fødselsnummer = it.string("fødselsnummer"),
            skjæringstidspunkt = it.localDate("skjæringstidspunkt"),
            opprettet = it.localDateTime("opprettet"),
            avviksprosent = it.double("avviksprosent"),
            sammenligningsgrunnlag = objectMapper.readValue<SammenligningsgrunnlagDto>(it.string("sammenligningsgrunnlag")),
            beregningsgrunnlag = objectMapper.readValue<BeregningsgrunnlagDto>(it.string("beregningsgrunnlag"))
        )
    }
}
