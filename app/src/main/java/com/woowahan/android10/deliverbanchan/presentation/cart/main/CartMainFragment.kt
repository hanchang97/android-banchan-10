package com.woowahan.android10.deliverbanchan.presentation.cart.main

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import android.view.View
import androidx.core.content.ContextCompat.getSystemService
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ConcatAdapter
import com.woowahan.android10.deliverbanchan.R
import com.woowahan.android10.deliverbanchan.background.DeliveryReceiver
import com.woowahan.android10.deliverbanchan.databinding.FragmentCartMainBinding
import com.woowahan.android10.deliverbanchan.domain.model.UiCartJoinItem
import com.woowahan.android10.deliverbanchan.domain.model.UiDishItem
import com.woowahan.android10.deliverbanchan.presentation.base.BaseFragment
import com.woowahan.android10.deliverbanchan.presentation.cart.CartViewModel
import com.woowahan.android10.deliverbanchan.presentation.cart.main.adapter.CartDishTopBodyAdapter
import com.woowahan.android10.deliverbanchan.presentation.cart.main.adapter.CartOrderInfoBottomBodyAdapter
import com.woowahan.android10.deliverbanchan.presentation.cart.main.adapter.CartRecentViewedFooterAdapter
import com.woowahan.android10.deliverbanchan.presentation.cart.main.adapter.CartSelectHeaderAdapter
import com.woowahan.android10.deliverbanchan.presentation.common.ORDER_REQUEST_CODE
import com.woowahan.android10.deliverbanchan.presentation.common.ext.showToast
import com.woowahan.android10.deliverbanchan.presentation.state.UiLocalState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

