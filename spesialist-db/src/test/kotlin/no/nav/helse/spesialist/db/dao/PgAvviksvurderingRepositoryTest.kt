package no.nav.helse.spesialist.db.dao

import no.nav.helse.modell.vilkårsprøving.Avviksvurdering
import no.nav.helse.modell.vilkårsprøving.Beregningsgrunnlag
import no.nav.helse.modell.vilkårsprøving.InnrapportertInntekt
import no.nav.helse.modell.vilkårsprøving.Inntekt
import no.nav.helse.modell.vilkårsprøving.OmregnetÅrsinntekt
import no.nav.helse.modell.vilkårsprøving.Sammenligningsgrunnlag
import no.nav.helse.spesialist.db.AbstractDBIntegrationTest
import no.nav.helse.spesialist.db.DBSessionContext
import no.nav.helse.spesialist.domain.testfixtures.jan
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagFødselsnummer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.UUID

internal class PgAvviksvurderingRepositoryTest : AbstractDBIntegrationTest() {
    private val fødselsnummer = lagFødselsnummer()
    private val avviksvurderingRepository = DBSessionContext(session).avviksvurderingRepository

    @Test
    fun `lagre avviksvurdering`() {
        val avviksvurderingId = UUID.randomUUID()
        val vilkårsgrunnlagId = UUID.randomUUID()

        avviksvurderingRepository.lagre(
            avviksvurdering(
                fødselsnummer = fødselsnummer,
                unikId = avviksvurderingId,
            ),
        )
        avviksvurderingRepository.opprettKobling(avviksvurderingId, vilkårsgrunnlagId)
        assertNotNull(avviksvurderingRepository.hentAvviksvurderingFor(avviksvurderingId))
    }

    @Test
    fun `finner avviksvurderinger basert på fødselsnummer`() {
        val skjæringstidspunkt = 1 jan 2018
        val unikId = UUID.randomUUID()
        val vilkårsgrunnlagId = UUID.randomUUID()
        val opprettet = LocalDateTime.now().withNano(0)
        avviksvurderingRepository.lagre(
            avviksvurdering(
                fødselsnummer = fødselsnummer,
                skjæringstidspunkt = skjæringstidspunkt,
                unikId = unikId,
                opprettet = opprettet,
            ),
        )
        avviksvurderingRepository.opprettKobling(unikId, vilkårsgrunnlagId)

        val avviksvurderinger = avviksvurderingRepository.finnAvviksvurderinger(fødselsnummer)
        val forventetAvviksvurdering =
            forventetAvviksvurdering(
                vilkårsgrunnlagId = vilkårsgrunnlagId,
                fødselsnummer = fødselsnummer,
                skjæringstidspunkt = skjæringstidspunkt,
                unikId = unikId,
                opprettet = opprettet,
            )
        assertEquals(forventetAvviksvurdering, avviksvurderinger.first())
    }

    @Test
    fun `samme avviksvurdering med forskjellig vilkårsgrunnlagId`() {
        val skjæringstidspunkt = 1 jan 2018
        val unikId = UUID.randomUUID()
        val vilkårsgrunnlagId1 = UUID.randomUUID()
        val vilkårsgrunnlagId2 = UUID.randomUUID()
        val opprettet = LocalDateTime.now()
        avviksvurderingRepository.lagre(
            avviksvurdering(
                fødselsnummer = fødselsnummer,
                skjæringstidspunkt = skjæringstidspunkt,
                unikId = unikId,
                opprettet = opprettet,
            ),
        )
        avviksvurderingRepository.opprettKobling(unikId, vilkårsgrunnlagId1)

        avviksvurderingRepository.lagre(
            avviksvurdering(
                fødselsnummer = fødselsnummer,
                skjæringstidspunkt = skjæringstidspunkt,
                unikId = unikId,
                opprettet = opprettet,
            ),
        )
        avviksvurderingRepository.opprettKobling(unikId, vilkårsgrunnlagId2)

        val avviksvurderinger = avviksvurderingRepository.finnAvviksvurderinger(fødselsnummer)

        assertEquals(2, avviksvurderinger.size)
        assertAntallKoblinger(unikId, 2)
        assertAntallAvviksvurderinger(unikId, 1)
    }

