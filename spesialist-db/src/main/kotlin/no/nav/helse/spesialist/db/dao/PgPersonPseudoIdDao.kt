package no.nav.helse.spesialist.db.dao

import kotliquery.Session
import no.nav.helse.spesialist.application.PersonPseudoId
import no.nav.helse.spesialist.application.PersonPseudoIdDao
import no.nav.helse.spesialist.db.DataSourceDbQuery
import no.nav.helse.spesialist.db.DbQuery
import no.nav.helse.spesialist.db.SessionDbQuery
import no.nav.helse.spesialist.domain.Identitetsnummer
import java.time.Instant
import javax.sql.DataSource

class PgPersonPseudoIdDao private constructor(
    private val dbQuery: DbQuery,
) : PersonPseudoIdDao {
    constructor(session: Session) : this(SessionDbQuery(session))
    constructor(dataSource: DataSource) : this(DataSourceDbQuery(dataSource))

    override fun nyPersonPseudoId(identitetsnummer: Identitetsnummer): PersonPseudoId {
        val personPseudoId = PersonPseudoId.ny()
        dbQuery.update(
            """INSERT INTO personpseudoid (pseudoid, identitetsnummer, opprettet_tidspunkt) VALUES (:pseudoid, :identitetsnummer, :opprettet_tidspunkt)""",
            "pseudoid" to personPseudoId.value,
            "identitetsnummer" to identitetsnummer.value,
            "opprettet_tidspunkt" to Instant.now(),
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
