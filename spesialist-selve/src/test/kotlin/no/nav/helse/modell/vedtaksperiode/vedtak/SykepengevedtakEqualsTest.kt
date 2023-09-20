package no.nav.helse.modell.vedtaksperiode.vedtak

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.januar
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test

class SykepengevedtakEqualsTest {
    private companion object {
        private const val fødselsnummer = "12345678910"
        private const val aktørId = "1234567891011"
        private const val organisasjonsnummer = "123456789"
        private val vedtaksperiodeId = UUID.randomUUID()
        private val utbetalingId = UUID.randomUUID()
        private val fom = 1.januar
        private val tom = 31.januar
        private val skjæringstidspunkt = 1.januar
        private val hendelser = listOf(UUID.randomUUID(), UUID.randomUUID())
        private const val sykepengegrunnlag = 600000.00
        private const val grunnlagForSykepengegrunnlag = 600000.00
        private val grunnlagForSykepengegrunnlagPerArbeidsgiver = mapOf(
            organisasjonsnummer to 300000.00,
            "987654321" to 300000.00
        )
        private const val begrensning = "ER_6G_BEGRENSET"
        private const val inntekt = 25000.00
        private val vedtakFattetTidspunkt = LocalDateTime.now()
        private const val omregnetÅrsinntekt = 300000.00
        private const val innrapportertÅrsinntekt = 300000.00
        private const val avviksprosent = 0.0
        private const val seksG2023 = 6 * 118620.0
        private val tags = listOf("IngenNyArbeidsgiverperiode")
    }

    @Test
    fun `referential equals - AuuVedtak`() {
        val vedtak = auuVedtak()

        assertEquals(vedtak, vedtak)
        assertEquals(vedtak.hashCode(), vedtak.hashCode())
    }

    @Test
    fun `referential equals - Vedtak`() {
        val vedtak = vedtak(
            sykepengegrunnlagsfakta = sykepengegrunnlagsfakta(Faktatype.ETTER_HOVEDREGEL)
        )

        assertEquals(vedtak, vedtak)
        assertEquals(vedtak.hashCode(), vedtak.hashCode())
    }

    @Test
    fun `structural equals - AuuVedtak`() {
        val vedtak1 = auuVedtak()
        val vedtak2 = auuVedtak()

        assertEquals(vedtak1, vedtak2)
        assertEquals(vedtak1.hashCode(), vedtak2.hashCode())
    }

    @Test
    fun `structural equals - Vedtak`() {
        val vedtak1 = vedtak(
            sykepengegrunnlagsfakta = sykepengegrunnlagsfakta(Faktatype.ETTER_HOVEDREGEL)
        )

        val vedtak2 = vedtak(
            sykepengegrunnlagsfakta = sykepengegrunnlagsfakta(Faktatype.ETTER_HOVEDREGEL)
        )

        assertEquals(vedtak1, vedtak2)
        assertEquals(vedtak1.hashCode(), vedtak2.hashCode())
    }

    @Test
    fun `not equals - AuuVedtak - fødselsnummer`() {
        val vedtak1 = auuVedtak(fødselsnummer = "fødselsnummer")
        val vedtak2 = auuVedtak(fødselsnummer = "annet fødselsnummer")

        assertNotEquals(vedtak1, vedtak2)
        assertNotEquals(vedtak1.hashCode(), vedtak2.hashCode())
    }

    @Test
    fun `not equals - AuuVedtak - aktørId`() {
        val vedtak1 = auuVedtak(aktørId = "aktørId")
        val vedtak2 = auuVedtak(aktørId = "annen aktørId")

        assertNotEquals(vedtak1, vedtak2)
        assertNotEquals(vedtak1.hashCode(), vedtak2.hashCode())
    }

    @Test
    fun `not equals - AuuVedtak - vedtaksperiodeId`() {
        val vedtak1 = auuVedtak(vedtaksperiodeId = UUID.randomUUID())
        val vedtak2 = auuVedtak(vedtaksperiodeId = UUID.randomUUID())

        assertNotEquals(vedtak1, vedtak2)
        assertNotEquals(vedtak1.hashCode(), vedtak2.hashCode())
    }

