package com.example.util.simpletimetracker.core.mapper

import com.example.util.simpletimetracker.core.R
import com.example.util.simpletimetracker.core.repo.ResourceRepo
import com.example.util.simpletimetracker.domain.model.Category
import com.example.util.simpletimetracker.domain.model.RecordTag
import com.example.util.simpletimetracker.domain.model.RecordType
import com.example.util.simpletimetracker.feature_base_adapter.ViewHolderType
import com.example.util.simpletimetracker.feature_base_adapter.category.CategoryViewData
import com.example.util.simpletimetracker.feature_base_adapter.empty.EmptyViewData
import com.example.util.simpletimetracker.feature_base_adapter.info.InfoViewData
import com.example.util.simpletimetracker.feature_views.viewData.RecordTypeIcon
import javax.inject.Inject

class CategoryViewDataMapper @Inject constructor(
    private val colorMapper: ColorMapper,
    private val iconMapper: IconMapper,
    private val resourceRepo: ResourceRepo
) {

    fun mapActivityTag(
        category: Category,
        isDarkTheme: Boolean,
        isFiltered: Boolean = false
    ): CategoryViewData.Activity {
        return CategoryViewData.Activity(
            id = category.id,
            name = category.name,
            iconColor = getTextColor(isDarkTheme, isFiltered),
            color = getColor(category.color, isDarkTheme, isFiltered)
        )
    }

    fun mapRecordTag(
        tag: RecordTag,
        type: RecordType?,
        isDarkTheme: Boolean,
        isFiltered: Boolean = false,
        showIcon: Boolean = true
    ): CategoryViewData.Record {
        val isTyped = tag.typeId != 0L
        val icon = type?.icon?.let(iconMapper::mapIcon).takeIf { isTyped }
        val color = type?.color.takeIf { isTyped } ?: tag.color

        return CategoryViewData.Record.Tagged(
            id = tag.id,
            name = tag.name,
            iconColor = getTextColor(isDarkTheme, isFiltered),
            iconAlpha = getIconAlpha(icon, isFiltered),
            color = getColor(color, isDarkTheme, isFiltered),
            icon = if (showIcon) icon else null
        )
    }

    fun mapToRecordTagsEmpty(): List<ViewHolderType> {
        return EmptyViewData(
            message = resourceRepo.getString(R.string.change_record_categories_empty)
        ).let(::listOf)
    }

    // TODO remove?
    fun mapToTypeNotSelected(): List<ViewHolderType> {
        return EmptyViewData(
            message = resourceRepo.getString(R.string.change_record_activity_not_selected)
        ).let(::listOf)
    }

    fun mapSelectedCategoriesHint(isEmpty: Boolean): ViewHolderType {
        return InfoViewData(
            text = if (isEmpty) {
                R.string.change_record_type_selected_categories_empty
            } else {
                R.string.change_record_type_selected_categories_hint
            }.let(resourceRepo::getString)
        )
    }

    fun getTextColor(
        isDarkTheme: Boolean,
        isFiltered: Boolean
    ): Int {
        return if (isFiltered) {
            colorMapper.toFilteredIconColor(isDarkTheme)
        } else {
            colorMapper.toIconColor(isDarkTheme)
        }
    }

    fun getIconAlpha(icon: RecordTypeIcon?, isFiltered: Boolean): Float {
        return if (icon is RecordTypeIcon.Emoji && isFiltered) {
            FILTERED_ICON_EMOJI_ALPHA
        } else {
            DEFAULT_ICON_EMOJI_ALPHA
        }
    }

    private fun getColor(
        colorId: Int,
        isDarkTheme: Boolean,
        isFiltered: Boolean
    ): Int {
        return if (isFiltered) {
            colorMapper.toFilteredColor(isDarkTheme)
        } else {
            colorId
                .let { colorMapper.mapToColorResId(it, isDarkTheme) }
                .let(resourceRepo::getColor)
        }
    }

    companion object {
        private const val DEFAULT_ICON_EMOJI_ALPHA = 1.0f
        private const val FILTERED_ICON_EMOJI_ALPHA = 0.3f
    }
}