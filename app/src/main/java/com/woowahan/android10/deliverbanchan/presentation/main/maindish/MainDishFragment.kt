package com.woowahan.android10.deliverbanchan.presentation.main.maindish

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.woowahan.android10.deliverbanchan.R
import com.woowahan.android10.deliverbanchan.databinding.FragmentMaindishBinding
import com.woowahan.android10.deliverbanchan.presentation.base.BaseFragment
import com.woowahan.android10.deliverbanchan.presentation.common.showToast
import com.woowahan.android10.deliverbanchan.presentation.common.toGone
import com.woowahan.android10.deliverbanchan.presentation.common.toVisible
import com.woowahan.android10.deliverbanchan.presentation.dialogs.CartBottomSheetFragment
import com.woowahan.android10.deliverbanchan.presentation.main.soupdish.SoupAdapter
import com.woowahan.android10.deliverbanchan.presentation.state.UiState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainDishFragment :
    BaseFragment<FragmentMaindishBinding>(R.layout.fragment_maindish, "MainDishFragment") {

    private val mainDishViewModel: MainDishViewModel by viewModels()
    private lateinit var mainDishLinearAdapter: MainDishLinearAdapter
    private lateinit var mainDishGridAdapter: MainDishGridAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initView()
        Log.e(TAG, "onViewCreated: ${binding.maindishTvHeader.paintFlags}")
    }

    private fun initView() {
        setRadioGroupListener()
        setRecyclerView()
        collectData()
        getData()
    }

    private fun setRadioGroupListener() {
        binding.maindishRg.setOnCheckedChangeListener { group, checkedId ->
            when (checkedId) {
                R.id.maindish_rb_grid -> {
                    binding.maindishRv.apply {
                        adapter = mainDishGridAdapter
                        layoutManager = GridLayoutManager(requireContext(), 2)
                    }
                    mainDishGridAdapter.submitList(mainDishViewModel.mainDishList.toList())
                }
                R.id.maindish_rb_linear -> {
                    binding.maindishRv.apply {
                        adapter = mainDishLinearAdapter
                        layoutManager = LinearLayoutManager(requireContext())
                    }
                    mainDishLinearAdapter.submitList(mainDishViewModel.mainDishList.toList())
                }
            }
        }
    }

    private fun setRecyclerView() {

        mainDishGridAdapter = MainDishGridAdapter {
            Log.e("TAG", "cart icon clicked")
            val cartBottomSheetFragment = CartBottomSheetFragment()
            val bundle = Bundle()
            bundle.putParcelable("UiDishItem", it)
            cartBottomSheetFragment.arguments = bundle
            cartBottomSheetFragment.show(childFragmentManager, "CartBottomSheet")
        }

        mainDishLinearAdapter = MainDishLinearAdapter {
            Log.e("TAG", "cart icon clicked")
            val cartBottomSheetFragment = CartBottomSheetFragment()
            val bundle = Bundle()
            bundle.putParcelable("UiDishItem", it)
            cartBottomSheetFragment.arguments = bundle
            cartBottomSheetFragment.show(childFragmentManager, "CartBottomSheet")
        }

        binding.maindishRv.apply {
            adapter = mainDishGridAdapter
            layoutManager = GridLayoutManager(requireContext(), 2)
        }
    }

    private fun collectData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                mainDishViewModel.mainDishState.collect {
                    handleStateChange(it)
                }
            }
        }
    }

    private fun handleStateChange(state: UiState) {
        when (state) {
            is UiState.IsLoading -> binding.maindishPb.toVisible()
            is UiState.Success -> {
                binding.maindishPb.toGone()
                mainDishGridAdapter.submitList(state.uiDishItems)
            }
            is UiState.ShowToast -> {
                binding.maindishPb.toGone()
                requireContext().showToast(state.message)
            }
        }
    }

    private fun getData() {
        mainDishViewModel.getMainDishList()
    }
}