    @Test
    fun `not equals - AuuVedtak - organisasjonsnummer`() {
        val vedtak1 = auuVedtak(organisasjonsnummer = "organisasjonsnummer")
        val vedtak2 = auuVedtak(organisasjonsnummer = "annet organisasjonsnummer")

        assertNotEquals(vedtak1, vedtak2)
        assertNotEquals(vedtak1.hashCode(), vedtak2.hashCode())
    }

    @Test
    fun `not equals - AuuVedtak - fom`() {
        val vedtak1 = auuVedtak(fom = LocalDate.now())
        val vedtak2 = auuVedtak(fom = LocalDate.now().plusDays(1))

        assertNotEquals(vedtak1, vedtak2)
        assertNotEquals(vedtak1.hashCode(), vedtak2.hashCode())
    }

    @Test
    fun `not equals - AuuVedtak - tom`() {
        val vedtak1 = auuVedtak(tom = LocalDate.now())
        val vedtak2 = auuVedtak(tom = LocalDate.now().plusDays(1))

        assertNotEquals(vedtak1, vedtak2)
        assertNotEquals(vedtak1.hashCode(), vedtak2.hashCode())
    }

    @Test
    fun `not equals - AuuVedtak - skjæringstidspunkt`() {
        val vedtak1 = auuVedtak(skjæringstidspunkt = LocalDate.now())
        val vedtak2 = auuVedtak(skjæringstidspunkt = LocalDate.now().plusDays(1))

        assertNotEquals(vedtak1, vedtak2)
        assertNotEquals(vedtak1.hashCode(), vedtak2.hashCode())
    }

    @Test
    fun `not equals - AuuVedtak - hendelser`() {
        val vedtak1 = auuVedtak(hendelser = listOf(UUID.randomUUID()))
        val vedtak2 = auuVedtak(hendelser = listOf(UUID.randomUUID()))

        assertNotEquals(vedtak1, vedtak2)
        assertNotEquals(vedtak1.hashCode(), vedtak2.hashCode())
    }

    @Test
    fun `not equals - AuuVedtak - sykepengegrunnlag`() {
        val vedtak1 = auuVedtak(sykepengegrunnlag = 1.0)
        val vedtak2 = auuVedtak(sykepengegrunnlag = 0.0)

        assertNotEquals(vedtak1, vedtak2)
        assertNotEquals(vedtak1.hashCode(), vedtak2.hashCode())
    }

    @Test
    fun `not equals - AuuVedtak - grunnlagForSykepengegrunnlag`() {
        val vedtak1 = auuVedtak(grunnlagForSykepengegrunnlag = 1.0)
        val vedtak2 = auuVedtak(grunnlagForSykepengegrunnlag = 0.0)

        assertNotEquals(vedtak1, vedtak2)
        assertNotEquals(vedtak1.hashCode(), vedtak2.hashCode())
    }

    @Test
    fun `not equals - AuuVedtak - grunnlagForSykepengegrunnlagPerArbeidsgiver`() {
        val vedtak1 = auuVedtak(grunnlagForSykepengegrunnlagPerArbeidsgiver = mapOf("organisasjonsnummer" to 1.0))
        val vedtak2 = auuVedtak(grunnlagForSykepengegrunnlagPerArbeidsgiver = mapOf("annet organisasjonsnummer" to 0.0))

        assertNotEquals(vedtak1, vedtak2)
        assertNotEquals(vedtak1.hashCode(), vedtak2.hashCode())
    }

    @Test
    fun `not equals - AuuVedtak - begrensning`() {
        val vedtak1 = auuVedtak(begrensning = "BEGRENSNING")
        val vedtak2 = auuVedtak(begrensning = "ANNEN BEGRENSNING")

        assertNotEquals(vedtak1, vedtak2)
        assertNotEquals(vedtak1.hashCode(), vedtak2.hashCode())
    }

    @Test
    fun `not equals - AuuVedtak - inntekt`() {
        val vedtak1 = auuVedtak(inntekt = 1.0)
        val vedtak2 = auuVedtak(inntekt = 0.0)

        assertNotEquals(vedtak1, vedtak2)
        assertNotEquals(vedtak1.hashCode(), vedtak2.hashCode())
    }

