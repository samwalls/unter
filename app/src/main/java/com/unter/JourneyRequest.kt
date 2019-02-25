package com.unter

import android.Manifest
import android.annotation.SuppressLint
import android.app.TimePickerDialog
import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v7.widget.CardView
import android.transition.Visibility
import android.util.Log.d
import android.util.Log.e
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.navigation.fragment.NavHostFragment
import com.mapbox.android.core.permissions.PermissionsManager
import com.mapbox.api.directions.v5.models.DirectionsResponse
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.geocoding.v5.GeocodingCriteria
import com.mapbox.api.geocoding.v5.MapboxGeocoding
import com.mapbox.api.geocoding.v5.models.GeocodingResponse
import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.camera.CameraPosition
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.constants.MapboxConstants
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.location.LocationComponent
import com.mapbox.mapboxsdk.location.modes.CameraMode
import com.mapbox.mapboxsdk.location.modes.RenderMode
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.style.layers.PropertyFactory.*
import com.mapbox.mapboxsdk.style.layers.SymbolLayer
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource
import com.mapbox.services.android.navigation.ui.v5.route.NavigationMapRoute
import com.mapbox.services.android.navigation.v5.navigation.NavigationRoute
import com.unter.model.JourneyRequestInfo
import com.unter.model.UnterAppModel
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.*
import java.util.Calendar.HOUR
import java.util.Calendar.MINUTE


/**
 * A simple [Fragment] subclass.
 * Activities that contain this fragment must implement the
 * [JourneyRequest.OnFragmentInteractionListener] interface
 * to handle interaction events.
 * Use the [JourneyRequest.newInstance] factory method to
 * create an instance of this fragment.
 *
 */
@SuppressLint("LogNotTimber")
class JourneyRequest : Fragment() {

    public val LOCATION_REQUEST_CODE: Int = 0

    private val TAG: String = JourneyRequest::class.java.canonicalName!!

    private lateinit var model: UnterAppModel

    private lateinit var journeyRequestCard: CardView

    private lateinit var textOrigin: TextView
    private lateinit var textDestination: TextView
    private lateinit var textArrivalTime: TextView
    private lateinit var textPickupTime: EditText

    private lateinit var textPriceEstimate: TextView

    private lateinit var buttonRequest: Button

    private var request: JourneyRequestInfo = JourneyRequestInfo()

    private lateinit var mapView: MapView
    private lateinit var mapboxMap: MapboxMap

    private lateinit var locationComponent: LocationComponent

    private var locationOrigin: LatLng? = null
    private var locationDestination: LatLng? = null

    // the current coordinate being selected which will be updated when the map is clicked
    private var locationSelection: LatLng? = null

    private lateinit var routePickupToDestination: DirectionsRoute
    private var navigationMapRoute: NavigationMapRoute? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        model = ViewModelProviders.of(activity!!).get(UnterAppModel::class.java)
        Mapbox.getInstance(context!!, resources.getString(R.string.mapbox_access_token))

        retainInstance = true
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        var v: View = inflater.inflate(R.layout.fragment_journey_request, container, false)
        return v
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        journeyRequestCard = view.findViewById(R.id.journey_confirm_card)
        textOrigin = view.findViewById(R.id.edittext_origin)
        textDestination = view.findViewById(R.id.edittext_destination)
        textArrivalTime = view.findViewById(R.id.textview_time_arrival)
        textPickupTime = view.findViewById(R.id.journey_time_pickup)
        textPriceEstimate = view.findViewById(R.id.textview_price_estimate)
        buttonRequest = view.findViewById(R.id.button_find_drivers)
        mapView = view.findViewById(R.id.mapView)
        mapView.onCreate(savedInstanceState)

        locationOrigin = null
        locationDestination = null
        locationSelection = null

        // hide the journey request card
        journeyRequestCard.isEnabled = false
        journeyRequestCard.visibility = View.GONE

        // set the initial target pickup time
        val targetTime: Calendar = Calendar.getInstance()
        targetTime.add(Calendar.MINUTE, 5)
        val targetHour = targetTime.get(HOUR)
        val targetMinute = targetTime.get(MINUTE)
        textPickupTime.setText("$targetHour:$targetMinute")

        textPickupTime.setOnClickListener {

            // whenever the pickup time is edited, default to the default target pickup time first
            val targetTime: Calendar = Calendar.getInstance()
            targetTime.add(Calendar.MINUTE, 5)
            val targetHour = targetTime.get(HOUR)
            val targetMinute = targetTime.get(MINUTE)
            textPickupTime.setText("$targetHour:$targetMinute")

            // get the true target pickup time from a time picker dialog
            val mTimePicker = TimePickerDialog(context, TimePickerDialog.OnTimeSetListener { view, hourOfDay, minute ->
                textPickupTime.setText("${hourOfDay}:${minute}")
            }, targetHour, targetMinute, true)

            mTimePicker.setTitle("Select Time")
            mTimePicker.show()
        }

