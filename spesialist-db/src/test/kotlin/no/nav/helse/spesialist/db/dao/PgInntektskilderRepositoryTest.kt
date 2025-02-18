package no.nav.helse.spesialist.db.dao

import com.fasterxml.jackson.module.kotlin.readValue
import kotliquery.Session
import no.nav.helse.db.DBSessionContext
import no.nav.helse.db.HelseDao.Companion.asSQL
import no.nav.helse.db.HelseDao.Companion.single
import no.nav.helse.modell.InntektskildetypeDto
import no.nav.helse.modell.KomplettInntektskildeDto
import no.nav.helse.modell.NyInntektskildeDto
import no.nav.helse.modell.vilkårsprøving.Avviksvurdering
import no.nav.helse.modell.vilkårsprøving.Beregningsgrunnlag
import no.nav.helse.modell.vilkårsprøving.InnrapportertInntekt
import no.nav.helse.modell.vilkårsprøving.Sammenligningsgrunnlag
import no.nav.helse.spesialist.db.AbstractDBIntegrationTest
import no.nav.helse.spesialist.db.jan
import no.nav.helse.spesialist.db.lagFødselsnummer
import no.nav.helse.spesialist.db.lagOrganisasjonsnavn
import no.nav.helse.spesialist.db.lagOrganisasjonsnummer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

internal class PgInntektskilderRepositoryTest : AbstractDBIntegrationTest() {
    private val avviksvurderingRepository = DBSessionContext(session) { _, _ -> false }.avviksvurderingRepository

    @Test
    fun `når det ikke finnes arbeidsgivere i databasen får vi kun tilbake nye inntektskilder`() {
        val organisasjonsnummer1 = lagOrganisasjonsnummer()
        val organisasjonsnummer2 = lagOrganisasjonsnummer()
        val funnet = inntektskilderRepository.finnInntektskilder(lagFødselsnummer(), listOf(organisasjonsnummer1, organisasjonsnummer2))
        assertEquals(2, funnet.size)
        assertTrue(funnet.all { it is NyInntektskildeDto })
    }

    @Test
    fun `når arbeidsgiver finnes i databasen får vi tilbake eksisterende inntektskilde`() {
        val organisasjonsnummer = lagOrganisasjonsnummer()
        val navn = lagOrganisasjonsnavn()

        opprettArbeidsgiver(organisasjonsnummer, navn, listOf("Uteliv", "Reise"))
        val funnet = inntektskilderRepository.finnInntektskilder(lagFødselsnummer(), listOf(organisasjonsnummer))
        assertEquals(1, funnet.size)
        val dto = funnet.single()
        check(dto is KomplettInntektskildeDto)
        assertEquals(organisasjonsnummer, dto.identifikator)
        assertEquals(navn, dto.navn)
        assertEquals(listOf("Uteliv", "Reise"), dto.bransjer)
    }

    @Test
    fun `når arbeidsgiver har fødselsnummer som id og starter med tallet 0 får vi riktig fødselsnummer ut igjen`() {
        val identifikator = lagFødselsnummer().replaceFirstChar { "0" }
        val navn = lagOrganisasjonsnavn()

        opprettArbeidsgiver(identifikator, navn, listOf("Uteliv", "Reise"))
        val funnet = inntektskilderRepository.finnInntektskilder(lagFødselsnummer(), listOf(identifikator))
        assertEquals(1, funnet.size)
        val dto = funnet.single()
        check(dto is KomplettInntektskildeDto)
        assertEquals(identifikator, dto.identifikator)
        assertEquals(navn, dto.navn)
        assertEquals(listOf("Uteliv", "Reise"), dto.bransjer)
    }

    @Test
    fun `når arbeidsgiver har fødselsnummer som id og starter med annet siffer enn 0 får vi riktig fødselsnummer ut igjen`() {
        val identifikator = lagFødselsnummer().replaceFirstChar { "1" }
        val navn = lagOrganisasjonsnavn()

        opprettArbeidsgiver(identifikator, navn, listOf("Uteliv", "Reise"))
        val funnet = inntektskilderRepository.finnInntektskilder(lagFødselsnummer(), listOf(identifikator))
        assertEquals(1, funnet.size)
        val dto = funnet.single()
        check(dto is KomplettInntektskildeDto)
        assertEquals(identifikator, dto.identifikator)
        assertEquals(navn, dto.navn)
        assertEquals(listOf("Uteliv", "Reise"), dto.bransjer)
    }

    @Test
    fun `når noen arbeidsgivere finnes i db og andre ikke får vi tilbake en kombinasjon av eksisterende og nye inntektskilder`() {
        val organisasjonsnummer1 = lagOrganisasjonsnummer()
        val organisasjonsnummer2 = lagOrganisasjonsnummer()
        val navn = lagOrganisasjonsnavn()

        opprettArbeidsgiver(organisasjonsnummer1, navn, listOf("Uteliv", "Reise"))
        val funnet = inntektskilderRepository.finnInntektskilder(lagFødselsnummer(), listOf(organisasjonsnummer1, organisasjonsnummer2))
        assertEquals(2, funnet.size)
        assertTrue(funnet[0] is KomplettInntektskildeDto)
        assertTrue(funnet[1] is NyInntektskildeDto)
    }

