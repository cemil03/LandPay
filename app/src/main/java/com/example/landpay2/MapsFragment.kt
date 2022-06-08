package com.example.landpay2

import android.animation.ObjectAnimator
import android.location.Address
import android.location.Geocoder
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SearchView
import android.widget.Toast
import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.core.animation.doOnEnd
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import app.futured.donut.DonutSection
import com.example.landpay2.databinding.FragmentMapsBinding
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.firebase.firestore.FirebaseFirestore
import com.google.maps.android.geojson.GeoJsonLayer
import java.sql.Time
import java.util.*
import kotlin.concurrent.timerTask
import kotlin.coroutines.coroutineContext


class MapsFragment : Fragment(), OnMapReadyCallback {
    private var _binding: FragmentMapsBinding? = null
    private val binding get() = _binding!!
    private var money: Money? = null
    var mMap: GoogleMap? = null
    private var mDataBase: FirebaseFirestore? = null
    private var mViewModel: CountriesViewModel? = null
    var countries: List<Country>? = null
    var anim: ObjectAnimator? = null
    var anim2: ObjectAnimator? = null
    var restart = true
    var tax: Int? = null
    var stanOfLiving = 0
    var section1: DonutSection? = null
    var section2: DonutSection? = null


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMapsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
        money = arguments?.getSerializable(SearchFragment.MONEY) as Money
        val salaryInDollars = getMoneyInDollars(money!!)
        mDataBase = FirebaseFirestore.getInstance()
        mapFragment?.getMapAsync(this)
        mViewModel = ViewModelProvider(this).get(CountriesViewModel::class.java)
        binding.photoView.scrollX = SearchFragment.START_POINT_X
        anim = ObjectAnimator.ofInt(
            binding.photoView, "scrollX", SearchFragment.END_POINT_X
        ).setDuration(LOW_DURATION)
        anim2 = ObjectAnimator.ofInt(
            binding.photoView, "scrollX", SearchFragment.START_POINT_X
        ).setDuration(LOW_DURATION)

        anim?.start()
        anim?.doOnEnd {
            anim2?.start()
        }
        anim2?.doOnEnd {
            anim?.start()
        }

//        Handler(Looper.getMainLooper()).postDelayed({ binding.groupMap.visibility = View.VISIBLE
//                                                     binding.groupMap.requestLayout()}, 5000L)


        binding.motionLayout.setTransitionListener(object : MotionLayout.TransitionListener {
            override fun onTransitionStarted(
                motionLayout: MotionLayout?,
                startId: Int,
                endId: Int
            ) {
            }

            override fun onTransitionChange(
                motionLayout: MotionLayout?,
                startId: Int,
                endId: Int,
                progress: Float
            ) {
            }

            override fun onTransitionCompleted(motionLayout: MotionLayout?, currentId: Int) {

                if (motionLayout?.targetPosition == 1f && restart) {
                    binding.donutProgStanOfLiving.clear()
                    binding.donutProgOfTax.clear()
                    binding.donutProgStanOfLiving.submitData(listOf(section1!!))
                    binding.donutProgOfTax.submitData(listOf(section2!!))

                    restart = false
                } else if (motionLayout?.targetPosition != 1f && !restart) {
                    binding.donutProgStanOfLiving.clear()
                    binding.donutProgOfTax.clear()
                    restart = true
                }

            }

            override fun onTransitionTrigger(
                motionLayout: MotionLayout?,
                triggerId: Int,
                positive: Boolean,
                progress: Float
            ) {
            }

        })

        binding.svCountry.setOnClickListener {
            binding.svCountry.isIconified = false
            getLocationFromSearch()
        }

        mViewModel?.getResponseUsingCallback()?.observe(this, {
            getStandardOfLiving(salaryInDollars, it.countries)
            countries = it.countries
        })
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap?.moveCamera(CameraUpdateFactory.newLatLng(LatLng(55.585901, -105.75)))


        mMap?.setOnPolygonClickListener {
            val latLng = getCenterCoordinatesCountry(it.points)
            val add =
                Geocoder(requireContext()).getFromLocation(latLng.latitude, latLng.longitude, 1)[0]
            getInfoCountryAndWriteViews(add, money!!,it.fillColor)

            section1 = DonutSection(
                name = "standard_of_living",
                color = when (it.fillColor) {
                    ContextCompat.getColor(requireContext(),R.color.red_transparent) ->
                        ContextCompat.getColor(requireContext(), R.color.red)
                    ContextCompat.getColor(requireContext(),R.color.orange_transparent) ->
                        ContextCompat.getColor(requireContext(), R.color.orange)
                    ContextCompat.getColor(requireContext(),R.color.green_transparent) ->
                        ContextCompat.getColor(requireContext(), R.color.green)
                    else -> 0
                },
                amount = stanOfLiving.toFloat()
            )
            section2 = DonutSection(
                name = "tax",
                color = ContextCompat.getColor(requireContext(), R.color.green_gradient_end),
                amount = tax?.toFloat()!!
            )

        }