        textOrigin.isEnabled = true
        textOrigin.isClickable = true
        textOrigin.setOnClickListener {
            locationSelection = locationOrigin
            Toast.makeText(context, "select a pickup location", Toast.LENGTH_LONG).show()
        }

        textDestination.isEnabled = true
        textDestination.isClickable = true
        textDestination.setOnClickListener {
            locationSelection = locationDestination
            Toast.makeText(context, "select a drop-off location", Toast.LENGTH_LONG).show()
        }

        buttonRequest.setOnClickListener {
            // if all fields are ready for a request to be sent, navigate to the next view
            if (isRequestEnabled()) {
                model.currentRequest = request

                val requestUri: Uri = Uri.parse("unter://ride/request" +
                        "?userId=${model.currentUser!!.id}" +
                        "&originLong=${model.currentRequest!!.originLong}" +
                        "&originLat=${model.currentRequest!!.originLat}" +
                        "&destinationLong=${model.currentRequest!!.originLong}" +
                        "&destinationLat=${model.currentRequest!!.originLat}")

                d(TAG, "emitting journey confirm intent with URI: '$requestUri'")
                startActivity(Intent(Intent.ACTION_VIEW, requestUri))
            }
        }

        mapView.getMapAsync { map: MapboxMap ->
            this.mapboxMap = map
            onMapReady()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_REQUEST_CODE && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            enableLocationComponent(mapboxMap.style!!)
            moveCameraToCurrentLocation()
        }
    }

    private fun onMapReady() {
        // get the map style from the unter mapbox style URL
        mapboxMap.setStyle(Style.Builder().fromUrl(context!!.getString(R.string.mapbox_unter_style))) { style: Style ->

            addDestinationMarkerLayer(mapboxMap.style!!)

            enableLocationComponent(mapboxMap.style!!)
            moveCameraToCurrentLocation()

            makeOriginCurrentLocation()
            locationSelection = locationDestination

            mapboxMap.addOnMapClickListener { position ->
                onMapClick(position)
            }
        }
    }

    private fun onMapClick(clickPos: LatLng): Boolean {

        // show the journey request card
        journeyRequestCard.isEnabled = true
        journeyRequestCard.visibility = View.VISIBLE

        if (locationOrigin == null)
                locationOrigin = LatLng(0.0, 0.0)

        if (locationDestination == null)
            locationDestination = LatLng(0.0, 0.0)

        if (locationSelection == null) {
            makeOriginCurrentLocation()
            locationSelection = locationDestination
        }

        // update the selected position
        locationSelection!!.longitude = clickPos.longitude
        locationSelection!!.latitude = clickPos.latitude

        val origin: Point = Point.fromLngLat(
            locationOrigin!!.longitude,
            locationOrigin!!.latitude
        )

        val destination = Point.fromLngLat(
            locationDestination!!.longitude,
            locationDestination!!.latitude
        )

        getRoute(origin, destination)

        d(TAG, "map clicked at coordinates: $clickPos")
        return false
    }

    private fun addDestinationMarkerLayer(style: Style) {
        val geoJsonSource = GeoJsonSource("destination-source-id")
        style.addSource(geoJsonSource)

        style.addImage(
            "destination-icon-id",
            BitmapFactory.decodeResource(resources, R.drawable.mapbox_marker_icon_default)
        )

        val symbolLayer = SymbolLayer("destination-symbol-layer-id", "destination-source-id")
        symbolLayer.withProperties(
            iconImage("destination-icon-id"),
            iconAllowOverlap(true),
            iconIgnorePlacement(true)
        )

        style.addLayer(symbolLayer)
    }

    private fun moveCameraToCurrentLocation() {
        @SuppressLint("MissingPermission")
        val lastLocation = mapboxMap.locationComponent.lastKnownLocation

        if (lastLocation != null) {
            val cameraPosition = CameraPosition.Builder()
                .target(LatLng(lastLocation.latitude, lastLocation.longitude))
                .zoom(5.0)
                .tilt(MapboxConstants.MINIMUM_TILT)
                .build()

            mapboxMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition), 1000)
        }
    }

    private fun makeOriginCurrentLocation() {
        @SuppressLint("MissingPermission")
        val lastLocation = mapboxMap.locationComponent.lastKnownLocation

        if (lastLocation != null) {

            if (locationOrigin == null)
                locationOrigin = LatLng(0.0, 0.0)

            locationOrigin!!.longitude = lastLocation.longitude
            locationOrigin!!.latitude = lastLocation.latitude

            if (locationDestination == null)
                locationDestination = LatLng(0.0, 0.0)

            // next location selection via map click will set the destination
            locationSelection = locationDestination
        }
    }

    private fun getRoute(origin: Point, destination: Point) {
        NavigationRoute.builder(context)
            .accessToken(Mapbox.getAccessToken()!!)
            .origin(origin)
            .destination(destination)
            .build()
            .getRoute(object: Callback<DirectionsResponse> {
                override fun onResponse(call: Call<DirectionsResponse>, response: Response<DirectionsResponse>) {
                    onRouteResponse(call, response)
                }

                override fun onFailure(call: Call<DirectionsResponse>, t: Throwable) {
                    onRouteFailure(call, t)
                }
            })
    }

    private fun onRouteResponse(call: Call<DirectionsResponse>, response: Response<DirectionsResponse>) {
        d(TAG, "response code: ${response.code()}")
        if (response.body() == null) {
            e(TAG, "route request response body is null")
            return
        } else if (response.body()!!.routes().size < 1) {
            e(TAG, "no routes found (make sure to set the right user token and access token)")
            return
        }

        // set the route for use on screen

        routePickupToDestination = response.body()!!.routes()[0]

        val waypoints = response.body()!!.waypoints()!!

        // if there is waypoint data
        if (waypoints.size > 1) {
            val originWaypoint = waypoints[0]!!
            val finalWaypoint = waypoints[waypoints.size - 1]!!

            // update information for use in the fragment
            locationOrigin!!.longitude = originWaypoint.location()!!.longitude()
            locationOrigin!!.latitude = originWaypoint.location()!!.latitude()

            locationDestination!!.longitude = finalWaypoint.location()!!.longitude()
            locationDestination!!.latitude = finalWaypoint.location()!!.latitude()

            textOrigin.text = originWaypoint.name()!!
            textDestination.text = finalWaypoint.name()!!

            // update the unter request model
            updateRequestModel(locationOrigin!!, locationDestination!!)

            // update the price estimate
            textPriceEstimate.isEnabled = true
        } else {
            textPriceEstimate.isEnabled = false
        }

        // update the most recent route on the screen
        if (navigationMapRoute != null) {
            navigationMapRoute!!.updateRouteArrowVisibilityTo(false)
            navigationMapRoute!!.updateRouteVisibilityTo(false)
        } else {
            navigationMapRoute = NavigationMapRoute(null, mapView, mapboxMap)
        }
        navigationMapRoute!!.addRoute(routePickupToDestination)
    }

    private fun onRouteFailure(call: Call<DirectionsResponse>, throwable: Throwable) {
        e(TAG, "error: ${throwable.message}")
    }

    private fun updateRequestModel(origin: LatLng, destination: LatLng) {
        // update unter unter request model
        request.originLong = origin.longitude
        request.originLat = origin.latitude
        request.destinationLong = destination.longitude
        request.destinationLat = destination.longitude

        // enable the request button if it's ok to do so
        val enabled = isRequestEnabled()
        buttonRequest.isEnabled = enabled
        textPickupTime.isEnabled = enabled
    }

    private fun setGeocodeLocationText(view: TextView, point: Point) {
        MapboxGeocoding.builder()
            .accessToken(resources.getString(R.string.mapbox_access_token))
            .query(point)
            .geocodingTypes(GeocodingCriteria.TYPE_ADDRESS)
            .build()
            .enqueueCall(object: Callback<GeocodingResponse> {
                override fun onResponse(call: Call<GeocodingResponse>, response: Response<GeocodingResponse>) {
                    if (response.body() == null) {
                        e(TAG, "geocoding request response body is null")
                        return
                    } else if (response.body()!!.features().isEmpty()) {
                        e(TAG, "geocoding request returned nothing")
                        view.text = ""
                        Toast.makeText(context, "we can't reach that location!", Toast.LENGTH_SHORT).show()
                        return
                    }

                    view.text = response.body()!!.features()[0].text()
                }

                override fun onFailure(call: Call<GeocodingResponse>, t: Throwable) {
                    e(TAG, "error: ${t.message}")
                }
            })
    }

    @SuppressWarnings("MissingPermission")
    private fun enableLocationComponent(style: Style) {

        // adapted from this source: https://docs.mapbox.com/android/maps/overview/location-component/

        // Check if permissions are enabled and if not request
        if (PermissionsManager.areLocationPermissionsGranted(context)) {

            // Get an instance of the component
            locationComponent = mapboxMap.locationComponent

            // Activate with options
            locationComponent.activateLocationComponent(context!!, style)

            // Enable to make component visible
            locationComponent.isLocationComponentEnabled = true

            // Set the component's camera mode
            locationComponent.cameraMode = CameraMode.TRACKING

            // Set the component's render mode
            locationComponent.renderMode = RenderMode.COMPASS

        } else {
            requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), PackageManager.PERMISSION_GRANTED)
        }
    }

    private fun isRequestEnabled(): Boolean {
        return request.originLat != null &&
                request.originLong != null &&
                request.destinationLat != null &&
                request.destinationLong != null
    }

    override fun onStart() {
        super.onStart()
        mapView.onStart()
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onDestroy() {
        super.onDestroy()
        // there seems to be a bug with the following line: https://github.com/mapbox/mapbox-gl-native/issues/10030
        //mapView.onDestroy()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView.onSaveInstanceState(outState)
    }
}
