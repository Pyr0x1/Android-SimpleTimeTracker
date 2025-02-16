package com.example.util.simpletimetracker.feature_change_running_record.interactor

import com.example.util.simpletimetracker.domain.extension.orZero
import com.example.util.simpletimetracker.domain.interactor.PrefsInteractor
import com.example.util.simpletimetracker.domain.interactor.RecordTagInteractor
import com.example.util.simpletimetracker.domain.interactor.RecordTypeInteractor
import com.example.util.simpletimetracker.domain.model.RunningRecord
import com.example.util.simpletimetracker.feature_change_running_record.mapper.ChangeRunningRecordViewDataMapper
import com.example.util.simpletimetracker.feature_change_running_record.viewData.ChangeRunningRecordViewData
import javax.inject.Inject

class ChangeRunningRecordViewDataInteractor @Inject constructor(
    private val prefsInteractor: PrefsInteractor,
    private val recordTypeInteractor: RecordTypeInteractor,
    private val recordTagInteractor: RecordTagInteractor,
    private val changeRunningRecordViewDataMapper: ChangeRunningRecordViewDataMapper
) {

    suspend fun getPreviewViewData(record: RunningRecord): ChangeRunningRecordViewData {
        val type = recordTypeInteractor.get(record.id)
        val tag = recordTagInteractor.get(record.tagIds.firstOrNull().orZero())
        val isDarkTheme = prefsInteractor.getDarkMode()
        val useMilitaryTime = prefsInteractor.getUseMilitaryTimeFormat()

        return changeRunningRecordViewDataMapper.map(
            runningRecord = record,
            recordType = type,
            recordTag = tag,
            isDarkTheme = isDarkTheme,
            useMilitaryTime = useMilitaryTime
        )
    }
}