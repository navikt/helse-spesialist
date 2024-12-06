package no.nav.helse.modell.vedtak

import no.nav.helse.modell.vedtak.Sykepengegrunnlagsfakta.Spleis
import no.nav.helse.modell.vedtak.Sykepengegrunnlagsfakta.Spleis.Arbeidsgiver
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test

class SykepengegrunnlagsfaktaEqualsTest {
    @Test
    fun `referential equals - EtterSkjønn`() {
        val sykepengegrunnlagsfakta =
            Spleis.EtterSkjønn(
                omregnetÅrsinntekt = 500000.0,
                seksG = 6 * 118620.0,
                skjønnsfastsatt = 600000.0,
                tags = mutableSetOf("TAG"),
                arbeidsgivere = listOf(Arbeidsgiver.EtterSkjønn("organisasjonsnummer", 500000.0, 600000.0)),
            )

        assertEquals(sykepengegrunnlagsfakta, sykepengegrunnlagsfakta)
        assertEquals(sykepengegrunnlagsfakta.hashCode(), sykepengegrunnlagsfakta.hashCode())
    }

    @Test
    fun `referential equals - EtterHovedregel`() {
        val sykepengegrunnlagsfakta =
            Spleis.EtterHovedregel(
                omregnetÅrsinntekt = 500000.0,
                seksG = 6 * 118620.0,
                tags = mutableSetOf("TAG"),
                arbeidsgivere = listOf(Arbeidsgiver.EtterHovedregel("organisasjonsnummer", 500000.0)),
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
        val sykepengegrunnlagsfakta1 =
            Spleis.EtterSkjønn(
                omregnetÅrsinntekt = 500000.0,
                seksG = 6 * 118620.0,
                skjønnsfastsatt = 600000.0,
                tags = mutableSetOf("TAG"),
                arbeidsgivere = listOf(Arbeidsgiver.EtterSkjønn("organisasjonsnummer", 500000.0, 600000.0)),
            )
        val sykepengegrunnlagsfakta2 =
            Spleis.EtterSkjønn(
                omregnetÅrsinntekt = 500000.0,
                seksG = 6 * 118620.0,
                skjønnsfastsatt = 600000.0,
                tags = mutableSetOf("TAG"),
                arbeidsgivere = listOf(Arbeidsgiver.EtterSkjønn("organisasjonsnummer", 500000.0, 600000.0)),
            )

        assertEquals(sykepengegrunnlagsfakta1, sykepengegrunnlagsfakta2)
        assertEquals(sykepengegrunnlagsfakta1.hashCode(), sykepengegrunnlagsfakta2.hashCode())
    }

    @Test
    fun `structural equals - EtterHovedregel`() {
        val sykepengegrunnlagsfakta1 =
            Spleis.EtterHovedregel(
                omregnetÅrsinntekt = 500000.0,
                seksG = 6 * 118620.0,
                tags = mutableSetOf("TAG"),
                arbeidsgivere = listOf(Arbeidsgiver.EtterHovedregel("organisasjonsnummer", 500000.0)),
            )
        val sykepengegrunnlagsfakta2 =
            Spleis.EtterHovedregel(
                omregnetÅrsinntekt = 500000.0,
                seksG = 6 * 118620.0,
                tags = mutableSetOf("TAG"),
                arbeidsgivere = listOf(Arbeidsgiver.EtterHovedregel("organisasjonsnummer", 500000.0)),
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
        val sykepengegrunnlagsfakta1 =
            Spleis.EtterSkjønn(
                omregnetÅrsinntekt = 500000.0,
                seksG = 6 * 118620.0,
                skjønnsfastsatt = 600000.0,
                tags = mutableSetOf("TAG"),
                arbeidsgivere = listOf(Arbeidsgiver.EtterSkjønn("organisasjonsnummer1", 500000.0, 600000.0)),
            )
        val sykepengegrunnlagsfakta2 =
            Spleis.EtterSkjønn(
                omregnetÅrsinntekt = 500000.0,
                seksG = 6 * 118620.0,
                skjønnsfastsatt = 600000.0,
                tags = mutableSetOf("TAG"),
                arbeidsgivere = listOf(Arbeidsgiver.EtterSkjønn("organisasjonsnummer2", 400000.0, 300000.0)),
            )

        assertNotEquals(sykepengegrunnlagsfakta1, sykepengegrunnlagsfakta2)
        assertNotEquals(sykepengegrunnlagsfakta1.hashCode(), sykepengegrunnlagsfakta2.hashCode())
    }

    @Test
    fun `not equals - EtterSkjønn - omregnetÅrsinntekt`() {
        val sykepengegrunnlagsfakta1 =
            Spleis.EtterSkjønn(
                omregnetÅrsinntekt = 500000.0,
                seksG = 6 * 118620.0,
                skjønnsfastsatt = 600000.0,
                tags = mutableSetOf("TAG"),
                arbeidsgivere = listOf(Arbeidsgiver.EtterSkjønn("organisasjonsnummer", 500000.0, 600000.0)),
            )
        val sykepengegrunnlagsfakta2 =
            Spleis.EtterSkjønn(
                omregnetÅrsinntekt = 400000.0,
                seksG = 6 * 118620.0,
                skjønnsfastsatt = 600000.0,
                tags = mutableSetOf("TAG"),
                arbeidsgivere = listOf(Arbeidsgiver.EtterSkjønn("organisasjonsnummer", 500000.0, 600000.0)),
            )

        assertNotEquals(sykepengegrunnlagsfakta1, sykepengegrunnlagsfakta2)
        assertNotEquals(sykepengegrunnlagsfakta1.hashCode(), sykepengegrunnlagsfakta2.hashCode())
    }

    @Test
    fun `not equals - EtterSkjønn - seksG`() {
        val sykepengegrunnlagsfakta1 =
            Spleis.EtterSkjønn(
                omregnetÅrsinntekt = 500000.0,
                seksG = 6 * 118620.0,
                skjønnsfastsatt = 600000.0,
                tags = mutableSetOf("TAG"),
                arbeidsgivere = listOf(Arbeidsgiver.EtterSkjønn("organisasjonsnummer", 500000.0, 600000.0)),
            )
        val sykepengegrunnlagsfakta2 =
            Spleis.EtterSkjønn(
                omregnetÅrsinntekt = 500000.0,
                seksG = 6 * 100000.0,
                skjønnsfastsatt = 600000.0,
                tags = mutableSetOf("TAG"),
                arbeidsgivere = listOf(Arbeidsgiver.EtterSkjønn("organisasjonsnummer", 500000.0, 600000.0)),
            )

        assertNotEquals(sykepengegrunnlagsfakta1, sykepengegrunnlagsfakta2)
        assertNotEquals(sykepengegrunnlagsfakta1.hashCode(), sykepengegrunnlagsfakta2.hashCode())
    }

    @Test
    fun `not equals - EtterSkjønn - skjønnsfastsatt`() {
        val sykepengegrunnlagsfakta1 =
            Spleis.EtterSkjønn(
                omregnetÅrsinntekt = 500000.0,
                seksG = 6 * 118620.0,
                skjønnsfastsatt = 600000.0,
                tags = mutableSetOf("TAG"),
                arbeidsgivere = listOf(Arbeidsgiver.EtterSkjønn("organisasjonsnummer", 500000.0, 600000.0)),
            )
        val sykepengegrunnlagsfakta2 =
            Spleis.EtterSkjønn(
                omregnetÅrsinntekt = 500000.0,
                seksG = 6 * 118620.0,
                skjønnsfastsatt = 500000.0,
                tags = mutableSetOf("TAG"),
                arbeidsgivere = listOf(Arbeidsgiver.EtterSkjønn("organisasjonsnummer", 500000.0, 600000.0)),
            )

        assertNotEquals(sykepengegrunnlagsfakta1, sykepengegrunnlagsfakta2)
        assertNotEquals(sykepengegrunnlagsfakta1.hashCode(), sykepengegrunnlagsfakta2.hashCode())
    }

    @Test
    fun `not equals - EtterSkjønn - tags`() {
        val sykepengegrunnlagsfakta1 =
            Spleis.EtterSkjønn(
                omregnetÅrsinntekt = 500000.0,
                seksG = 6 * 118620.0,
                skjønnsfastsatt = 600000.0,
                tags = mutableSetOf("TAG1"),
                arbeidsgivere = listOf(Arbeidsgiver.EtterSkjønn("organisasjonsnummer", 500000.0, 600000.0)),
            )
        val sykepengegrunnlagsfakta2 =
            Spleis.EtterSkjønn(
                omregnetÅrsinntekt = 500000.0,
                seksG = 6 * 118620.0,
                skjønnsfastsatt = 600000.0,
                tags = mutableSetOf("TAG2"),
                arbeidsgivere = listOf(Arbeidsgiver.EtterSkjønn("organisasjonsnummer", 500000.0, 600000.0)),
            )

        assertNotEquals(sykepengegrunnlagsfakta1, sykepengegrunnlagsfakta2)
        assertNotEquals(sykepengegrunnlagsfakta1.hashCode(), sykepengegrunnlagsfakta2.hashCode())
    }

    @Test
    fun `not equals - EtterHovedregel - arbeidsgivere`() {
        val sykepengegrunnlagsfakta1 =
            Spleis.EtterHovedregel(
                omregnetÅrsinntekt = 500000.0,
                seksG = 6 * 118620.0,
                tags = mutableSetOf("TAG"),
                arbeidsgivere = listOf(Arbeidsgiver.EtterHovedregel("organisasjonsnummer1", 500000.0)),
            )
        val sykepengegrunnlagsfakta2 =
            Spleis.EtterHovedregel(
                omregnetÅrsinntekt = 500000.0,
                seksG = 6 * 118620.0,
                tags = mutableSetOf("TAG"),
                arbeidsgivere = listOf(Arbeidsgiver.EtterHovedregel("organisasjonsnummer2", 400000.0)),
            )

        assertNotEquals(sykepengegrunnlagsfakta1, sykepengegrunnlagsfakta2)
        assertNotEquals(sykepengegrunnlagsfakta1.hashCode(), sykepengegrunnlagsfakta2.hashCode())
    }

    @Test
    fun `not equals - EtterHovedregel - omregnetÅrsinntekt`() {
        val sykepengegrunnlagsfakta1 =
            Spleis.EtterHovedregel(
                omregnetÅrsinntekt = 500000.0,
                seksG = 6 * 118620.0,
                tags = mutableSetOf("TAG"),
                arbeidsgivere = listOf(Arbeidsgiver.EtterHovedregel("organisasjonsnummer", 500000.0)),
            )
        val sykepengegrunnlagsfakta2 =
            Spleis.EtterHovedregel(
                omregnetÅrsinntekt = 400000.0,
                seksG = 6 * 118620.0,
                tags = mutableSetOf("TAG"),
                arbeidsgivere = listOf(Arbeidsgiver.EtterHovedregel("organisasjonsnummer", 500000.0)),
            )

        assertNotEquals(sykepengegrunnlagsfakta1, sykepengegrunnlagsfakta2)
        assertNotEquals(sykepengegrunnlagsfakta1.hashCode(), sykepengegrunnlagsfakta2.hashCode())
    }

    @Test
    fun `not equals - EtterHovedregel - seksG`() {
        val sykepengegrunnlagsfakta1 =
            Spleis.EtterHovedregel(
                omregnetÅrsinntekt = 500000.0,
                seksG = 6 * 118620.0,
                tags = mutableSetOf("TAG"),
                arbeidsgivere = listOf(Arbeidsgiver.EtterHovedregel("organisasjonsnummer", 500000.0)),
            )
        val sykepengegrunnlagsfakta2 =
            Spleis.EtterHovedregel(
                omregnetÅrsinntekt = 500000.0,
                seksG = 6 * 100000.0,
                tags = mutableSetOf("TAG"),
                arbeidsgivere = listOf(Arbeidsgiver.EtterHovedregel("organisasjonsnummer", 500000.0)),
            )

        assertNotEquals(sykepengegrunnlagsfakta1, sykepengegrunnlagsfakta2)
        assertNotEquals(sykepengegrunnlagsfakta1.hashCode(), sykepengegrunnlagsfakta2.hashCode())
    }

    @Test
    fun `not equals - EtterHovedregel - tags`() {
        val sykepengegrunnlagsfakta1 =
            Spleis.EtterHovedregel(
                omregnetÅrsinntekt = 500000.0,
                seksG = 6 * 118620.0,
                tags = mutableSetOf("TAG1"),
                arbeidsgivere = listOf(Arbeidsgiver.EtterHovedregel("organisasjonsnummer", 500000.0)),
            )
        val sykepengegrunnlagsfakta2 =
            Spleis.EtterHovedregel(
                omregnetÅrsinntekt = 500000.0,
                seksG = 6 * 118620.0,
                tags = mutableSetOf("TAG2"),
                arbeidsgivere = listOf(Arbeidsgiver.EtterHovedregel("organisasjonsnummer", 500000.0)),
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
