package com.unter.activity

import android.Manifest
import android.annotation.SuppressLint
import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.CardView
import android.util.Log.d
import android.util.Log.e
import android.view.View
import android.widget.*
import com.mapbox.android.core.permissions.PermissionsManager
import com.mapbox.api.directions.v5.models.DirectionsResponse
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.geojson.Feature
import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.geometry.LatLngBounds
import com.mapbox.mapboxsdk.location.LocationComponent
import com.mapbox.mapboxsdk.location.modes.CameraMode
import com.mapbox.mapboxsdk.location.modes.RenderMode
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.style.layers.PropertyFactory
import com.mapbox.mapboxsdk.style.layers.SymbolLayer
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource
import com.mapbox.services.android.navigation.ui.v5.route.NavigationMapRoute
import com.mapbox.services.android.navigation.v5.navigation.NavigationRoute
import com.unter.DriverListArrayAdapter
import com.unter.R
import com.unter.model.*
import com.unter.model.exception.JourneyConfirmationException
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.*
import java.util.Calendar.HOUR_OF_DAY
import java.util.Calendar.MINUTE

@SuppressLint("LogNotTimber")
class JourneyConfirmActivity : AppCompatActivity() {

    private val TAG: String = JourneyConfirmActivity::class.java.canonicalName!!

    private lateinit var mapView: MapView
    private lateinit var mapboxMap: MapboxMap

    private lateinit var cardTooltip: CardView
    private lateinit var textViewTooltip: TextView

    private lateinit var locationComponent: LocationComponent

    private var navigationMapRoute: NavigationMapRoute? = null
    private lateinit var routeOriginToDestination: DirectionsRoute

    private lateinit var model: UnterAppModel

    private lateinit var driverListCardView: CardView
    private lateinit var driverListView: ListView

