package no.nav.helse.modell.overstyring

import kotliquery.Session
import kotliquery.queryOf
import no.nav.helse.mediator.kafka.meldinger.Dagtype
import no.nav.helse.mediator.kafka.meldinger.OverstyringMessage
import no.nav.helse.modell.arbeidsgiver.findArbeidsgiverByOrgnummer
import no.nav.helse.modell.person.findPersonByFødselsnummer
import no.nav.helse.modell.person.toFødselsnummer
import org.intellij.lang.annotations.Language
import java.time.LocalDate

fun Session.persisterOverstyring(
    fødselsnummer: String,
    organisasjonsnummer: String,
    begrunnelse: String,
    unntaFraInnsyn: Boolean,
    overstyrteDager: List<OverstyringMessage.OverstyringMessageDag>
): Long? {
    @Language("PostgreSQL")
    val opprettOverstyringQuery = """
        INSERT INTO overstyring(person_ref, arbeidsgiver_ref, begrunnelse, unntafrainnsyn)
        VALUES (:person_ref,
                :arbeidsgiver_ref,
                :begrunnelse,
                :unntafrainnsyn)
    """

    @Language("PostgreSQL")
    val opprettOverstyringDagQuery = """
        INSERT INTO overstyrtdag(overstyring_ref, dato, dagtype, grad)
        VALUES (:overstyring_ref,
                :dato,
                :dagtype,
                :grad)
    """

    val person_ref = findPersonByFødselsnummer(fødselsnummer.toLong())
    val arbeidsgiver_ref = findArbeidsgiverByOrgnummer(organisasjonsnummer.toLong())

    val overstyringRef = this.run(
        queryOf(
            opprettOverstyringQuery,
            mapOf(
                "person_ref" to person_ref,
                "arbeidsgiver_ref" to arbeidsgiver_ref,
                "begrunnelse" to begrunnelse,
                "unntafrainnsyn" to unntaFraInnsyn
            )
        ).asUpdateAndReturnGeneratedKey
    )

    this.transaction { transactionalSession ->
        overstyrteDager.forEach { dag ->
            transactionalSession.run(
                queryOf(
                    opprettOverstyringDagQuery,
                    mapOf(
                        "overstyring_ref" to overstyringRef,
                        "dato" to dag.dato,
                        "dagtype" to dag.type.toString(),
                        "grad" to dag.grad
                    )
                ).asUpdate
            )
        }
    }
    return overstyringRef
}

fun Session.finnOverstyring(fødselsnummer: String, organisasjonsnummer: String): List<OverstyringDto> {
    @Language("PostgreSQL")
    val finnOverstyringQuery = """
SELECT o.id as overstyring_id, *
FROM overstyring o
         INNER JOIN person p ON p.id = o.person_ref
         INNER JOIN arbeidsgiver a on a.id = o.arbeidsgiver_ref
WHERE p.fodselsnummer = ?
  AND a.orgnummer = ?
    """
    return this.run(queryOf(finnOverstyringQuery, fødselsnummer.toLong(), organisasjonsnummer.toLong()).map { overstyringRow ->
        val id = overstyringRow.long("overstyring_id")

        OverstyringDto(
            fødselsnummer = overstyringRow.long("fodselsnummer").toFødselsnummer(),
            organisasjonsnummer = overstyringRow.int("orgnummer").toString(),
            begrunnelse = overstyringRow.string("begrunnelse"),
            unntaFraInnsyn = overstyringRow.boolean("unntaFraInnsyn"),
            overstyrteDager = this.run(queryOf(
                "SELECT * FROM overstyrtdag WHERE overstyring_ref = ?", id
            ).map { overstyringDagRow ->
                OverstyringDagDto(
                    dato = overstyringDagRow.localDate("dato"),
                    dagtype = enumValueOf(overstyringDagRow.string("dagtype")),
                    grad = overstyringDagRow.intOrNull("grad")
                )
            }.asList
            )
        )
    }.asList)
}


data class OverstyringDto(
    val fødselsnummer: String,
    val organisasjonsnummer: String,
    val begrunnelse: String,
    val unntaFraInnsyn: Boolean,
    val overstyrteDager: List<OverstyringDagDto>
)

data class OverstyringDagDto(
    val dato: LocalDate,
    val dagtype: Dagtype,
    val grad: Int?
)
