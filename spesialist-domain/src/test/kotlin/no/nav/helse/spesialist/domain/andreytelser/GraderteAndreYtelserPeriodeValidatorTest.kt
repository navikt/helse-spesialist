package no.nav.helse.spesialist.domain.andreytelser

import no.nav.helse.modell.person.vedtaksperiode.BehandlingDto
import no.nav.helse.modell.person.vedtaksperiode.TilstandDto
import no.nav.helse.modell.person.vedtaksperiode.VedtaksperiodeDto
import no.nav.helse.modell.vedtaksperiode.Yrkesaktivitetstype
import no.nav.helse.spesialist.domain.Periode.Companion.tilOgMed
import no.nav.helse.spesialist.domain.TotrinnsvurderingId
import no.nav.helse.spesialist.domain.andreytelser.AndreYtelserPeriode.GraderteAndreYtelserPeriode
import no.nav.helse.spesialist.domain.testfixtures.feb
import no.nav.helse.spesialist.domain.testfixtures.jan
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagIdentitetsnummer
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagSaksbehandler
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate
import java.util.*
import kotlin.random.Random
import kotlin.test.Test

class GraderteAndreYtelserPeriodeValidatorTest {
    @Test
    fun `kaster feil når perioder i ny liste overlapper hverandre`() {
        assertThrows<IllegalStateException> {
            validerGraderteAndreYtelserPeriode(
                nyGraderteAndreYtelserPerioder =
                    listOf(
                        GraderteAndreYtelserPeriode(periode = (1 jan 2024) tilOgMed (31 jan 2024), grad = 50),
                        GraderteAndreYtelserPeriode(periode = (15 jan 2024) tilOgMed (28 feb 2024), grad = 80),
                    ),
                nyGraderteAndreYtelserType = GraderteAndreYtelserType.FORELDREPENGER,
                eksisterendeGraderteAndreYtelser = emptyList(),
                vedtaksperioder = listOf(lagVedtaksperiode(fom = 1 jan 2024, tom = 28 feb 2024)),
            )
        }
    }

    @Test
    fun `kaster feil når periode ikke overlapper med sykefraværstilfelle`() {
        assertThrows<IllegalStateException> {
            validerGraderteAndreYtelserPeriode(
                nyGraderteAndreYtelserPerioder =
                    listOf(
                        GraderteAndreYtelserPeriode(periode = (1 jan 2024) tilOgMed (31 jan 2024), grad = 50),
                    ),
                nyGraderteAndreYtelserType = GraderteAndreYtelserType.FORELDREPENGER,
                eksisterendeGraderteAndreYtelser = emptyList(),
                vedtaksperioder = listOf(lagVedtaksperiode(fom = 1 feb 2024, tom = 29 feb 2024)),
            )
        }
    }

    @Test
    fun `godtar periode som overlapper med sykefraværstilfelle`() {
        validerGraderteAndreYtelserPeriode(
            nyGraderteAndreYtelserPerioder =
                listOf(
                    GraderteAndreYtelserPeriode(periode = (1 jan 2024) tilOgMed (31 jan 2024), grad = 50),
                ),
            nyGraderteAndreYtelserType = GraderteAndreYtelserType.FORELDREPENGER,
            eksisterendeGraderteAndreYtelser = emptyList(),
            vedtaksperioder = listOf(lagVedtaksperiode(fom = 1 jan 2024, tom = 31 jan 2024)),
        )
    }

