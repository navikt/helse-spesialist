package no.nav.helse.db

import no.nav.helse.db.overstyring.ArbeidsforholdForDatabase
import no.nav.helse.db.overstyring.LovhjemmelForDatabase
import no.nav.helse.db.overstyring.MinimumSykdomsgradForDatabase
import no.nav.helse.db.overstyring.OverstyringForDatabase
import no.nav.helse.db.overstyring.OverstyrtArbeidsforholdForDatabase
import no.nav.helse.db.overstyring.OverstyrtArbeidsgiverForDatabase
import no.nav.helse.db.overstyring.OverstyrtInntektOgRefusjonForDatabase
import no.nav.helse.db.overstyring.OverstyrtTidslinjeForDatabase
import no.nav.helse.db.overstyring.OverstyrtTidslinjedagForDatabase
import no.nav.helse.db.overstyring.RefusjonselementForDatabase
import no.nav.helse.db.overstyring.SkjønnsfastsattArbeidsgiverForDatabase
import no.nav.helse.db.overstyring.SkjønnsfastsattSykepengegrunnlagForDatabase
import no.nav.helse.db.overstyring.SkjønnsfastsettingstypeForDatabase
import no.nav.helse.modell.EksisterendeId
import no.nav.helse.modell.saksbehandler.Tilgangskontroll
import no.nav.helse.modell.saksbehandler.handlinger.Arbeidsforhold
import no.nav.helse.modell.saksbehandler.handlinger.MinimumSykdomsgrad
import no.nav.helse.modell.saksbehandler.handlinger.MinimumSykdomsgradArbeidsgiver
import no.nav.helse.modell.saksbehandler.handlinger.MinimumSykdomsgradPeriode
import no.nav.helse.modell.saksbehandler.handlinger.Overstyring
import no.nav.helse.modell.saksbehandler.handlinger.OverstyrtArbeidsforhold
import no.nav.helse.modell.saksbehandler.handlinger.OverstyrtArbeidsgiver
import no.nav.helse.modell.saksbehandler.handlinger.OverstyrtInntektOgRefusjon
import no.nav.helse.modell.saksbehandler.handlinger.OverstyrtTidslinje
import no.nav.helse.modell.saksbehandler.handlinger.OverstyrtTidslinjedag
import no.nav.helse.modell.saksbehandler.handlinger.Refusjonselement
import no.nav.helse.modell.saksbehandler.handlinger.SkjønnsfastsattArbeidsgiver
import no.nav.helse.modell.saksbehandler.handlinger.SkjønnsfastsattSykepengegrunnlag
import no.nav.helse.modell.totrinnsvurdering.Totrinnsvurdering
import no.nav.helse.modell.vilkårsprøving.Lovhjemmel

