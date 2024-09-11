package no.nav.helse.db

import DatabaseIntegrationTest
import no.nav.helse.januar
import no.nav.helse.modell.InntektskildetypeDto
import no.nav.helse.modell.KomplettInntektskildeDto
import no.nav.helse.modell.NyInntektskildeDto
import no.nav.helse.modell.vilkårsprøving.AvviksvurderingDto
import no.nav.helse.modell.vilkårsprøving.BeregningsgrunnlagDto
import no.nav.helse.modell.vilkårsprøving.InnrapportertInntektDto
import no.nav.helse.modell.vilkårsprøving.SammenligningsgrunnlagDto
import no.nav.helse.spesialist.test.lagFødselsnummer
import no.nav.helse.spesialist.test.lagOrganisasjonsnavn
import no.nav.helse.spesialist.test.lagOrganisasjonsnummer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class InntektskilderDaoTest: DatabaseIntegrationTest() {
    private val dao = InntektskilderDao(dataSource)
    private val avviksvurderingDao = AvviksvurderingDao(dataSource)

    @Test
    fun `når det ikke finnes arbeidsgivere i databasen får vi kun tilbake nye inntektskilder`() {
        val organisasjonsnummer1 = lagOrganisasjonsnummer()
        val organisasjonsnummer2 = lagOrganisasjonsnummer()
        val funnet = dao.finnInntektskilder(lagFødselsnummer(), listOf(organisasjonsnummer1, organisasjonsnummer2))
        assertEquals(2, funnet.size)
        assertTrue(funnet.all { it is NyInntektskildeDto })
    }

    @Test
    fun `når arbeidsgiver finnes i databasen får vi tilbake eksisterende inntektskilde`() {
        val organisasjonsnummer = lagOrganisasjonsnummer()
        val navn = lagOrganisasjonsnavn()

        opprettArbeidsgiver(organisasjonsnummer, navn, listOf("Uteliv", "Reise"))
        val funnet = dao.finnInntektskilder(lagFødselsnummer(), listOf(organisasjonsnummer))
        assertEquals(1, funnet.size)
        val dto = funnet.single()
        check(dto is KomplettInntektskildeDto)
        assertEquals(organisasjonsnummer, dto.organisasjonsnummer)
        assertEquals(navn, dto.navn)
        assertEquals(listOf("Uteliv", "Reise"), dto.bransjer)
    }

    @Test
    fun `når noen arbeidsgivere finnes i db og andre ikke får vi tilbake en kombinasjon av eksisterende og nye inntektskilder`() {
        val organisasjonsnummer1 = lagOrganisasjonsnummer()
        val organisasjonsnummer2 = lagOrganisasjonsnummer()
        val navn = lagOrganisasjonsnavn()

        opprettArbeidsgiver(organisasjonsnummer1, navn, listOf("Uteliv", "Reise"))
        val funnet = dao.finnInntektskilder(lagFødselsnummer(), listOf(organisasjonsnummer1, organisasjonsnummer2))
        assertEquals(2, funnet.size)
        assertTrue(funnet[0] is KomplettInntektskildeDto)
        assertTrue(funnet[1] is NyInntektskildeDto)
    }

    @Test
    fun `Får med arbeidsgivere som finnes i sammenligningsgrunnlag`() {
        val fødselsnummer = lagFødselsnummer()
        val organisasjonsnummer = lagOrganisasjonsnummer()

        avviksvurderingDao.lagre(
            AvviksvurderingDto(
                unikId = UUID.randomUUID(),
                vilkårsgrunnlagId = UUID.randomUUID(),
                fødselsnummer = fødselsnummer,
                skjæringstidspunkt = 1.januar,
                opprettet = LocalDateTime.now(),
                avviksprosent = 0.0,
                sammenligningsgrunnlag = SammenligningsgrunnlagDto(600_000.0,
                    listOf(InnrapportertInntektDto(organisasjonsnummer, emptyList())
                    )
                ),
                beregningsgrunnlag = BeregningsgrunnlagDto(600_000.0, emptyList())
            )
        )

        val funnet = dao.finnInntektskilder(fødselsnummer, listOf(organisasjonsnummer))
        assertEquals(1, funnet.size)
        assertTrue(funnet.single() is NyInntektskildeDto)
    }

    @Test
    fun `Hvis en inntektskilde i sammenligningsgrunnlaget finnes fra før får vi tilbake en eksisterende inntektskilde`() {
        val fødselsnummer = lagFødselsnummer()
        val organisasjonsnummer = lagOrganisasjonsnummer()
        val navn = lagOrganisasjonsnavn()

        avviksvurderingDao.lagre(
            AvviksvurderingDto(
                unikId = UUID.randomUUID(),
                vilkårsgrunnlagId = UUID.randomUUID(),
                fødselsnummer = fødselsnummer,
                skjæringstidspunkt = 1.januar,
                opprettet = LocalDateTime.now(),
                avviksprosent = 0.0,
                sammenligningsgrunnlag = SammenligningsgrunnlagDto(600_000.0,
                    listOf(InnrapportertInntektDto(organisasjonsnummer, emptyList())
                    )
                ),
                beregningsgrunnlag = BeregningsgrunnlagDto(600_000.0, emptyList())
            )
        )

        opprettArbeidsgiver(organisasjonsnummer, navn, listOf("Uteliv", "Reise"))

        val funnet = dao.finnInntektskilder(fødselsnummer, listOf(organisasjonsnummer))
        assertEquals(1, funnet.size)
        assertTrue(funnet.single() is KomplettInntektskildeDto)
    }

    @Test
    fun `Kan lagre komplette inntektskilder`() {
        val organisasjonsnummer1 = lagOrganisasjonsnummer()
        val organisasjonsnummer2 = lagOrganisasjonsnummer()
        val navn1 = lagOrganisasjonsnavn()
        val navn2 = lagOrganisasjonsnavn()
        val bransjer1 = listOf("Uteliv", "Reise")
        val bransjer2 = listOf("Hotell")

        dao.lagreInntektskilder(
            listOf(
                KomplettInntektskildeDto(
                    organisasjonsnummer = organisasjonsnummer1,
                    type = InntektskildetypeDto.ORDINÆR,
                    navn = navn1,
                    bransjer = bransjer1,
                    sistOppdatert = LocalDate.now(),
                ),
                KomplettInntektskildeDto(
                    organisasjonsnummer = organisasjonsnummer2,
                    type = InntektskildetypeDto.ORDINÆR,
                    navn = navn2,
                    bransjer = bransjer2,
                    sistOppdatert = LocalDate.now()
                )
            )
        )

        val funnet = dao.finnInntektskilder(lagFødselsnummer(), listOf(organisasjonsnummer1, organisasjonsnummer2))
        assertEquals(2, funnet.size)
        val første = funnet[0]
        val andre = funnet[1]
        check(første is KomplettInntektskildeDto && andre is KomplettInntektskildeDto)

        assertEquals(organisasjonsnummer1, første.organisasjonsnummer)
        assertEquals(navn1, første.navn)
        assertEquals(bransjer1, første.bransjer)

        assertEquals(organisasjonsnummer2, andre.organisasjonsnummer)
        assertEquals(navn2, andre.navn)
        assertEquals(bransjer2, andre.bransjer)
    }
}
