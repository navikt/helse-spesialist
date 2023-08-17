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
    }

    @Test
    fun `referential equals - AuuVedtak`() {
        val vedtak = Sykepengevedtak.AuuVedtak(
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
            vedtakFattetTidspunkt = vedtakFattetTidspunkt
        )

        assertEquals(vedtak, vedtak)
        assertEquals(vedtak.hashCode(), vedtak.hashCode())
    }

    @Test
    fun `referential equals - Vedtak`() {
        val vedtak = Sykepengevedtak.Vedtak(
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
            sykepengegrunnlagsfakta = sykepengegrunnlagsfakta(Faktatype.ETTER_HOVEDREGEL),
            utbetalingId = utbetalingId,
            begrunnelseFraMal = "Mal",
            begrunnelseFraFritekst = "Fritekst",
            begrunnelseFraKonklusjon = "Konklusjon"
        )

        assertEquals(vedtak, vedtak)
        assertEquals(vedtak.hashCode(), vedtak.hashCode())
    }

    @Test
    fun `structural equals - AuuVedtak`() {
        val vedtak1 = Sykepengevedtak.AuuVedtak(
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
        )
        val vedtak2 = Sykepengevedtak.AuuVedtak(
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
        )

        assertEquals(vedtak1, vedtak2)
        assertEquals(vedtak1.hashCode(), vedtak2.hashCode())
    }

    @Test
    fun `structural equals - Vedtak`() {
        val vedtak1 = Sykepengevedtak.Vedtak(
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
            sykepengegrunnlagsfakta = sykepengegrunnlagsfakta(Faktatype.ETTER_HOVEDREGEL),
            utbetalingId = utbetalingId,
            begrunnelseFraMal = "Mal",
            begrunnelseFraFritekst = "Fritekst",
            begrunnelseFraKonklusjon = "Konklusjon"
        )

        val vedtak2 = Sykepengevedtak.Vedtak(
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
            sykepengegrunnlagsfakta = sykepengegrunnlagsfakta(Faktatype.ETTER_HOVEDREGEL),
            utbetalingId = utbetalingId,
            begrunnelseFraMal = "Mal",
            begrunnelseFraFritekst = "Fritekst",
            begrunnelseFraKonklusjon = "Konklusjon"
        )

        assertEquals(vedtak1, vedtak2)
        assertEquals(vedtak1.hashCode(), vedtak2.hashCode())
    }

    @Test
    fun `not equals - AuuVedtak - fødselsnummer`() {
        val vedtak1 = Sykepengevedtak.AuuVedtak(
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
        )
        val vedtak2 = Sykepengevedtak.AuuVedtak(
            fødselsnummer = "annet fødselsnummer",
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
        )

        assertNotEquals(vedtak1, vedtak2)
        assertNotEquals(vedtak1.hashCode(), vedtak2.hashCode())
    }

    @Test
    fun `not equals - AuuVedtak - aktørId`() {
        val vedtak1 = Sykepengevedtak.AuuVedtak(
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
        )
        val vedtak2 = Sykepengevedtak.AuuVedtak(
            fødselsnummer = fødselsnummer,
            aktørId = "annen aktørId",
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
        )

        assertNotEquals(vedtak1, vedtak2)
        assertNotEquals(vedtak1.hashCode(), vedtak2.hashCode())
    }

    @Test
    fun `not equals - AuuVedtak - vedtaksperiodeId`() {
        val vedtak1 = Sykepengevedtak.AuuVedtak(
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
        )
        val vedtak2 = Sykepengevedtak.AuuVedtak(
            fødselsnummer = fødselsnummer,
            aktørId = aktørId,
            vedtaksperiodeId = UUID.randomUUID(),
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
        )

        assertNotEquals(vedtak1, vedtak2)
        assertNotEquals(vedtak1.hashCode(), vedtak2.hashCode())
    }

    @Test
    fun `not equals - AuuVedtak - organisasjonsnummer`() {
        val vedtak1 = Sykepengevedtak.AuuVedtak(
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
        )
        val vedtak2 = Sykepengevedtak.AuuVedtak(
            fødselsnummer = fødselsnummer,
            aktørId = aktørId,
            vedtaksperiodeId = vedtaksperiodeId,
            organisasjonsnummer = "annet organisasjonsnummer",
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
        )

        assertNotEquals(vedtak1, vedtak2)
        assertNotEquals(vedtak1.hashCode(), vedtak2.hashCode())
    }

    @Test
    fun `not equals - AuuVedtak - fom`() {
        val vedtak1 = Sykepengevedtak.AuuVedtak(
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
        )
        val vedtak2 = Sykepengevedtak.AuuVedtak(
            fødselsnummer = fødselsnummer,
            aktørId = aktørId,
            vedtaksperiodeId = vedtaksperiodeId,
            organisasjonsnummer = organisasjonsnummer,
            fom = LocalDate.now(),
            tom = tom,
            skjæringstidspunkt = skjæringstidspunkt,
            hendelser = hendelser,
            sykepengegrunnlag = sykepengegrunnlag,
            grunnlagForSykepengegrunnlag = grunnlagForSykepengegrunnlag,
            grunnlagForSykepengegrunnlagPerArbeidsgiver = grunnlagForSykepengegrunnlagPerArbeidsgiver,
            begrensning = begrensning,
            inntekt = inntekt,
            vedtakFattetTidspunkt = vedtakFattetTidspunkt,
        )

        assertNotEquals(vedtak1, vedtak2)
        assertNotEquals(vedtak1.hashCode(), vedtak2.hashCode())
    }

    @Test
    fun `not equals - AuuVedtak - tom`() {
        val vedtak1 = Sykepengevedtak.AuuVedtak(
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
        )
        val vedtak2 = Sykepengevedtak.AuuVedtak(
            fødselsnummer = fødselsnummer,
            aktørId = aktørId,
            vedtaksperiodeId = vedtaksperiodeId,
            organisasjonsnummer = organisasjonsnummer,
            fom = fom,
            tom = LocalDate.now(),
            skjæringstidspunkt = skjæringstidspunkt,
            hendelser = hendelser,
            sykepengegrunnlag = sykepengegrunnlag,
            grunnlagForSykepengegrunnlag = grunnlagForSykepengegrunnlag,
            grunnlagForSykepengegrunnlagPerArbeidsgiver = grunnlagForSykepengegrunnlagPerArbeidsgiver,
            begrensning = begrensning,
            inntekt = inntekt,
            vedtakFattetTidspunkt = vedtakFattetTidspunkt,
        )

        assertNotEquals(vedtak1, vedtak2)
        assertNotEquals(vedtak1.hashCode(), vedtak2.hashCode())
    }

    @Test
    fun `not equals - AuuVedtak - skjæringstidspunkt`() {
        val vedtak1 = Sykepengevedtak.AuuVedtak(
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
        )
        val vedtak2 = Sykepengevedtak.AuuVedtak(
            fødselsnummer = fødselsnummer,
            aktørId = aktørId,
            vedtaksperiodeId = vedtaksperiodeId,
            organisasjonsnummer = organisasjonsnummer,
            fom = fom,
            tom = tom,
            skjæringstidspunkt = LocalDate.now(),
            hendelser = hendelser,
            sykepengegrunnlag = sykepengegrunnlag,
            grunnlagForSykepengegrunnlag = grunnlagForSykepengegrunnlag,
            grunnlagForSykepengegrunnlagPerArbeidsgiver = grunnlagForSykepengegrunnlagPerArbeidsgiver,
            begrensning = begrensning,
            inntekt = inntekt,
            vedtakFattetTidspunkt = vedtakFattetTidspunkt,
        )

        assertNotEquals(vedtak1, vedtak2)
        assertNotEquals(vedtak1.hashCode(), vedtak2.hashCode())
    }

    @Test
    fun `not equals - AuuVedtak - hendelser`() {
        val vedtak1 = Sykepengevedtak.AuuVedtak(
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
        )
        val vedtak2 = Sykepengevedtak.AuuVedtak(
            fødselsnummer = fødselsnummer,
            aktørId = aktørId,
            vedtaksperiodeId = vedtaksperiodeId,
            organisasjonsnummer = organisasjonsnummer,
            fom = fom,
            tom = tom,
            skjæringstidspunkt = skjæringstidspunkt,
            hendelser = emptyList(),
            sykepengegrunnlag = sykepengegrunnlag,
            grunnlagForSykepengegrunnlag = grunnlagForSykepengegrunnlag,
            grunnlagForSykepengegrunnlagPerArbeidsgiver = grunnlagForSykepengegrunnlagPerArbeidsgiver,
            begrensning = begrensning,
            inntekt = inntekt,
            vedtakFattetTidspunkt = vedtakFattetTidspunkt,
        )

        assertNotEquals(vedtak1, vedtak2)
        assertNotEquals(vedtak1.hashCode(), vedtak2.hashCode())
    }

    @Test
    fun `not equals - AuuVedtak - sykepengegrunnlag`() {
        val vedtak1 = Sykepengevedtak.AuuVedtak(
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
        )
        val vedtak2 = Sykepengevedtak.AuuVedtak(
            fødselsnummer = fødselsnummer,
            aktørId = aktørId,
            vedtaksperiodeId = vedtaksperiodeId,
            organisasjonsnummer = organisasjonsnummer,
            fom = fom,
            tom = tom,
            skjæringstidspunkt = skjæringstidspunkt,
            hendelser = hendelser,
            sykepengegrunnlag = 0.0,
            grunnlagForSykepengegrunnlag = grunnlagForSykepengegrunnlag,
            grunnlagForSykepengegrunnlagPerArbeidsgiver = grunnlagForSykepengegrunnlagPerArbeidsgiver,
            begrensning = begrensning,
            inntekt = inntekt,
            vedtakFattetTidspunkt = vedtakFattetTidspunkt,
        )

        assertNotEquals(vedtak1, vedtak2)
        assertNotEquals(vedtak1.hashCode(), vedtak2.hashCode())
    }

    @Test
    fun `not equals - AuuVedtak - grunnlagForSykepengegrunnlag`() {
        val vedtak1 = Sykepengevedtak.AuuVedtak(
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
        )
        val vedtak2 = Sykepengevedtak.AuuVedtak(
            fødselsnummer = fødselsnummer,
            aktørId = aktørId,
            vedtaksperiodeId = vedtaksperiodeId,
            organisasjonsnummer = organisasjonsnummer,
            fom = fom,
            tom = tom,
            skjæringstidspunkt = skjæringstidspunkt,
            hendelser = hendelser,
            sykepengegrunnlag = sykepengegrunnlag,
            grunnlagForSykepengegrunnlag = 0.0,
            grunnlagForSykepengegrunnlagPerArbeidsgiver = grunnlagForSykepengegrunnlagPerArbeidsgiver,
            begrensning = begrensning,
            inntekt = inntekt,
            vedtakFattetTidspunkt = vedtakFattetTidspunkt,
        )

        assertNotEquals(vedtak1, vedtak2)
        assertNotEquals(vedtak1.hashCode(), vedtak2.hashCode())
    }

    @Test
    fun `not equals - AuuVedtak - grunnlagForSykepengegrunnlagPerArbeidsgiver`() {
        val vedtak1 = Sykepengevedtak.AuuVedtak(
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
        )
        val vedtak2 = Sykepengevedtak.AuuVedtak(
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
            grunnlagForSykepengegrunnlagPerArbeidsgiver = emptyMap(),
            begrensning = begrensning,
            inntekt = inntekt,
            vedtakFattetTidspunkt = vedtakFattetTidspunkt,
        )

        assertNotEquals(vedtak1, vedtak2)
        assertNotEquals(vedtak1.hashCode(), vedtak2.hashCode())
    }

    @Test
    fun `not equals - AuuVedtak - begrensning`() {
        val vedtak1 = Sykepengevedtak.AuuVedtak(
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
        )
        val vedtak2 = Sykepengevedtak.AuuVedtak(
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
            begrensning = "ANNEN BEGRENSNING",
            inntekt = inntekt,
            vedtakFattetTidspunkt = vedtakFattetTidspunkt,
        )

        assertNotEquals(vedtak1, vedtak2)
        assertNotEquals(vedtak1.hashCode(), vedtak2.hashCode())
    }

    @Test
    fun `not equals - AuuVedtak - inntekt`() {
        val vedtak1 = Sykepengevedtak.AuuVedtak(
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
        )
        val vedtak2 = Sykepengevedtak.AuuVedtak(
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
            inntekt = 0.0,
            vedtakFattetTidspunkt = vedtakFattetTidspunkt,
        )

        assertNotEquals(vedtak1, vedtak2)
        assertNotEquals(vedtak1.hashCode(), vedtak2.hashCode())
    }

    @Test
    fun `not equals - AuuVedtak - vedtakFattetTidspunkt`() {
        val vedtak1 = Sykepengevedtak.AuuVedtak(
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
        )
        val vedtak2 = Sykepengevedtak.AuuVedtak(
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
            vedtakFattetTidspunkt = LocalDateTime.now().minusDays(1),
        )

        assertNotEquals(vedtak1, vedtak2)
        assertNotEquals(vedtak1.hashCode(), vedtak2.hashCode())
    }

    @Test
    fun `not equals - Vedtak - sykepengegrunnlagsfakta`() {
        val vedtak1 = Sykepengevedtak.Vedtak(
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
            sykepengegrunnlagsfakta = sykepengegrunnlagsfakta(Faktatype.ETTER_HOVEDREGEL),
            utbetalingId = utbetalingId,
            begrunnelseFraMal = "Mal",
            begrunnelseFraFritekst = "Fritekst",
            begrunnelseFraKonklusjon = "Konklusjon"
        )

        val vedtak2 = Sykepengevedtak.Vedtak(
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
            sykepengegrunnlagsfakta = sykepengegrunnlagsfakta(Faktatype.ETTER_SKJØNN),
            utbetalingId = utbetalingId,
            begrunnelseFraMal = "Mal",
            begrunnelseFraFritekst = "Fritekst",
            begrunnelseFraKonklusjon = "Konklusjon"
        )

        assertNotEquals(vedtak1, vedtak2)
        assertNotEquals(vedtak1.hashCode(), vedtak2.hashCode())
    }

    @Test
    fun `not equals - Vedtak - utbetalingId`() {
        val vedtak1 = Sykepengevedtak.Vedtak(
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
            sykepengegrunnlagsfakta = sykepengegrunnlagsfakta(Faktatype.ETTER_HOVEDREGEL),
            utbetalingId = utbetalingId,
            begrunnelseFraMal = "Mal",
            begrunnelseFraFritekst = "Fritekst",
            begrunnelseFraKonklusjon = "Konklusjon"
        )

        val vedtak2 = Sykepengevedtak.Vedtak(
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
            sykepengegrunnlagsfakta = sykepengegrunnlagsfakta(Faktatype.ETTER_HOVEDREGEL),
            utbetalingId = UUID.randomUUID(),
            begrunnelseFraMal = "Mal",
            begrunnelseFraFritekst = "Fritekst",
            begrunnelseFraKonklusjon = "Konklusjon"
        )

        assertNotEquals(vedtak1, vedtak2)
        assertNotEquals(vedtak1.hashCode(), vedtak2.hashCode())
    }

    private fun sykepengegrunnlagsfakta(faktatype: Faktatype): Sykepengegrunnlagsfakta {
        return when(faktatype) {
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