    @Test
    fun `not equals - AuuVedtak - vedtakFattetTidspunkt`() {
        val vedtak1 = auuVedtak(vedtakFattetTidspunkt = LocalDateTime.now())
        val vedtak2 = auuVedtak(vedtakFattetTidspunkt = LocalDateTime.now().plusDays(1))

        assertNotEquals(vedtak1, vedtak2)
        assertNotEquals(vedtak1.hashCode(), vedtak2.hashCode())
    }

    @Test
    fun `not equals - Vedtak - sykepengegrunnlagsfakta`() {
        val vedtak1 = vedtak(sykepengegrunnlagsfakta = sykepengegrunnlagsfakta(Faktatype.ETTER_HOVEDREGEL))
        val vedtak2 = vedtak(sykepengegrunnlagsfakta = sykepengegrunnlagsfakta(Faktatype.ETTER_SKJØNN))

        assertNotEquals(vedtak1, vedtak2)
        assertNotEquals(vedtak1.hashCode(), vedtak2.hashCode())
    }

    @Test
    fun `not equals - Vedtak - utbetalingId`() {
        val vedtak1 = vedtak(
            sykepengegrunnlagsfakta = sykepengegrunnlagsfakta(Faktatype.ETTER_HOVEDREGEL),
            utbetalingId = UUID.randomUUID(),
        )

        val vedtak2 = vedtak(
            sykepengegrunnlagsfakta = sykepengegrunnlagsfakta(Faktatype.ETTER_HOVEDREGEL),
            utbetalingId = UUID.randomUUID(),
        )

        assertNotEquals(vedtak1, vedtak2)
        assertNotEquals(vedtak1.hashCode(), vedtak2.hashCode())
    }

    private fun auuVedtak(
        fødselsnummer: String = Companion.fødselsnummer,
        aktørId: String = Companion.aktørId,
        vedtaksperiodeId: UUID = Companion.vedtaksperiodeId,
        organisasjonsnummer: String = Companion.organisasjonsnummer,
        fom: LocalDate = Companion.fom,
        tom: LocalDate = Companion.tom,
        skjæringstidspunkt: LocalDate = Companion.skjæringstidspunkt,
        hendelser: List<UUID> = Companion.hendelser,
        sykepengegrunnlag: Double = Companion.sykepengegrunnlag,
        grunnlagForSykepengegrunnlag: Double = Companion.grunnlagForSykepengegrunnlag,
        grunnlagForSykepengegrunnlagPerArbeidsgiver: Map<String, Double> = Companion.grunnlagForSykepengegrunnlagPerArbeidsgiver,
        begrensning: String = Companion.begrensning,
        inntekt: Double = Companion.inntekt,
        vedtakFattetTidspunkt: LocalDateTime = Companion.vedtakFattetTidspunkt,
        tags: List<String> = Companion.tags,
    ) = Sykepengevedtak.AuuVedtak(
        fødselsnummer = fødselsnummer,
        aktørId = aktørId,
        vedtaksperiodeId = vedtaksperiodeId,
        organisasjonsnummer = organisasjonsnummer,
        fom = fom,
        tom = tom,
        skjæringstidspunkt = skjæringstidspunkt,
        hendelser = hendelser,
        sykepengegrunnlag = sykepengegrunnlag,
        grunnlagForSykepengegrunnlag = grunnlagForSykepengegrunnlag,
        grunnlagForSykepengegrunnlagPerArbeidsgiver = grunnlagForSykepengegrunnlagPerArbeidsgiver,
        begrensning = begrensning,
        inntekt = inntekt,
        vedtakFattetTidspunkt = vedtakFattetTidspunkt,
        tags = tags,
    )

