package com.kid.productscanner.presentation.select_tracking

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.kid.productscanner.R
import com.kid.productscanner.databinding.FragmentSelectTrackingBinding
import com.kid.productscanner.presentation.application.ScannerApplication
import com.kid.productscanner.presentation.select_tracking.viewmodel.SelectTrackingViewModel
import com.kid.productscanner.presentation.select_tracking.viewmodel.SelectTrackingViewModelFactory
import com.kid.productscanner.repository.ScannerRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * A simple [Fragment] subclass as the second destination in the navigation.
 */
class SelectTrackingFragment : Fragment() {

    private lateinit var binding: FragmentSelectTrackingBinding

    private val TAG: String = "chi.trinh"

    private val viewModel: SelectTrackingViewModel by viewModels {
        val repository =
            ScannerRepository((requireActivity().application as ScannerApplication).scannerDatabase)
        SelectTrackingViewModelFactory(repository)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        binding = FragmentSelectTrackingBinding.inflate(inflater, container, false)
        binding.viewModel = this@SelectTrackingFragment.viewModel
        binding.lifecycleOwner = viewLifecycleOwner
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.atvSelectTracking.apply {
            setOnItemClickListener { parent, view, position, id ->
                val selectedItem = parent.getItemAtPosition(position) as String

                binding.textShortestPn.text = "Đang tìm kiếm Part Number ngắn nhất..."
                viewModel.findShortestPartNumber(selectedItem)
            }
            setOnFocusChangeListener { _, focus ->
                if (focus) {
                    showDropDown()
                }
            }
        }
        fillTrackingNumbersToDropdown()

        binding.buttonClear.setOnClickListener {
            binding.atvSelectTracking.setText("")
        }

        binding.buttonScan.setOnClickListener {
            val action =
                SelectTrackingFragmentDirections.actionSelectTrackingFragmentToScanFragment(
                    viewModel.selectedTrackingNumber ?: ""
                )
            findNavController().navigate(action)
        }

        viewModel.shortestPartNumberLiveData.observe(viewLifecycleOwner) { shortestPartNumber ->
            val text = if (shortestPartNumber.isNullOrEmpty()) {
                ""
            } else {
                "Partnumber ngắn nhất: $shortestPartNumber\n(${shortestPartNumber.length} ký tự)"
            }
            binding.textShortestPn.text = text
        }
    }

    private fun fillTrackingNumbersToDropdown() {
        lifecycleScope.launch(Dispatchers.IO) {
            val distinctTrackingNumbers = viewModel.getDistinctTrackingNumbers()
            withContext(Dispatchers.Main) {
                val adapter = ArrayAdapter(
                    requireContext(),
                    android.R.layout.simple_spinner_dropdown_item,
                    distinctTrackingNumbers
                )
                binding.atvSelectTracking.setAdapter(adapter)
            }
        }
    }
}