    @Test
    fun `kaster feil når ny periode overlapper eksisterende periode av samme type`() {
        val identitetsnummer = lagIdentitetsnummer()
        val eksisterende =
            GraderteAndreYtelser.ny(
                identitetsnummer = identitetsnummer,
                saksbehandlerIdent = lagSaksbehandler().ident,
                notatTilBeslutter = "notat",
                totrinnsvurderingId = TotrinnsvurderingId(Random.nextLong()),
                graderteAndreYtelserPerioder =
                    listOf(
                        GraderteAndreYtelserPeriode(periode = (1 jan 2024) tilOgMed (31 jan 2024), grad = 50),
                    ),
                graderteAndreYtelserType = GraderteAndreYtelserType.FORELDREPENGER,
            )

        assertThrows<IllegalStateException> {
            validerGraderteAndreYtelserPeriode(
                nyGraderteAndreYtelserPerioder =
                    listOf(
                        GraderteAndreYtelserPeriode(periode = (15 jan 2024) tilOgMed (28 feb 2024), grad = 60),
                    ),
                nyGraderteAndreYtelserType = GraderteAndreYtelserType.FORELDREPENGER,
                eksisterendeGraderteAndreYtelser = listOf(eksisterende),
                vedtaksperioder = listOf(lagVedtaksperiode(fom = 1 jan 2024, tom = 28 feb 2024)),
            )
        }
    }

    @Test
    fun `godtar periode som overlapper eksisterende periode av annen type`() {
        val identitetsnummer = lagIdentitetsnummer()
        val eksisterende =
            GraderteAndreYtelser.ny(
                identitetsnummer = identitetsnummer,
                saksbehandlerIdent = lagSaksbehandler().ident,
                notatTilBeslutter = "notat",
                totrinnsvurderingId = TotrinnsvurderingId(Random.nextLong()),
                graderteAndreYtelserPerioder =
                    listOf(
                        GraderteAndreYtelserPeriode(periode = (1 jan 2024) tilOgMed (31 jan 2024), grad = 50),
                    ),
                graderteAndreYtelserType = GraderteAndreYtelserType.FORELDREPENGER,
            )

        validerGraderteAndreYtelserPeriode(
            nyGraderteAndreYtelserPerioder =
                listOf(
                    GraderteAndreYtelserPeriode(periode = (15 jan 2024) tilOgMed (28 feb 2024), grad = 40),
                ),
            nyGraderteAndreYtelserType = GraderteAndreYtelserType.PLEIEPENGER,
            eksisterendeGraderteAndreYtelser = listOf(eksisterende),
            vedtaksperioder = listOf(lagVedtaksperiode(fom = 1 jan 2024, tom = 28 feb 2024)),
        )
    }

    @Test
    fun `gir feil når overlappende perioder overstiger 99 prosent`() {
        val identitetsnummer = lagIdentitetsnummer()
        val eksisterende =
            GraderteAndreYtelser.ny(
                identitetsnummer = identitetsnummer,
                saksbehandlerIdent = lagSaksbehandler().ident,
                notatTilBeslutter = "notat",
                totrinnsvurderingId = TotrinnsvurderingId(Random.nextLong()),
                graderteAndreYtelserPerioder =
                    listOf(
                        GraderteAndreYtelserPeriode(periode = (1 jan 2024) tilOgMed (31 jan 2024), grad = 50),
                    ),
                graderteAndreYtelserType = GraderteAndreYtelserType.FORELDREPENGER,
            )

        assertThrows<IllegalStateException> {
            validerGraderteAndreYtelserPeriode(
                nyGraderteAndreYtelserPerioder =
                    listOf(
                        GraderteAndreYtelserPeriode(periode = (15 jan 2024) tilOgMed (28 feb 2024), grad = 60),
                    ),
                nyGraderteAndreYtelserType = GraderteAndreYtelserType.PLEIEPENGER,
                eksisterendeGraderteAndreYtelser = listOf(eksisterende),
                vedtaksperioder = listOf(lagVedtaksperiode(fom = 1 jan 2024, tom = 28 feb 2024)),
            )
        }
    }

