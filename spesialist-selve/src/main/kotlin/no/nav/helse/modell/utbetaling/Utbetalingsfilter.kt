package no.nav.helse.modell.utbetaling

import no.nav.helse.modell.WarningDao
import no.nav.helse.modell.person.PersonDao
import no.nav.helse.modell.person.PersonDao.Utbetalingen
import no.nav.helse.modell.person.PersonDao.Utbetalingen.Companion.utbetalingTilArbeidsgiver
import no.nav.helse.modell.person.PersonDao.Utbetalingen.Companion.utbetalingTilSykmeldt
import no.nav.helse.modell.utbetaling.Utbetalingtype.REVURDERING
import no.nav.helse.modell.vedtak.Warning
import no.nav.helse.modell.vedtaksperiode.Inntektskilde
import no.nav.helse.modell.vedtaksperiode.Inntektskilde.EN_ARBEIDSGIVER
import no.nav.helse.modell.vedtaksperiode.Periodetype
import no.nav.helse.modell.vedtaksperiode.Periodetype.FORLENGELSE
import no.nav.helse.modell.vedtaksperiode.Periodetype.FØRSTEGANGSBEHANDLING
import java.util.UUID

internal class Utbetalingsfilter(
    private val fødselsnummer: String,
    private val utbetalingTilArbeidsgiver: Boolean,
    private val utbetalingTilSykmeldt: Boolean,
    private val periodetype: Periodetype,
    private val inntektskilde: Inntektskilde,
    private val warnings: List<Warning>,
    private val utbetalingtype: Utbetalingtype
) {
    private constructor(
        fødselsnummer: String,
        utbetalingen: Utbetalingen?,
        warnings: List<Warning>,
        periodetype: Periodetype,
        inntektskilde: Inntektskilde,
        utbetalingtype: Utbetalingtype
    ) : this(
        fødselsnummer = fødselsnummer,
        utbetalingTilArbeidsgiver = utbetalingen.utbetalingTilArbeidsgiver(),
        utbetalingTilSykmeldt = utbetalingen.utbetalingTilSykmeldt(),
        periodetype = periodetype,
        inntektskilde = inntektskilde,
        warnings = warnings,
        utbetalingtype = utbetalingtype
    )

    internal constructor(
        vedtaksperiodeId: UUID,
        utbetalingId: UUID,
        fødselsnummer: String,
        warningDao: WarningDao,
        personDao: PersonDao,
        periodetype: Periodetype,
        inntektskilde: Inntektskilde,
        utbetalingtype: Utbetalingtype
    ) : this(
        fødselsnummer = fødselsnummer,
        utbetalingen = personDao.findVedtaksperiodeUtbetalingElement(fødselsnummer, utbetalingId),
        periodetype = periodetype,
        inntektskilde = inntektskilde,
        warnings = warningDao.finnWarnings(vedtaksperiodeId),
        utbetalingtype = utbetalingtype
    )

    private val årsaker = mutableListOf<String>()
    private fun nyÅrsak(årsak: String) = årsaker.add("Utbetalingsfilter: $årsak")

    private fun evaluer(): Boolean{
        if (!utbetalingTilSykmeldt) return true // Full refusjon / ingen utbetaling kan alltid utbetales
        if (utbetalingTilArbeidsgiver && utbetalingTilSykmeldt) nyÅrsak("Utbetalingen består av delvis refusjon")
        if (!fødselsnummer.startsWith("31")) nyÅrsak("Fødselsdag passer ikke")
        if (periodetype !in tillatePeriodetyper) nyÅrsak("Perioden er ikke førstegangsbehandling eller forlengelse")
        if (inntektskilde != EN_ARBEIDSGIVER) nyÅrsak("Inntektskilden er ikke for en arbeidsgiver")
        // Unngå ping-pong om en av de utvalgte utbetalingene til sykmeldt revurderes og får warning
        if (warnings.isNotEmpty() && utbetalingtype != REVURDERING) nyÅrsak("Vedtaksperioden har warnings")
        return årsaker.isEmpty()
    }

    internal val kanUtbetales by lazy {evaluer()}
    internal val kanIkkeUtbetales get() = !kanUtbetales

    internal fun årsaker(): List<String> {
        require(kanIkkeUtbetales) { "Årsaker skal kun brukes for vedtaksperioder vi ikke kan utbetale" }
        require(årsaker.isNotEmpty()) { "Må være minst en årsak til at vi ikke kan utbetale en vedtaksperiode" }
        return årsaker
    }

    private companion object {
        private val tillatePeriodetyper = setOf(FØRSTEGANGSBEHANDLING, FORLENGELSE)
    }
}
