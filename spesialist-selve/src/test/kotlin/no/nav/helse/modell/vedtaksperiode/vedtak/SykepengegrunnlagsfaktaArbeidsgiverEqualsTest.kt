package no.nav.helse.modell.vedtaksperiode.vedtak

import no.nav.helse.modell.vedtaksperiode.vedtak.Sykepengegrunnlagsfakta.Spleis.Arbeidsgiver
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test

internal class SykepengegrunnlagsfaktaArbeidsgiverEqualsTest {
    @Test
    fun `referential equals`() {
        val arbeidsgiverEtterSkjønn = Arbeidsgiver.EtterSkjønn("organisasjonsnummer", 500000.0, 500000.0, 600000.0)
        val arbeidsgiverEtterHovedregel = Arbeidsgiver.EtterHovedregel("organisasjonsnummer", 500000.0, 500000.0)
        assertEquals(arbeidsgiverEtterSkjønn, arbeidsgiverEtterSkjønn)
        assertEquals(arbeidsgiverEtterSkjønn.hashCode(), arbeidsgiverEtterSkjønn.hashCode())
        assertEquals(arbeidsgiverEtterHovedregel, arbeidsgiverEtterHovedregel)
        assertEquals(arbeidsgiverEtterHovedregel.hashCode(), arbeidsgiverEtterHovedregel.hashCode())
    }

    @Test
    fun `structural equals`() {
        val arbeidsgiverEtterSkjønn1 = Arbeidsgiver.EtterSkjønn("organisasjonsnummer", 500000.0, 500000.0, 600000.0)
        val arbeidsgiverEtterSkjønn2 = Arbeidsgiver.EtterSkjønn("organisasjonsnummer", 500000.0, 500000.0, 600000.0)
        val arbeidsgiverEtterHovedregel1 = Arbeidsgiver.EtterHovedregel("organisasjonsnummer", 500000.0, 500000.0)
        val arbeidsgiverEtterHovedregel2 = Arbeidsgiver.EtterHovedregel("organisasjonsnummer", 500000.0, 500000.0)

        assertEquals(arbeidsgiverEtterSkjønn1, arbeidsgiverEtterSkjønn2)
        assertEquals(arbeidsgiverEtterSkjønn1.hashCode(), arbeidsgiverEtterSkjønn2.hashCode())
        assertEquals(arbeidsgiverEtterHovedregel1, arbeidsgiverEtterHovedregel2)
        assertEquals(arbeidsgiverEtterHovedregel1.hashCode(), arbeidsgiverEtterHovedregel2.hashCode())
    }

    @Test
    fun `not equals - EtterSkjønn vs EtterHovedregel`() {
        val arbeidsgiverEtterSkjønn = Arbeidsgiver.EtterSkjønn("organisasjonsnummer", 500000.0, 500000.0, 600000.0)
        val arbeidsgiverEtterHovedregel = Arbeidsgiver.EtterHovedregel("organisasjonsnummer", 500000.0, 500000.0)
        assertNotEquals(arbeidsgiverEtterSkjønn.hashCode(), arbeidsgiverEtterHovedregel.hashCode())
    }

    @Test
    fun `not equals - EtterSkjønn - organisasjonsnummer`() {
        val arbeidsgiver1 = Arbeidsgiver.EtterSkjønn("organisasjonsnummer1", 500000.0, 500000.0, 600000.0)
        val arbeidsgiver2 = Arbeidsgiver.EtterSkjønn("organisasjonsnummer2", 500000.0, 500000.0, 600000.0)
        assertNotEquals(arbeidsgiver1, arbeidsgiver2)
        assertNotEquals(arbeidsgiver1.hashCode(), arbeidsgiver2.hashCode())
    }

    @Test
    fun `not equals - EtterHovedregel - organisasjonsnummer`() {
        val arbeidsgiver1 = Arbeidsgiver.EtterHovedregel("organisasjonsnummer1", 500000.0, 500000.0)
        val arbeidsgiver2 = Arbeidsgiver.EtterHovedregel("organisasjonsnummer2", 500000.0, 500000.0)
        assertNotEquals(arbeidsgiver1, arbeidsgiver2)
        assertNotEquals(arbeidsgiver1.hashCode(), arbeidsgiver2.hashCode())
    }

    @Test
    fun `not equals - EtterSkjønn - omregnetÅrsinntekt`() {
        val arbeidsgiver1 = Arbeidsgiver.EtterSkjønn("organisasjonsnummer", 500000.0, 500000.0, 600000.0)
        val arbeidsgiver2 = Arbeidsgiver.EtterSkjønn("organisasjonsnummer", 400000.0, 400000.0, 600000.0)
        assertNotEquals(arbeidsgiver1, arbeidsgiver2)
        assertNotEquals(arbeidsgiver1.hashCode(), arbeidsgiver2.hashCode())
    }

    @Test
    fun `not equals - EtterHovedregel - omregnetÅrsinntekt`() {
        val arbeidsgiver1 = Arbeidsgiver.EtterHovedregel("organisasjonsnummer", 500000.0, 500000.0)
        val arbeidsgiver2 = Arbeidsgiver.EtterHovedregel("organisasjonsnummer", 400000.0, 400000.0)
        assertNotEquals(arbeidsgiver1, arbeidsgiver2)
        assertNotEquals(arbeidsgiver1.hashCode(), arbeidsgiver2.hashCode())
    }

    @Test
    fun `not equals - EtterSkjønn - skjønnsfastsatt`() {
        val arbeidsgiver1 = Arbeidsgiver.EtterSkjønn("organisasjonsnummer", 500000.0, 500000.0, 600000.0)
        val arbeidsgiver2 = Arbeidsgiver.EtterSkjønn("organisasjonsnummer", 500000.0, 500000.0, 700000.0)
        assertNotEquals(arbeidsgiver1, arbeidsgiver2)
        assertNotEquals(arbeidsgiver1.hashCode(), arbeidsgiver2.hashCode())
    }
}