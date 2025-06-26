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
) : QueryRunner by MedSession(session), ArbeidsgiverRepository {
    override fun lagre(arbeidsgiver: Arbeidsgiver) {
        if (finn(arbeidsgiver.id()) == null) {
            insertArbeidsgiver(arbeidsgiver)
        } else {
            updateArbeidsgiver(arbeidsgiver)
        }
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

    private fun insertArbeidsgiver(arbeidsgiver: Arbeidsgiver) =
        asSQL(
            """
            INSERT INTO arbeidsgiver (identifikator, navn, navn_sist_oppdatert_dato) 
            VALUES (:identifikator, :navn, :navn_sist_oppdatert_dato)
            """.trimIndent(),
            "identifikator" to arbeidsgiver.id().tilDbIdentifikator(),
            "navn" to arbeidsgiver.navn?.navn,
            "navn_sist_oppdatert_dato" to arbeidsgiver.navn?.sistOppdatertDato,
        ).updateAndReturnGeneratedKey().toInt()

    private fun updateArbeidsgiver(arbeidsgiver: Arbeidsgiver) {
        asSQL(
            """
            UPDATE arbeidsgiver 
            SET navn = :navn, navn_sist_oppdatert_dato = :navn_sist_oppdatert_dato 
            WHERE identifikator = :identifikator
            """.trimIndent(),
            "navn" to arbeidsgiver.navn?.navn,
            "navn_sist_oppdatert_dato" to arbeidsgiver.navn?.sistOppdatertDato,
            "identifikator" to arbeidsgiver.id().tilDbIdentifikator(),
        ).update()
    }

    private fun Row.toArbeidsgiver(): Arbeidsgiver =
        Arbeidsgiver.Factory.fraLagring(
            id = fraDbIdentifikator(string("identifikator")),
            navn =
                stringOrNull("navn")?.let { navn ->
                    Arbeidsgiver.Navn(
                        navn = navn,
                        sistOppdatertDato = localDate("navn_sist_oppdatert_dato"),
                    )
                },
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
