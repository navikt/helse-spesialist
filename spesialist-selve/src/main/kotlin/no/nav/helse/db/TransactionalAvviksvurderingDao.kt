package no.nav.helse.db

import com.fasterxml.jackson.module.kotlin.readValue
import kotliquery.Session
import kotliquery.queryOf
import no.nav.helse.HelseDao.Companion.asSQL
import no.nav.helse.HelseDao.Companion.list
import no.nav.helse.modell.vilkårsprøving.AvviksvurderingDto
import no.nav.helse.modell.vilkårsprøving.BeregningsgrunnlagDto
import no.nav.helse.modell.vilkårsprøving.SammenligningsgrunnlagDto
import no.nav.helse.objectMapper
import org.intellij.lang.annotations.Language
import java.util.UUID

internal class TransactionalAvviksvurderingDao(private val session: Session) : AvviksvurderingRepository {
    override fun opprettKobling(
        avviksvurderingId: UUID,
        vilkårsgrunnlagId: UUID,
    ) {
        @Language("PostgreSQL")
        val statement =
            """
            INSERT INTO vilkarsgrunnlag_per_avviksvurdering(avviksvurdering_ref, vilkårsgrunnlag_id)
            VALUES (:unik_id, :vilkarsgrunnlag_id) ON CONFLICT DO NOTHING;
            """.trimIndent()
        session.run(
            queryOf(statement, mapOf("unik_id" to avviksvurderingId, "vilkarsgrunnlag_id" to vilkårsgrunnlagId)).asUpdate,
        )
    }

    override fun finnAvviksvurderinger(fødselsnummer: String): List<AvviksvurderingDto> =
        asSQL(
            """
            SELECT av.unik_id, vpa.vilkårsgrunnlag_id, av.fødselsnummer, av.skjæringstidspunkt, av.opprettet, avviksprosent, beregningsgrunnlag, sg.sammenligningsgrunnlag FROM avviksvurdering av 
            INNER JOIN sammenligningsgrunnlag sg ON av.sammenligningsgrunnlag_ref = sg.id
            INNER JOIN vilkarsgrunnlag_per_avviksvurdering vpa ON vpa.avviksvurdering_ref = av.unik_id
            WHERE av.fødselsnummer = :fodselsnummer
            AND av.slettet IS NULL;
            """.trimIndent(),
            "fodselsnummer" to fødselsnummer,
        ).list(session) {
            AvviksvurderingDto(
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
}
