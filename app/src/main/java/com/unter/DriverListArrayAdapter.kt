package com.unter

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.RatingBar
import android.widget.TextView
import com.mapbox.api.directions.v5.models.DirectionsResponse
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.services.android.navigation.v5.navigation.NavigationRoute
import com.unter.model.DriverInfo
import com.unter.model.JourneyRequestInfo
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt
import android.os.StrictMode
import java.math.RoundingMode
import java.text.DecimalFormat


class DriverListArrayAdapter(val request: JourneyRequestInfo, context: Context, private val drivers: List<DriverInfo>) : ArrayAdapter<DriverInfo>(context, -1, drivers) {

    private val TAG: String = DriverListArrayAdapter::class.java.canonicalName!!

    private lateinit var routePickupToDestination: DirectionsRoute

    init {
        val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)

        getRoute(
            Point.fromLngLat(request.originLong, request.originLat),
            Point.fromLngLat(request.destinationLong, request.destinationLat))
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val inflater: LayoutInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val rowView = inflater.inflate(R.layout.driver_list_item, parent, false)

        val driver: DriverInfo = drivers[position]

        val timeArrive: String = makeTimeEstimate()
        val timeSeconds: Int = routePickupToDestination.duration()!!.roundToInt()
        val priceEstimate: Double = (timeSeconds / 60).toDouble() * driver.price
        val df = DecimalFormat("#.##")
        df.roundingMode = RoundingMode.CEILING

        rowView.findViewById<TextView>(R.id.driver_list_item_name).text = driver.name
        rowView.findViewById<TextView>(R.id.driver_list_item_vehicle).text = driver.vehicle
        rowView.findViewById<RatingBar>(R.id.driver_list_item_rating).rating = driver.rating
        rowView.findViewById<TextView>(R.id.driver_list_item_rate).text = "£${df.format(priceEstimate)} @ £${driver.price}/min"

        return rowView
    }

    private fun getRoute(origin: Point, destination: Point) {
        val response: Response<DirectionsResponse> = NavigationRoute.builder(context)
            .accessToken(Mapbox.getAccessToken()!!)
            .origin(origin)
            .destination(destination)
            .build()
            .call.execute()

        if (response.isSuccessful)
            onRouteResponse(response)
        else
            onRouteFailure(response)
    }

    private fun onRouteResponse(response: Response<DirectionsResponse>) {
        Log.d(TAG, "mapbox route request response code: ${response.code()}")
        if (response.body() == null) {
            Log.e(TAG, "route request response body is null")
            return
        } else if (response.body()!!.routes().size < 1) {
            Log.e(TAG, "no routes found (make sure to set the right user token and access token)")
            return
        }

        // set the route for use on screen

        routePickupToDestination = response.body()!!.routes()[0]
    }

    @SuppressLint("LogNotTimber")
    private fun onRouteFailure(response: Response<DirectionsResponse>) {
        Log.e(TAG, "there was an error in the mapbox route response")
    }

    private fun makeTimeEstimate(): String {
        val calendar: Calendar = Calendar.getInstance()
        val t = request.pickupTime + routePickupToDestination.duration()!!.roundToInt()
        val df = SimpleDateFormat("HH:mm")
        df.timeZone = calendar.timeZone
        return df.format(Date(t))
    }
}