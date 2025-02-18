package no.nav.helse.db

import com.fasterxml.jackson.module.kotlin.readValue
import kotliquery.Session
import no.nav.helse.db.HelseDao.Companion.asSQL
import no.nav.helse.modell.vilkårsprøving.AvviksvurderingDto
import no.nav.helse.modell.vilkårsprøving.BeregningsgrunnlagDto
import no.nav.helse.modell.vilkårsprøving.SammenligningsgrunnlagDto
import no.nav.helse.spesialist.api.Avviksvurderinghenter
import no.nav.helse.spesialist.api.avviksvurdering.Avviksvurdering
import no.nav.helse.spesialist.api.avviksvurdering.Beregningsgrunnlag
import no.nav.helse.spesialist.api.avviksvurdering.Sammenligningsgrunnlag
import no.nav.helse.spesialist.db.objectMapper
import java.util.UUID
import javax.sql.DataSource
import no.nav.helse.spesialist.api.avviksvurdering.Avviksvurdering as ApiAvviksvurdering

class PgAvviksvurderingDao private constructor(private val queryRunner: QueryRunner) : AvviksvurderingDao, Avviksvurderinghenter, QueryRunner by queryRunner {
    internal constructor(session: Session) : this(MedSession(session))
    internal constructor(dataSource: DataSource) : this(MedDataSource(dataSource))

    override fun finnAvviksvurderinger(fødselsnummer: String): List<AvviksvurderingDto> =
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
        ).list {
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
        ).singleOrNull {
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
}
