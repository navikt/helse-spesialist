package no.nav.helse.spesialist.db.repository

import kotliquery.Session
import no.nav.helse.db.overstyring.venting.MeldingId
import no.nav.helse.db.overstyring.venting.VenterPåKvitteringForOverstyring
import no.nav.helse.db.overstyring.venting.VenterPåKvitteringForOverstyringRepository
import no.nav.helse.spesialist.db.HelseDao.Companion.asSQL
import no.nav.helse.spesialist.db.MedSession
import no.nav.helse.spesialist.db.QueryRunner
import no.nav.helse.spesialist.domain.Identitetsnummer

class PgVenterPåKvitteringForOverstyringRepository(
    session: Session,
) : QueryRunner by MedSession(session),
    VenterPåKvitteringForOverstyringRepository {
    override fun lagre(venterPåKvitteringForOverstyring: VenterPåKvitteringForOverstyring) {
        asSQL(
            """
            INSERT INTO venter_paa_kvittering_for_overstyring (
                melding_id,
                identitetsnummer
            )
            VALUES (
                :meldingId,
                :identitetsnummer
            )
            """.trimIndent(),
            "meldingId" to venterPåKvitteringForOverstyring.id.value,
            "identitetsnummer" to venterPåKvitteringForOverstyring.identitetsnummer.value,
        ).update()
    }

    override fun finn(meldingId: MeldingId): VenterPåKvitteringForOverstyring? =
        asSQL(
            """
            SELECT * FROM venter_paa_kvittering_for_overstyring 
            WHERE melding_id = :meldingId
            """.trimIndent(),
            "meldingId" to meldingId.value,
        ).singleOrNull {
            VenterPåKvitteringForOverstyring.fraLagring(
                meldingId = MeldingId(it.uuid("melding_id")),
                identitetsnummer = Identitetsnummer.fraString(it.string("identitetsnummer")),
            )
        }

    override fun slett(meldingId: MeldingId) {
        asSQL(
            "DELETE FROM venter_paa_kvittering_for_overstyring WHERE melding_id = :meldingId",
            "meldingId" to meldingId.value,
        ).update()
    }
}
