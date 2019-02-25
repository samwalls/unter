package com.unter.activity

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log.d
import android.util.Log.e
import android.widget.Toast
import com.mapbox.android.core.permissions.PermissionsManager
import com.mapbox.api.directions.v5.models.DirectionsResponse
import com.mapbox.api.directions.v5.models.DirectionsRoute
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
import com.unter.R
import com.unter.model.JourneyRequestInfo
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

@SuppressLint("LogNotTimber")
class JourneyConfirmActivity : Activity() {

    private val TAG: String = JourneyConfirmActivity::class.java.canonicalName!!

    private lateinit var mapView: MapView
    private lateinit var mapboxMap: MapboxMap

    private lateinit var locationComponent: LocationComponent

    private lateinit var journeyRequestInfo: JourneyRequestInfo

    private var navigationMapRoute: NavigationMapRoute? = null
    private lateinit var routeOriginToDestination: DirectionsRoute

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_journey_confirm)

        handleIntents()

        mapView = findViewById(R.id.mapView)
        mapView.onCreate(savedInstanceState)

        mapView.getMapAsync { map ->
            mapboxMap = map
            onMapReady()
        }
    }

    private fun onMapReady() {

        mapboxMap.setStyle(Style.Builder().fromUrl(getString(R.string.mapbox_unter_style))) { style: Style ->

            addDestinationMarkerLayer(style)
            enableLocationComponent(style)

            val latLngBounds: LatLngBounds = LatLngBounds.Builder()
                .include(LatLng(journeyRequestInfo.originLat, journeyRequestInfo.originLong))
                .include(LatLng(journeyRequestInfo.destinationLat, journeyRequestInfo.destinationLong))
                .build()

            mapboxMap.animateCamera(CameraUpdateFactory.newLatLngBounds(latLngBounds, 25), 1000)

            NavigationRoute.builder(this)
                .accessToken(Mapbox.getAccessToken()!!)
                .origin(Point.fromLngLat(journeyRequestInfo.originLong, journeyRequestInfo.originLat))
                .destination(Point.fromLngLat(journeyRequestInfo.destinationLong, journeyRequestInfo.destinationLat))
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
    }

    private fun handleIntents() {
        if (intent?.action == Intent.ACTION_VIEW) {
            d(TAG, "received ACTION_VIEW intent for: '${intent.dataString}'")

            // query parameters in the URI
            val userId: String? = intent.data!!.getQueryParameter("userId")
            val originLong: String? = intent.data!!.getQueryParameter("originLong")
            val originLat: String? = intent.data!!.getQueryParameter("originLat")
            val destinationLong: String? = intent.data!!.getQueryParameter("destinationLong")
            val destinationLat: String? = intent.data!!.getQueryParameter("destinationLat")

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

            journeyRequestInfo = JourneyRequestInfo()
            journeyRequestInfo.originLong = originLong!!.toDouble()
            journeyRequestInfo.originLat = originLat!!.toDouble()
            journeyRequestInfo.destinationLong = destinationLong!!.toDouble()
            journeyRequestInfo.destinationLat = destinationLat!!.toDouble()
        }
    }

    private fun error(message: String, error: String) {
        e(TAG, error)
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        finish()
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

    private fun addDestinationMarkerLayer(style: Style) {
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

    @SuppressLint("MissingPermission")
    private fun enableLocationComponent(style: Style) {
        // adapted from this source: https://docs.mapbox.com/android/maps/overview/location-component/

        // Check if permissions are enabled and if not request
        if (PermissionsManager.areLocationPermissionsGranted(this)) {

            // Get an instance of the component
            locationComponent = mapboxMap.locationComponent

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
}