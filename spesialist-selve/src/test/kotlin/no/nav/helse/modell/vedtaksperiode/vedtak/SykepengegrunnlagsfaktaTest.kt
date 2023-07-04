package no.nav.helse.modell.vedtaksperiode.vedtak

import no.nav.helse.modell.vedtaksperiode.vedtak.Sykepengegrunnlagsfakta.Spleis
import no.nav.helse.modell.vedtaksperiode.vedtak.Sykepengegrunnlagsfakta.Spleis.Arbeidsgiver
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test

class SykepengegrunnlagsfaktaTest {

    @Test
    fun `referential equals - EtterSkjønn`() {
        val sykepengegrunnlagsfakta = Spleis.EtterSkjønn(
            arbeidsgivere = listOf(Arbeidsgiver.EtterSkjønn("organisasjonsnummer", 500000.0, 600000.0)),
            omregnetÅrsinntekt = 500000.0,
            innrapportertÅrsinntekt = 500000.0,
            avviksprosent = 25.0,
            seksG = 6 * 118620,
            skjønnsfastsatt = 600000.0,
            tags = listOf("TAG")
        )

        assertEquals(sykepengegrunnlagsfakta, sykepengegrunnlagsfakta)
        assertEquals(sykepengegrunnlagsfakta.hashCode(), sykepengegrunnlagsfakta.hashCode())
    }

    @Test
    fun `referential equals - EtterHovedregel`() {
        val sykepengegrunnlagsfakta = Spleis.EtterHovedregel(
            arbeidsgivere = listOf(Arbeidsgiver.EtterHovedregel("organisasjonsnummer", 500000.0)),
            omregnetÅrsinntekt = 500000.0,
            innrapportertÅrsinntekt = 500000.0,
            avviksprosent = 25.0,
            seksG = 6 * 118620,
            tags = listOf("TAG")
        )

        assertEquals(sykepengegrunnlagsfakta, sykepengegrunnlagsfakta)
        assertEquals(sykepengegrunnlagsfakta.hashCode(), sykepengegrunnlagsfakta.hashCode())
    }

    @Test
    fun `referential equals - Infotrygd`() {
        val sykepengegrunnlagsfakta = Sykepengegrunnlagsfakta.Infotrygd(omregnetÅrsinntekt = 500000.0)

        assertEquals(sykepengegrunnlagsfakta, sykepengegrunnlagsfakta)
        assertEquals(sykepengegrunnlagsfakta.hashCode(), sykepengegrunnlagsfakta.hashCode())
    }

    @Test
    fun `structural equals - EtterSkjønn`() {
        val sykepengegrunnlagsfakta1 = Spleis.EtterSkjønn(
            arbeidsgivere = listOf(Arbeidsgiver.EtterSkjønn("organisasjonsnummer", 500000.0, 600000.0)),
            omregnetÅrsinntekt = 500000.0,
            innrapportertÅrsinntekt = 500000.0,
            avviksprosent = 25.0,
            seksG = 6 * 118620,
            skjønnsfastsatt = 600000.0,
            tags = listOf("TAG")
        )
        val sykepengegrunnlagsfakta2 = Spleis.EtterSkjønn(
            arbeidsgivere = listOf(Arbeidsgiver.EtterSkjønn("organisasjonsnummer", 500000.0, 600000.0)),
            omregnetÅrsinntekt = 500000.0,
            innrapportertÅrsinntekt = 500000.0,
            avviksprosent = 25.0,
            seksG = 6 * 118620,
            skjønnsfastsatt = 600000.0,
            tags = listOf("TAG")
        )

        assertEquals(sykepengegrunnlagsfakta1, sykepengegrunnlagsfakta2)
        assertEquals(sykepengegrunnlagsfakta1.hashCode(), sykepengegrunnlagsfakta2.hashCode())
    }

    @Test
    fun `structural equals - EtterHovedregel`() {
        val sykepengegrunnlagsfakta1 = Spleis.EtterHovedregel(
            arbeidsgivere = listOf(Arbeidsgiver.EtterHovedregel("organisasjonsnummer", 500000.0)),
            omregnetÅrsinntekt = 500000.0,
            innrapportertÅrsinntekt = 500000.0,
            avviksprosent = 25.0,
            seksG = 6 * 118620,
            tags = listOf("TAG")
        )
        val sykepengegrunnlagsfakta2 = Spleis.EtterHovedregel(
            arbeidsgivere = listOf(Arbeidsgiver.EtterHovedregel("organisasjonsnummer", 500000.0)),
            omregnetÅrsinntekt = 500000.0,
            innrapportertÅrsinntekt = 500000.0,
            avviksprosent = 25.0,
            seksG = 6 * 118620,
            tags = listOf("TAG")
        )

        assertEquals(sykepengegrunnlagsfakta1, sykepengegrunnlagsfakta2)
        assertEquals(sykepengegrunnlagsfakta1.hashCode(), sykepengegrunnlagsfakta2.hashCode())
    }