    @Test
    fun `samme avviksvurdering med samme vilkårsgrunnlagId`() {
        val skjæringstidspunkt = 1 jan 2018
        val unikId = UUID.randomUUID()
        val vilkårsgrunnlagId = UUID.randomUUID()
        val opprettet = LocalDateTime.now()
        avviksvurderingRepository.lagre(
            avviksvurdering(
                fødselsnummer = fødselsnummer,
                skjæringstidspunkt = skjæringstidspunkt,
                unikId = unikId,
                opprettet = opprettet,
            ),
        )
        avviksvurderingRepository.opprettKobling(unikId, vilkårsgrunnlagId)

        avviksvurderingRepository.lagre(
            avviksvurdering(
                fødselsnummer = fødselsnummer,
                skjæringstidspunkt = skjæringstidspunkt,
                unikId = unikId,
                opprettet = opprettet,
            ),
        )
        avviksvurderingRepository.opprettKobling(unikId, vilkårsgrunnlagId)

        val avviksvurderinger = avviksvurderingRepository.finnAvviksvurderinger(fødselsnummer)

        assertEquals(1, avviksvurderinger.size)
        assertAntallKoblinger(unikId, 1)
        assertAntallAvviksvurderinger(unikId, 1)
    }

    @Test
    fun `forskjellige avviksvurderinger`() {
        val skjæringstidspunkt = 1 jan 2018
        val unikId1 = UUID.randomUUID()
        val unikId2 = UUID.randomUUID()
        val vilkårsgrunnlagId1 = UUID.randomUUID()
        val vilkårsgrunnlagId2 = UUID.randomUUID()
        val opprettet = LocalDateTime.now()
        avviksvurderingRepository.lagre(
            avviksvurdering(
                fødselsnummer = fødselsnummer,
                skjæringstidspunkt = skjæringstidspunkt,
                unikId = unikId1,
                opprettet = opprettet,
            ),
        )
        avviksvurderingRepository.opprettKobling(unikId1, vilkårsgrunnlagId1)

        avviksvurderingRepository.lagre(
            avviksvurdering(
                fødselsnummer = fødselsnummer,
                skjæringstidspunkt = skjæringstidspunkt,
                unikId = unikId2,
                opprettet = opprettet,
            ),
        )
        avviksvurderingRepository.opprettKobling(unikId2, vilkårsgrunnlagId2)

        val avviksvurderinger = avviksvurderingRepository.finnAvviksvurderinger(fødselsnummer)

        assertEquals(2, avviksvurderinger.size)
        assertAntallKoblinger(unikId1, 1)
        assertAntallKoblinger(unikId2, 1)
        assertAntallAvviksvurderinger(unikId1, 1)
        assertAntallAvviksvurderinger(unikId2, 1)
    }

    @Test
    fun `opprettes kobling hvis vilkårsgrunnlagId ikke er null`() {
        val skjæringstidspunkt = 1 jan 2018
        val unikId = UUID.randomUUID()
        val vilkårsgrunnlagId = UUID.randomUUID()
        val opprettet = LocalDateTime.now()
        avviksvurderingRepository.lagre(
            avviksvurdering(
                fødselsnummer = fødselsnummer,
                vilkårsgrunnlagId = vilkårsgrunnlagId,
                skjæringstidspunkt = skjæringstidspunkt,
                unikId = unikId,
                opprettet = opprettet,
            ),
        )

        val avviksvurderinger = avviksvurderingRepository.finnAvviksvurderinger(fødselsnummer)

        assertEquals(1, avviksvurderinger.size)
        assertAntallKoblinger(unikId, 1)
        assertAntallAvviksvurderinger(unikId, 1)
    }

    @Test
    fun `opprettes ikke kobling hvis vilkårsgrunnlagId er null`() {
        val skjæringstidspunkt = 1 jan 2018
        val unikId = UUID.randomUUID()
        val opprettet = LocalDateTime.now()
        avviksvurderingRepository.lagre(
            avviksvurdering(
                fødselsnummer = fødselsnummer,
                vilkårsgrunnlagId = null,
                skjæringstidspunkt = skjæringstidspunkt,
                unikId = unikId,
                opprettet = opprettet,
            ),
        )

        assertAntallKoblinger(unikId, 0)
    }

