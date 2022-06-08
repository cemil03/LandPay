package com.example.landpay2

import android.animation.ObjectAnimator
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.core.animation.doOnEnd
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.findNavController
import com.example.landpay2.databinding.FragmentSearchBinding
import java.util.*


class SearchFragment : Fragment() {

    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!
    var currency: Currency? = null
    var money: Money? = null
    var anim: ObjectAnimator? = null
    var anim2: ObjectAnimator? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSearchBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.photoView.scrollX = START_POINT_X
        anim = ObjectAnimator.ofInt(
            binding.photoView, "scrollX", END_POINT_X
        ).setDuration(LONG_DURATION)
        anim2 = ObjectAnimator.ofInt(
            binding.photoView, "scrollX", START_POINT_X
        ).setDuration(LONG_DURATION)

        anim?.start()
        anim?.doOnEnd {
            anim2?.start()
        }
        anim2?.doOnEnd {
            anim?.start()
        }

        ArrayAdapter.createFromResource(
            requireContext(), R.array.currency, android.R.layout.simple_spinner_dropdown_item
        ).also { adapter ->
            binding.spinner.adapter = adapter
        }



        binding.spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {

            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, position: Int, p3: Long) {
                when (position) {
                    USD -> {
                        binding.tLayoutSalary.startIconDrawable =
                            ContextCompat.getDrawable(requireContext(), R.drawable.dollar)
                        currency = Currency.getInstance("USD")
                    }
                    EUR -> {
                        binding.tLayoutSalary.startIconDrawable =
                            ContextCompat.getDrawable(requireContext(), R.drawable.euro)
                        currency = Currency.getInstance("EUR")
                    }
                    RUB -> {
                        binding.tLayoutSalary.startIconDrawable =
                            ContextCompat.getDrawable(requireContext(), R.drawable.default_currency)
                        currency = Currency.getInstance("RUB")
                    }
                }
            }

            override fun onNothingSelected(p0: AdapterView<*>?) {
                currency = Currency.getInstance("USD")
            }

        }


        binding.btnSearch.setOnClickListener {
            val bundle = Bundle()
            val salary = binding.etSalary.text.toString()
            if (salary != "" && currency != null) {
                money = Money(salary.toInt(), currency)
                bundle.putSerializable(MONEY, money)
                findNavController().navigate(R.id.action_searchFragment_to_mapsFragment, bundle)
            } else {
                Toast.makeText(requireContext(), "Salary is empty!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    companion object {
        const val MONEY = "money"
        const val START_POINT_X = -1000
        const val END_POINT_X = 1000
        const val LONG_DURATION = 50000L
        const val USD = 0
        const val EUR = 1
        const val RUB = 2

    }


}