    @Test
    fun `kaster feil når en ytelse endres til samme type som en overlappende eksisterende ytelse`() {
        val identitetsnummer = lagIdentitetsnummer()
        val førsteYtelse =
            GraderteAndreYtelser.ny(
                identitetsnummer = identitetsnummer,
                saksbehandlerIdent = lagSaksbehandler().ident,
                notatTilBeslutter = "notat",
                totrinnsvurderingId = TotrinnsvurderingId(Random.nextLong()),
                graderteAndreYtelserPerioder =
                    listOf(
                        GraderteAndreYtelserPeriode(periode = (1 jan 2024) tilOgMed (31 jan 2024), grad = 50),
                    ),
                graderteAndreYtelserType = GraderteAndreYtelserType.PLEIEPENGER,
            )
        val andreYtelser =
            GraderteAndreYtelser.ny(
                identitetsnummer = identitetsnummer,
                saksbehandlerIdent = lagSaksbehandler().ident,
                notatTilBeslutter = "notat",
                totrinnsvurderingId = TotrinnsvurderingId(Random.nextLong()),
                graderteAndreYtelserPerioder =
                    listOf(
                        GraderteAndreYtelserPeriode(periode = (15 jan 2024) tilOgMed (28 feb 2024), grad = 60),
                    ),
                graderteAndreYtelserType = GraderteAndreYtelserType.FORELDREPENGER,
            )

        assertThrows<IllegalStateException> {
            validerGraderteAndreYtelserPeriode(
                nyGraderteAndreYtelserPerioder = førsteYtelse.perioder,
                nyGraderteAndreYtelserType = GraderteAndreYtelserType.FORELDREPENGER,
                eksisterendeGraderteAndreYtelser = listOf(andreYtelser),
                vedtaksperioder = listOf(lagVedtaksperiode(fom = 1 jan 2024, tom = 28 feb 2024)),
            )
        }

        assertEquals(GraderteAndreYtelserType.PLEIEPENGER, førsteYtelse.graderteAndreYtelserType)
        assertEquals(1, førsteYtelse.events.size)
    }

    @Test
    fun `godtar sammenhengende sykefraværstilfelle over flere vedtaksperioder`() {
        validerGraderteAndreYtelserPeriode(
            nyGraderteAndreYtelserPerioder =
                listOf(
                    GraderteAndreYtelserPeriode(periode = (1 jan 2024) tilOgMed (29 feb 2024), grad = 50),
                ),
            nyGraderteAndreYtelserType = GraderteAndreYtelserType.SVANGERSKAPSPENGER,
            eksisterendeGraderteAndreYtelser = emptyList(),
            vedtaksperioder =
                listOf(
                    lagVedtaksperiode(fom = 1 jan 2024, tom = 31 jan 2024),
                    lagVedtaksperiode(fom = 1 feb 2024, tom = 29 feb 2024),
                ),
        )
    }

    @Test
    fun `godtar periode som spenner over gap mellom to sykefraværstilfeller`() {
        validerGraderteAndreYtelserPeriode(
            nyGraderteAndreYtelserPerioder =
                listOf(
                    GraderteAndreYtelserPeriode(periode = (1 jan 2024) tilOgMed (29 feb 2024), grad = 50),
                ),
            nyGraderteAndreYtelserType = GraderteAndreYtelserType.PLEIEPENGER,
            eksisterendeGraderteAndreYtelser = emptyList(),
            vedtaksperioder =
                listOf(
                    lagVedtaksperiode(fom = 1 jan 2024, tom = 20 jan 2024),
                    lagVedtaksperiode(fom = 1 feb 2024, tom = 29 feb 2024),
                ),
        )
    }

    private fun lagVedtaksperiode(
        fom: LocalDate,
        tom: LocalDate,
        skjæringstidspunkt: LocalDate = fom,
    ): VedtaksperiodeDto {
        val vedtaksperiodeId = UUID.randomUUID()
        return VedtaksperiodeDto(
            organisasjonsnummer = "123456789",
            vedtaksperiodeId = vedtaksperiodeId,
            forkastet = false,
            behandlinger =
                listOf(
                    BehandlingDto(
                        id = UUID.randomUUID(),
                        vedtaksperiodeId = vedtaksperiodeId,
                        utbetalingId = UUID.randomUUID(),
                        spleisBehandlingId = UUID.randomUUID(),
                        skjæringstidspunkt = skjæringstidspunkt,
                        fom = fom,
                        tom = tom,
                        tilstand = TilstandDto.KlarTilBehandling,
                        tags = emptyList(),
                        varsler = emptyList(),
                        yrkesaktivitetstype = Yrkesaktivitetstype.ARBEIDSTAKER,
                    ),
                ),
        )
    }
}