    private var user: UserInfo? = null
    private var request: JourneyRequestInfo? = null
    private var driver: DriverInfo? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_journey_confirm)

        model = ViewModelProviders.of(this).get(UnterAppModel::class.java)

        // only initialise the model if not already initialised (this activity may be spawned from anywhere)
        if (!model.isInitialised())
            model.initStorage(applicationContext)
        model.load()

        Mapbox.getInstance(this, resources.getString(R.string.mapbox_access_token))

        mapView = findViewById(R.id.mapView)
        mapView.onCreate(savedInstanceState)

        mapView.getMapAsync { map ->
            mapboxMap = map
            locationComponent = mapboxMap.locationComponent
            handleIntents()
        }
    }

    private fun setupTooltip() {
        textViewTooltip = findViewById(R.id.textview_tooltip)
        cardTooltip = findViewById(R.id.card_tooltip)
        disableTooltip()

        enableTooltip("head towards the marker for the pickup time: ${makeFormattedDate(request!!.pickupTime)}")
    }

    private fun setupDriverList() {
        driverListView = findViewById(R.id.listview_driver_list)
        driverListCardView = findViewById(R.id.card_driver_list)

        driverListCardView.visibility = View.VISIBLE

        val adapter = DriverListArrayAdapter(request!!, this, model.getDriverList())
        driverListView.adapter = adapter
        driverListView.onItemClickListener = AdapterView.OnItemClickListener { parent: AdapterView<*>, view, position, id ->
            onDriverListItemClick(parent, view, position, id)
        }
    }

    private fun onMapReadyDefault() {
        mapboxMap.setStyle(Style.Builder().fromUrl(getString(R.string.mapbox_unter_style))) { style: Style ->
            setMapStyleDefault(style)
        }
    }

    private fun setMapStyleDefault(style: Style) {
        addGPSMarkerLayer(style)
        enableLocationComponent(style)

        val latLngBounds: LatLngBounds = LatLngBounds.Builder()
            .include(LatLng(request!!.originLat, request!!.originLong))
            .include(LatLng(request!!.destinationLat, request!!.destinationLong))
            .build()

        mapboxMap.animateCamera(CameraUpdateFactory.newLatLngBounds(latLngBounds, 25), 1000)

        NavigationRoute.builder(this)
            .accessToken(Mapbox.getAccessToken()!!)
            .origin(Point.fromLngLat(request!!.originLong, request!!.originLat))
            .destination(Point.fromLngLat(request!!.destinationLong, request!!.destinationLat))
            .build()
            .getRoute(object: Callback<DirectionsResponse> {
                override fun onResponse(call: Call<DirectionsResponse>, response: Response<DirectionsResponse>) {
                    onRouteResponse(call, response)
                }

                override fun onFailure(call: Call<DirectionsResponse>, t: Throwable) {
                    onRouteFailure(call, t)
                }
            })
        // TODO: also build a GPS follow route from the user to the pickup location
    }

    private fun handleIntents() {
        if (intent.action == Intent.ACTION_VIEW) {
            d(TAG, "received ACTION_VIEW intent for: '${intent.dataString}'")
            when {
                intent?.data!!.path!! == "/request" -> handleRideRequestIntent(intent)
                intent?.data!!.path!! == "/confirm" -> handleRideConfirmIntent(intent)
            }
        }
    }

    private fun handleRideRequestIntent(intent: Intent) {
        d(TAG, "handling intent as ride/request...")
        // query parameters in the URI
        val userId: String? = intent.data!!.getQueryParameter("userId")
        val originLong: String? = intent.data!!.getQueryParameter("originLong")
        val originLat: String? = intent.data!!.getQueryParameter("originLat")
        val destinationLong: String? = intent.data!!.getQueryParameter("destinationLong")
        val destinationLat: String? = intent.data!!.getQueryParameter("destinationLat")
        val pickupTime: String? = intent.data!!.getQueryParameter("pickup")

        if (userId == null)
            error("could not handle ride request", "no user ID provided in ride request")
        if (originLong == null)
            error("could not handle ride request", "no origin longitude provided in ride request")
        if (originLat == null)
            error("could not handle ride request", "no origin latitude provided in ride request")
        if (destinationLong == null)
            error("could not handle ride request", "no destination longitude provided in ride request")
        if (destinationLat == null)
            error("could not handle ride request", "no destination latitude provided in ride request")
        if (pickupTime == null)
            error("could not handle ride request", "no pickup time provided in ride request")

        request = JourneyRequestInfo()
        request!!.originLong = originLong!!.toDouble()
        request!!.originLat = originLat!!.toDouble()
        request!!.destinationLong = destinationLong!!.toDouble()
        request!!.destinationLat = destinationLat!!.toDouble()
        request!!.pickupTime = pickupTime!!.toLong()

        this.user = model.getUser(userId!!)

        // add the journey request to the model
        d(TAG, "adding request to storage model...")

        request = model.getJourneyRequest(model.addJourneyRequest(request!!))

        model.save()

        d(TAG, "added request to storage model with id '${request!!.id}'")

        onMapReadyDefault()
        setupDriverList()
    }

    private fun handleRideConfirmIntent(intent: Intent) {
        d(TAG, "handling intent as ride/confirm...")
        // query parameters in the URI
        val userId: String? = intent.data!!.getQueryParameter("userId")
        val requestId: String? = intent.data!!.getQueryParameter("requestId")
        val driverId: String? = intent.data!!.getQueryParameter("driverId")

        if (userId == null)
            error("could not handle ride confirmation", "no user ID provided in ride confirmation")
        if (requestId == null)
            error("could not handle ride confirmation", "no request ID provided in the ride confirmation")
        if (driverId == null)
            error("could not handle ride confirmation", "no driver ID provided in the ride confirmation")

        request = model.getJourneyRequest(requestId!!)
        user = model.getUser(userId!!)
        driver = model.getDriver(driverId!!)

        d(TAG, "adding confirmed journey for user '$userId' with driver '$driverId' on request '$requestId' to storage model")
        try {
            val journey = model.confirmJourney(user!!, request!!, driver!!)
            model.save()

            // move to the info page
            val uri = Uri.parse("unter://ride/info?journeyId=${journey!!.id}")
            val infoIntent: Intent = Intent(Intent.ACTION_VIEW, uri)
            infoIntent.action = Intent.ACTION_VIEW
            finish()
            startActivity(infoIntent)
        } catch (e: JourneyConfirmationException) {
            error("you're already in a journey!", e.message?: "can't add user '${user!!.id} to a journey because they are already in journey '${user!!.journeyId}'")
            finish()
        }
    }

    private fun error(message: String, error: String) {
        e(TAG, error)
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        finish()
    }

    private fun onDriverListItemClick(parent: AdapterView<*>, view: View, position: Int, id: Long) {
        this.driver  = parent.adapter.getItem(position) as DriverInfo
        d(TAG, "user '${user!!.id}' selected driver with id '${driver!!.id}'")

        // emit ride/confirm intent here
        val requestUri: Uri = Uri.parse("unter://ride/confirm" +
                "?userId=${user!!.id}" +
                "&requestId=${request!!.id}" +
                "&driverId=${driver!!.id}")

        model.save()
        finish()
        d(TAG, "emitting journey confirm intent with URI: '$requestUri'")
        startActivity(Intent(Intent.ACTION_VIEW, requestUri))
    }

    private fun onRouteResponse(call: Call<DirectionsResponse>, response: Response<DirectionsResponse>) {
        d(TAG, "mapbox route request response code: ${response.code()}")
        if (response.body() == null) {
            e(TAG, "route request response body is null")
            return
        } else if (response.body()!!.routes().size < 1) {
            e(TAG, "no routes found (make sure to set the right user token and access token)")
            return
        }

        // set the route for use on screen

        routeOriginToDestination = response.body()!!.routes()[0]
        // update the most recent route on the screen
        if (navigationMapRoute != null) {
            navigationMapRoute!!.updateRouteArrowVisibilityTo(false)
            navigationMapRoute!!.updateRouteVisibilityTo(false)
        } else {
            navigationMapRoute = NavigationMapRoute(null, mapView, mapboxMap)
        }
        navigationMapRoute!!.addRoute(routeOriginToDestination)
    }

    private fun onRouteFailure(call: Call<DirectionsResponse>, t: Throwable) {
        e(TAG, "error: ${t.message}")
    }

    private fun addGPSMarkerLayer(style: Style) {
        val geoJsonSource = GeoJsonSource("destination-source-id")
        style.addSource(geoJsonSource)

        style.addImage(
            "destination-icon-id",
            BitmapFactory.decodeResource(resources, R.drawable.mapbox_marker_icon_default)
        )

        val symbolLayer = SymbolLayer("destination-symbol-layer-id", "destination-source-id")
        symbolLayer.withProperties(
            PropertyFactory.iconImage("destination-icon-id"),
            PropertyFactory.iconAllowOverlap(true),
            PropertyFactory.iconIgnorePlacement(true)
        )

        style.addLayer(symbolLayer)
    }

    private fun addMarker(style: Style, position: LatLng) {
        style.addImage("marker-icon-id", BitmapFactory.decodeResource(
            resources,
            R.drawable.mapbox_marker_icon_default))

        val geoJsonSource = GeoJsonSource(
            "source-id",
            Feature.fromGeometry(Point.fromLngLat(
                position.longitude, position.latitude)
            )
        )

        style.addSource(geoJsonSource)

        val symbolLayer = SymbolLayer("layer-id", "source-id")
        symbolLayer.withProperties(PropertyFactory.iconImage("marker-icon-id"))
        style.addLayer(symbolLayer)
    }

    @SuppressLint("MissingPermission", "NewApi" /* TODO: why do we need to suppress this warning? */)
    private fun enableLocationComponent(style: Style) {
        // adapted from this source: https://docs.mapbox.com/android/maps/overview/location-component/

        // Check if permissions are enabled and if not request
        if (PermissionsManager.areLocationPermissionsGranted(this)) {

            // Activate with options
            locationComponent.activateLocationComponent(this, style)

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

    private fun enableTooltip(message: String) {
        cardTooltip.visibility = View.VISIBLE
        textViewTooltip.text = message
    }

    private fun disableTooltip() {
        cardTooltip.visibility = View.GONE
    }

    private fun makeFormattedDate(timestamp: Long): String {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timestamp * 1000
        return "${calendar.get(HOUR_OF_DAY)}:${calendar.get(MINUTE)}"
    }

    override fun onDestroy() {
        super.onDestroy()
        model.save()
        mapView.onDestroy()
        locationComponent.onDestroy()
    }

    override fun onPause() {
        super.onPause()
        model.save()
        mapView.onPause()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        model.save()
        mapView.onLowMemory()
    }

    override fun onResume() {
        super.onResume()
        model.load()
        mapView.onResume()
    }

    override fun onStart() {
        super.onStart()
        model.load()
        mapView.onStart()
    }

    override fun onStop() {
        super.onStop()
        model.save()
        mapView.onStop()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView.onSaveInstanceState(outState)
    }
}