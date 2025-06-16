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
            """
                    SELECT ag.id, ag.organisasjonsnummer, an.navn, an.navn_oppdatert FROM arbeidsgiver ag
                    LEFT JOIN arbeidsgiver_navn an on an.id = ag.navn_ref
                    WHERE ag.id IN (:id)
                """,
            "id" to id.value,
        ).singleOrNull { it.toArbeidsgiver() }

    override fun finnForIdentifikator(identifikator: Arbeidsgiver.Identifikator): Arbeidsgiver? =
        asSQL(
            """
                    SELECT ag.id, ag.organisasjonsnummer, an.navn, an.navn_oppdatert FROM arbeidsgiver ag
                    LEFT JOIN arbeidsgiver_navn an on an.id = ag.navn_ref
                    WHERE ag.organisasjonsnummer IN (:organisasjonsnummer)
                """,
            "organisasjonsnummer" to identifikator.tilDbOrganisasjonsnummer(),
        ).singleOrNull { it.toArbeidsgiver() }

    override fun finnAlleForIdentifikatorer(identifikatorer: Set<Arbeidsgiver.Identifikator>): List<Arbeidsgiver> =
        if (identifikatorer.isEmpty()) {
            emptyList()
        } else {
            asSQL(
                """
                    SELECT ag.id, ag.organisasjonsnummer, an.navn, an.navn_oppdatert FROM arbeidsgiver ag
                    LEFT JOIN arbeidsgiver_navn an on an.id = ag.navn_ref
                    WHERE ag.organisasjonsnummer = ANY (:organisasjonsnumre)
                """,
                "organisasjonsnumre" to identifikatorer.map { it.tilDbOrganisasjonsnummer() }.toTypedArray(),
            ).list { it.toArbeidsgiver() }
        }

    private fun insertArbeidsgiver(arbeidsgiver: Arbeidsgiver): Int {
        val arbeidsgiverNavnId =
            arbeidsgiver.navn?.let { navn ->
                insertArbeidsgiverNavn(navn)
            }
        return asSQL(
            "INSERT INTO arbeidsgiver (organisasjonsnummer, navn_ref) VALUES (:organisasjonsnummer, :navn_ref)",
            "organisasjonsnummer" to arbeidsgiver.identifikator.tilDbOrganisasjonsnummer(),
            "navn_ref" to arbeidsgiverNavnId,
        ).updateAndReturnGeneratedKey().toInt()
    }

    private fun updateArbeidsgiver(arbeidsgiver: Arbeidsgiver) {
        val eksisterendeArbeidsgiverNavnId =
            asSQL(
                "SELECT navn_ref FROM arbeidsgiver WHERE id = :id",
                "id" to arbeidsgiver.id().value,
            ).singleOrNull { it.intOrNull("navn_ref") }

        val arbeidsgiverNavnId =
            arbeidsgiver.navn?.let { navn ->
                eksisterendeArbeidsgiverNavnId
                    ?.also { updateArbeidsgiverNavn(navn, eksisterendeArbeidsgiverNavnId) }
                    ?: insertArbeidsgiverNavn(navn)
            }

        asSQL(
            "UPDATE arbeidsgiver SET organisasjonsnummer = :organisasjonsnummer, navn_ref = :navn_ref WHERE id = :id",
            "organisasjonsnummer" to arbeidsgiver.identifikator.tilDbOrganisasjonsnummer(),
            "navn_ref" to arbeidsgiverNavnId,
            "id" to arbeidsgiver.id().value,
        ).update()

        if (eksisterendeArbeidsgiverNavnId != null && arbeidsgiverNavnId == null) {
            deleteArbeidsgiverNavn(eksisterendeArbeidsgiverNavnId)
        }
    }

    private fun Row.toArbeidsgiver(): Arbeidsgiver {
        return Arbeidsgiver.Factory.fraLagring(
            id = ArbeidsgiverId(int("id")),
            identifikator = fraDbOrganisasjonsnummer(string("organisasjonsnummer")),
            navn =
                stringOrNull("navn")?.let { navn ->
                    Arbeidsgiver.Navn(
                        navn = navn,
                        sistOppdatertDato = localDate("navn_oppdatert"),
                    )
                },
        )
    }

    private fun insertArbeidsgiverNavn(navn: Arbeidsgiver.Navn): Int =
        asSQL(
            "INSERT INTO arbeidsgiver_navn (navn, navn_oppdatert) VALUES (:navn, :navn_oppdatert)",
            "navn" to navn.navn,
            "navn_oppdatert" to navn.sistOppdatertDato,
        ).updateAndReturnGeneratedKey().toInt()

    private fun updateArbeidsgiverNavn(
        navn: Arbeidsgiver.Navn,
        arbeidsgiverNavnId: Int,
    ) {
        asSQL(
            "UPDATE arbeidsgiver_navn SET navn = :navn, navn_oppdatert = :navn_oppdatert WHERE id = :id",
            "navn" to navn.navn,
            "navn_oppdatert" to navn.sistOppdatertDato,
            "id" to arbeidsgiverNavnId,
        ).update()
    }

    private fun deleteArbeidsgiverNavn(arbeidsgiverNavnId: Int) {
        asSQL(
            "DELETE FROM arbeidsgiver_navn WHERE id = :id",
            "id" to arbeidsgiverNavnId,
        ).update()
    }

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
