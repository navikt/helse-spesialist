package no.nav.helse.modell.overstyring

import kotliquery.Session
import kotliquery.queryOf
import no.nav.helse.mediator.kafka.meldinger.OverstyringMessage
import no.nav.helse.objectMapper
import org.intellij.lang.annotations.Language

fun Session.persisterOverstyring(
    fødselsnummer: String,
    organisasjonsnummer: String,
    begrunnelse: String,
    unntaFraInnsyn: Boolean,
    overstyrteDager: List<OverstyringMessage.OverstyringMessageDag>
): Long? {
    @Language("PostgreSQL")
    val opprettOverstyringQuery = """
        INSERT INTO overstyring(fodselsnummer, organisasjonsnummer, begrunnelse, unntafrainnsyn)
        VALUES (:fodselsnummer,
                :organisasjonsnummer,
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

    val overstyringRef = this.run(
        queryOf(
            opprettOverstyringQuery,
            mapOf(
                "fodselsnummer" to fødselsnummer.toLong(),
                "organisasjonsnummer" to organisasjonsnummer.toLong(),
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
                        "dagtype" to dag.dagtype,
                        "grad" to dag.grad
                    )
                ).asUpdate
            )
        }
    }

    return this.run(
        queryOf(
            opprettOverstyringQuery,
            mapOf(
                "fodselsnummer" to fødselsnummer.toLong(),
                "organisasjonsnummer" to organisasjonsnummer.toLong(),
                "begrunnelse" to begrunnelse,
                "unntafrainnsyn" to unntaFraInnsyn,
                "overstyrtedager" to objectMapper.writeValueAsString(overstyrteDager)
            )
        ).asUpdateAndReturnGeneratedKey
    )
}
