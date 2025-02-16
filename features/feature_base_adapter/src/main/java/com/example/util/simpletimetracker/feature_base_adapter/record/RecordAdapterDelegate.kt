package com.example.util.simpletimetracker.feature_base_adapter.record

import androidx.core.view.ViewCompat
import com.example.util.simpletimetracker.feature_base_adapter.createRecyclerBindingAdapterDelegate
import com.example.util.simpletimetracker.feature_views.TransitionNames
import com.example.util.simpletimetracker.feature_views.extension.setOnClick
import com.example.util.simpletimetracker.feature_base_adapter.databinding.ItemRecordLayoutBinding as Binding
import com.example.util.simpletimetracker.feature_base_adapter.record.RecordViewData as ViewData

fun createRecordAdapterDelegate(
    onItemClick: ((ViewData, Map<Any, String>) -> Unit)
) = createRecyclerBindingAdapterDelegate<ViewData, Binding>(
    Binding::inflate
) { binding, item, _ ->

    with(binding.viewRecordItem) {
        item as ViewData
        val transitionName = TransitionNames.RECORD + item.getUniqueId()

        itemColor = item.color
        itemIcon = item.iconId
        itemName = item.name
        itemTagName = item.tagName
        itemTimeStarted = item.timeStarted
        itemTimeEnded = item.timeFinished
        itemDuration = item.duration
        itemComment = item.comment

        setOnClick { onItemClick(item, mapOf(this to transitionName)) }
        ViewCompat.setTransitionName(this, transitionName)
    }
}