package no.nav.helse.spesialist.db.repository

import kotliquery.Row
import kotliquery.Session
import no.nav.helse.spesialist.application.SaksbehandlerStansRepository
import no.nav.helse.spesialist.db.HelseDao.Companion.asSQL
import no.nav.helse.spesialist.db.MedSession
import no.nav.helse.spesialist.db.QueryRunner
import no.nav.helse.spesialist.domain.Identitetsnummer
import no.nav.helse.spesialist.domain.NAVIdent
import no.nav.helse.spesialist.domain.saksbehandlerstans.SaksbehandlerStans
import no.nav.helse.spesialist.domain.saksbehandlerstans.SaksbehandlerStansId

class PgSaksbehandlerStansRepository(
    session: Session,
) : QueryRunner by MedSession(session),
    SaksbehandlerStansRepository {
    override fun lagre(saksbehandlerStans: SaksbehandlerStans) {
        asSQL(
            """
            INSERT INTO saksbehandler_stans (
                id,
                identitetsnummer,
                utfort_av_ident,
                begrunnelse,
                opprettet,
                opphevet_av_ident,
                opphevet_begrunnelse,
                opphevet_tidspunkt
            )
            VALUES (
                :id,
                :identitetsnummer,
                :utfort_av_ident,
                :begrunnelse,
                :opprettet,
                :opphevet_av_ident,
                :opphevet_begrunnelse,
                :opphevet_tidspunkt
            )
            ON CONFLICT (id) DO UPDATE SET
                opphevet_av_ident = EXCLUDED.opphevet_av_ident,
                opphevet_begrunnelse = EXCLUDED.opphevet_begrunnelse,
                opphevet_tidspunkt = EXCLUDED.opphevet_tidspunkt
            """.trimIndent(),
            "id" to saksbehandlerStans.id.value,
            "identitetsnummer" to saksbehandlerStans.identitetsnummer.value,
            "utfort_av_ident" to saksbehandlerStans.utførtAv.value,
            "begrunnelse" to saksbehandlerStans.begrunnelse,
            "opprettet" to saksbehandlerStans.opprettet,
            "opphevet_av_ident" to saksbehandlerStans.stansOpphevet?.utførtAv?.value,
            "opphevet_begrunnelse" to saksbehandlerStans.stansOpphevet?.begrunnelse,
            "opphevet_tidspunkt" to saksbehandlerStans.stansOpphevet?.tidspunkt,
        ).update()
    }

    override fun finnAlle(identitetsnummer: Identitetsnummer): List<SaksbehandlerStans> =
        asSQL(
            """
            SELECT * FROM saksbehandler_stans
            WHERE identitetsnummer = :identitetsnummer
            ORDER BY opprettet DESC
            """.trimIndent(),
            "identitetsnummer" to identitetsnummer.value,
        ).list { it.tilSaksbehandlerStans() }

    override fun finnAktiv(identitetsnummer: Identitetsnummer): SaksbehandlerStans? =
        asSQL(
            """
            SELECT * FROM saksbehandler_stans
            WHERE identitetsnummer = :identitetsnummer
            AND opphevet_tidspunkt IS NULL
            ORDER BY opprettet DESC
            LIMIT 1
            """.trimIndent(),
            "identitetsnummer" to identitetsnummer.value,
        ).singleOrNull { it.tilSaksbehandlerStans() }

    private fun Row.tilSaksbehandlerStans(): SaksbehandlerStans {
        val opphevetAvIdent = stringOrNull("opphevet_av_ident")
        val opphevetBegrunnelse = stringOrNull("opphevet_begrunnelse")
        val opphevetTidspunkt = instantOrNull("opphevet_tidspunkt")

        val stansOpphevet =
            if (opphevetAvIdent != null && opphevetBegrunnelse != null && opphevetTidspunkt != null) {
                SaksbehandlerStans.StansOpphevet(
                    utførtAv = NAVIdent(opphevetAvIdent),
                    begrunnelse = opphevetBegrunnelse,
                    tidspunkt = opphevetTidspunkt,
                )
            } else {
                null
            }

        return SaksbehandlerStans.fraLagring(
            id = SaksbehandlerStansId(uuid("id")),
            identitetsnummer = Identitetsnummer.fraString(string("identitetsnummer")),
            utførtAv = NAVIdent(string("utfort_av_ident")),
            begrunnelse = string("begrunnelse"),
            opprettet = instant("opprettet"),
            stansOpphevet = stansOpphevet,
        )
    }
}
