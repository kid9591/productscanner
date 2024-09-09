package com.kid.productscanner.presentation.scan

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.kid.productscanner.R
import com.kid.productscanner.databinding.FragmentScanBinding
import com.kid.productscanner.databinding.FragmentSelectTrackingBinding
import com.kid.productscanner.presentation.application.ScannerApplication
import com.kid.productscanner.presentation.scan.viewmodel.ScanViewModel
import com.kid.productscanner.presentation.scan.viewmodel.ScanViewModelFactory
import com.kid.productscanner.presentation.select_tracking.viewmodel.SelectTrackingViewModel
import com.kid.productscanner.presentation.select_tracking.viewmodel.SelectTrackingViewModelFactory
import com.kid.productscanner.repository.ScannerRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * A simple [Fragment] subclass as the second destination in the navigation.
 */
class ScanFragment : Fragment() {

    private lateinit var binding: FragmentScanBinding

    private val TAG: String = "chi.trinh"

    private val args: ScanFragmentArgs by navArgs()

    private val viewModel: ScanViewModel by viewModels {
        val repository =
            ScannerRepository((requireActivity().application as ScannerApplication).scannerDatabase)
        ScanViewModelFactory(repository)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        binding = FragmentScanBinding.inflate(inflater, container, false)
        binding.viewModel = this@ScanFragment.viewModel
        binding.lifecycleOwner = viewLifecycleOwner
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.setSelectedTrackingNumber(args.trackingNumber)

        binding.atvSelectPartNumber.apply {
            setOnItemClickListener { parent, view, position, id ->
                val selectedItem = parent.getItemAtPosition(position) as String

                //TODO: show dialog input quantity

//                binding.textShortestPn.text = "Đang tìm kiếm Part Number ngắn nhất..."
//                viewModel.findShortestPartNumber(selectedItem)
            }
            setOnFocusChangeListener { _, focus ->
                if (focus) {
                    showDropDown()
                }
            }
        }
        fillPartNumbersToDropdown()

        binding.buttonClear.setOnClickListener {
            binding.atvSelectPartNumber.setText("")
        }

//        binding.buttonScan.setOnClickListener {
//            findNavController().navigate(R.id.action_SelectTrackingFragment_to_scanFragment)
//        }

    }

    private fun fillPartNumbersToDropdown() {
        lifecycleScope.launch {
            val partNumbers = viewModel.getPartNumbersOf(args.trackingNumber)
            withContext(Dispatchers.Main) {
                val adapter = ArrayAdapter(
                    requireContext(),
                    android.R.layout.simple_spinner_dropdown_item,
                    partNumbers
                )
                binding.atvSelectPartNumber.setAdapter(adapter)
            }
        }
    }
}