    @Test
    fun `Får med arbeidsgivere som finnes i sammenligningsgrunnlag`() {
        val fødselsnummer = lagFødselsnummer()
        val organisasjonsnummer = lagOrganisasjonsnummer()

        avviksvurderingRepository.lagre(
            Avviksvurdering(
                unikId = UUID.randomUUID(),
                vilkårsgrunnlagId = UUID.randomUUID(),
                fødselsnummer = fødselsnummer,
                skjæringstidspunkt = 1 jan 2018,
                opprettet = LocalDateTime.now(),
                avviksprosent = 0.0,
                sammenligningsgrunnlag = Sammenligningsgrunnlag(
                    600_000.0, listOf(InnrapportertInntekt(organisasjonsnummer, emptyList()))
                ),
                beregningsgrunnlag = Beregningsgrunnlag(600_000.0, emptyList())
            )
        )

        val funnet = inntektskilderRepository.finnInntektskilder(fødselsnummer, listOf(organisasjonsnummer))
        assertEquals(1, funnet.size)
        assertTrue(funnet.single() is NyInntektskildeDto)
    }


    @Test
    fun `Takler at arbeidsgivere kan opptre kun i sammenligningsgrunnlag, uten å være med i godkjenningsbehovet`() {
        val fødselsnummer = lagFødselsnummer()
        val organisasjonsnummerSomSpleisKjennerTil = lagOrganisasjonsnummer()
        val enAnnenArbeidsgiverForPersonen = lagOrganisasjonsnummer()

        avviksvurderingRepository.lagre(
            Avviksvurdering(
                unikId = UUID.randomUUID(),
                vilkårsgrunnlagId = UUID.randomUUID(),
                fødselsnummer = fødselsnummer,
                skjæringstidspunkt = 1 jan 2018,
                opprettet = LocalDateTime.now(),
                avviksprosent = 0.0,
                sammenligningsgrunnlag = Sammenligningsgrunnlag(
                    600_000.0, listOf(
                        InnrapportertInntekt(organisasjonsnummerSomSpleisKjennerTil, emptyList()),
                        InnrapportertInntekt(enAnnenArbeidsgiverForPersonen, emptyList())
                    )
                ),
                beregningsgrunnlag = Beregningsgrunnlag(600_000.0, emptyList())
            )
        )

        val funnet = inntektskilderRepository.finnInntektskilder(fødselsnummer, listOf(organisasjonsnummerSomSpleisKjennerTil))
        assertEquals(2, funnet.size)
        assertTrue(funnet.all { it is NyInntektskildeDto})
    }

    @Test
    fun `Hvis en inntektskilde i sammenligningsgrunnlaget finnes fra før får vi tilbake en eksisterende inntektskilde`() {
        val fødselsnummer = lagFødselsnummer()
        val organisasjonsnummer = lagOrganisasjonsnummer()
        val navn = lagOrganisasjonsnavn()

        avviksvurderingRepository.lagre(
            Avviksvurdering(
                unikId = UUID.randomUUID(),
                vilkårsgrunnlagId = UUID.randomUUID(),
                fødselsnummer = fødselsnummer,
                skjæringstidspunkt = 1 jan 2018,
                opprettet = LocalDateTime.now(),
                avviksprosent = 0.0,
                sammenligningsgrunnlag = Sammenligningsgrunnlag(
                    600_000.0,
                    listOf(
                        InnrapportertInntekt(organisasjonsnummer, emptyList())
                    )
                ),
                beregningsgrunnlag = Beregningsgrunnlag(600_000.0, emptyList())
            )
        )

        opprettArbeidsgiver(organisasjonsnummer, navn, listOf("Uteliv", "Reise"))

        val funnet = inntektskilderRepository.finnInntektskilder(fødselsnummer, listOf(organisasjonsnummer))
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

        inntektskilderRepository.lagreInntektskilder(
            listOf(
                KomplettInntektskildeDto(
                    identifikator = organisasjonsnummer1,
                    type = InntektskildetypeDto.ORDINÆR,
                    navn = navn1,
                    bransjer = bransjer1,
                    sistOppdatert = LocalDate.now(),
                ),
                KomplettInntektskildeDto(
                    identifikator = organisasjonsnummer2,
                    type = InntektskildetypeDto.ORDINÆR,
                    navn = navn2,
                    bransjer = bransjer2,
                    sistOppdatert = LocalDate.now()
                )
            )
        )
        session.finnInntektskilde(organisasjonsnummer1).let { første ->
            checkNotNull(første)
            assertEquals(organisasjonsnummer1, første.organisasjonsnummer)
            assertEquals(navn1, første.navn)
            assertEquals(bransjer1, første.bransjer)
        }
        session.finnInntektskilde(organisasjonsnummer2).let { andre ->
            checkNotNull(andre)
            assertEquals(organisasjonsnummer2, andre.organisasjonsnummer)
            assertEquals(navn2, andre.navn)
            assertEquals(bransjer2, andre.bransjer)
        }
    }

    private fun Session.finnInntektskilde(organisasjonsnummer: String) = asSQL(
        """
        select organisasjonsnummer, navn, bransjer
        from arbeidsgiver
        join arbeidsgiver_bransjer ab on arbeidsgiver.bransjer_ref = ab.id
        join arbeidsgiver_navn an on arbeidsgiver.navn_ref = an.id
        where organisasjonsnummer = :organisasjonsnummer
        """.trimIndent(),
        "organisasjonsnummer" to organisasjonsnummer
    ).single(this) { row ->
        ArbeidsgiverDto(
            organisasjonsnummer = row.string("organisasjonsnummer"),
            navn = row.string("navn"),
            bransjer = objectMapper.readValue(row.string("bransjer")),
        )
    }

    private data class ArbeidsgiverDto(
        val organisasjonsnummer: String,
        val navn: String,
        val bransjer: List<String>,
    )
}
