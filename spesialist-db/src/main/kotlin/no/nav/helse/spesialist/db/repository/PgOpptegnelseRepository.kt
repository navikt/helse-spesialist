package no.nav.helse.spesialist.db.repository

import kotliquery.Row
import kotliquery.Session
import no.nav.helse.spesialist.application.OpptegnelseRepository
import no.nav.helse.spesialist.db.HelseDao.Companion.asSQL
import no.nav.helse.spesialist.db.MedSession
import no.nav.helse.spesialist.db.QueryRunner
import no.nav.helse.spesialist.domain.Identitetsnummer
import no.nav.helse.spesialist.domain.Opptegnelse
import no.nav.helse.spesialist.domain.Sekvensnummer

internal class PgOpptegnelseRepository(
    session: Session,
) : QueryRunner by MedSession(session),
    OpptegnelseRepository {
    override fun lagre(opptegnelse: Opptegnelse) {
        if (!opptegnelse.harFåttTildeltId()) {
            insertOpptegnelse(opptegnelse)
        } else {
            error("Kan ikke oppdatere opptegnelse")
        }
    }

    override fun finnAlleForPersonEtter(
        opptegnelseId: Sekvensnummer,
        personIdentitetsnummer: Identitetsnummer,
    ): List<Opptegnelse> =
        asSQL(
            """
            SELECT o.*, p.fødselsnummer
            FROM opptegnelse o
                INNER JOIN person p on p.id = o.person_id
            WHERE sekvensnummer > :id
                AND p.fødselsnummer = :identitetsnummer
            """.trimIndent(),
            "id" to opptegnelseId.value,
            "identitetsnummer" to personIdentitetsnummer.value,
        ).list { it.tilOpptegnelse() }

    override fun finnNyesteSekvensnummer(): Sekvensnummer =
        Sekvensnummer(
            asSQL(
                """
                SELECT MAX(sekvensnummer)
                FROM opptegnelse
                """.trimIndent(),
            ).singleOrNull { it.intOrNull(1) } ?: 0,
        )

    private fun insertOpptegnelse(opptegnelse: Opptegnelse) =
        asSQL(
            """
            INSERT INTO opptegnelse (person_id, type, payload) 
            VALUES ((SELECT id FROM person WHERE fødselsnummer = :identitetsnummer), :type, '{}')
            """.trimIndent(),
            "identitetsnummer" to opptegnelse.identitetsnummer.value,
            "type" to opptegnelse.type.toString(),
        ).update()

    private fun Row.tilOpptegnelse(): Opptegnelse =
        Opptegnelse.fraLagring(
            id = Sekvensnummer(int("sekvensnummer")),
            identitetsnummer = Identitetsnummer.fraString(string("fødselsnummer")),
            type = Opptegnelse.Type.valueOf(string("type")),
        )
}
