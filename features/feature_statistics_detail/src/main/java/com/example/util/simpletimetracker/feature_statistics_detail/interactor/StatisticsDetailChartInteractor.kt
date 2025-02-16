package com.example.util.simpletimetracker.feature_statistics_detail.interactor

import com.example.util.simpletimetracker.core.extension.isNotFiltered
import com.example.util.simpletimetracker.core.extension.setWeekToFirstDay
import com.example.util.simpletimetracker.core.interactor.TypesFilterInteractor
import com.example.util.simpletimetracker.core.mapper.RangeMapper
import com.example.util.simpletimetracker.core.mapper.TimeMapper
import com.example.util.simpletimetracker.domain.interactor.RecordInteractor
import com.example.util.simpletimetracker.domain.model.DayOfWeek
import com.example.util.simpletimetracker.domain.model.RangeLength
import com.example.util.simpletimetracker.feature_statistics_detail.mapper.StatisticsDetailViewDataMapper
import com.example.util.simpletimetracker.feature_statistics_detail.model.ChartBarDataDuration
import com.example.util.simpletimetracker.feature_statistics_detail.model.ChartBarDataRange
import com.example.util.simpletimetracker.feature_statistics_detail.model.ChartGrouping
import com.example.util.simpletimetracker.feature_statistics_detail.model.ChartLength
import com.example.util.simpletimetracker.feature_statistics_detail.viewData.StatisticsDetailChartViewData
import com.example.util.simpletimetracker.navigation.params.screen.TypesFilterParams
import java.util.Calendar
import javax.inject.Inject

