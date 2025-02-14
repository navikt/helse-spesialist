package no.nav.helse.db

import com.fasterxml.jackson.module.kotlin.readValue
import kotliquery.Session
import no.nav.helse.db.HelseDao.Companion.asSQL
import no.nav.helse.db.HelseDao.Companion.list
import no.nav.helse.db.HelseDao.Companion.update
import no.nav.helse.db.HelseDao.Companion.updateAndReturnGeneratedKey
import no.nav.helse.modell.vilkårsprøving.Avviksvurdering
import no.nav.helse.modell.vilkårsprøving.Beregningsgrunnlag
import no.nav.helse.modell.vilkårsprøving.Sammenligningsgrunnlag
import no.nav.helse.spesialist.db.objectMapper
import java.util.UUID

class PgAvviksvurderingRepository(private val session: Session) : AvviksvurderingRepository {
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
            ).updateAndReturnGeneratedKey(session)

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
        ).update(session)

        if (avviksvurdering.vilkårsgrunnlagId != null) {
            asSQL(
                """
                INSERT INTO vilkarsgrunnlag_per_avviksvurdering (avviksvurdering_ref, vilkårsgrunnlag_id)
                VALUES (:unikId, :vilkaarsgrunnlagId) ON CONFLICT DO NOTHING;
                """.trimIndent(),
                "unikId" to avviksvurdering.unikId,
                "vilkaarsgrunnlagId" to avviksvurdering.vilkårsgrunnlagId,
            ).update(session)
        }
    }

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
        ).list(session) {
            Avviksvurdering(
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
        ).update(session)
    }
}
