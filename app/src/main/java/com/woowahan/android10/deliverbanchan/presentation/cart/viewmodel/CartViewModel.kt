package com.woowahan.android10.deliverbanchan.presentation.cart.viewmodel

import android.util.Log
import androidx.lifecycle.*
import com.woowahan.android10.deliverbanchan.BanChanApplication
import com.woowahan.android10.deliverbanchan.di.IoDispatcher
import com.woowahan.android10.deliverbanchan.domain.model.TempOrder
import com.woowahan.android10.deliverbanchan.domain.model.UiCartOrderDishJoinItem
import com.woowahan.android10.deliverbanchan.domain.model.UiDishItem
import com.woowahan.android10.deliverbanchan.domain.model.UiOrderInfo
import com.woowahan.android10.deliverbanchan.domain.usecase.CartUseCase
import com.woowahan.android10.deliverbanchan.domain.usecase.GetAllOrderInfoListUseCase
import com.woowahan.android10.deliverbanchan.domain.usecase.RecentUseCase
import com.woowahan.android10.deliverbanchan.presentation.cart.model.UiCartBottomBody
import com.woowahan.android10.deliverbanchan.presentation.cart.model.UiCartCompleteHeader
import com.woowahan.android10.deliverbanchan.presentation.cart.model.UiCartHeader
import com.woowahan.android10.deliverbanchan.presentation.state.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CartViewModel @Inject constructor(
    private val getAllOrderInfoListUseCase: GetAllOrderInfoListUseCase,
    private val cartUseCase: CartUseCase,
    private val recentUseCase: RecentUseCase,
    @IoDispatcher private val dispatcher: CoroutineDispatcher
) : ViewModel() {

    companion object {
        const val TAG = "CartViewModel"
    }

    val fragmentArrayIndex = MutableLiveData(0)

    val appBarTitle = MutableLiveData("")
    val orderDetailMode = MutableLiveData(false)

    private val _allCartJoinState =
        MutableStateFlow<UiState<List<UiCartOrderDishJoinItem>>>(UiState.Init)
    val allCartJoinState: StateFlow<UiState<List<UiCartOrderDishJoinItem>>>
        get() = _allCartJoinState.stateIn(
            initialValue = UiState.Init,
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000)
        )

    private val _allRecentSevenJoinState =
        MutableStateFlow<UiState<List<UiDishItem>>>(UiState.Init)
    val allRecentSevenJoinState: StateFlow<UiState<List<UiDishItem>>>
        get() = _allRecentSevenJoinState.stateIn(
            initialValue = UiState.Init,
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000)
        )

    private var _itemCartBottomBodyProductTotalPrice = 0
    var currentOrderTimeStamp = 0L

    private val _itemCartHeaderData = MutableLiveData(UiCartHeader.emptyItem())
    val itemCartHeaderData: LiveData<UiCartHeader> get() = _itemCartHeaderData

    private val _uiCartJoinList = MutableLiveData<List<UiCartOrderDishJoinItem>>(emptyList())
    val uiCartJoinList: LiveData<List<UiCartOrderDishJoinItem>> get() = _uiCartJoinList
    private val _uiCartJoinArrayList = ArrayList<UiCartOrderDishJoinItem>()

    private val _itemCartBottomBodyData = MutableLiveData(UiCartBottomBody.emptyItem())
    val itemCartBottomBodyData: LiveData<UiCartBottomBody> get() = _itemCartBottomBodyData

    private val _selectedCartItem = mutableSetOf<TempOrder>()

    private val _toBeDeletedCartItem = mutableSetOf<String>()

    private val _orderCompleteTopItem = MutableStateFlow(UiCartCompleteHeader.emptyItem())
    val orderCompleteTopItem: StateFlow<UiCartCompleteHeader> get() = _orderCompleteTopItem

    private val _orderCompleteBodyItem =
        MutableStateFlow<List<UiCartOrderDishJoinItem>>(emptyList())
    val orderCompleteBodyItem: StateFlow<List<UiCartOrderDishJoinItem>> get() = _orderCompleteBodyItem

    private val _orderCompleteFooterItem = MutableStateFlow(UiOrderInfo.emptyItem())
    val orderCompleteFooterItem: StateFlow<UiOrderInfo> get() = _orderCompleteFooterItem

    private val _orderButtonClicked = MutableSharedFlow<Boolean>()
    val orderButtonClicked: SharedFlow<Boolean> = _orderButtonClicked.asSharedFlow()
    val orderBtnClickLiveData = _orderButtonClicked.asLiveData()

    val orderHashList = ArrayList<String>()
    var orderFirstItemTitle = "Title"

    private val _reloadBtnClicked = MutableLiveData(false)
    val reloadBtnClicked: LiveData<Boolean> get() = _reloadBtnClicked

    var forInitValue: Boolean? = true

    init {
        getAllRecentSevenJoinList()
        getAllCartJoinList()
        observeOrderInfo()
    }

    fun setReloadBtnValue() {
        _reloadBtnClicked.value = true
    }

    private fun observeOrderInfo() {
        viewModelScope.launch {
            getAllOrderInfoListUseCase(this).collect {
                val orderListTimeStampMap = it.groupBy { it.timeStamp }
                if (_orderCompleteBodyItem.value.isNotEmpty()) {
                    if (orderListTimeStampMap.keys.contains(currentOrderTimeStamp)) {
                        _orderCompleteTopItem.value =
                            _orderCompleteTopItem.value.copy(
                                isDelivering = orderListTimeStampMap[currentOrderTimeStamp]!!.first().isDelivering
                            )
                    }
                }
            }
        }
    }

    private fun getAllCartJoinList() = viewModelScope.launch {
        cartUseCase.getCartJoinList().onStart {
            _allCartJoinState.value = UiState.Loading(true)
        }.flowOn(dispatcher).catch { exception ->
            _allCartJoinState.value = UiState.Loading(false)
            _allCartJoinState.value = UiState.Error(exception.message.toString())
        }.onEach { uiCartJoinItemList ->
            //calcCartBottomBodyAndHeaderVal(uiCartJoinItemList)
        }.collect {
            Log.e(TAG, "getAllCartJoinList: 카트조인리스트 컬렉트------------------------", )
            _allCartJoinState.value = UiState.Loading(false)
            _allCartJoinState.value = UiState.Success(it)
            _uiCartJoinArrayList.clear()
            _uiCartJoinArrayList.addAll(it)
            _uiCartJoinList.value = _uiCartJoinArrayList
        }
    }

    fun calcCartBottomBodyAndHeaderVal(uiCartOrderDishJoinItemList: List<UiCartOrderDishJoinItem>) {
        _itemCartBottomBodyProductTotalPrice = 0
        _selectedCartItem.clear()
        val checkedUiJoinCartItem = uiCartOrderDishJoinItemList.filter { it.checked }
        checkedUiJoinCartItem.forEach { checkedUiCartJoinItem ->
            _selectedCartItem.add(
                TempOrder(
                    checkedUiCartJoinItem.hash,
                    checkedUiCartJoinItem.amount,
                    checkedUiCartJoinItem.title
                )
            )
            _itemCartBottomBodyProductTotalPrice += checkedUiCartJoinItem.totalPrice
        }
        if (checkedUiJoinCartItem.size == uiCartOrderDishJoinItemList.size) {
            _itemCartHeaderData.value = UiCartHeader(
                checkBoxText = UiCartHeader.TEXT_SELECT_RELEASE,
                checkBoxChecked = true
            )
        } else {
            _itemCartHeaderData.value = UiCartHeader(
                checkBoxText = UiCartHeader.TEXT_SELECT_ALL,
                checkBoxChecked = false
            )
        }
        setItemCartBottomBodyData()
    }

    private fun setItemCartBottomBodyData() {
        var deliveryPrice = 2500
        var totalPrice = _itemCartBottomBodyProductTotalPrice + deliveryPrice
        val isAvailableDelivery =
            _itemCartBottomBodyProductTotalPrice >= UiCartBottomBody.MIN_DELIVERY_PRICE
        var isAvailableFreeDelivery = false
        if (_itemCartBottomBodyProductTotalPrice >= UiCartBottomBody.DELIVERY_FREE_PRICE) {
            totalPrice -= deliveryPrice
            deliveryPrice = 0
            if (isAvailableDelivery) isAvailableFreeDelivery = true
        }
        _itemCartBottomBodyData.value = UiCartBottomBody(
            productTotalPrice = _itemCartBottomBodyProductTotalPrice,
            deliveryPrice = deliveryPrice,
            totalPrice = totalPrice,
            isAvailableDelivery = isAvailableDelivery,
            isAvailableFreeDelivery = isAvailableFreeDelivery and isAvailableDelivery,
        )
    }

    private fun getAllRecentSevenJoinList() = viewModelScope.launch {
        cartUseCase.getAllRecentJoinListLimitSeven().onStart {
            _allRecentSevenJoinState.value = UiState.Loading(true)
        }.flowOn(dispatcher).catch { exception ->
            _allRecentSevenJoinState.value = UiState.Loading(false)
            _allRecentSevenJoinState.value = UiState.Error(exception.message.toString())
        }.collect {
            _allRecentSevenJoinState.value = UiState.Loading(false)
            _allRecentSevenJoinState.value = UiState.Success(it)
        }
    }

    fun setAppBarTitle(string: String) {
        appBarTitle.value = string
    }

    fun updateUiCartCheckedValue(position: Int, checked: Boolean) {
        if (position == -1) return // 클릭했을때, 없어진 view의 경우 position == -1
        _uiCartJoinArrayList[position].checked = checked
        _uiCartJoinList.value = _uiCartJoinArrayList
        _uiCartJoinList.value!!.forEach {
            Log.e(TAG, "updateUiCartCheckedValue: ${it.title} ${it.checked}", )
        }
    }

    fun updateUiCartAmountValue(position: Int, amount: Int) {
        if (position == -1) return // 클릭했을때, 없어진 view의 경우 position == -1
        _uiCartJoinArrayList[position].apply {
            this.amount = amount
            totalPrice = sPrice * amount
        }
        _uiCartJoinList.value = _uiCartJoinArrayList
    }

    fun deleteUiCartItemByPos(position: Int, hash: String) {
        if (position == -1) return // 클릭했을때, 없어진 view의 경우 position == -1
        _toBeDeletedCartItem.add(hash)
        _uiCartJoinArrayList.removeAt(position)
        _uiCartJoinList.value = _uiCartJoinArrayList
    }

    fun deleteUiCartItemByHash(completion: (complete: Boolean) -> Unit) {
        val success = _uiCartJoinArrayList.removeAll(
            _uiCartJoinArrayList.filter {
                _selectedCartItem.contains(TempOrder(it.hash, it.amount, it.title))
            }.toSet()
        )
        _toBeDeletedCartItem.addAll(_selectedCartItem.map { it.hash })
        _uiCartJoinList.value = _uiCartJoinArrayList
        completion(true)
    }

    fun changeCheckedState(checkedValue: Boolean) {
        _uiCartJoinList.value = _uiCartJoinArrayList.onEach {
            it.checked = checkedValue
        }
        _uiCartJoinList.value!!.forEach {
            Log.e(TAG, "updateUiCartCheckedValue: ${it.title} ${it.checked}", )
        }
    }

    fun setOrderCompleteCartItem() {
        // 주문 완료 화면에 대한 리스트 세팅
        val tempHashList = _selectedCartItem.map { it.hash }.toList()
        _orderCompleteBodyItem.value =
            _uiCartJoinArrayList.filter { tempHashList.contains(it.hash) }.sortedBy { it.title }.toList()
        orderFirstItemTitle = _orderCompleteBodyItem.value.first().title
        val deliveryPrice = _itemCartBottomBodyData.value!!.deliveryPrice
        val priceTotal = _orderCompleteBodyItem.value
            .map { Pair(it.sPrice, it.amount) }
            .fold(0) { acc, pair ->
                acc + pair.first * pair.second
            }
        val totalPrice = priceTotal + deliveryPrice
        val orderItemCount = _orderCompleteBodyItem.value.map { it.amount }
            .reduce { sum, eachAmount -> sum + eachAmount }
        _orderCompleteTopItem.value = UiCartCompleteHeader(
            isDelivering = true,
            orderTimeStamp = System.currentTimeMillis(),
            orderItemCount = orderItemCount
        )
        _orderCompleteFooterItem.value = UiOrderInfo(
            itemPrice = priceTotal,
            deliveryFee = deliveryPrice,
            totalPrice = totalPrice
        )
        orderDetailMode.value = true
        insertOrderInfoDeleteCartInfo()
    }

    private fun insertOrderInfoDeleteCartInfo() {
        BanChanApplication.applicationScope.launch {
            currentOrderTimeStamp = System.currentTimeMillis()
            orderHashList.clear()
            //orderFirstItemTitle = "Title"
            orderHashList.addAll(_selectedCartItem.map { it.hash })
//            _selectedCartItem.forEachIndexed { index, tempOrder ->
//                //if (index == 0) orderFirstItemTitle = tempOrder.title
//                orderHashList.add(tempOrder.hash)
//            }
            cartUseCase.insertVarArgOrderInfo(
                tempOrderSet = _selectedCartItem,
                timeStamp = currentOrderTimeStamp,
                isDelivering = true,
                deliveryPrice = _itemCartBottomBodyData.value!!.deliveryPrice
            )
            _orderButtonClicked.emit(true)
            _selectedCartItem.forEach {
                Log.e(TAG, "insertOrderInfoDeleteCartInfo: 지워질것들: ${it.title} ${it.hash}", )
            }
            cartUseCase.insertAndDeleteCartItems(
                _uiCartJoinList.value!!, _toBeDeletedCartItem.toList()
            )
            recentUseCase.updateVarArgRecentIsInsertedFalseInCartUseCase(_toBeDeletedCartItem.toList())
            recentUseCase.updateVarArgRecentIsInsertedFalseInCartUseCase(_selectedCartItem.map { it.hash }.toList())
            cartUseCase.deleteCartInfoByHashList(_selectedCartItem.map { it.hash }.toList())
        }
    }

    fun updateAllCartItemChanged() {
        BanChanApplication.applicationScope.launch {

            Log.e(TAG, "--------: 업데이트 아이템 ---------", )
            _uiCartJoinList.value!!.forEach {
                Log.e(TAG, "업뎃되는 아이템: ${it.title} ${it.checked} ${it.amount}", )
            }
            _toBeDeletedCartItem.forEach {
                Log.e(TAG, "지워지는 아이템: $it", )
            }

            cartUseCase.insertAndDeleteCartItems(
                _uiCartJoinList.value!!, _toBeDeletedCartItem.toList()
            )
            recentUseCase.updateVarArgRecentIsInsertedFalseInCartUseCase(_toBeDeletedCartItem.toList())
            _toBeDeletedCartItem.clear()
        }
    }
}