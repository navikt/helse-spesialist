package no.nav.helse.spesialist.db.repository

import kotliquery.Session
import no.nav.helse.db.IndividuellBegrunnelseRepository
import no.nav.helse.modell.vedtak.Utfall
import no.nav.helse.spesialist.db.DbQuery
import no.nav.helse.spesialist.db.SessionDbQuery
import no.nav.helse.spesialist.domain.IndividuellBegrunnelse
import no.nav.helse.spesialist.domain.SaksbehandlerOid
import no.nav.helse.spesialist.domain.SpleisBehandlingId
import no.nav.helse.spesialist.domain.VedtakBegrunnelseId
import java.time.LocalDateTime

class PgIndividuellBegrunnelseRepository private constructor(
    private val dbQuery: DbQuery,
) : IndividuellBegrunnelseRepository {
    internal constructor(session: Session) : this(SessionDbQuery(session))

    override fun finn(spleisBehandlingId: SpleisBehandlingId): IndividuellBegrunnelse? =
        dbQuery.singleOrNull(
            """
            SELECT b.id, b.type, b.tekst, b.saksbehandler_ref, vb.invalidert FROM begrunnelse b 
            JOIN vedtak_begrunnelse vb ON b.id = vb.begrunnelse_ref 
            JOIN behandling beh ON beh.id = vb.behandling_ref
            WHERE beh.spleis_behandling_id = :spleisBehandlingId
                AND vb.invalidert = false
            ORDER BY vb.opprettet DESC LIMIT 1
            """.trimIndent(),
            "spleisBehandlingId" to spleisBehandlingId.value,
        ) { row ->
            IndividuellBegrunnelse.fraLagring(
                id = VedtakBegrunnelseId(row.long("id")),
                spleisBehandlingId = spleisBehandlingId,
                tekst = row.string("tekst"),
                utfall = enumValueOf<Utfall>(row.string("type")),
                invalidert = row.boolean("invalidert"),
                saksbehandlerOid = SaksbehandlerOid(row.uuid("saksbehandler_ref")),
            )
        }

    override fun lagre(individuellBegrunnelse: IndividuellBegrunnelse) {
        if (individuellBegrunnelse.harFåttTildeltId()) {
            oppdater(individuellBegrunnelse)
        } else {
            lagreNy(individuellBegrunnelse)
        }
    }

    private fun lagreNy(individuellBegrunnelse: IndividuellBegrunnelse) {
        val begrunnelseId =
            dbQuery.updateAndReturnGeneratedKey(
                """
                INSERT INTO begrunnelse(type, tekst, saksbehandler_ref) VALUES (:type, :tekst, :saksbehandlerOid)
                """.trimIndent(),
                "type" to individuellBegrunnelse.utfall.name,
                "tekst" to individuellBegrunnelse.tekst,
                "saksbehandlerOid" to individuellBegrunnelse.saksbehandlerOid.value,
            ) ?: error("Kunne ikke lagre begrunnelse")
        dbQuery.update(
            """
            INSERT INTO vedtak_begrunnelse (vedtaksperiode_id, begrunnelse_ref, behandling_ref, opprettet, invalidert) 
            SELECT beh.vedtaksperiode_id, :begrunnelseId, beh.id, :opprettet, false
            FROM behandling beh
            WHERE beh.spleis_behandling_id = :spleisBehandlingId
            """.trimIndent(),
            "begrunnelseId" to begrunnelseId,
            "opprettet" to LocalDateTime.now(),
            "spleisBehandlingId" to individuellBegrunnelse.spleisBehandlingId.value,
        )
        individuellBegrunnelse.tildelId(VedtakBegrunnelseId(begrunnelseId))
    }

    private fun oppdater(individuellBegrunnelse: IndividuellBegrunnelse) {
        dbQuery.update(
            """
            UPDATE vedtak_begrunnelse SET invalidert = :invalidert WHERE begrunnelse_ref = :begrunnelse_ref
            """.trimIndent(),
            "invalidert" to individuellBegrunnelse.invalidert,
            "begrunnelse_ref" to individuellBegrunnelse.id().value,
        )
    }
}
