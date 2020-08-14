package no.nav.helse.modell.command.ny

import junit.framework.Assert.assertTrue
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.mediator.kafka.meldinger.OverstyringMessage
import no.nav.helse.modell.command.Command
import no.nav.helse.modell.command.Løsninger
import no.nav.helse.modell.overstyring.OverstyringCommand
import no.nav.helse.setupDataSourceMedFlyway
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.*
import kotlin.test.assertEquals

class OverstyringCommandTest {
    @Test
    fun `overstyring-command legger ovserstyringsmelding på rapid`() {
        val dataSource = setupDataSourceMedFlyway()

        val overstyringMessage = OverstyringMessage(
            saksbehandlerEpost = "tbd@nav.no",
            saksbehandlerOid = UUID.randomUUID(),
            aktørId = "aktørid",
            fødselsnummer = "12345678910",
            organisasjonsnummer = "987654321",
            begrunnelse = "En god grunn",
            unntaFraInnsyn = false,
            dager = listOf(
                OverstyringMessage.OverstyringMessageDag(
                    dato = LocalDate.of(2020, 1, 1),
                    dagtype = "Syk",
                    grad = 100
                ),
                OverstyringMessage.OverstyringMessageDag(
                    dato = LocalDate.of(2020, 1, 2),
                    dagtype = "Syk",
                    grad = 100
                )
            )
        )

        val overstyringCommand = OverstyringCommand(UUID.randomUUID(), null)
        val resultat = sessionOf(dataSource, returnGeneratedKey = true).use {
            overstyringCommand.resume(it, Løsninger().apply {
                add(overstyringMessage)
            })
            overstyringCommand.execute(it)
        }
        assertTrue(resultat is Command.Resultat.Ok.Løst)

        val løsning = (resultat as Command.Resultat.Ok.Løst).løsning

        assertEquals("overstyr_dager", løsning["@event_name"])
        assertEquals(overstyringMessage.aktørId, løsning["aktørId"])
        assertEquals(overstyringMessage.fødselsnummer, løsning["fødselsnummer"])
        assertEquals(overstyringMessage.organisasjonsnummer, løsning["organisasjonsnummer"])
        assertEquals(overstyringMessage.dager, løsning["dager"])

        @Language("PostgreSQL")
        val hentOvertstyringQuery = """
             SELECT *
             FROM overstyrtdag
             INNER JOIN overstyring o on o.id = overstyrtdag.overstyring_ref
             WHERE o.id = :overstyring_ref
        """

        val overstyring = sessionOf(dataSource).use {
            it.run(
                queryOf(
                    hentOvertstyringQuery,
                    mapOf(
                        "overstyring_ref" to 1
                    )
                )
                    .map { row ->
                        DbResult(
                            fødselsnummer = row.long("fodselsnummer"),
                            organisasjonsnummer = row.long("organisasjonsnummer"),
                            begrunnelse = row.string("begrunnelse"),
                            unntaFraInnsyn = row.boolean("unntafrainnsyn"),
                            grad = row.int("grad"),
                            dato = row.localDate("dato"),
                            dagtype = row.string("dagtype")
                        )
                    }
                    .asList
            ).let { dager ->
                OverstyringDbDto(
                    fødselsnummer = dager[0].fødselsnummer,
                    organisasjonsnummer = dager[0].organisasjonsnummer,
                    begrunnelse = dager[0].begrunnelse,
                    unntaFraInnsyn = dager[0].unntaFraInnsyn,
                    dager = dager.map { dag ->
                        OverstyringDbDto.Dag(
                            grad = dag.grad,
                            dato = dag.dato,
                            dagtype = dag.dagtype
                        )
                    }
                )
            }
        }

        assertEquals(overstyring.dager.size, 2)
    }
}

class OverstyringDbDto(
    val fødselsnummer: Long,
    val organisasjonsnummer: Long,
    val begrunnelse: String,
    val unntaFraInnsyn: Boolean,
    val dager: List<Dag>
) {
    class Dag(
        val grad: Int,
        val dato: LocalDate,
        val dagtype: String
    )
}

data class DbResult(
    val fødselsnummer: Long,
    val organisasjonsnummer: Long,
    val begrunnelse: String,
    val unntaFraInnsyn: Boolean,
    val grad: Int,
    val dato: LocalDate,
    val dagtype: String
)