    @Test
    fun `structural equals - Infotrygd`() {
        val sykepengegrunnlagsfakta1 = Sykepengegrunnlagsfakta.Infotrygd(omregnetÅrsinntekt = 500000.0)
        val sykepengegrunnlagsfakta2 = Sykepengegrunnlagsfakta.Infotrygd(omregnetÅrsinntekt = 500000.0)

        assertEquals(sykepengegrunnlagsfakta1, sykepengegrunnlagsfakta2)
        assertEquals(sykepengegrunnlagsfakta1.hashCode(), sykepengegrunnlagsfakta2.hashCode())
    }

    @Test
    fun `not equals - EtterSkjønn - arbeidsgivere`() {
        val sykepengegrunnlagsfakta1 = Spleis.EtterSkjønn(
            arbeidsgivere = listOf(Arbeidsgiver.EtterSkjønn("organisasjonsnummer1", 500000.0, 600000.0)),
            omregnetÅrsinntekt = 500000.0,
            innrapportertÅrsinntekt = 500000.0,
            avviksprosent = 25.0,
            seksG = 6 * 118620,
            skjønnsfastsatt = 600000.0,
            tags = listOf("TAG")
        )
        val sykepengegrunnlagsfakta2 = Spleis.EtterSkjønn(
            arbeidsgivere = listOf(Arbeidsgiver.EtterSkjønn("organisasjonsnummer2", 400000.0, 300000.0)),
            omregnetÅrsinntekt = 500000.0,
            innrapportertÅrsinntekt = 500000.0,
            avviksprosent = 25.0,
            seksG = 6 * 118620,
            skjønnsfastsatt = 600000.0,
            tags = listOf("TAG")
        )

        assertNotEquals(sykepengegrunnlagsfakta1, sykepengegrunnlagsfakta2)
        assertNotEquals(sykepengegrunnlagsfakta1.hashCode(), sykepengegrunnlagsfakta2.hashCode())
    }

    @Test
    fun `not equals - EtterSkjønn - omregnetÅrsinntekt`() {
        val sykepengegrunnlagsfakta1 = Spleis.EtterSkjønn(
            arbeidsgivere = listOf(Arbeidsgiver.EtterSkjønn("organisasjonsnummer", 500000.0, 600000.0)),
            omregnetÅrsinntekt = 500000.0,
            innrapportertÅrsinntekt = 500000.0,
            avviksprosent = 25.0,
            seksG = 6 * 118620,
            skjønnsfastsatt = 600000.0,
            tags = listOf("TAG")
        )
        val sykepengegrunnlagsfakta2 = Spleis.EtterSkjønn(
            arbeidsgivere = listOf(Arbeidsgiver.EtterSkjønn("organisasjonsnummer", 500000.0, 600000.0)),
            omregnetÅrsinntekt = 400000.0,
            innrapportertÅrsinntekt = 500000.0,
            avviksprosent = 25.0,
            seksG = 6 * 118620,
            skjønnsfastsatt = 600000.0,
            tags = listOf("TAG")
        )

        assertNotEquals(sykepengegrunnlagsfakta1, sykepengegrunnlagsfakta2)
        assertNotEquals(sykepengegrunnlagsfakta1.hashCode(), sykepengegrunnlagsfakta2.hashCode())
    }

    @Test
    fun `not equals - EtterSkjønn - innrapporterÅrsinntekt`() {
        val sykepengegrunnlagsfakta1 = Spleis.EtterSkjønn(
            arbeidsgivere = listOf(Arbeidsgiver.EtterSkjønn("organisasjonsnummer", 500000.0, 600000.0)),
            omregnetÅrsinntekt = 500000.0,
            innrapportertÅrsinntekt = 500000.0,
            avviksprosent = 25.0,
            seksG = 6 * 118620,
            skjønnsfastsatt = 600000.0,
            tags = listOf("TAG")
        )
        val sykepengegrunnlagsfakta2 = Spleis.EtterSkjønn(
            arbeidsgivere = listOf(Arbeidsgiver.EtterSkjønn("organisasjonsnummer", 500000.0, 600000.0)),
            omregnetÅrsinntekt = 500000.0,
            innrapportertÅrsinntekt = 400000.0,
            avviksprosent = 25.0,
            seksG = 6 * 118620,
            skjønnsfastsatt = 600000.0,
            tags = listOf("TAG")
        )

        assertNotEquals(sykepengegrunnlagsfakta1, sykepengegrunnlagsfakta2)
        assertNotEquals(sykepengegrunnlagsfakta1.hashCode(), sykepengegrunnlagsfakta2.hashCode())
    }

