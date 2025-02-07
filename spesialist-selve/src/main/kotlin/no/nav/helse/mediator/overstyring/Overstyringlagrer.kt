package no.nav.helse.mediator.overstyring

import no.nav.helse.db.OverstyringDao
import no.nav.helse.db.overstyring.ArbeidsforholdForDatabase
import no.nav.helse.db.overstyring.LovhjemmelForDatabase
import no.nav.helse.db.overstyring.MinimumSykdomsgradForDatabase
import no.nav.helse.db.overstyring.OverstyrtArbeidsforholdForDatabase
import no.nav.helse.db.overstyring.OverstyrtArbeidsgiverForDatabase
import no.nav.helse.db.overstyring.OverstyrtInntektOgRefusjonForDatabase
import no.nav.helse.db.overstyring.OverstyrtTidslinjeForDatabase
import no.nav.helse.db.overstyring.OverstyrtTidslinjedagForDatabase
import no.nav.helse.db.overstyring.RefusjonselementForDatabase
import no.nav.helse.db.overstyring.SkjønnsfastsattArbeidsgiverForDatabase
import no.nav.helse.db.overstyring.SkjønnsfastsattSykepengegrunnlagForDatabase
import no.nav.helse.db.overstyring.SkjønnsfastsettingstypeForDatabase
import no.nav.helse.modell.saksbehandler.handlinger.MinimumSykdomsgrad
import no.nav.helse.modell.saksbehandler.handlinger.Overstyring
import no.nav.helse.modell.saksbehandler.handlinger.OverstyrtArbeidsforhold
import no.nav.helse.modell.saksbehandler.handlinger.OverstyrtInntektOgRefusjon
import no.nav.helse.modell.saksbehandler.handlinger.OverstyrtTidslinje
import no.nav.helse.modell.saksbehandler.handlinger.Refusjonselement
import no.nav.helse.modell.saksbehandler.handlinger.SkjønnsfastsattArbeidsgiver
import no.nav.helse.modell.saksbehandler.handlinger.SkjønnsfastsattSykepengegrunnlag
import no.nav.helse.modell.vilkårsprøving.Lovhjemmel
import java.time.LocalDateTime
import java.util.UUID

class Overstyringlagrer(private val overstyringDao: OverstyringDao) {
    fun lagre(
        overstyring: Overstyring,
        saksbehandlerOid: UUID,
    ) {
        when (overstyring) {
            is OverstyrtTidslinje -> lagreOverstyrTidslinje(overstyring, saksbehandlerOid)
            is OverstyrtInntektOgRefusjon -> lagreOverstyrtInntektOgRefusjon(overstyring, saksbehandlerOid)
            is OverstyrtArbeidsforhold -> lagreOverstyrtArbeidsforhold(overstyring, saksbehandlerOid)
            is SkjønnsfastsattSykepengegrunnlag -> lagreSkjønnsfastsattSykepengegrunnlag(overstyring, saksbehandlerOid)
            is MinimumSykdomsgrad -> lagreMinimumSykdomsgrad(overstyring, saksbehandlerOid)
        }
    }

    private fun lagreOverstyrTidslinje(
        overstyring: OverstyrtTidslinje,
        saksbehandlerOid: UUID,
    ) {
        overstyringDao.persisterOverstyringTidslinje(overstyring.tilDatabase(), saksbehandlerOid)
    }

    private fun lagreOverstyrtInntektOgRefusjon(
        overstyring: OverstyrtInntektOgRefusjon,
        saksbehandlerOid: UUID,
    ) {
        overstyringDao.persisterOverstyringInntektOgRefusjon(overstyring.tilDatabase(), saksbehandlerOid)
    }

    private fun lagreOverstyrtArbeidsforhold(
        overstyring: OverstyrtArbeidsforhold,
        saksbehandlerOid: UUID,
    ) {
        overstyringDao.persisterOverstyringArbeidsforhold(overstyring.tilDatabase(), saksbehandlerOid)
    }

    private fun lagreSkjønnsfastsattSykepengegrunnlag(
        overstyring: SkjønnsfastsattSykepengegrunnlag,
        saksbehandlerOid: UUID,
    ) {
        overstyringDao.persisterSkjønnsfastsettingSykepengegrunnlag(overstyring.tilDatabase(), saksbehandlerOid)
    }

    private fun lagreMinimumSykdomsgrad(
        overstyring: MinimumSykdomsgrad,
        saksbehandlerOid: UUID,
    ) {
        overstyringDao.persisterMinimumSykdomsgrad(overstyring.tilDatabase(), saksbehandlerOid)
    }

    private fun OverstyrtTidslinje.tilDatabase() =
        OverstyrtTidslinjeForDatabase(
            id = id,
            aktørId = aktørId,
            fødselsnummer = fødselsnummer,
            organisasjonsnummer = organisasjonsnummer,
            vedtaksperiodeId = vedtaksperiodeId,
            begrunnelse = begrunnelse,
            opprettet = LocalDateTime.now(),
            dager =
                dager.map {
                    OverstyrtTidslinjedagForDatabase(
                        dato = it.dato,
                        type = it.type,
                        fraType = it.fraType,
                        grad = it.grad,
                        fraGrad = it.fraGrad,
                        lovhjemmel = it.lovhjemmel?.tilDatabase(),
                    )
                },
        )