        var a = R.raw.border_egypt

    }

    private fun getMoneyInDollars(money: Money): Double {
        var moneyInDollars = 0.0
        when (money.currency) {
            Currency.getInstance("USD") -> {
                moneyInDollars = money.salary.toDouble()
            }
            Currency.getInstance("EUR") -> {
                moneyInDollars = money.salary * EURO_TO_DOLLAR
            }
            Currency.getInstance("RUB") -> {
                moneyInDollars = money.salary * RUB_TO_DOLLAR
            }
        }
        return moneyInDollars
    }

    private fun getLocationFromSearch() {
        var addresses: List<Address>? = null
        binding.svCountry.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(location: String?): Boolean {
                if (!(location == null && location == "")) {
                    val geo = Geocoder(requireContext())
                    addresses = geo.getFromLocationName(location, 1)
                    if (addresses!!.isEmpty()) {
                        Toast.makeText(
                            requireContext(),
                            "Location does not exist",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        val latLng = LatLng(addresses!![0].latitude, addresses!![0].longitude)
                        mMap?.moveCamera(CameraUpdateFactory.newLatLng(latLng))
                    }
                }
                return false
            }

            override fun onQueryTextChange(p0: String?): Boolean {
                return false
            }
        })
    }

    private fun fillColorOfCountry(borderFile: Int, color: Int) {
        val layer = GeoJsonLayer(mMap, borderFile, requireContext())
        val polygon = layer.defaultPolygonStyle
        polygon.fillColor = color
        polygon.strokeWidth = 2f
        layer.addLayerToMap()
    }


    private fun getStandardOfLiving(salary: Double, countries: List<Country>?) {
        for (country in countries!!) {
            if (salary <= country.minSalary) {
                fillColorOfCountry(
                    country.borderFile,
                    ContextCompat.getColor(requireContext(), R.color.red_transparent)
                )
            } else if (salary >= country.minSalary && salary <= country.averageSalary) {
                fillColorOfCountry(
                    country.borderFile,
                    ContextCompat.getColor(requireContext(), R.color.orange_transparent)
                )
            } else {
                fillColorOfCountry(
                    country.borderFile,
                    ContextCompat.getColor(requireContext(), R.color.green_transparent)
                )
            }
        }

    }

    private fun getCenterCoordinatesCountry(points: List<LatLng>): LatLng {
        val builder = LatLngBounds.Builder()
        for (i in points.indices) {
            builder.include(points[i])
        }
        val bounds: LatLngBounds = builder.build()
        return bounds.center
    }

    private fun getInfoCountryAndWriteViews(address: Address, money: Money, color : Int) {
        binding.tvCountry.text = address.countryName
        for (country in countries!!) {
            if (country.name == address.countryName) {
                tax = country.tax
                binding.tvMinWage.text = country.minSalary.toString()
                binding.tvAverageWage.text = country.averageSalary.toString()
                binding.tvMaxWage.text = country.highWage.toString()
                binding.tvTax.text = "${country.tax}%"
                when(color){
                    ContextCompat.getColor(requireContext(),R.color.red_transparent) ->
                        stanOfLiving = (money.salary * 35 ) / country.minSalary
                    ContextCompat.getColor(requireContext(),R.color.orange_transparent) ->
                        stanOfLiving = (money.salary * 70 ) / country.averageSalary
                    ContextCompat.getColor(requireContext(),R.color.green_transparent) ->
                        stanOfLiving = (money.salary * 100 ) / country.highWage
                }
                if (stanOfLiving > 100) {
                    stanOfLiving = 100
                }
                binding.tvStandardOfLiving.text = "${stanOfLiving}%"
                break
            }
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        anim = null
        anim2 = null

        Log.d("DESTROY", "DESTROYED")

    }

    companion object {
        const val EURO_TO_DOLLAR = 1.05
        const val RUB_TO_DOLLAR = 0.01
        const val LOADING_DELAY = 4000L
        const val LOW_DURATION = 20000L
    }

}