    @Test
    fun `not equals - EtterSkjønn - avviksprosent`() {
        val sykepengegrunnlagsfakta1 = Spleis.EtterSkjønn(
            arbeidsgivere = listOf(Arbeidsgiver.EtterSkjønn("organisasjonsnummer", 500000.0, 600000.0)),
            omregnetÅrsinntekt = 500000.0,
            innrapportertÅrsinntekt = 500000.0,
            avviksprosent = 25.0,
            seksG = 6 * 118620,
            skjønnsfastsatt = 600000.0,
            tags = listOf("TAG")
        )
        val sykepengegrunnlagsfakta2 = Spleis.EtterSkjønn(
            arbeidsgivere = listOf(Arbeidsgiver.EtterSkjønn("organisasjonsnummer", 500000.0, 600000.0)),
            omregnetÅrsinntekt = 500000.0,
            innrapportertÅrsinntekt = 500000.0,
            avviksprosent = 26.0,
            seksG = 6 * 118620,
            skjønnsfastsatt = 600000.0,
            tags = listOf("TAG")
        )

        assertNotEquals(sykepengegrunnlagsfakta1, sykepengegrunnlagsfakta2)
        assertNotEquals(sykepengegrunnlagsfakta1.hashCode(), sykepengegrunnlagsfakta2.hashCode())
    }

    @Test
    fun `not equals - EtterSkjønn - seksG`() {
        val sykepengegrunnlagsfakta1 = Spleis.EtterSkjønn(
            arbeidsgivere = listOf(Arbeidsgiver.EtterSkjønn("organisasjonsnummer", 500000.0, 600000.0)),
            omregnetÅrsinntekt = 500000.0,
            innrapportertÅrsinntekt = 500000.0,
            avviksprosent = 25.0,
            seksG = 6 * 118620,
            skjønnsfastsatt = 600000.0,
            tags = listOf("TAG")
        )
        val sykepengegrunnlagsfakta2 = Spleis.EtterSkjønn(
            arbeidsgivere = listOf(Arbeidsgiver.EtterSkjønn("organisasjonsnummer", 500000.0, 600000.0)),
            omregnetÅrsinntekt = 500000.0,
            innrapportertÅrsinntekt = 500000.0,
            avviksprosent = 25.0,
            seksG = 6 * 100000,
            skjønnsfastsatt = 600000.0,
            tags = listOf("TAG")
        )

        assertNotEquals(sykepengegrunnlagsfakta1, sykepengegrunnlagsfakta2)
        assertNotEquals(sykepengegrunnlagsfakta1.hashCode(), sykepengegrunnlagsfakta2.hashCode())
    }

    @Test
    fun `not equals - EtterSkjønn - skjønnsfastsatt`() {
        val sykepengegrunnlagsfakta1 = Spleis.EtterSkjønn(
            arbeidsgivere = listOf(Arbeidsgiver.EtterSkjønn("organisasjonsnummer", 500000.0, 600000.0)),
            omregnetÅrsinntekt = 500000.0,
            innrapportertÅrsinntekt = 500000.0,
            avviksprosent = 25.0,
            seksG = 6 * 118620,
            skjønnsfastsatt = 600000.0,
            tags = listOf("TAG")
        )
        val sykepengegrunnlagsfakta2 = Spleis.EtterSkjønn(
            arbeidsgivere = listOf(Arbeidsgiver.EtterSkjønn("organisasjonsnummer", 500000.0, 600000.0)),
            omregnetÅrsinntekt = 500000.0,
            innrapportertÅrsinntekt = 500000.0,
            avviksprosent = 25.0,
            seksG = 6 * 118620,
            skjønnsfastsatt = 500000.0,
            tags = listOf("TAG")
        )

        assertNotEquals(sykepengegrunnlagsfakta1, sykepengegrunnlagsfakta2)
        assertNotEquals(sykepengegrunnlagsfakta1.hashCode(), sykepengegrunnlagsfakta2.hashCode())
    }

