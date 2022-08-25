package com.woowahan.android10.deliverbanchan.domain.usecase

import com.woowahan.android10.deliverbanchan.data.remote.model.response.BaseResult
import com.woowahan.android10.deliverbanchan.domain.model.UiExhibitionItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GetUiExhibitionItemsUseCase @Inject constructor(
    private val getExhibitionListUseCase: GetExhibitionListUseCase,
    private val mapDishItemListToUiDishItemListUseCase: MapDishItemListToUiDishItemListUseCase
) {
    suspend operator fun invoke(): Flow<BaseResult<List<UiExhibitionItem>, Int>> {
        return getExhibitionListUseCase().map { response ->
            when (response) {
                is BaseResult.Success -> {
                    BaseResult.Success(
                        response.data.map { categoryItem ->
                            UiExhibitionItem(
                                categoryId = categoryItem.categoryId,
                                categoryName = categoryItem.name,
                                uiDishItems = mapDishItemListToUiDishItemListUseCase(categoryItem.items)
                            )
                        }
                    )
                }
                is BaseResult.Error -> {
                    BaseResult.Error(errorCode = response.errorCode)
                }
            }
        }
    }
}