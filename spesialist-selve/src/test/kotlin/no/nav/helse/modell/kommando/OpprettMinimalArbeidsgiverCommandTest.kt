package no.nav.helse.modell.kommando

import no.nav.helse.db.InntektskilderRepository
import no.nav.helse.modell.InntektskildeDto
import no.nav.helse.modell.InntektskildetypeDto
import no.nav.helse.modell.NyInntektskildeDto
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID

internal class OpprettMinimalArbeidsgiverCommandTest {
    private companion object {
        private const val ORGNR = "123456789"
    }

    private lateinit var context: CommandContext
    private val lagredeInntektskilder = mutableListOf<InntektskildeDto>()

    @BeforeEach
    fun setup() {
        context = CommandContext(UUID.randomUUID())
        lagredeInntektskilder.clear()
    }

    @Test
    fun `opprett arbeidsgiver`() {
        val command = lagCommand(null)
        assertTrue(command.execute(context))
        assertEquals(1, lagredeInntektskilder.size)
        assertEquals(
            NyInntektskildeDto(organisasjonsnummer = ORGNR, type = InntektskildetypeDto.ORDINÆR),
            lagredeInntektskilder.single()
        )
    }


    @Test
    fun `oppretter ikke arbeidsgiver når den finnes`() {
        val command = lagCommand(1)
        assertTrue(command.execute(context))
        assertEquals(0, lagredeInntektskilder.size)
    }

    private fun lagCommand(ekisterendeId: Long?) : OpprettMinimalArbeidsgiverCommand {
        val repository = object : InntektskilderRepository  {
            override fun lagreInntektskilder(inntektskilder: List<InntektskildeDto>) {
                lagredeInntektskilder.addAll(inntektskilder)
            }

            override fun finnInntektskildeMedOrgnummer(orgnummer: String) = ekisterendeId

            override fun finnInntektskilder(fødselsnummer: String, andreOrganisasjonsnumre: List<String>) =
                emptyList<InntektskildeDto>()
        }
        return OpprettMinimalArbeidsgiverCommand(ORGNR, repository)
    }
}