    @Test
    fun `not equals - EtterSkjønn - tags`() {
        val sykepengegrunnlagsfakta1 = Spleis.EtterSkjønn(
            arbeidsgivere = listOf(Arbeidsgiver.EtterSkjønn("organisasjonsnummer", 500000.0, 600000.0)),
            omregnetÅrsinntekt = 500000.0,
            innrapportertÅrsinntekt = 500000.0,
            avviksprosent = 25.0,
            seksG = 6 * 118620,
            skjønnsfastsatt = 600000.0,
            tags = listOf("TAG1")
        )
        val sykepengegrunnlagsfakta2 = Spleis.EtterSkjønn(
            arbeidsgivere = listOf(Arbeidsgiver.EtterSkjønn("organisasjonsnummer", 500000.0, 600000.0)),
            omregnetÅrsinntekt = 500000.0,
            innrapportertÅrsinntekt = 500000.0,
            avviksprosent = 25.0,
            seksG = 6 * 118620,
            skjønnsfastsatt = 600000.0,
            tags = listOf("TAG2")
        )

        assertNotEquals(sykepengegrunnlagsfakta1, sykepengegrunnlagsfakta2)
        assertNotEquals(sykepengegrunnlagsfakta1.hashCode(), sykepengegrunnlagsfakta2.hashCode())
    }

    @Test
    fun `not equals - EtterHovedregel - arbeidsgivere`() {
        val sykepengegrunnlagsfakta1 = Spleis.EtterHovedregel(
            arbeidsgivere = listOf(Arbeidsgiver.EtterHovedregel("organisasjonsnummer1", 500000.0)),
            omregnetÅrsinntekt = 500000.0,
            innrapportertÅrsinntekt = 500000.0,
            avviksprosent = 25.0,
            seksG = 6 * 118620,
            tags = listOf("TAG")
        )
        val sykepengegrunnlagsfakta2 = Spleis.EtterHovedregel(
            arbeidsgivere = listOf(Arbeidsgiver.EtterHovedregel("organisasjonsnummer2", 400000.0)),
            omregnetÅrsinntekt = 500000.0,
            innrapportertÅrsinntekt = 500000.0,
            avviksprosent = 25.0,
            seksG = 6 * 118620,
            tags = listOf("TAG")
        )

        assertNotEquals(sykepengegrunnlagsfakta1, sykepengegrunnlagsfakta2)
        assertNotEquals(sykepengegrunnlagsfakta1.hashCode(), sykepengegrunnlagsfakta2.hashCode())
    }

    @Test
    fun `not equals - EtterHovedregel - omregnetÅrsinntekt`() {
        val sykepengegrunnlagsfakta1 = Spleis.EtterHovedregel(
            arbeidsgivere = listOf(Arbeidsgiver.EtterHovedregel("organisasjonsnummer", 500000.0)),
            omregnetÅrsinntekt = 500000.0,
            innrapportertÅrsinntekt = 500000.0,
            avviksprosent = 25.0,
            seksG = 6 * 118620,
            tags = listOf("TAG")
        )
        val sykepengegrunnlagsfakta2 = Spleis.EtterHovedregel(
            arbeidsgivere = listOf(Arbeidsgiver.EtterHovedregel("organisasjonsnummer", 500000.0)),
            omregnetÅrsinntekt = 400000.0,
            innrapportertÅrsinntekt = 500000.0,
            avviksprosent = 25.0,
            seksG = 6 * 118620,
            tags = listOf("TAG")
        )

        assertNotEquals(sykepengegrunnlagsfakta1, sykepengegrunnlagsfakta2)
        assertNotEquals(sykepengegrunnlagsfakta1.hashCode(), sykepengegrunnlagsfakta2.hashCode())
    }

    @Test
    fun `not equals - EtterHovedregel - innrapporterÅrsinntekt`() {
        val sykepengegrunnlagsfakta1 = Spleis.EtterHovedregel(
            arbeidsgivere = listOf(Arbeidsgiver.EtterHovedregel("organisasjonsnummer", 500000.0)),
            omregnetÅrsinntekt = 500000.0,
            innrapportertÅrsinntekt = 500000.0,
            avviksprosent = 25.0,
            seksG = 6 * 118620,
            tags = listOf("TAG")
        )
        val sykepengegrunnlagsfakta2 = Spleis.EtterHovedregel(
            arbeidsgivere = listOf(Arbeidsgiver.EtterHovedregel("organisasjonsnummer", 500000.0)),
            omregnetÅrsinntekt = 500000.0,
            innrapportertÅrsinntekt = 400000.0,
            avviksprosent = 25.0,
            seksG = 6 * 118620,
            tags = listOf("TAG")
        )

        assertNotEquals(sykepengegrunnlagsfakta1, sykepengegrunnlagsfakta2)
        assertNotEquals(sykepengegrunnlagsfakta1.hashCode(), sykepengegrunnlagsfakta2.hashCode())
    }

