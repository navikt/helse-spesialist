package no.nav.helse.spesialist.domain.testfixtures

import java.time.LocalDate

infix fun Int.jan(år: Int): LocalDate = LocalDate.of(år, 1, this)
infix fun Int.feb(år: Int): LocalDate = LocalDate.of(år, 2, this)
infix fun Int.mar(år: Int): LocalDate = LocalDate.of(år, 3, this)
infix fun Int.apr(år: Int): LocalDate = LocalDate.of(år, 4, this)
infix fun Int.mai(år: Int): LocalDate = LocalDate.of(år, 5, this)
infix fun Int.jun(år: Int): LocalDate = LocalDate.of(år, 6, this)
infix fun Int.jul(år: Int): LocalDate = LocalDate.of(år, 7, this)
infix fun Int.aug(år: Int): LocalDate = LocalDate.of(år, 8, this)
infix fun Int.sep(år: Int): LocalDate = LocalDate.of(år, 9, this)
infix fun Int.okt(år: Int): LocalDate = LocalDate.of(år, 10, this)
infix fun Int.nov(år: Int): LocalDate = LocalDate.of(år, 11, this)
infix fun Int.des(år: Int): LocalDate = LocalDate.of(år, 12, this)