@AndroidEntryPoint
class CartMainFragment : BaseFragment<FragmentCartMainBinding>(
    R.layout.fragment_cart_main, "CartMainFragment"
) {

    @Inject
    lateinit var cartHeaderAdapter: CartSelectHeaderAdapter

    @Inject
    lateinit var cartTopBodyAdapter: CartDishTopBodyAdapter

    @Inject
    lateinit var cartBottomBodyAdapter: CartOrderInfoBottomBodyAdapter

    @Inject
    lateinit var cartRecentViewedFooterAdapter: CartRecentViewedFooterAdapter
    private val concatAdapter: ConcatAdapter by lazy {
        ConcatAdapter(
            cartHeaderAdapter,
            cartTopBodyAdapter,
            cartBottomBodyAdapter,
            cartRecentViewedFooterAdapter
        )
    }
    private val cartViewModel: CartViewModel by activityViewModels()
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initInterface()
        initAdapterList()
        initRecyclerView()
        observeOrderButtonClickEvent()
    }

    private fun initInterface() {
        cartBottomBodyAdapter.onCartBottomBodyItemClickListener =
            object : CartOrderInfoBottomBodyAdapter.OnCartBottomBodyItemClickListener {
                override fun onClickOrderBtn() {
                    cartViewModel.setOrderCompleteCartItem()
                }
            }
        cartHeaderAdapter.onCartTopBodyItemClickListener =
            object : CartSelectHeaderAdapter.OnCartTopBodyItemClickListener {
                override fun onClickSelectedDelete() {
                    cartViewModel.deleteUiCartItemByHash { success ->
                        if (success) requireContext().showToast("삭제에 성공했습니다.")
                        else requireContext().showToast("삭제에 실패했습니다.")
                    }
                }

                override fun onClickSelectedStateChange(checkedState: Boolean) {
                    cartViewModel.changeCheckedState(!checkedState)
                }
            }
        cartTopBodyAdapter.onCartTopBodyItemClickListener =
            object : CartDishTopBodyAdapter.OnCartTopBodyItemClickListener {
                override fun onClickDeleteBtn(position: Int, hash: String) {
                    cartViewModel.deleteUiCartItemByPos(position, hash)
                    cartTopBodyAdapter.notifyItemChanged(position)
                }

                override fun onCheckBoxCheckedChanged(
                    position: Int,
                    hash: String,
                    checked: Boolean
                ) {
                    cartViewModel.updateUiCartCheckedValue(position, !checked)
                    cartTopBodyAdapter.notifyItemChanged(position)
                }

                override fun onClickAmountBtn(position: Int, hash: String, amount: Int) {
                    cartViewModel.updateUiCartAmountValue(position, amount)
                    cartTopBodyAdapter.notifyItemChanged(position)
                }
            }
        cartRecentViewedFooterAdapter.onCartFooterItemClickListener =
            object : CartRecentViewedFooterAdapter.OnCartFooterItemClickListener {
                override fun onClickShowAllBtn() {
                    cartViewModel.fragmentArrayIndex.value = 2
                }
            }
    }

    private fun initAdapterList() {
        with(cartViewModel) {
            allCartJoinState.flowWithLifecycle(lifecycle).onEach { uiLocalState ->
                handleState(cartTopBodyAdapter, uiLocalState)
            }.launchIn(lifecycleScope)

            allRecentlyJoinState.flowWithLifecycle(lifecycle).onEach { uiLocalState ->
                handleState(cartRecentViewedFooterAdapter, uiLocalState)
            }.launchIn(lifecycleScope)

            itemCartHeaderData.observe(viewLifecycleOwner) { uiCartHeader ->
                with(cartHeaderAdapter) {
                    selectHeaderList = listOf(uiCartHeader)
                    notifyDataSetChanged()
                }
            }
            itemCartBottomBodyData.observe(viewLifecycleOwner) { uiCartBottomBody ->
                with(cartBottomBodyAdapter) {
                    bottomBodyList = listOf(uiCartBottomBody)
                    notifyDataSetChanged()
                }
            }
            uiCartJoinList.observe(viewLifecycleOwner) {
                cartTopBodyAdapter.submitList(it.toList())
                cartViewModel.calcCartBottomBodyAndHeaderVal(it)
            }
        }
    }

    private fun observeOrderButtonClickEvent() {
        with(cartViewModel) {
            orderButtonClicked.flowWithLifecycle(lifecycle).onEach {
                if (it) {
                    Log.e("CartMainFragment", "button clicked observed in CartMainFragment")
                    val alarmManager =
                        (requireContext().getSystemService(Context.ALARM_SERVICE)) as AlarmManager
                    val intent = Intent(requireContext(), DeliveryReceiver::class.java)
                    intent.putStringArrayListExtra("orderHashList", cartViewModel.orderHashList)
                    intent.putExtra("firstItemTitle", cartViewModel.orderFirstItemTitle)

                    val pendingIntent = PendingIntent.getBroadcast(
                        requireContext(),
                        ORDER_REQUEST_CODE++,
                        intent,
                        PendingIntent.FLAG_MUTABLE
                    )

                    val triggerTime = (SystemClock.elapsedRealtime() + 10 * 1000) // 테스트용 = 현재 10초
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.ELAPSED_REALTIME_WAKEUP,
                            triggerTime,
                            pendingIntent
                        )
                    } else {
                        alarmManager.set(
                            AlarmManager.ELAPSED_REALTIME_WAKEUP,
                            triggerTime,
                            pendingIntent
                        )
                    }
                    Log.e("CartmainFragment", "Alarm Register")
                }
            }.launchIn(lifecycleScope)
        }
    }

    private fun <A, T> handleState(adapter: A, uiLocalState: UiLocalState<T>) {
        when (uiLocalState) {
            is UiLocalState.IsEmpty -> {}
            is UiLocalState.IsLoading -> {}
            is UiLocalState.ShowToast -> {
                requireContext().showToast(uiLocalState.message)
            }
            is UiLocalState.Success -> {
                when (adapter) {
                    is CartDishTopBodyAdapter -> {
                        adapter.submitList(uiLocalState.uiDishItems as List<UiCartJoinItem>)
                    }
                    is CartRecentViewedFooterAdapter -> {
                        with(adapter) {
                            recentOnDishItemClickListener = this@CartMainFragment
                            uiRecentJoinList = uiLocalState.uiDishItems as List<UiDishItem>
                            notifyDataSetChanged()
                        }
                    }
                }
            }
            is UiLocalState.Error -> {}
        }
    }

    private fun initRecyclerView() {
        with(binding.cartMainRv) {
            adapter = concatAdapter
        }
    }

}