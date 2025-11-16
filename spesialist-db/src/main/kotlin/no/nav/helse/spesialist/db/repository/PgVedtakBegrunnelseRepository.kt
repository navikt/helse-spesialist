package no.nav.helse.spesialist.db.repository

import kotliquery.Session
import no.nav.helse.db.VedtakBegrunnelseRepository
import no.nav.helse.modell.vedtak.Utfall
import no.nav.helse.spesialist.db.DbQuery
import no.nav.helse.spesialist.db.SessionDbQuery
import no.nav.helse.spesialist.domain.SaksbehandlerOid
import no.nav.helse.spesialist.domain.SpleisBehandlingId
import no.nav.helse.spesialist.domain.VedtakBegrunnelse
import no.nav.helse.spesialist.domain.VedtakBegrunnelseId
import java.time.LocalDateTime

class PgVedtakBegrunnelseRepository private constructor(
    private val dbQuery: DbQuery,
) : VedtakBegrunnelseRepository {
    internal constructor(session: Session) : this(SessionDbQuery(session))

    override fun finn(spleisBehandlingId: SpleisBehandlingId): VedtakBegrunnelse? =
        dbQuery.singleOrNull(
            """
            SELECT b.id, b.type, b.tekst, b.saksbehandler_ref, vb.invalidert FROM begrunnelse b 
            JOIN vedtak_begrunnelse vb ON b.id = vb.begrunnelse_ref 
            JOIN behandling beh ON beh.id = vb.generasjon_ref
            WHERE beh.spleis_behandling_id = :spleisBehandlingId
                AND vb.invalidert = false
            ORDER BY vb.opprettet DESC LIMIT 1
            """.trimIndent(),
            "spleisBehandlingId" to spleisBehandlingId.value,
        ) { row ->
            VedtakBegrunnelse.fraLagring(
                id = VedtakBegrunnelseId(row.long("id")),
                spleisBehandlingId = spleisBehandlingId,
                tekst = row.string("tekst"),
                utfall = enumValueOf<Utfall>(row.string("type")),
                invalidert = row.boolean("invalidert"),
                saksbehandlerOid = SaksbehandlerOid(row.uuid("saksbehandler_ref")),
            )
        }

    override fun lagre(vedtakBegrunnelse: VedtakBegrunnelse) {
        if (vedtakBegrunnelse.harFåttTildeltId()) {
            oppdater(vedtakBegrunnelse)
        } else {
            lagreNy(vedtakBegrunnelse)
        }
    }

    private fun lagreNy(vedtakBegrunnelse: VedtakBegrunnelse) {
        val begrunnelseId =
            dbQuery.updateAndReturnGeneratedKey(
                """
                INSERT INTO begrunnelse(type, tekst, saksbehandler_ref) VALUES (:type, :tekst, :saksbehandlerOid)
                """.trimIndent(),
                "type" to vedtakBegrunnelse.utfall.name,
                "tekst" to vedtakBegrunnelse.tekst,
                "saksbehandlerOid" to vedtakBegrunnelse.saksbehandlerOid.value,
            ) ?: error("Kunne ikke lagre begrunnelse")
        dbQuery.update(
            """
            INSERT INTO vedtak_begrunnelse (vedtaksperiode_id, begrunnelse_ref, generasjon_ref, opprettet, invalidert) 
            SELECT beh.vedtaksperiode_id, :begrunnelseId, beh.id, :opprettet, false
            FROM behandling beh
            WHERE beh.spleis_behandling_id = :spleisBehandlingId
            """.trimIndent(),
            "begrunnelseId" to begrunnelseId,
            "opprettet" to LocalDateTime.now(),
            "spleisBehandlingId" to vedtakBegrunnelse.spleisBehandlingId.value,
        )
        vedtakBegrunnelse.tildelId(VedtakBegrunnelseId(begrunnelseId))
    }

    private fun oppdater(vedtakBegrunnelse: VedtakBegrunnelse) {
        dbQuery.update(
            """
            UPDATE vedtak_begrunnelse SET invalidert = :invalidert WHERE begrunnelse_ref = :begrunnelse_ref
            """.trimIndent(),
            "invalidert" to vedtakBegrunnelse.invalidert,
            "begrunnelse_ref" to vedtakBegrunnelse.id?.value,
        )
    }
}
