package no.nav.helse.modell.overstyring

import kotliquery.Session
import kotliquery.queryOf
import no.nav.helse.objectMapper
import org.intellij.lang.annotations.Language
import java.time.LocalDate

fun Session.persisterOverstyring(
    fødselsnummer: String,
    organisasjonsnummer: String,
    begrunnelse: String,
    unntaFraInnsyn: Boolean,
    overstyrteDager: List<LocalDate>
): Long? {
    @Language("PostgreSQL")
    val query = """
        INSERT INTO overstyring(fodselsnummer, organisasjonsnummer, begrunnelse, unntafrainnsyn, overstyrtedager)
        VALUES (:fodselsnummer,
                :organisasjonsnummer,
                :begrunnelse,
                :unntafrainnsyn,
                :overstyrtedager::jsonb)
    """

    return this.run(
        queryOf(
            query,
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
