package no.nav.helse.spesialist.db.repository

import kotliquery.Row
import kotliquery.Session
import no.nav.helse.modell.totrinnsvurdering.Totrinnsvurdering
import no.nav.helse.modell.totrinnsvurdering.TotrinnsvurderingId
import no.nav.helse.spesialist.application.TotrinnsvurderingRepository
import no.nav.helse.spesialist.db.HelseDao.Companion.asSQL
import no.nav.helse.spesialist.db.MedSession
import no.nav.helse.spesialist.db.QueryRunner
import no.nav.helse.spesialist.domain.SaksbehandlerOid

class PgTotrinnsvurderingRepository(
    session: Session,
) : QueryRunner by MedSession(session),
    TotrinnsvurderingRepository {
    private val overstyringRepository = PgOverstyringRepository(session)

    override fun finn(id: TotrinnsvurderingId): Totrinnsvurdering? =
        asSQL(
            """
            SELECT tv.id,
                   p.fødselsnummer,
                   tv.saksbehandler as saksbehandler_oid,
                   tv.beslutter as beslutter_oid,
                   ui.utbetaling_id,
                   tv.tilstand,
                   tv.vedtaksperiode_forkastet,
                   tv.opprettet,
                   tv.oppdatert
            FROM totrinnsvurdering tv
                     INNER JOIN person p on tv.person_ref = p.id
                     LEFT JOIN utbetaling_id ui on ui.id = tv.utbetaling_id_ref
            WHERE tv.id = :id
            """.trimIndent(),
            "id" to id.value,
        ).singleOrNull { it.toTotrinnsvurdering() }

    override fun finnAktivForPerson(fødselsnummer: String): Totrinnsvurdering? =
        asSQL(
            """
            SELECT tv.id,
                   p.fødselsnummer,
                   tv.saksbehandler as saksbehandler_oid,
                   tv.beslutter as beslutter_oid,
                   ui.utbetaling_id,
                   tv.tilstand,
                   tv.vedtaksperiode_forkastet,
                   tv.opprettet,
                   tv.oppdatert
            FROM totrinnsvurdering tv
                     INNER JOIN person p on tv.person_ref = p.id
                     LEFT JOIN utbetaling_id ui on ui.id = tv.utbetaling_id_ref
            WHERE p.fødselsnummer = :fodselsnummer
              AND tv.tilstand != 'GODKJENT'
              AND tv.vedtaksperiode_forkastet = false
              order by opprettet desc limit 1
            """.trimIndent(),
            "fodselsnummer" to fødselsnummer,
        ).singleOrNull { it.toTotrinnsvurdering() }

    override fun lagre(totrinnsvurdering: Totrinnsvurdering) {
        if (totrinnsvurdering.harFåttTildeltId()) {
            update(totrinnsvurdering)
        } else {
            insert(totrinnsvurdering).let(::TotrinnsvurderingId).let(totrinnsvurdering::tildelId)
        }

        overstyringRepository.lagre(totrinnsvurdering.overstyringer, totrinnsvurdering.id())
    }

    private fun insert(totrinnsvurdering: Totrinnsvurdering): Long =
        asSQL(
            """
            INSERT INTO totrinnsvurdering (vedtaksperiode_id, saksbehandler, beslutter, person_ref, tilstand, opprettet, oppdatert)
            SELECT :vedtaksperiodeId, :saksbehandler, :beslutter, p.id, CAST(:tilstand AS totrinnsvurdering_tilstand), :opprettet, null
            FROM person p 
            WHERE p.fødselsnummer = :fodselsnummer
            """.trimIndent(),
            "saksbehandler" to totrinnsvurdering.saksbehandler?.value,
            "beslutter" to totrinnsvurdering.beslutter?.value,
            "fodselsnummer" to totrinnsvurdering.fødselsnummer,
            "tilstand" to totrinnsvurdering.tilstand.name,
            "opprettet" to totrinnsvurdering.opprettet,
        ).updateAndReturnGeneratedKey()

    private fun update(totrinnsvurdering: Totrinnsvurdering) {
        asSQL(
            """
            UPDATE totrinnsvurdering 
            SET saksbehandler       = :saksbehandler,
                beslutter           = :beslutter,
                utbetaling_id_ref   = (SELECT id from utbetaling_id ui WHERE ui.utbetaling_id = :utbetalingId),
                tilstand            = CAST(:tilstand AS totrinnsvurdering_tilstand),
                vedtaksperiode_forkastet = :vedtaksperiodeForkastet,
                oppdatert           = :oppdatert
            WHERE id = :id
            """.trimIndent(),
            "id" to totrinnsvurdering.id().value,
            "saksbehandler" to totrinnsvurdering.saksbehandler?.value,
            "beslutter" to totrinnsvurdering.beslutter?.value,
            "utbetalingId" to null,
            "tilstand" to totrinnsvurdering.tilstand.name,
            "vedtaksperiodeForkastet" to totrinnsvurdering.vedtaksperiodeForkastet,
            "oppdatert" to totrinnsvurdering.oppdatert,
        ).update()
    }

    private fun Row.toTotrinnsvurdering(): Totrinnsvurdering =
        Totrinnsvurdering.fraLagring(
            id = TotrinnsvurderingId(long("id")),
            fødselsnummer = string("fødselsnummer"),
            saksbehandler = uuidOrNull("saksbehandler_oid")?.let(::SaksbehandlerOid),
            beslutter = uuidOrNull("beslutter_oid")?.let(::SaksbehandlerOid),
            opprettet = localDateTime("opprettet"),
            oppdatert = localDateTimeOrNull("oppdatert"),
            tilstand = enumValueOf(string("tilstand")),
            vedtaksperiodeForkastet = boolean("vedtaksperiode_forkastet"),
            overstyringer = overstyringRepository.finnAktive(TotrinnsvurderingId(long("id"))),
        )
}
