package no.nav.helse.spesialist.db.dao

import com.fasterxml.jackson.module.kotlin.readValue
import kotliquery.Row
import kotliquery.Session
import no.nav.helse.db.AvviksvurderingRepository
import no.nav.helse.db.HelseDao.Companion.asSQL
import no.nav.helse.db.MedSession
import no.nav.helse.db.QueryRunner
import no.nav.helse.modell.vilkårsprøving.Avviksvurdering
import no.nav.helse.modell.vilkårsprøving.Beregningsgrunnlag
import no.nav.helse.modell.vilkårsprøving.Sammenligningsgrunnlag
import no.nav.helse.spesialist.db.objectMapper
import java.util.UUID

class PgAvviksvurderingRepository(session: Session) : AvviksvurderingRepository, QueryRunner by MedSession(session) {
    override fun lagre(avviksvurdering: Avviksvurdering) {
        val sammenligningsgrunnlagRef =
            asSQL(
                """
                INSERT INTO sammenligningsgrunnlag (fødselsnummer, skjæringstidspunkt, opprettet, sammenligningsgrunnlag)
                VALUES (:foedselsnummer, :skjaeringstidspunkt, :opprettet, CAST(:sammenligningsgrunnlag as json));
                """.trimIndent(),
                "foedselsnummer" to avviksvurdering.fødselsnummer,
                "skjaeringstidspunkt" to avviksvurdering.skjæringstidspunkt,
                "opprettet" to avviksvurdering.opprettet,
                "sammenligningsgrunnlag" to objectMapper.writeValueAsString(avviksvurdering.sammenligningsgrunnlag),
            ).updateAndReturnGeneratedKey()

        asSQL(
            """
            INSERT INTO avviksvurdering (unik_id, fødselsnummer, skjæringstidspunkt, opprettet, avviksprosent, beregningsgrunnlag, sammenligningsgrunnlag_ref)
            VALUES (:unikId, :foedselsnummer, :skjaeringstidspunkt, :opprettet, :avviksprosent, CAST(:beregningsgrunnlag as json), :sammenligningsgrunnlagRef)
            ON CONFLICT (unik_id) DO UPDATE SET opprettet = excluded.opprettet, avviksprosent = excluded.avviksprosent, beregningsgrunnlag = excluded.beregningsgrunnlag, sammenligningsgrunnlag_ref = excluded.sammenligningsgrunnlag_ref;
            """.trimIndent(),
            "unikId" to avviksvurdering.unikId,
            "foedselsnummer" to avviksvurdering.fødselsnummer,
            "skjaeringstidspunkt" to avviksvurdering.skjæringstidspunkt,
            "opprettet" to avviksvurdering.opprettet,
            "avviksprosent" to avviksvurdering.avviksprosent,
            "beregningsgrunnlag" to objectMapper.writeValueAsString(avviksvurdering.beregningsgrunnlag),
            "sammenligningsgrunnlagRef" to sammenligningsgrunnlagRef,
        ).update()

        val vilkårsgrunnlagId = avviksvurdering.vilkårsgrunnlagId
        if (vilkårsgrunnlagId != null) {
            opprettKobling(avviksvurdering.unikId, vilkårsgrunnlagId)
        }
    }

    override fun hentAvviksvurdering(vilkårsgrunnlagId: UUID): Avviksvurdering? =
        asSQL(
            """
            SELECT av.unik_id, vpa.vilkårsgrunnlag_id, av.fødselsnummer, av.skjæringstidspunkt, av.opprettet, avviksprosent, beregningsgrunnlag, sg.sammenligningsgrunnlag 
            FROM avviksvurdering av 
            INNER JOIN sammenligningsgrunnlag sg ON av.sammenligningsgrunnlag_ref = sg.id
            INNER JOIN vilkarsgrunnlag_per_avviksvurdering vpa ON vpa.avviksvurdering_ref = av.unik_id
            WHERE vpa.vilkårsgrunnlag_id = :vilkaarsgrunnlagId AND av.slettet IS NULL
            ORDER BY av.opprettet DESC
            LIMIT 1;
            """.trimIndent(),
            "vilkaarsgrunnlagId" to vilkårsgrunnlagId,
        ).singleOrNull { it.avviksvurdering() }

    override fun finnAvviksvurderinger(fødselsnummer: String): List<Avviksvurdering> =
        asSQL(
            """
            SELECT av.unik_id, vpa.vilkårsgrunnlag_id, av.fødselsnummer, av.skjæringstidspunkt, av.opprettet, avviksprosent, beregningsgrunnlag, sg.sammenligningsgrunnlag
            FROM avviksvurdering av 
            INNER JOIN sammenligningsgrunnlag sg ON av.sammenligningsgrunnlag_ref = sg.id
            INNER JOIN vilkarsgrunnlag_per_avviksvurdering vpa ON vpa.avviksvurdering_ref = av.unik_id
            WHERE av.fødselsnummer = :foedselsnummer
            AND av.slettet IS NULL;
            """.trimIndent(),
            "foedselsnummer" to fødselsnummer,
        ).list { it.avviksvurdering() }

    override fun opprettKobling(
        avviksvurderingId: UUID,
        vilkårsgrunnlagId: UUID,
    ) {
        asSQL(
            """
            INSERT INTO vilkarsgrunnlag_per_avviksvurdering (avviksvurdering_ref, vilkårsgrunnlag_id)
            VALUES (:avviksvurderingId, :vilkaarsgrunnlagId) ON CONFLICT DO NOTHING;
            """.trimIndent(),
            "avviksvurderingId" to avviksvurderingId,
            "vilkaarsgrunnlagId" to vilkårsgrunnlagId,
        ).update()
    }

    private fun Row.avviksvurdering() =
        Avviksvurdering(
            unikId = uuid("unik_id"),
            vilkårsgrunnlagId = uuid("vilkårsgrunnlag_id"),
            fødselsnummer = string("fødselsnummer"),
            skjæringstidspunkt = localDate("skjæringstidspunkt"),
            opprettet = localDateTime("opprettet"),
            avviksprosent = double("avviksprosent"),
            sammenligningsgrunnlag = objectMapper.readValue<Sammenligningsgrunnlag>(string("sammenligningsgrunnlag")),
            beregningsgrunnlag = objectMapper.readValue<Beregningsgrunnlag>(string("beregningsgrunnlag")),
        )
}