class PgTotrinnsvurderingRepository(
    private val overstyringDao: PgOverstyringDao,
    private val saksbehandlerDao: PgSaksbehandlerDao,
    private val totrinnsvurderingDao: PgTotrinnsvurderingDao,
    private val tilgangskontroll: Tilgangskontroll,
) : TotrinnsvurderingRepository {
    override fun finnTotrinnsvurdering(fødselsnummer: String): Totrinnsvurdering? {
        val (id, totrinnsvurdering) = totrinnsvurderingDao.hentAktivTotrinnsvurdering(fødselsnummer) ?: return null

        return Totrinnsvurdering(
            id = EksisterendeId(id),
            vedtaksperiodeId = totrinnsvurdering.vedtaksperiodeId,
            erRetur = totrinnsvurdering.erRetur,
            saksbehandler =
                totrinnsvurdering.saksbehandler?.let {
                    saksbehandlerDao.finnSaksbehandler(
                        it,
                        tilgangskontroll,
                    )
                },
            beslutter = totrinnsvurdering.beslutter?.let { saksbehandlerDao.finnSaksbehandler(it, tilgangskontroll) },
            utbetalingId = totrinnsvurdering.utbetalingId,
            opprettet = totrinnsvurdering.opprettet,
            oppdatert = totrinnsvurdering.oppdatert,
            ferdigstilt = false,
            overstyringer = overstyringDao.finnOverstyringer(fødselsnummer).tilDomene(),
        )
    }

    private fun List<OverstyringForDatabase>.tilDomene(): List<Overstyring> =
        map { overstyring ->
            when (overstyring) {
                is OverstyrtTidslinjeForDatabase -> overstyring.tilDomene()
                is MinimumSykdomsgradForDatabase -> overstyring.tilDomene()
                is OverstyrtArbeidsforholdForDatabase -> overstyring.tilDomene()
                is OverstyrtInntektOgRefusjonForDatabase -> overstyring.tilDomene()
                is SkjønnsfastsattSykepengegrunnlagForDatabase -> overstyring.tilDomene()
            }
        }

    private fun OverstyrtTidslinjeForDatabase.tilDomene(): OverstyrtTidslinje =
        OverstyrtTidslinje(
            id = id,
            vedtaksperiodeId = vedtaksperiodeId,
            aktørId = aktørId,
            fødselsnummer = fødselsnummer,
            organisasjonsnummer = organisasjonsnummer,
            dager = dager.map { it.tilDomene() },
            begrunnelse = begrunnelse,
        )

    private fun MinimumSykdomsgradForDatabase.tilDomene(): MinimumSykdomsgrad =
        MinimumSykdomsgrad(
            id = id,
            aktørId = aktørId,
            fødselsnummer = fødselsnummer,
            perioderVurdertOk = perioderVurdertOk.map { it.tilDomene() },
            perioderVurdertIkkeOk = perioderVurdertIkkeOk.map { it.tilDomene() },
            begrunnelse = begrunnelse,
            arbeidsgivere = arbeidsgivere.map { it.tilDomene() },
            initierendeVedtaksperiodeId = initierendeVedtaksperiodeId,
        )

    private fun OverstyrtArbeidsforholdForDatabase.tilDomene() =
        OverstyrtArbeidsforhold(
            id = id,
            vedtaksperiodeId = vedtaksperiodeId,
            fødselsnummer = fødselsnummer,
            aktørId = aktørId,
            skjæringstidspunkt = skjæringstidspunkt,
            overstyrteArbeidsforhold = overstyrteArbeidsforhold.map { it.tilDomene() },
        )

    private fun OverstyrtInntektOgRefusjonForDatabase.tilDomene() =
        OverstyrtInntektOgRefusjon(
            id = id,
            vedtaksperiodeId = vedtaksperiodeId,
            aktørId = aktørId,
            fødselsnummer = fødselsnummer,
            skjæringstidspunkt = skjæringstidspunkt,
            arbeidsgivere = arbeidsgivere.map { it.tilDomene() },
        )

    private fun SkjønnsfastsattSykepengegrunnlagForDatabase.tilDomene() =
        SkjønnsfastsattSykepengegrunnlag(
            id = id,
            aktørId = aktørId,
            fødselsnummer = fødselsnummer,
            skjæringstidspunkt = skjæringstidspunkt,
            arbeidsgivere = arbeidsgivere.map { it.tilDomene() },
            vedtaksperiodeId = vedtaksperiodeId,
        )

    private fun SkjønnsfastsattArbeidsgiverForDatabase.tilDomene() =
        SkjønnsfastsattArbeidsgiver(
            organisasjonsnummer = organisasjonsnummer,
            årlig = årlig,
            fraÅrlig = fraÅrlig,
            årsak = årsak,
            type = type.tilDomene(),
            begrunnelseMal = begrunnelseMal,
            begrunnelseFritekst = begrunnelseFritekst,
            begrunnelseKonklusjon = begrunnelseKonklusjon,
            lovhjemmel = lovhjemmel?.tilDomene(),
            initierendeVedtaksperiodeId = initierendeVedtaksperiodeId,
        )

    private fun SkjønnsfastsettingstypeForDatabase.tilDomene(): SkjønnsfastsattArbeidsgiver.Skjønnsfastsettingstype =
        when (this) {
            SkjønnsfastsettingstypeForDatabase.OMREGNET_ÅRSINNTEKT -> SkjønnsfastsattArbeidsgiver.Skjønnsfastsettingstype.OMREGNET_ÅRSINNTEKT
            SkjønnsfastsettingstypeForDatabase.RAPPORTERT_ÅRSINNTEKT -> SkjønnsfastsattArbeidsgiver.Skjønnsfastsettingstype.RAPPORTERT_ÅRSINNTEKT
            SkjønnsfastsettingstypeForDatabase.ANNET -> SkjønnsfastsattArbeidsgiver.Skjønnsfastsettingstype.ANNET
        }

    private fun OverstyrtArbeidsgiverForDatabase.tilDomene() =
        OverstyrtArbeidsgiver(
            organisasjonsnummer = organisasjonsnummer,
            månedligInntekt = månedligInntekt,
            fraMånedligInntekt = fraMånedligInntekt,
            refusjonsopplysninger = refusjonsopplysninger?.map { it.tilDomene() },
            fraRefusjonsopplysninger = fraRefusjonsopplysninger?.map { it.tilDomene() },
            begrunnelse = begrunnelse,
            forklaring = forklaring,
            lovhjemmel = lovhjemmel?.tilDomene(),
            fom = fom,
            tom = fom,
        )

    private fun RefusjonselementForDatabase.tilDomene() =
        Refusjonselement(
            fom = fom,
            tom = tom,
            beløp = beløp,
        )

    private fun ArbeidsforholdForDatabase.tilDomene() =
        Arbeidsforhold(
            organisasjonsnummer = organisasjonsnummer,
            deaktivert = deaktivert,
            begrunnelse = begrunnelse,
            forklaring = forklaring,
            lovhjemmel = null,
        )

    private fun MinimumSykdomsgradForDatabase.MinimumSykdomsgradPeriodeForDatabase.tilDomene(): MinimumSykdomsgradPeriode =
        MinimumSykdomsgradPeriode(
            fom = fom,
            tom = tom,
        )

    private fun MinimumSykdomsgradForDatabase.MinimumSykdomsgradArbeidsgiverForDatabase.tilDomene(): MinimumSykdomsgradArbeidsgiver =
        MinimumSykdomsgradArbeidsgiver(
            organisasjonsnummer = organisasjonsnummer,
            berørtVedtaksperiodeId = berørtVedtaksperiodeId,
        )

    private fun OverstyrtTidslinjedagForDatabase.tilDomene() =
        OverstyrtTidslinjedag(
            dato = dato,
            type = type,
            grad = grad,
            fraGrad = fraGrad,
            fraType = fraType,
            lovhjemmel = lovhjemmel?.tilDomene(),
        )

    private fun LovhjemmelForDatabase.tilDomene() =
        Lovhjemmel(
            paragraf = paragraf,
            ledd = ledd,
            bokstav = bokstav,
            lovverk = "",
            lovverksversjon = "",
        )
}
