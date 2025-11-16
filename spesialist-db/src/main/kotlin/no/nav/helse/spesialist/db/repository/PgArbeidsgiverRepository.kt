package no.nav.helse.spesialist.db.repository

import kotliquery.Row
import kotliquery.Session
import no.nav.helse.spesialist.application.ArbeidsgiverRepository
import no.nav.helse.spesialist.db.HelseDao.Companion.asSQL
import no.nav.helse.spesialist.db.MedSession
import no.nav.helse.spesialist.db.QueryRunner
import no.nav.helse.spesialist.domain.Arbeidsgiver
import no.nav.helse.spesialist.domain.ArbeidsgiverIdentifikator

internal class PgArbeidsgiverRepository(
    private val session: Session,
) : QueryRunner by MedSession(session),
    ArbeidsgiverRepository {
    override fun lagre(arbeidsgiver: Arbeidsgiver) {
        asSQL(
            """
            INSERT INTO arbeidsgiver (identifikator, navn, navn_sist_oppdatert_dato) 
            VALUES (:identifikator, :navn, :navn_sist_oppdatert_dato)
            ON CONFLICT (identifikator) DO UPDATE
            SET navn = excluded.navn, navn_sist_oppdatert_dato = excluded.navn_sist_oppdatert_dato
            """.trimIndent(),
            "identifikator" to arbeidsgiver.id.tilDbIdentifikator(),
            "navn" to arbeidsgiver.navn.navn,
            "navn_sist_oppdatert_dato" to arbeidsgiver.navn.sistOppdatertDato,
        ).update()
    }

    override fun finn(id: ArbeidsgiverIdentifikator): Arbeidsgiver? =
        asSQL(
            "SELECT * FROM arbeidsgiver WHERE identifikator = :identifikator",
            "identifikator" to id.tilDbIdentifikator(),
        ).singleOrNull { it.toArbeidsgiver() }

    override fun finnAlle(ider: Set<ArbeidsgiverIdentifikator>): List<Arbeidsgiver> =
        if (ider.isEmpty()) {
            emptyList()
        } else {
            asSQL(
                "SELECT * FROM arbeidsgiver WHERE identifikator = ANY (:identifikatorer)",
                "identifikatorer" to ider.map { it.tilDbIdentifikator() }.toTypedArray(),
            ).list { it.toArbeidsgiver() }
        }

    private fun Row.toArbeidsgiver(): Arbeidsgiver =
        Arbeidsgiver.Factory.fraLagring(
            id = fraDbIdentifikator(string("identifikator")),
            navn =
                Arbeidsgiver.Navn(
                    navn = string("navn"),
                    sistOppdatertDato = localDate("navn_sist_oppdatert_dato"),
                ),
        )

    private fun ArbeidsgiverIdentifikator.tilDbIdentifikator(): String =
        when (this) {
            is ArbeidsgiverIdentifikator.Organisasjonsnummer -> this.organisasjonsnummer
            is ArbeidsgiverIdentifikator.Fødselsnummer -> this.fødselsnummer
        }

    private fun fraDbIdentifikator(identifikator: String): ArbeidsgiverIdentifikator =
        if (identifikator.length == 9) {
            ArbeidsgiverIdentifikator.Organisasjonsnummer(identifikator)
        } else {
            ArbeidsgiverIdentifikator.Fødselsnummer(identifikator)
        }
}