    @Test
    fun `ignorerer slettede avviksvurderinger`() {
        val unikId = UUID.randomUUID()
        avviksvurderingRepository.lagre(
            avviksvurdering(
                fødselsnummer = fødselsnummer,
                vilkårsgrunnlagId = UUID.randomUUID(),
                skjæringstidspunkt = 1 jan 2018,
                unikId = unikId,
                opprettet = LocalDateTime.now(),
            ),
        )

        val antallAvviksvurderinger = avviksvurderingRepository.finnAvviksvurderinger(fødselsnummer).size
        assertEquals(1, antallAvviksvurderinger)
        slettAvviksvurdering(unikId)
        val antallAvviksvurderingerEtterSletting = avviksvurderingRepository.finnAvviksvurderinger(fødselsnummer).size
        assertEquals(0, antallAvviksvurderingerEtterSletting)
    }

    private fun assertAntallKoblinger(
        avviksvurderingUnikId: UUID,
        forventetAntall: Int,
    ) {
        val antall =
            dbQuery.single(
                "select count(*) from vilkarsgrunnlag_per_avviksvurdering where avviksvurdering_ref = :unik_id",
                "unik_id" to avviksvurderingUnikId,
            ) { it.int(1) }
        assertEquals(forventetAntall, antall)
    }

    private fun assertAntallAvviksvurderinger(
        avviksvurderingUnikId: UUID,
        @Suppress("SameParameterValue") forventetAntall: Int,
    ) {
        val antall =
            dbQuery.single(
                "select count(*) from avviksvurdering where unik_id = :unik_id",
                "unik_id" to avviksvurderingUnikId,
            ) { it.int(1) }
        assertEquals(forventetAntall, antall)
    }

    private fun slettAvviksvurdering(avviksvurderingUnikId: UUID) {
        dbQuery.update(
            "update avviksvurdering set slettet = now() where unik_id = :unik_id",
            "unik_id" to avviksvurderingUnikId,
        )
    }

    private fun forventetAvviksvurdering(
        fødselsnummer: String,
        skjæringstidspunkt: LocalDate,
        unikId: UUID,
        vilkårsgrunnlagId: UUID,
        opprettet: LocalDateTime,
    ): Avviksvurdering =
        Avviksvurdering(
            unikId = unikId,
            vilkårsgrunnlagId = vilkårsgrunnlagId,
            fødselsnummer = fødselsnummer,
            skjæringstidspunkt = skjæringstidspunkt,
            opprettet = opprettet,
            avviksprosent = 26.0,
            sammenligningsgrunnlag = sammenligningsgrunnlag(),
            beregningsgrunnlag = beregningsggrunnlag(),
        )

    private fun avviksvurdering(
        fødselsnummer: String = "12345678910",
        skjæringstidspunkt: LocalDate = 1 jan 2018,
        unikId: UUID = UUID.randomUUID(),
        vilkårsgrunnlagId: UUID? = null,
        opprettet: LocalDateTime = LocalDateTime.now(),
    ): Avviksvurdering =
        Avviksvurdering(
            unikId = unikId,
            vilkårsgrunnlagId = vilkårsgrunnlagId,
            fødselsnummer = fødselsnummer,
            skjæringstidspunkt = skjæringstidspunkt,
            opprettet = opprettet,
            avviksprosent = 26.0,
            sammenligningsgrunnlag = sammenligningsgrunnlag(),
            beregningsgrunnlag = beregningsggrunnlag(),
        )

    private fun sammenligningsgrunnlag(): Sammenligningsgrunnlag =
        Sammenligningsgrunnlag(
            totalbeløp = 50000.0,
            innrapporterteInntekter = listOf(innrapportertInntekt()),
        )

    private fun innrapportertInntekt(): InnrapportertInntekt =
        InnrapportertInntekt(
            arbeidsgiverreferanse = "000000000",
            inntekter = listOf(inntekt()),
        )

    private fun inntekt(): Inntekt =
        Inntekt(
            årMåned = YearMonth.of(2018, 1),
            beløp = 50000.0,
        )

    private fun beregningsggrunnlag(): Beregningsgrunnlag =
        Beregningsgrunnlag(
            totalbeløp = 120000.0,
            omregnedeÅrsinntekter = listOf(omregnetÅrsinntekt()),
        )

    private fun omregnetÅrsinntekt(): OmregnetÅrsinntekt =
        OmregnetÅrsinntekt(
            arbeidsgiverreferanse = "000000000",
            beløp = 10000.0,
        )
}
