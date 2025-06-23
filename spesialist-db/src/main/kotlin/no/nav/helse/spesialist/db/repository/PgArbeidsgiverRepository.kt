package no.nav.helse.spesialist.db.repository

import kotliquery.Row
import kotliquery.Session
import no.nav.helse.spesialist.application.ArbeidsgiverRepository
import no.nav.helse.spesialist.db.HelseDao.Companion.asSQL
import no.nav.helse.spesialist.db.MedSession
import no.nav.helse.spesialist.db.QueryRunner
import no.nav.helse.spesialist.domain.Arbeidsgiver
import no.nav.helse.spesialist.domain.ArbeidsgiverId

internal class PgArbeidsgiverRepository(
    private val session: Session,
) : QueryRunner by MedSession(session), ArbeidsgiverRepository {
    override fun lagre(arbeidsgiver: Arbeidsgiver) {
        if (!arbeidsgiver.harFåttTildeltId()) {
            insertArbeidsgiver(arbeidsgiver).let(::ArbeidsgiverId).let(arbeidsgiver::tildelId)
        } else {
            updateArbeidsgiver(arbeidsgiver)
        }
    }

    override fun finn(id: ArbeidsgiverId): Arbeidsgiver? =
        asSQL(
            "SELECT * FROM arbeidsgiver WHERE id = :id",
            "id" to id.value,
        ).singleOrNull { it.toArbeidsgiver() }

    override fun finnForIdentifikator(identifikator: Arbeidsgiver.Identifikator): Arbeidsgiver? =
        asSQL(
            "SELECT * FROM arbeidsgiver WHERE organisasjonsnummer = :organisasjonsnummer",
            "organisasjonsnummer" to identifikator.tilDbOrganisasjonsnummer(),
        ).singleOrNull { it.toArbeidsgiver() }

    override fun finnAlleForIdentifikatorer(identifikatorer: Set<Arbeidsgiver.Identifikator>): List<Arbeidsgiver> =
        if (identifikatorer.isEmpty()) {
            emptyList()
        } else {
            asSQL(
                "SELECT * FROM arbeidsgiver WHERE organisasjonsnummer = ANY (:organisasjonsnumre)",
                "organisasjonsnumre" to identifikatorer.map { it.tilDbOrganisasjonsnummer() }.toTypedArray(),
            ).list { it.toArbeidsgiver() }
        }

    private fun insertArbeidsgiver(arbeidsgiver: Arbeidsgiver): Int {
        return asSQL(
            """
            INSERT INTO arbeidsgiver (organisasjonsnummer, identifikator, navn, navn_sist_oppdatert_dato) 
            VALUES (:organisasjonsnummer, :identifikator, :navn, :navn_sist_oppdatert_dato)
            """.trimIndent(),
            "organisasjonsnummer" to arbeidsgiver.identifikator.tilDbOrganisasjonsnummer(),
            "identifikator" to arbeidsgiver.identifikator.tilDbOrganisasjonsnummer(),
            "navn" to arbeidsgiver.navn?.navn,
            "navn_sist_oppdatert_dato" to arbeidsgiver.navn?.sistOppdatertDato,
        ).updateAndReturnGeneratedKey().toInt()
    }

    private fun updateArbeidsgiver(arbeidsgiver: Arbeidsgiver) {
        asSQL(
            """
            UPDATE arbeidsgiver 
            SET organisasjonsnummer = :organisasjonsnummer, identifikator = :identifikator, navn = :navn, navn_sist_oppdatert_dato = :navn_sist_oppdatert_dato 
            WHERE id = :id
            """.trimIndent(),
            "organisasjonsnummer" to arbeidsgiver.identifikator.tilDbOrganisasjonsnummer(),
            "identifikator" to arbeidsgiver.identifikator.tilDbOrganisasjonsnummer(),
            "id" to arbeidsgiver.id().value,
            "navn" to arbeidsgiver.navn?.navn,
            "navn_sist_oppdatert_dato" to arbeidsgiver.navn?.sistOppdatertDato,
        ).update()
    }

    private fun Row.toArbeidsgiver(): Arbeidsgiver =
        Arbeidsgiver.Factory.fraLagring(
            id = ArbeidsgiverId(int("id")),
            identifikator = fraDbOrganisasjonsnummer(string("organisasjonsnummer")),
            navn =
                stringOrNull("navn")?.let { navn ->
                    Arbeidsgiver.Navn(
                        navn = navn,
                        sistOppdatertDato = localDate("navn_sist_oppdatert_dato"),
                    )
                },
        )

    private fun Arbeidsgiver.Identifikator.tilDbOrganisasjonsnummer(): String =
        when (this) {
            is Arbeidsgiver.Identifikator.Organisasjonsnummer -> this.organisasjonsnummer
            is Arbeidsgiver.Identifikator.Fødselsnummer -> this.fødselsnummer
        }

    private fun fraDbOrganisasjonsnummer(organisasjonsnummer: String): Arbeidsgiver.Identifikator =
        if (organisasjonsnummer.length == 9) {
            Arbeidsgiver.Identifikator.Organisasjonsnummer(organisasjonsnummer)
        } else {
            Arbeidsgiver.Identifikator.Fødselsnummer(organisasjonsnummer)
        }
}
