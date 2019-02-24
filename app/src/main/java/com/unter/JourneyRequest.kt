package com.unter

import android.annotation.SuppressLint
import android.app.TimePickerDialog
import android.arch.lifecycle.ViewModelProviders
import android.graphics.BitmapFactory
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.util.Log.d
import android.util.Log.e
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.navigation.NavGraph
import androidx.navigation.Navigation
import androidx.navigation.fragment.NavHostFragment
import com.mapbox.android.core.permissions.PermissionsListener
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
class JourneyRequest : Fragment(), PermissionsListener {

    private val TAG: String = JourneyRequest::class.java.canonicalName!!

    private lateinit var model: UnterAppModel

    private lateinit var textOrigin: TextView
    private lateinit var textDestination: TextView
    private lateinit var textArrivalTime: TextView
    private lateinit var textPickupTime: EditText

    private lateinit var buttonRequest: Button

    private var request: JourneyRequestInfo = JourneyRequestInfo()

    private lateinit var mapView: MapView
    private lateinit var mapboxMap: MapboxMap

    private lateinit var permissionsManager: PermissionsManager
    private lateinit var locationComponent: LocationComponent

    private lateinit var routePickupToDestination: DirectionsRoute
    private var navigationMapRoute: NavigationMapRoute? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        model = ViewModelProviders.of(activity!!).get(UnterAppModel::class.java)
        Mapbox.getInstance(context!!, resources.getString(R.string.mapbox_access_token))
        // clear back stack so that navigation graph resolves properly to this fragment as the root
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
        textOrigin = view.findViewById(R.id.edittext_origin)
        textDestination = view.findViewById(R.id.edittext_destination)
        textArrivalTime = view.findViewById(R.id.textview_time_arrival)
        textPickupTime = view.findViewById(R.id.journey_time_pickup)
        buttonRequest = view.findViewById(R.id.button_find_drivers)
        mapView = view.findViewById(R.id.mapView)
        mapView.onCreate(savedInstanceState)

        textPickupTime.setOnClickListener {

            val currentTime: Calendar = Calendar.getInstance()
            val currentHour = currentTime.get(HOUR)
            val currentMinute = currentTime.get(MINUTE)

            textPickupTime.setText("$currentHour:$currentMinute")

            val mTimePicker = TimePickerDialog(context, TimePickerDialog.OnTimeSetListener { view, hourOfDay, minute ->
                textPickupTime.setText("${hourOfDay}:${minute}")
            }, currentHour, currentMinute, true)

            mTimePicker.setTitle("Select Time")
            mTimePicker.show()
        }

        buttonRequest.setOnClickListener {
            if (isRequestEnabled()) {
                NavHostFragment.findNavController(this).navigate(R.id.action_home_to_journeyConfirm)
            }
        }

        mapView.getMapAsync { map: MapboxMap ->
            this.mapboxMap = map
            onMapReady()
        }
    }

    override fun onPermissionResult(granted: Boolean) {
        if (granted)
            enableLocationComponent(mapboxMap.style!!)
    }

    override fun onExplanationNeeded(permissionsToExplain: MutableList<String>?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    private fun onMapReady() {
        // get the map style from the unter mapbox style URL
        mapboxMap.setStyle(Style.Builder().fromUrl(context!!.getString(R.string.mapbox_unter_style))) { style: Style ->

            addDestinationMarkerLayer(mapboxMap.style!!)

            enableLocationComponent(mapboxMap.style!!)
            moveCameraToCurrentLocation()

            mapboxMap.addOnMapClickListener { position ->
                onMapClick(position)
            }
        }
    }

    private fun onMapClick(clickPos: LatLng): Boolean {

        @SuppressLint("MissingPermission")
        val origin: Point = Point.fromLngLat(
            mapboxMap.locationComponent.lastKnownLocation!!.longitude,
            mapboxMap.locationComponent.lastKnownLocation!!.latitude
        )

        val destination = Point.fromLngLat(clickPos.longitude, clickPos.latitude)

        getRoute(origin, destination)
//        setGeocodeLocationText(textOrigin, origin)
//        setGeocodeLocationText(textDestination, destination)

        // enable the request button if it's ok to do so
        buttonRequest.isEnabled = isRequestEnabled()

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
                .tilt(0.0)
                .build()

            mapboxMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition), 1000)
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

        // set the route on screen

        routePickupToDestination = response.body()!!.routes()[0]

        val waypoints = response.body()!!.waypoints()!!
        if (waypoints.size > 1) {
            val originWaypoint = waypoints[0]!!
            val finalWaypoint = waypoints[waypoints.size - 1]!!

            request.originLong = originWaypoint.location()!!.longitude()
            request.originLat = originWaypoint.location()!!.latitude()
            request.destinationLong = finalWaypoint.location()!!.longitude()
            request.destinationLat = finalWaypoint.location()!!.latitude()

            textOrigin.text = originWaypoint.name()!!
            textDestination.text = finalWaypoint.name()!!
        }
        if (navigationMapRoute != null) {
            // TODO change this deprecated code
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
            permissionsManager = PermissionsManager(this)
            permissionsManager.requestLocationPermissions(activity)
        }
    }

    private fun isRequestEnabled(): Boolean {
        return request.originLat != null &&
                request.originLong != null &&
                request.destinationLat != null &&
                request.destinationLong != null
    }

    private fun clearBackStack() {
        val manager: FragmentManager = fragmentManager!!
        if (manager.backStackEntryCount > 0) {
            val first = manager.getBackStackEntryAt(0)
            manager.popBackStack(first.id, FragmentManager.POP_BACK_STACK_INCLUSIVE)
        }
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
