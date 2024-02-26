package no.nav.helse.mediator.overstyring

import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.db.LovhjemmelForDatabase
import no.nav.helse.db.OverstyrtArbeidsgiverForDatabase
import no.nav.helse.db.OverstyrtInntektOgRefusjonForDatabase
import no.nav.helse.db.OverstyrtTidslinjeForDatabase
import no.nav.helse.db.OverstyrtTidslinjedagForDatabase
import no.nav.helse.db.RefusjonselementForDatabase
import no.nav.helse.db.SkjønnsfastsattArbeidsgiverForDatabase
import no.nav.helse.db.SkjønnsfastsattSykepengegrunnlagForDatabase
import no.nav.helse.db.SkjønnsfastsettingstypeForDatabase
import no.nav.helse.modell.overstyring.OverstyringDao
import no.nav.helse.modell.saksbehandler.handlinger.Overstyring
import no.nav.helse.modell.saksbehandler.handlinger.OverstyrtInntektOgRefusjon
import no.nav.helse.modell.saksbehandler.handlinger.OverstyrtTidslinje
import no.nav.helse.modell.saksbehandler.handlinger.SkjønnsfastsattArbeidsgiver
import no.nav.helse.modell.saksbehandler.handlinger.SkjønnsfastsattSykepengegrunnlag
import no.nav.helse.modell.saksbehandler.handlinger.dto.OverstyrtInntektOgRefusjonDto
import no.nav.helse.modell.saksbehandler.handlinger.dto.OverstyrtTidslinjeDto
import no.nav.helse.modell.saksbehandler.handlinger.dto.RefusjonselementDto
import no.nav.helse.modell.saksbehandler.handlinger.dto.SkjønnsfastsattSykepengegrunnlagDto
import no.nav.helse.modell.vilkårsprøving.LovhjemmelDto

class Overstyringlagrer(private val overstyringDao: OverstyringDao) {
    internal fun lagre(overstyring: Overstyring, saksbehandlerOid: UUID) {
        when (overstyring) {
            is OverstyrtTidslinje -> lagreOverstyrTidslinje(overstyring, saksbehandlerOid)
            is OverstyrtInntektOgRefusjon -> lagreOverstyrtInntektOgRefusjon(overstyring, saksbehandlerOid)
            is SkjønnsfastsattSykepengegrunnlag -> lagreSkjønnsfastsattSykepengegrunnlag(overstyring, saksbehandlerOid)
        }
    }

    private fun lagreOverstyrTidslinje(overstyring: OverstyrtTidslinje, saksbehandlerOid: UUID) {
        overstyringDao.persisterOverstyringTidslinje(overstyring.toDto().tilDatabase(), saksbehandlerOid)
    }

    private fun lagreOverstyrtInntektOgRefusjon(overstyring: OverstyrtInntektOgRefusjon, saksbehandlerOid: UUID) {
        overstyringDao.persisterOverstyringInntektOgRefusjon(overstyring.toDto().tilDatabase(), saksbehandlerOid)
    }

    private fun lagreSkjønnsfastsattSykepengegrunnlag(
        overstyring: SkjønnsfastsattSykepengegrunnlag,
        saksbehandlerOid: UUID,
    ) {
        overstyringDao.persisterSkjønnsfastsettingSykepengegrunnlag(overstyring.toDto().tilDatabase(), saksbehandlerOid)
    }

    private fun OverstyrtTidslinjeDto.tilDatabase() = OverstyrtTidslinjeForDatabase(
        id = id,
        aktørId = aktørId,
        fødselsnummer = fødselsnummer,
        organisasjonsnummer = organisasjonsnummer,
        begrunnelse = begrunnelse,
        dager = dager.map {
            OverstyrtTidslinjedagForDatabase(
                dato = it.dato,
                type = it.type,
                fraType = it.fraType,
                grad = it.grad,
                fraGrad = it.fraGrad,
                lovhjemmel = it.lovhjemmel?.tilDatabase(),
            )
        },
        opprettet = LocalDateTime.now(),
    )

    private fun OverstyrtInntektOgRefusjonDto.tilDatabase() = OverstyrtInntektOgRefusjonForDatabase(
        id = id,
        aktørId = aktørId,
        fødselsnummer = fødselsnummer,
        skjæringstidspunkt = skjæringstidspunkt,
        arbeidsgivere = arbeidsgivere.map {
            OverstyrtArbeidsgiverForDatabase(
                organisasjonsnummer = it.organisasjonsnummer,
                månedligInntekt = it.månedligInntekt,
                fraMånedligInntekt = it.fraMånedligInntekt,
                refusjonsopplysninger = it.refusjonsopplysninger?.tilDatabase(),
                fraRefusjonsopplysninger = it.fraRefusjonsopplysninger?.tilDatabase(),
                begrunnelse = it.begrunnelse,
                forklaring = it.forklaring,
                lovhjemmel = it.lovhjemmel?.tilDatabase(),
            )
        },
        opprettet = LocalDateTime.now(),
    )

    private fun SkjønnsfastsattSykepengegrunnlagDto.tilDatabase() = SkjønnsfastsattSykepengegrunnlagForDatabase(
        id = id,
        aktørId = aktørId,
        fødselsnummer = fødselsnummer,
        skjæringstidspunkt = skjæringstidspunkt,
        arbeidsgivere = arbeidsgivere.map {
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
        opprettet = LocalDateTime.now(),
    )

    private fun LovhjemmelDto.tilDatabase() = LovhjemmelForDatabase(paragraf = paragraf, ledd = ledd, bokstav = bokstav)

    private fun List<RefusjonselementDto>.tilDatabase() = this.map {
        RefusjonselementForDatabase(fom = it.fom, tom = it.tom, beløp = it.beløp)
    }

    private fun SkjønnsfastsattArbeidsgiver.Skjønnsfastsettingstype.tilDatabase() = when (this) {
        SkjønnsfastsattArbeidsgiver.Skjønnsfastsettingstype.OMREGNET_ÅRSINNTEKT -> SkjønnsfastsettingstypeForDatabase.OMREGNET_ÅRSINNTEKT
        SkjønnsfastsattArbeidsgiver.Skjønnsfastsettingstype.RAPPORTERT_ÅRSINNTEKT -> SkjønnsfastsettingstypeForDatabase.RAPPORTERT_ÅRSINNTEKT
        SkjønnsfastsattArbeidsgiver.Skjønnsfastsettingstype.ANNET -> SkjønnsfastsettingstypeForDatabase.ANNET
    }
}