    @Test
    fun `not equals - EtterHovedregel - avviksprosent`() {
        val sykepengegrunnlagsfakta1 = Spleis.EtterHovedregel(
            arbeidsgivere = listOf(Arbeidsgiver.EtterHovedregel("organisasjonsnummer", 500000.0)),
            omregnetÅrsinntekt = 500000.0,
            innrapportertÅrsinntekt = 500000.0,
            avviksprosent = 25.0,
            seksG = 6 * 118620,
            tags = listOf("TAG")
        )
        val sykepengegrunnlagsfakta2 = Spleis.EtterHovedregel(
            arbeidsgivere = listOf(Arbeidsgiver.EtterHovedregel("organisasjonsnummer", 500000.0)),
            omregnetÅrsinntekt = 500000.0,
            innrapportertÅrsinntekt = 500000.0,
            avviksprosent = 26.0,
            seksG = 6 * 118620,
            tags = listOf("TAG")
        )

        assertNotEquals(sykepengegrunnlagsfakta1, sykepengegrunnlagsfakta2)
        assertNotEquals(sykepengegrunnlagsfakta1.hashCode(), sykepengegrunnlagsfakta2.hashCode())
    }

    @Test
    fun `not equals - EtterHovedregel - seksG`() {
        val sykepengegrunnlagsfakta1 = Spleis.EtterHovedregel(
            arbeidsgivere = listOf(Arbeidsgiver.EtterHovedregel("organisasjonsnummer", 500000.0)),
            omregnetÅrsinntekt = 500000.0,
            innrapportertÅrsinntekt = 500000.0,
            avviksprosent = 25.0,
            seksG = 6 * 118620,
            tags = listOf("TAG")
        )
        val sykepengegrunnlagsfakta2 = Spleis.EtterHovedregel(
            arbeidsgivere = listOf(Arbeidsgiver.EtterHovedregel("organisasjonsnummer", 500000.0)),
            omregnetÅrsinntekt = 500000.0,
            innrapportertÅrsinntekt = 500000.0,
            avviksprosent = 25.0,
            seksG = 6 * 100000,
            tags = listOf("TAG")
        )

        assertNotEquals(sykepengegrunnlagsfakta1, sykepengegrunnlagsfakta2)
        assertNotEquals(sykepengegrunnlagsfakta1.hashCode(), sykepengegrunnlagsfakta2.hashCode())
    }

    @Test
    fun `not equals - EtterHovedregel - tags`() {
        val sykepengegrunnlagsfakta1 = Spleis.EtterHovedregel(
            arbeidsgivere = listOf(Arbeidsgiver.EtterHovedregel("organisasjonsnummer", 500000.0)),
            omregnetÅrsinntekt = 500000.0,
            innrapportertÅrsinntekt = 500000.0,
            avviksprosent = 25.0,
            seksG = 6 * 118620,
            tags = listOf("TAG1")
        )
        val sykepengegrunnlagsfakta2 = Spleis.EtterHovedregel(
            arbeidsgivere = listOf(Arbeidsgiver.EtterHovedregel("organisasjonsnummer", 500000.0)),
            omregnetÅrsinntekt = 500000.0,
            innrapportertÅrsinntekt = 500000.0,
            avviksprosent = 25.0,
            seksG = 6 * 118620,
            tags = listOf("TAG2")
        )

        assertNotEquals(sykepengegrunnlagsfakta1, sykepengegrunnlagsfakta2)
        assertNotEquals(sykepengegrunnlagsfakta1.hashCode(), sykepengegrunnlagsfakta2.hashCode())
    }

    @Test
    fun `not equals - Infotrygd - omregnetÅrsinntekt`() {
        val sykepengegrunnlagsfakta1 = Sykepengegrunnlagsfakta.Infotrygd(omregnetÅrsinntekt = 500000.0)
        val sykepengegrunnlagsfakta2 = Sykepengegrunnlagsfakta.Infotrygd(omregnetÅrsinntekt = 600000.0)

        assertNotEquals(sykepengegrunnlagsfakta1, sykepengegrunnlagsfakta2)
        assertNotEquals(sykepengegrunnlagsfakta1.hashCode(), sykepengegrunnlagsfakta2.hashCode())
    }
}