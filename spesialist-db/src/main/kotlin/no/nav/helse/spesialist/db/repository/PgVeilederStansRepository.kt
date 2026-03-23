package no.nav.helse.spesialist.db.repository

import kotliquery.Row
import kotliquery.Session
import no.nav.helse.spesialist.application.VeilederStansRepository
import no.nav.helse.spesialist.db.HelseDao.Companion.asSQL
import no.nav.helse.spesialist.db.MedSession
import no.nav.helse.spesialist.db.QueryRunner
import no.nav.helse.spesialist.domain.Identitetsnummer
import no.nav.helse.spesialist.domain.NAVIdent
import no.nav.helse.spesialist.domain.VeilederStans
import no.nav.helse.spesialist.domain.VeilederStans.StansÅrsak
import no.nav.helse.spesialist.domain.VeilederStansId

class PgVeilederStansRepository(
    session: Session,
) : QueryRunner by MedSession(session),
    VeilederStansRepository {
    override fun lagre(veilederStans: VeilederStans) {
        asSQL(
            """
            INSERT INTO veileder_stans (
                id,
                identitetsnummer,
                arsaker,
                opprettet,
                original_melding_id,
                opphevet_av_saksbehandler_ident,
                opphevet_begrunnelse,
                opphevet_tidspunkt
            )
            VALUES (
                :id,
                :identitetsnummer,
                :arsaker,
                :opprettet,
                :original_melding_id,
                :opphevet_av_saksbehandler_ident,
                :opphevet_begrunnelse,
                :opphevet_tidspunkt
            )
            ON CONFLICT (id) DO UPDATE SET
                opphevet_av_saksbehandler_ident = EXCLUDED.opphevet_av_saksbehandler_ident,
                opphevet_begrunnelse = EXCLUDED.opphevet_begrunnelse,
                opphevet_tidspunkt = EXCLUDED.opphevet_tidspunkt
            """.trimIndent(),
            "id" to veilederStans.id.value,
            "identitetsnummer" to veilederStans.identitetsnummer.value,
            "arsaker" to veilederStans.årsaker.map { it.name }.toTypedArray(),
            "opprettet" to veilederStans.opprettet,
            "original_melding_id" to veilederStans.originalMeldingId,
            "opphevet_av_saksbehandler_ident" to veilederStans.stansOpphevet?.opphevetAvSaksbehandlerIdent?.value,
            "opphevet_begrunnelse" to veilederStans.stansOpphevet?.begrunnelse,
            "opphevet_tidspunkt" to veilederStans.stansOpphevet?.opphevetTidspunkt,
        ).update()
    }

    override fun finnAlle(identitetsnummer: Identitetsnummer): List<VeilederStans> =
        asSQL(
            """
            SELECT * FROM veileder_stans 
            WHERE identitetsnummer = :identitetsnummer
            ORDER BY opprettet DESC
            """.trimIndent(),
            "identitetsnummer" to identitetsnummer.value,
        ).list { it.tilVeilederStans() }

    override fun finnAktiv(identitetsnummer: Identitetsnummer): VeilederStans? =
        asSQL(
            """
            SELECT * FROM veileder_stans 
            WHERE identitetsnummer = :identitetsnummer
            AND opphevet_tidspunkt IS NULL
            ORDER BY opprettet DESC
            LIMIT 1
            """.trimIndent(),
            "identitetsnummer" to identitetsnummer.value,
        ).singleOrNull { it.tilVeilederStans() }

    private fun Row.tilVeilederStans(): VeilederStans {
        val opphevetAvSaksbehandlerIdent = stringOrNull("opphevet_av_saksbehandler_ident")
        val opphevetBegrunnelse = stringOrNull("opphevet_begrunnelse")
        val opphevetTidspunkt = instantOrNull("opphevet_tidspunkt")

        val stansOpphevet =
            if (opphevetAvSaksbehandlerIdent != null && opphevetBegrunnelse != null && opphevetTidspunkt != null) {
                VeilederStans.StansOpphevet(
                    opphevetAvSaksbehandlerIdent = NAVIdent(opphevetAvSaksbehandlerIdent),
                    begrunnelse = opphevetBegrunnelse,
                    opphevetTidspunkt = opphevetTidspunkt,
                )
            } else {
                null
            }

        return VeilederStans.fraLagring(
            id = VeilederStansId(uuid("id")),
            identitetsnummer = Identitetsnummer.fraString(string("identitetsnummer")),
            årsaker = array<String>("arsaker").map { StansÅrsak.valueOf(it) }.toSet(),
            opprettet = instant("opprettet"),
            originalMeldingId = uuid("original_melding_id"),
            stansOpphevet = stansOpphevet,
        )
    }
}
