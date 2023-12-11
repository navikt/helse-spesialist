package no.nav.helse.db

import DatabaseIntegrationTest
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.UUID
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.januar
import no.nav.helse.modell.avviksvurdering.Avviksvurdering
import no.nav.helse.modell.avviksvurdering.AvviksvurderingDto
import no.nav.helse.modell.avviksvurdering.BeregningsgrunnlagDto
import no.nav.helse.modell.avviksvurdering.InnrapportertInntektDto
import no.nav.helse.modell.avviksvurdering.InntektDto
import no.nav.helse.modell.avviksvurdering.OmregnetÅrsinntektDto
import no.nav.helse.modell.avviksvurdering.SammenligningsgrunnlagDto
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

internal class AvviksvurderingDaoTest: DatabaseIntegrationTest() {

    private val avviksvurderingDao = AvviksvurderingDao(dataSource)

    @Test
    fun `lagre avviksvurdering`() {
        avviksvurderingDao.lagre(avviksvurdering(fødselsnummer = FNR))
        assertNotNull(avviksvurderingDao.finnAvviksvurderinger(FNR).first())
    }

    @Test
    fun `finner avviksvurderinger basert på fødselsnummer`() {
        val skjæringstidspunkt = 1.januar
        val unikId = UUID.randomUUID()
        val vilkårsgrunnlagId = UUID.randomUUID()
        val opprettet = LocalDateTime.now()
        avviksvurderingDao.lagre(avviksvurdering(
            fødselsnummer = FNR,
            vilkårsgrunnlagId = vilkårsgrunnlagId,
            skjæringstidspunkt = skjæringstidspunkt,
            unikId = unikId,
            opprettet = opprettet
        ))

        val avviksvurderinger = avviksvurderingDao.finnAvviksvurderinger(FNR)
        val forventetAvviksvurdering = forventetAvviksvurdering(
            fødselsnummer = FNR,
            vilkårsgrunnlagId = vilkårsgrunnlagId,
            skjæringstidspunkt = skjæringstidspunkt,
            unikId = unikId,
            opprettet = opprettet,
        )
        assertEquals(forventetAvviksvurdering, avviksvurderinger.first())
    }

    @Test
    fun `samme avviksvurdering med forskjellig vilkårsgrunnlagId`() {
        val skjæringstidspunkt = 1.januar
        val unikId = UUID.randomUUID()
        val vilkårsgrunnlagId = UUID.randomUUID()
        val opprettet = LocalDateTime.now()
        avviksvurderingDao.lagre(avviksvurdering(
            fødselsnummer = FNR,
            vilkårsgrunnlagId = vilkårsgrunnlagId,
            skjæringstidspunkt = skjæringstidspunkt,
            unikId = unikId,
            opprettet = opprettet
        ))
        avviksvurderingDao.lagre(avviksvurdering(
            fødselsnummer = FNR,
            vilkårsgrunnlagId = UUID.randomUUID(),
            skjæringstidspunkt = skjæringstidspunkt,
            unikId = unikId,
            opprettet = opprettet
        ))

        val avviksvurderinger = avviksvurderingDao.finnAvviksvurderinger(FNR)

        assertEquals(2, avviksvurderinger.size)
        assertAntallKoblinger(unikId, 2)
        assertAntallAvviksvurderinger(unikId, 1)
    }

    @Test
    fun `samme avviksvurdering med samme vilkårsgrunnlagId`() {
        val skjæringstidspunkt = 1.januar
        val unikId = UUID.randomUUID()
        val vilkårsgrunnlagId = UUID.randomUUID()
        val opprettet = LocalDateTime.now()
        avviksvurderingDao.lagre(avviksvurdering(
            fødselsnummer = FNR,
            vilkårsgrunnlagId = vilkårsgrunnlagId,
            skjæringstidspunkt = skjæringstidspunkt,
            unikId = unikId,
            opprettet = opprettet
        ))
        avviksvurderingDao.lagre(avviksvurdering(
            fødselsnummer = FNR,
            vilkårsgrunnlagId = vilkårsgrunnlagId,
            skjæringstidspunkt = skjæringstidspunkt,
            unikId = unikId,
            opprettet = opprettet
        ))

        val avviksvurderinger = avviksvurderingDao.finnAvviksvurderinger(FNR)

        assertEquals(1, avviksvurderinger.size)
        assertAntallKoblinger(unikId, 1)
        assertAntallAvviksvurderinger(unikId, 1)
    }