    private fun vedtak(
        fødselsnummer: String = Companion.fødselsnummer,
        aktørId: String = Companion.aktørId,
        vedtaksperiodeId: UUID = Companion.vedtaksperiodeId,
        organisasjonsnummer: String = Companion.organisasjonsnummer,
        fom: LocalDate = Companion.fom,
        tom: LocalDate = Companion.tom,
        skjæringstidspunkt: LocalDate = Companion.skjæringstidspunkt,
        hendelser: List<UUID> = Companion.hendelser,
        sykepengegrunnlag: Double = Companion.sykepengegrunnlag,
        grunnlagForSykepengegrunnlag: Double = Companion.grunnlagForSykepengegrunnlag,
        grunnlagForSykepengegrunnlagPerArbeidsgiver: Map<String, Double> = Companion.grunnlagForSykepengegrunnlagPerArbeidsgiver,
        begrensning: String = Companion.begrensning,
        inntekt: Double = Companion.inntekt,
        vedtakFattetTidspunkt: LocalDateTime = Companion.vedtakFattetTidspunkt,
        sykepengegrunnlagsfakta: Sykepengegrunnlagsfakta,
        utbetalingId: UUID = Companion.utbetalingId,
        begrunnelseFraMal: String? = null,
        begrunnelseFraFritekst: String? = null,
        begrunnelseFraKonklusjon: String? = null,
    ) = Sykepengevedtak.Vedtak(
        fødselsnummer = fødselsnummer,
        aktørId = aktørId,
        vedtaksperiodeId = vedtaksperiodeId,
        organisasjonsnummer = organisasjonsnummer,
        fom = fom,
        tom = tom,
        skjæringstidspunkt = skjæringstidspunkt,
        hendelser = hendelser,
        sykepengegrunnlag = sykepengegrunnlag,
        grunnlagForSykepengegrunnlag = grunnlagForSykepengegrunnlag,
        grunnlagForSykepengegrunnlagPerArbeidsgiver = grunnlagForSykepengegrunnlagPerArbeidsgiver,
        begrensning = begrensning,
        inntekt = inntekt,
        sykepengegrunnlagsfakta = sykepengegrunnlagsfakta,
        utbetalingId = utbetalingId,
        begrunnelseFraMal = begrunnelseFraMal,
        begrunnelseFraFritekst = begrunnelseFraFritekst,
        begrunnelseFraKonklusjon = begrunnelseFraKonklusjon,
        vedtakFattetTidspunkt = vedtakFattetTidspunkt,
        tags = tags,
    )

    private fun sykepengegrunnlagsfakta(faktatype: Faktatype): Sykepengegrunnlagsfakta {
        return when (faktatype) {
            Faktatype.ETTER_SKJØNN -> Sykepengegrunnlagsfakta.Spleis.EtterSkjønn(
                omregnetÅrsinntekt = omregnetÅrsinntekt,
                innrapportertÅrsinntekt = innrapportertÅrsinntekt,
                avviksprosent = avviksprosent,
                seksG = seksG2023,
                skjønnsfastsatt = 650000.0,
                tags = emptyList(),
                arbeidsgivere = listOf(
                    Sykepengegrunnlagsfakta.Spleis.Arbeidsgiver.EtterSkjønn(organisasjonsnummer, 300000.0, 325000.0),
                    Sykepengegrunnlagsfakta.Spleis.Arbeidsgiver.EtterSkjønn("987654321", 300000.0, 325000.0)
                )
            )

            Faktatype.ETTER_HOVEDREGEL -> Sykepengegrunnlagsfakta.Spleis.EtterHovedregel(
                omregnetÅrsinntekt = omregnetÅrsinntekt,
                innrapportertÅrsinntekt = innrapportertÅrsinntekt,
                avviksprosent = avviksprosent,
                seksG = seksG2023,
                tags = emptyList(),
                arbeidsgivere = listOf(
                    Sykepengegrunnlagsfakta.Spleis.Arbeidsgiver.EtterHovedregel(organisasjonsnummer, 300000.0),
                    Sykepengegrunnlagsfakta.Spleis.Arbeidsgiver.EtterHovedregel("987654321", 300000.0)
                )
            )

            Faktatype.I_INFOTRYGD -> Sykepengegrunnlagsfakta.Infotrygd(
                omregnetÅrsinntekt = omregnetÅrsinntekt,
            )
        }
    }
}
