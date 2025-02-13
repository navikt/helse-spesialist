package no.nav.helse.spesialist.db

import kotliquery.Row
import kotliquery.Session
import no.nav.helse.db.HelseDao.Companion.asSQL
import no.nav.helse.db.MedSession
import no.nav.helse.db.PgSaksbehandlerDao
import no.nav.helse.db.QueryRunner
import no.nav.helse.modell.saksbehandler.Tilgangskontroll
import no.nav.helse.modell.totrinnsvurdering.Totrinnsvurdering
import no.nav.helse.modell.totrinnsvurdering.TotrinnsvurderingId
import no.nav.helse.spesialist.application.TotrinnsvurderingRepository
import java.util.UUID

class PgTotrinnsvurderingRepository(
    session: Session,
    tilgangskontroll: Tilgangskontroll,
) : QueryRunner by MedSession(session), TotrinnsvurderingRepository {
    private val overstyringRepository = PgOverstyringRepository(session)
    private val saksbehandlerDao = PgSaksbehandlerDao(session, tilgangskontroll)

    override fun finn(fødselsnummer: String): Totrinnsvurdering? {
        return finnAktivTotrinnsvurdering(fødselsnummer)
    }

    @Deprecated("Skal fjernes, midlertidig i bruk for å tette et hull")
    override fun finn(vedtaksperiodeId: UUID): Totrinnsvurdering? {
        return finnAktivTotrinnsvurdering(vedtaksperiodeId)
    }

    override fun lagre(
        totrinnsvurdering: Totrinnsvurdering,
        fødselsnummer: String,
    ) {
        if (totrinnsvurdering.harFåttTildeltId()) {
            update(totrinnsvurdering)
        } else {
            insert(totrinnsvurdering).let(::TotrinnsvurderingId).let(totrinnsvurdering::tildelId)
        }

        overstyringRepository.lagre(totrinnsvurdering.overstyringer)
    }

    private fun finnAktivTotrinnsvurdering(fødselsnummer: String): Totrinnsvurdering? =
        asSQL(
            """
            SELECT tv.id,
                   v.vedtaksperiode_id,
                   er_retur,
                   tv.saksbehandler as saksbehandler_oid,
                   tv.beslutter as beslutter_oid,
                   ui.id as utbetaling_id,
                   tv.opprettet,
                   tv.oppdatert,
                   p.fødselsnummer
            FROM totrinnsvurdering tv
                     INNER JOIN vedtak v on tv.vedtaksperiode_id = v.vedtaksperiode_id
                     INNER JOIN person p on v.person_ref = p.id
                     LEFT JOIN utbetaling_id ui on ui.id = tv.utbetaling_id_ref
            WHERE p.fødselsnummer = :fodselsnummer
              AND utbetaling_id_ref IS NULL
            """.trimIndent(),
            "fodselsnummer" to fødselsnummer,
        ).singleOrNull { it.toTotrinnsvurdering() }

    @Deprecated("Skal fjernes, midlertidig i bruk for å tette et hull")
    private fun finnAktivTotrinnsvurdering(vedtaksperiodeId: UUID): Totrinnsvurdering? =
        asSQL(
            """
            SELECT DISTINCT ON (tv.id)
                   v.vedtaksperiode_id,
                   tv.id,
                   er_retur,
                   tv.saksbehandler as saksbehandler_oid,
                   tv.beslutter as beslutter_oid,
                   ui.id as utbetaling_id,
                   tv.opprettet,
                   tv.oppdatert,
                   p.fødselsnummer
            FROM totrinnsvurdering tv
                     INNER JOIN vedtak v on tv.vedtaksperiode_id = v.vedtaksperiode_id
                     INNER JOIN person p on v.person_ref = p.id
                     INNER JOIN oppgave o on v.id = o.vedtak_ref
                     LEFT JOIN utbetaling_id ui on ui.id = tv.utbetaling_id_ref
            WHERE v.vedtaksperiode_id = :vedtaksperiodeId
              AND utbetaling_id_ref IS NULL
            """.trimIndent(),
            "vedtaksperiodeId" to vedtaksperiodeId,
        ).singleOrNull { it.toTotrinnsvurdering() }

    private fun insert(totrinnsvurdering: Totrinnsvurdering): Long =
        asSQL(
            """
            INSERT INTO totrinnsvurdering (vedtaksperiode_id, er_retur, saksbehandler, beslutter, opprettet, oppdatert)
            VALUES (:vedtaksperiodeId, :erRetur, :saksbehandler, :beslutter, :opprettet, null)
            """.trimIndent(),
            "vedtaksperiodeId" to totrinnsvurdering.vedtaksperiodeId,
            "erRetur" to totrinnsvurdering.erRetur,
            "saksbehandler" to totrinnsvurdering.saksbehandler?.oid,
            "beslutter" to totrinnsvurdering.beslutter?.oid,
            "opprettet" to totrinnsvurdering.opprettet,
        ).updateAndReturnGeneratedKey()

    private fun update(totrinnsvurdering: Totrinnsvurdering) {
        asSQL(
            """
            UPDATE totrinnsvurdering 
            SET er_retur            = :erRetur,
                saksbehandler       = :saksbehandler,
                beslutter           = :beslutter,
                utbetaling_id_ref   = (SELECT id from utbetaling_id ui WHERE ui.utbetaling_id = :utbetalingId),
                oppdatert           = :oppdatert
            WHERE id = :id
            """.trimIndent(),
            "id" to totrinnsvurdering.id().value,
            "erRetur" to totrinnsvurdering.erRetur,
            "saksbehandler" to totrinnsvurdering.saksbehandler?.oid,
            "beslutter" to totrinnsvurdering.beslutter?.oid,
            "utbetalingId" to totrinnsvurdering.utbetalingId,
            "oppdatert" to totrinnsvurdering.oppdatert,
        ).update()
    }

    private fun Row.toTotrinnsvurdering(): Totrinnsvurdering =
        Totrinnsvurdering.fraLagring(
            id = TotrinnsvurderingId(long("id")),
            vedtaksperiodeId = uuid("vedtaksperiode_id"),
            erRetur = boolean("er_retur"),
            saksbehandler = uuidOrNull("saksbehandler_oid")?.let { saksbehandlerDao.finnSaksbehandler(it) },
            beslutter = uuidOrNull("beslutter_oid")?.let { saksbehandlerDao.finnSaksbehandler(it) },
            utbetalingId = uuidOrNull("utbetaling_id"),
            opprettet = localDateTime("opprettet"),
            oppdatert = localDateTimeOrNull("oppdatert"),
            ferdigstilt = false,
            overstyringer = overstyringRepository.finn(string("fødselsnummer")),
        )
}
