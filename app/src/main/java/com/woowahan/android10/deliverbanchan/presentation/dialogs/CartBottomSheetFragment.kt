package com.woowahan.android10.deliverbanchan.presentation.dialogs

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.woowahan.android10.deliverbanchan.R
import com.woowahan.android10.deliverbanchan.databinding.FragmentCartBottomSheetBinding
import com.woowahan.android10.deliverbanchan.domain.model.UiDishItem
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class CartBottomSheetFragment : BottomSheetDialogFragment() {

    private var _binding: FragmentCartBottomSheetBinding? = null
    private val binding: FragmentCartBottomSheetBinding get() = checkNotNull(_binding)
    private val cartBottomSheetViewModel: CartBottomSheetViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setStyle(STYLE_NORMAL, R.style.AppBottomSheetDialogTheme)
    }

    override fun onStart() {
        super.onStart()
        val behavior = BottomSheetBehavior.from(requireView().parent as View)
        behavior.state = BottomSheetBehavior.STATE_EXPANDED
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentCartBottomSheetBinding.inflate(layoutInflater, container, false)
        binding.lifecycleOwner = this.viewLifecycleOwner

        arguments?.let {
            val uiDishItem = it.getParcelable<UiDishItem>("UiDishItem")
            Log.e("AppTest", "$uiDishItem")
            uiDishItem?.let {
                cartBottomSheetViewModel.currentUiDishItem.value = it
                //binding.item = it
                binding.viewModel = cartBottomSheetViewModel
            }
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initView()
    }

    private fun initView() {
        setButton()
        cartBottomSheetViewModel.getCartInfoByHash()
        //observeItemCount()
    }

    private fun setButton() {
        binding.btnGoToCart.setOnClickListener {
            cartBottomSheetViewModel.getCartInfoByHash()
        }
    }

}