    @Test
    fun `forskjellige avviksvurderinger`() {
        val skjæringstidspunkt = 1.januar
        val unikId1 = UUID.randomUUID()
        val unikId2 = UUID.randomUUID()
        val vilkårsgrunnlagId = UUID.randomUUID()
        val opprettet = LocalDateTime.now()
        avviksvurderingDao.lagre(avviksvurdering(
            fødselsnummer = FNR,
            vilkårsgrunnlagId = vilkårsgrunnlagId,
            skjæringstidspunkt = skjæringstidspunkt,
            unikId = unikId1,
            opprettet = opprettet
        ))
        avviksvurderingDao.lagre(avviksvurdering(
            fødselsnummer = FNR,
            vilkårsgrunnlagId = UUID.randomUUID(),
            skjæringstidspunkt = skjæringstidspunkt,
            unikId = unikId2,
            opprettet = opprettet
        ))

        val avviksvurderinger = avviksvurderingDao.finnAvviksvurderinger(FNR)

        assertEquals(2, avviksvurderinger.size)
        assertAntallKoblinger(unikId1, 1)
        assertAntallKoblinger(unikId2, 1)
        assertAntallAvviksvurderinger(unikId1, 1)
        assertAntallAvviksvurderinger(unikId2, 1)
    }


    private fun assertAntallKoblinger(avviksvurderingUnikId: UUID, forventetAntall: Int) {
        @Language("PostgreSQL")
        val query = """select count(1) from vilkarsgrunnlag_per_avviksvurdering where avviksvurdering_ref = :unik_id;"""

        val antall = sessionOf(dataSource).use { session ->
            session.run(queryOf(query, mapOf("unik_id" to avviksvurderingUnikId)).map { it.int(1) }.asSingle)
        }
        assertEquals(forventetAntall, antall)
    }

    private fun assertAntallAvviksvurderinger(avviksvurderingUnikId: UUID, forventetAntall: Int) {
        @Language("PostgreSQL")
        val query = """select count(1) from avviksvurdering where unik_id = :unik_id;"""

        val antall = sessionOf(dataSource).use { session ->
            session.run(queryOf(query, mapOf("unik_id" to avviksvurderingUnikId)).map { it.int(1) }.asSingle)
        }
        assertEquals(forventetAntall, antall)
    }

    private fun forventetAvviksvurdering(fødselsnummer: String, vilkårsgrunnlagId: UUID, skjæringstidspunkt: LocalDate, unikId: UUID, opprettet: LocalDateTime): Avviksvurdering {
        return Avviksvurdering(
            unikId = unikId,
            vilkårsgrunnlagId = vilkårsgrunnlagId,
            fødselsnummer = fødselsnummer,
            skjæringstidspunkt = skjæringstidspunkt,
            opprettet = opprettet,
            avviksprosent = 26.0,
            sammenligningsgrunnlag = sammenligningsgrunnlag(unikId),
            beregningsgrunnlag = beregningsggrunnlag(),
        )
    }

    private fun avviksvurdering(fødselsnummer: String = "12345678910", vilkårsgrunnlagId: UUID = UUID.randomUUID(), skjæringstidspunkt: LocalDate = 1.januar, unikId: UUID = UUID.randomUUID(), opprettet: LocalDateTime = LocalDateTime.now()): AvviksvurderingDto = AvviksvurderingDto(
        unikId = unikId,
        vilkårsgrunnlagId = vilkårsgrunnlagId,
        fødselsnummer = fødselsnummer,
        skjæringstidspunkt = skjæringstidspunkt,
        opprettet = opprettet,
        avviksprosent = 26.0,
        sammenligningsgrunnlag = sammenligningsgrunnlag(unikId),
        beregningsgrunnlag = beregningsggrunnlag(),
    )

    private fun sammenligningsgrunnlag(unikId: UUID = UUID.randomUUID()): SammenligningsgrunnlagDto = SammenligningsgrunnlagDto(
        unikId = unikId,
        totalbeløp = 50000.0,
        innrapporterteInntekter = listOf(innrapportertInntekt())
    )

    private fun innrapportertInntekt(): InnrapportertInntektDto = InnrapportertInntektDto(
        arbeidsgiverreferanse = "000000000",
        inntekter = listOf(inntekt())
    )

    private fun inntekt(): InntektDto = InntektDto(
        årMåned = YearMonth.of(2018, 1),
        beløp = 50000.0
    )

    private fun beregningsggrunnlag(): BeregningsgrunnlagDto = BeregningsgrunnlagDto(
        totalbeløp = 120000.0,
        omregnedeÅrsinntekter = listOf(omregnetÅrsinntekt())
    )

    private fun omregnetÅrsinntekt(): OmregnetÅrsinntektDto = OmregnetÅrsinntektDto(
        arbeidsgiverreferanse = "000000000",
        beløp = 10000.0
    )
}