package com.unter.activity

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.support.v4.app.NotificationCompat
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.CardView
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.RatingBar
import android.widget.TextView
import android.widget.Toast
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
import com.unter.R
import com.unter.model.JourneyInfo
import com.unter.model.JourneyRequestInfo
import com.unter.model.UnterAppModel
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.*
import java.util.Calendar.HOUR_OF_DAY
import java.util.Calendar.MINUTE

@SuppressLint("LogNotTimber")
class JourneyInfoActivity : AppCompatActivity() {

    private val TAG: String = JourneyInfoActivity::class.java.canonicalName!!

    private lateinit var mapView: MapView
    private lateinit var mapboxMap: MapboxMap

    private lateinit var cardTooltip: CardView
    private lateinit var textViewTooltip: TextView

    // debug buttons
    private lateinit var buttonDebugDriverArrive: Button
    private lateinit var buttonDebugJourneyFinish: Button

    // assessment screen UI

    private lateinit var cardAssessmentView: CardView

    private lateinit var textViewDriverName: TextView
    private lateinit var textViewDriverFee: TextView
    private lateinit var ratingBarDriverRating: RatingBar
    private lateinit var buttonDriverPay: Button

    // background components for maps

    private lateinit var locationComponent: LocationComponent

    private var navigationMapRoute: NavigationMapRoute? = null
    private lateinit var routeOriginToDestination: DirectionsRoute

    private lateinit var model: UnterAppModel

    private var journey: JourneyInfo? = null
    private var request: JourneyRequestInfo? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Mapbox.getInstance(this, resources.getString(R.string.mapbox_access_token))
        setContentView(R.layout.activity_journey_info)

        createNotificationChannel()

        model = ViewModelProviders.of(this).get(UnterAppModel::class.java)

        // only initialise the model if not already initialised (this activity may be spawned from anywhere)
        if (!model.isInitialised())
            model.initStorage(applicationContext)
        model.load()

        mapView = findViewById(R.id.info_mapView)
        mapView.onCreate(savedInstanceState)

        buttonDebugDriverArrive = findViewById(R.id.button_debug_driver_arrive)
        buttonDebugJourneyFinish = findViewById(R.id.button_debug_journey_finish)

        cardAssessmentView = findViewById(R.id.card_assessment_view)
        cardAssessmentView.visibility = View.GONE

        textViewDriverName = findViewById(R.id.driver_name)
        textViewDriverFee = findViewById(R.id.text_driver_fee)
        ratingBarDriverRating = findViewById(R.id.ratingbar_driver_rating)
        buttonDriverPay = findViewById(R.id.button_driver_pay)

        buttonDebugDriverArrive.setOnClickListener {

            val uri = Uri.parse("unter://ride/info?journeyId=${journey!!.id}")
            val intent = Intent(Intent.ACTION_VIEW, uri)
            val pendingIntent: PendingIntent = PendingIntent.getActivity(this, 0, intent, 0)

            var builder = NotificationCompat.Builder(this, "unter")
                .setSmallIcon(R.drawable.mapbox_compass_icon)
                .setContentTitle("Unter")
                .setContentText("your driver is nearly here!")
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)

            // TODO: make this happen in response to updates
            val notif = builder.build()
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).notify(0, notif)
        }

        buttonDebugJourneyFinish.setOnClickListener {
            cardAssessmentView.visibility = View.VISIBLE
        }

        buttonDriverPay.setOnClickListener {
            // assess the journey
            journey!!.finish()
            journey!!.rating = ratingBarDriverRating.rating

            // allow the user to take a new journey
            model.getUser(journey!!.userId).journeyId = null

            // save the model changes
            model.save()

            finish()
            startActivity(Intent(this, MainActivity::class.java))
        }

        mapView.getMapAsync { map ->
            mapboxMap = map
            locationComponent = mapboxMap.locationComponent
            handleIntents()
        }
    }

    private fun handleIntents() {
        Log.d(TAG, "received ACTION_VIEW intent for: '${intent.dataString}'")
        if (intent.action == Intent.ACTION_VIEW) {
            when {
                intent.data!!.path == "/info" -> handleInfoIntent(intent)
            }
        }
    }

    private fun handleInfoIntent(intent: Intent) {
        // query parameters in the URI
        val journeyId: String? = intent.data!!.getQueryParameter("journeyId")

        if (journeyId == null)
            error("could not get journey information", "no journey ID provided in ride info request")

        journey = model.getJourney(journeyId!!)
        request = model.getJourneyRequest(journey!!.requestId)

        onMapReadyWithMarker()
        setupTooltip()
        enableTooltip("head towards the marker for the pickup time: ${makeFormattedDate(request!!.pickupTime)}")
    }

    private fun onMapReadyWithMarker() {
        mapboxMap.setStyle(Style.Builder().fromUrl(getString(R.string.mapbox_unter_style))) { style: Style ->
            setMapStyleDefault(style)
            // set up the map accordingly
            addMarker(style, LatLng(request!!.originLat, request!!.originLong))
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
    }

    private fun updateRouteWithGPS() {
        @SuppressLint("MissingPermission")
        val lastLocation = mapboxMap.locationComponent.lastKnownLocation

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

    private fun onRouteResponse(call: Call<DirectionsResponse>, response: Response<DirectionsResponse>) {
        Log.d(TAG, "mapbox route request response code: ${response.code()}")
        if (response.body() == null) {
            Log.e(TAG, "route request response body is null")
            return
        } else if (response.body()!!.routes().size < 1) {
            Log.e(TAG, "no routes found (make sure to set the right user token and access token)")
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
        Log.e(TAG, "error: ${t.message}")
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

    private fun setupTooltip() {
        textViewTooltip = findViewById(R.id.info_textview_tooltip)
        cardTooltip = findViewById(R.id.info_card_tooltip)
        disableTooltip()
    }

    private fun createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel("unter", "unter", importance).apply {
                description = "a channel for notifying unter users about journey updates"
            }
            // Register the channel with the system
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
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

    private fun error(message: String, error: String) {
        Log.e(TAG, error)
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        model.save()
//        mapView.onDestroy()
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