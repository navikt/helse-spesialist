package no.nav.helse.spesialist.db.dao

import kotliquery.Session
import no.nav.helse.spesialist.application.PersonPseudoId
import no.nav.helse.spesialist.application.PersonPseudoIdDao
import no.nav.helse.spesialist.db.SessionDbQuery
import no.nav.helse.spesialist.domain.Identitetsnummer

class PgPersonPseudoIdDao(
    session: Session,
) : PersonPseudoIdDao {
    private val dbQuery = SessionDbQuery(session)

    override fun nyPersonPseudoId(identitetsnummer: Identitetsnummer): PersonPseudoId {
        val personPseudoId = PersonPseudoId.ny()
        dbQuery.update(
            """INSERT INTO personpseudoid (pseudoid, identitetsnummer) VALUES (:pseudoid, :identitetsnummer)""",
            "pseudoid" to personPseudoId.value,
            "identitetsnummer" to identitetsnummer.value,
        )
        return personPseudoId
    }

    override fun hentIdentitetsnummer(personPseudoId: PersonPseudoId): Identitetsnummer? =
        dbQuery.singleOrNull(
            """SELECT identitetsnummer FROM personpseudoid WHERE pseudoid = :person_pseudo_id""",
            "person_pseudo_id" to personPseudoId.value,
        ) {
            val identitetsnummer = it.string("identitetsnummer")
            Identitetsnummer.fraString(identitetsnummer)
        }
}
