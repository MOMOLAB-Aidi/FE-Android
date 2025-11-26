package com.example.momolabfe.utils

import com.example.momolabfe.remote.record.data.DayWeek
import java.time.DayOfWeek

fun weekdayShortKorean(dow: DayOfWeek): String = when (dow) {
    DayOfWeek.SUNDAY -> "일"
    DayOfWeek.MONDAY -> "월"
    DayOfWeek.TUESDAY -> "화"
    DayOfWeek.WEDNESDAY -> "수"
    DayOfWeek.THURSDAY -> "목"
    DayOfWeek.FRIDAY -> "금"
    DayOfWeek.SATURDAY -> "토"
}

fun DayOfWeek.toDayWeek(): DayWeek = when (this) {
    DayOfWeek.MONDAY    -> DayWeek.MON
    DayOfWeek.TUESDAY   -> DayWeek.TUE
    DayOfWeek.WEDNESDAY -> DayWeek.WED
    DayOfWeek.THURSDAY  -> DayWeek.THU
    DayOfWeek.FRIDAY    -> DayWeek.FRI
    DayOfWeek.SATURDAY  -> DayWeek.SAT
    DayOfWeek.SUNDAY    -> DayWeek.SUN
}

fun DayWeek.korean(): String = when (this) {
    DayWeek.MON -> "(월)"
    DayWeek.TUE -> "(화)"
    DayWeek.WED -> "(수)"
    DayWeek.THU -> "(목)"
    DayWeek.FRI -> "(금)"
    DayWeek.SAT -> "(토)"
    DayWeek.SUN -> "(일)"
}