class StatisticsDetailChartInteractor @Inject constructor(
    private val recordInteractor: RecordInteractor,
    private val timeMapper: TimeMapper,
    private val rangeMapper: RangeMapper,
    private val typesFilterInteractor: TypesFilterInteractor,
    private val statisticsDetailViewDataMapper: StatisticsDetailViewDataMapper
) {

    suspend fun getChartViewData(
        filter: TypesFilterParams,
        chartGrouping: ChartGrouping,
        chartLength: ChartLength,
        rangeLength: RangeLength,
        rangePosition: Int,
        firstDayOfWeek: DayOfWeek
    ): StatisticsDetailChartViewData {
        val data = getChartData(
            filter = filter,
            grouping = chartGrouping,
            chartLength = chartLength,
            rangeLength = rangeLength,
            rangePosition = rangePosition,
            firstDayOfWeek = firstDayOfWeek
        )

        return statisticsDetailViewDataMapper.mapToChartViewData(data, rangeLength)
    }

    private suspend fun getChartData(
        filter: TypesFilterParams,
        grouping: ChartGrouping,
        chartLength: ChartLength,
        rangeLength: RangeLength,
        rangePosition: Int,
        firstDayOfWeek: DayOfWeek
    ): List<ChartBarDataDuration> {
        val ranges: List<ChartBarDataRange> = getRanges(
            grouping = grouping,
            chartLength = chartLength,
            rangeLength = rangeLength,
            rangePosition = rangePosition,
            firstDayOfWeek = firstDayOfWeek
        )
        val typeIds = typesFilterInteractor.getTypeIds(filter)

        val records = recordInteractor.getFromRange(
            start = ranges.first().rangeStart,
            end = ranges.last().rangeEnd
        ).filter { it.typeId in typeIds && it.isNotFiltered(filter) }

        if (records.isEmpty()) {
            return ranges.map { ChartBarDataDuration(legend = it.legend, duration = 0L) }
        }

        return ranges
            .map { data ->
                val duration = rangeMapper.getRecordsFromRange(records, data.rangeStart, data.rangeEnd)
                    .map { record -> rangeMapper.clampToRange(record, data.rangeStart, data.rangeEnd) }
                    .let(rangeMapper::mapToDuration)

                ChartBarDataDuration(
                    legend = data.legend,
                    duration = duration
                )
            }
    }

    private fun getRanges(
        grouping: ChartGrouping,
        chartLength: ChartLength,
        rangeLength: RangeLength,
        rangePosition: Int,
        firstDayOfWeek: DayOfWeek
    ): List<ChartBarDataRange> {
        return when (rangeLength) {
            RangeLength.DAY -> {
                val startDate = timeMapper.getRangeStartAndEnd(
                    RangeLength.DAY, rangePosition, firstDayOfWeek
                ).second - 1
                val numberOfGroups = 1
                getDailyGrouping(startDate, numberOfGroups)
            }
            RangeLength.WEEK -> {
                val startDate = timeMapper.getRangeStartAndEnd(
                    RangeLength.WEEK, rangePosition, firstDayOfWeek
                ).second - 1
                val numberOfGroups = 7
                getDailyGrouping(startDate, numberOfGroups)
            }
            RangeLength.MONTH -> {
                val startDate = timeMapper.getRangeStartAndEnd(
                    RangeLength.MONTH, rangePosition, firstDayOfWeek
                ).second - 1
                val numberOfGroups = Calendar.getInstance()
                    .apply { timeInMillis = startDate }
                    .getActualMaximum(Calendar.DAY_OF_MONTH)
                getDailyGrouping(startDate, numberOfGroups)
            }
            RangeLength.YEAR -> {
                val startDate = timeMapper.getRangeStartAndEnd(
                    RangeLength.YEAR, rangePosition, firstDayOfWeek
                ).second - 1
                when (grouping) {
                    ChartGrouping.DAILY -> {
                        val numberOfGroups = Calendar.getInstance()
                            .apply { timeInMillis = startDate }
                            .getActualMaximum(Calendar.DAY_OF_YEAR)
                        getDailyGrouping(startDate, numberOfGroups)
                    }
                    ChartGrouping.WEEKLY -> {
                        val dayOfWeek = timeMapper.toCalendarDayOfWeek(firstDayOfWeek)
                        val numberOfGroups = Calendar.getInstance()
                            .apply { timeInMillis = startDate }
                            .apply { this.firstDayOfWeek = dayOfWeek }
                            .getActualMaximum(Calendar.WEEK_OF_YEAR)
                        getWeeklyGrouping(startDate, numberOfGroups, firstDayOfWeek)
                    }
                    else -> {
                        val numberOfGroups = 12
                        getMonthlyGrouping(startDate, numberOfGroups)
                    }
                }
            }
            RangeLength.ALL -> {
                val startDate = System.currentTimeMillis()
                val numberOfGroups = getNumberOfGroups(chartLength)
                when (grouping) {
                    ChartGrouping.DAILY -> getDailyGrouping(startDate, numberOfGroups)
                    ChartGrouping.WEEKLY -> getWeeklyGrouping(startDate, numberOfGroups, firstDayOfWeek)
                    ChartGrouping.MONTHLY -> getMonthlyGrouping(startDate, numberOfGroups)
                    ChartGrouping.YEARLY -> getYearlyGrouping(numberOfGroups)
                }
            }
        }
    }

    private fun getNumberOfGroups(
        chartLength: ChartLength
    ): Int {
        return when (chartLength) {
            ChartLength.TEN -> 10
            ChartLength.FIFTY -> 50
            ChartLength.HUNDRED -> 100
        }
    }

    private fun getDailyGrouping(
        startDate: Long,
        numberOfDays: Int
    ): List<ChartBarDataRange> {
        val calendar = Calendar.getInstance()

        return (numberOfDays - 1 downTo 0).map { shift ->
            calendar.apply {
                timeInMillis = startDate
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            calendar.add(Calendar.DATE, -shift)

            val legend = timeMapper.formatShortDay(calendar.timeInMillis)
            val rangeStart = calendar.timeInMillis
            val rangeEnd = calendar.apply { add(Calendar.DATE, 1) }.timeInMillis

            ChartBarDataRange(
                legend = legend,
                rangeStart = rangeStart,
                rangeEnd = rangeEnd
            )
        }
    }

    private fun getWeeklyGrouping(
        startDate: Long,
        numberOfWeeks: Int,
        firstDayOfWeek: DayOfWeek
    ): List<ChartBarDataRange> {
        val calendar = Calendar.getInstance()
        val dayOfWeek = timeMapper.toCalendarDayOfWeek(firstDayOfWeek)

        return (numberOfWeeks - 1 downTo 0).map { shift ->
            calendar.apply {
                this.firstDayOfWeek = dayOfWeek
                timeInMillis = startDate
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            calendar.setWeekToFirstDay()
            calendar.add(Calendar.DATE, -shift * 7)

            val legend = timeMapper.formatShortMonth(calendar.timeInMillis)
            val rangeStart = calendar.timeInMillis
            val rangeEnd = calendar.apply { add(Calendar.DATE, 7) }.timeInMillis

            ChartBarDataRange(
                legend = legend,
                rangeStart = rangeStart,
                rangeEnd = rangeEnd
            )
        }
    }

    private fun getMonthlyGrouping(
        startDate: Long,
        numberOfMonths: Int
    ): List<ChartBarDataRange> {
        val calendar = Calendar.getInstance()

        return (numberOfMonths - 1 downTo 0).map { shift ->
            calendar.apply {
                timeInMillis = startDate
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            calendar.set(Calendar.DAY_OF_MONTH, 1)
            calendar.add(Calendar.MONTH, -shift)

            val legend = timeMapper.formatShortMonth(calendar.timeInMillis)
            val rangeStart = calendar.timeInMillis
            val rangeEnd = calendar.apply { add(Calendar.MONTH, 1) }.timeInMillis

            ChartBarDataRange(
                legend = legend,
                rangeStart = rangeStart,
                rangeEnd = rangeEnd
            )
        }
    }

    private fun getYearlyGrouping(
        numberOfYears: Int
    ): List<ChartBarDataRange> {
        val calendar = Calendar.getInstance()

        return (numberOfYears - 1 downTo 0).map { shift ->
            calendar.apply {
                timeInMillis = System.currentTimeMillis()
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            calendar.set(Calendar.DAY_OF_YEAR, 1)
            calendar.add(Calendar.YEAR, -shift)

            val legend = timeMapper.formatShortYear(calendar.timeInMillis)
            val rangeStart = calendar.timeInMillis
            val rangeEnd = calendar.apply { add(Calendar.YEAR, 1) }.timeInMillis

            ChartBarDataRange(
                legend = legend,
                rangeStart = rangeStart,
                rangeEnd = rangeEnd
            )
        }
    }
}