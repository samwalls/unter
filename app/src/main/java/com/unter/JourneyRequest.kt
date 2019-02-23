package com.unter

import android.annotation.SuppressLint
import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.v4.app.Fragment
import android.util.Log.d
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.mapbox.android.core.permissions.PermissionsListener
import com.mapbox.android.core.permissions.PermissionsManager
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
import com.mapbox.mapboxsdk.plugins.annotation.SymbolManager
import com.mapbox.mapboxsdk.plugins.annotation.SymbolOptions
import com.unter.model.UnterAppModel



/**
 * A simple [Fragment] subclass.
 * Activities that contain this fragment must implement the
 * [JourneyRequest.OnFragmentInteractionListener] interface
 * to handle interaction events.
 * Use the [JourneyRequest.newInstance] factory method to
 * create an instance of this fragment.
 *
 */
class JourneyRequest : Fragment(), PermissionsListener {

    private lateinit var model: UnterAppModel

    private lateinit var mapView: MapView
    private lateinit var mapboxMap: MapboxMap

    private lateinit var permissionsManager: PermissionsManager
    private lateinit var locationComponent: LocationComponent

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
        mapView = view.findViewById(R.id.mapView)
        mapView.onCreate(savedInstanceState)
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

            enableLocationComponent(mapboxMap.style!!)
            moveCameraToCurrentLocation()

            // get a symbol manager to add markers as required
            val symbolManager = SymbolManager(mapView, mapboxMap, style)

            mapboxMap.addOnMapClickListener { position ->
                val symbol = symbolManager.create(SymbolOptions().withLatLng(position))
                symbol.textField = "destination"
                d(JourneyRequest::class.java.canonicalName, "map clicked at coordinates: $position")
                false
            }
        }
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
        mapView.onDestroy()
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
