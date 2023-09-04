package no.nav.helse.spesialist.api.reservasjon

import java.util.UUID
import javax.sql.DataSource
import no.nav.helse.HelseDao

class ReservasjonDao(dataSource: DataSource) : HelseDao(dataSource) {
    fun reserverPerson(saksbehandlerOid: UUID, fødselsnummer: String, påVent: Boolean) {
        asSQL(
            """
            INSERT INTO reserver_person(saksbehandler_ref, person_ref, sett_på_vent_flagg)
            SELECT :saksbehandler_ref, person.id, :sett_paa_vent_flagg
            FROM person
            WHERE person.fodselsnummer = :fodselsnummer
            ON CONFLICT (person_ref)
                DO UPDATE SET gyldig_til = current_date + time '23:59:59',
                              saksbehandler_ref = :saksbehandler_ref,
                              sett_på_vent_flagg = :sett_paa_vent_flagg;
            """, mapOf(
                "saksbehandler_ref" to saksbehandlerOid,
                "fodselsnummer" to fødselsnummer.toLong(),
                "sett_paa_vent_flagg" to påVent,
            )
        ).update()

    }

    fun hentReservasjonFor(fødselsnummer: String): Reservasjonsinfo? = asSQL(
        """ SELECT r.* FROM reserver_person r
            JOIN person p ON p.id = r.person_ref
            WHERE p.fodselsnummer = :fnr AND r.gyldig_til > now();
        """, mapOf("fnr" to fødselsnummer.toLong())
    ).single { Reservasjonsinfo(it.uuid("saksbehandler_ref"), it.boolean("sett_på_vent_flagg")) }
}

data class Reservasjonsinfo(
    val reservertTil: UUID,
    val settPåVentFlagg: Boolean,
)