    private fun OverstyrtInntektOgRefusjon.tilDatabase() =
        OverstyrtInntektOgRefusjonForDatabase(
            id = id,
            aktørId = aktørId,
            fødselsnummer = fødselsnummer,
            skjæringstidspunkt = skjæringstidspunkt,
            vedtaksperiodeId = vedtaksperiodeId,
            opprettet = LocalDateTime.now(),
            arbeidsgivere =
                arbeidsgivere.map {
                    OverstyrtArbeidsgiverForDatabase(
                        organisasjonsnummer = it.organisasjonsnummer,
                        månedligInntekt = it.månedligInntekt,
                        fraMånedligInntekt = it.fraMånedligInntekt,
                        refusjonsopplysninger = it.refusjonsopplysninger?.tilDatabase(),
                        fraRefusjonsopplysninger = it.fraRefusjonsopplysninger?.tilDatabase(),
                        begrunnelse = it.begrunnelse,
                        forklaring = it.forklaring,
                        lovhjemmel = it.lovhjemmel?.tilDatabase(),
                        fom = it.fom,
                        tom = it.tom,
                    )
                },
        )

    private fun OverstyrtArbeidsforhold.tilDatabase() =
        OverstyrtArbeidsforholdForDatabase(
            id = id,
            aktørId = aktørId,
            fødselsnummer = fødselsnummer,
            skjæringstidspunkt = skjæringstidspunkt,
            opprettet = LocalDateTime.now(),
            vedtaksperiodeId = vedtaksperiodeId,
            overstyrteArbeidsforhold =
                overstyrteArbeidsforhold.map {
                    ArbeidsforholdForDatabase(
                        organisasjonsnummer = it.organisasjonsnummer,
                        deaktivert = it.deaktivert,
                        forklaring = it.forklaring,
                        begrunnelse = it.begrunnelse,
                    )
                },
        )

    private fun SkjønnsfastsattSykepengegrunnlag.tilDatabase() =
        SkjønnsfastsattSykepengegrunnlagForDatabase(
            id = id,
            aktørId = aktørId,
            fødselsnummer = fødselsnummer,
            skjæringstidspunkt = skjæringstidspunkt,
            opprettet = LocalDateTime.now(),
            vedtaksperiodeId = vedtaksperiodeId,
            arbeidsgivere =
                arbeidsgivere.map {
                    SkjønnsfastsattArbeidsgiverForDatabase(
                        organisasjonsnummer = it.organisasjonsnummer,
                        årlig = it.årlig,
                        fraÅrlig = it.fraÅrlig,
                        årsak = it.årsak,
                        type = it.type.tilDatabase(),
                        begrunnelseMal = it.begrunnelseMal,
                        begrunnelseFritekst = it.begrunnelseFritekst,
                        begrunnelseKonklusjon = it.begrunnelseKonklusjon,
                        lovhjemmel = it.lovhjemmel?.tilDatabase(),
                        initierendeVedtaksperiodeId = it.initierendeVedtaksperiodeId,
                    )
                },
        )

    private fun MinimumSykdomsgrad.tilDatabase() =
        MinimumSykdomsgradForDatabase(
            id = id,
            aktørId = aktørId,
            fødselsnummer = fødselsnummer,
            perioderVurdertOk =
                perioderVurdertOk.map {
                    MinimumSykdomsgradForDatabase.MinimumSykdomsgradPeriodeForDatabase(
                        fom = it.fom,
                        tom = it.tom,
                    )
                },
            perioderVurdertIkkeOk =
                perioderVurdertIkkeOk.map {
                    MinimumSykdomsgradForDatabase.MinimumSykdomsgradPeriodeForDatabase(
                        fom = it.fom,
                        tom = it.tom,
                    )
                },
            begrunnelse = begrunnelse,
            arbeidsgivere =
                arbeidsgivere.map {
                    MinimumSykdomsgradForDatabase.MinimumSykdomsgradArbeidsgiverForDatabase(
                        organisasjonsnummer = it.organisasjonsnummer,
                        berørtVedtaksperiodeId = it.berørtVedtaksperiodeId,
                    )
                },
            opprettet = LocalDateTime.now(),
            initierendeVedtaksperiodeId = initierendeVedtaksperiodeId,
        )

    private fun Lovhjemmel.tilDatabase() = LovhjemmelForDatabase(paragraf = paragraf, ledd = ledd, bokstav = bokstav)

    private fun List<Refusjonselement>.tilDatabase() =
        this.map {
            RefusjonselementForDatabase(fom = it.fom, tom = it.tom, beløp = it.beløp)
        }

    private fun SkjønnsfastsattArbeidsgiver.Skjønnsfastsettingstype.tilDatabase() =
        when (this) {
            SkjønnsfastsattArbeidsgiver.Skjønnsfastsettingstype.OMREGNET_ÅRSINNTEKT -> SkjønnsfastsettingstypeForDatabase.OMREGNET_ÅRSINNTEKT
            SkjønnsfastsattArbeidsgiver.Skjønnsfastsettingstype.RAPPORTERT_ÅRSINNTEKT -> SkjønnsfastsettingstypeForDatabase.RAPPORTERT_ÅRSINNTEKT
            SkjønnsfastsattArbeidsgiver.Skjønnsfastsettingstype.ANNET -> SkjønnsfastsettingstypeForDatabase.ANNET
        }
}
