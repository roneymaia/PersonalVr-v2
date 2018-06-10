package net.sytes.roneymaia.personalvr

import android.location.Location
import android.location.LocationListener
import android.support.v7.app.AppCompatActivity
import android.os.Bundle

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import android.location.LocationManager.NETWORK_PROVIDER
import android.content.pm.PackageManager
import android.support.v4.app.ActivityCompat
import android.content.Context
import android.location.LocationManager
import com.google.android.gms.maps.model.*
import com.google.android.gms.maps.model.Marker
import android.os.Handler
import android.os.Message
import android.util.Base64
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.widget.Toast
import com.facebook.login.LoginManager
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.IgnoreExtraProperties


class MapsActivity : AppCompatActivity(), OnMapReadyCallback, LocationListener, ValueEventListener {

    private var mMap: GoogleMap? = null
    private var markerPlayer: Marker? = null
    private var locationManager: LocationManager? = null
    private var userLocNow: LatLng? = null
    private var mapActHandler: Handler = HandlerMaps(this)
    private var mapActThread: Thread = ThreadMaps(mapActHandler)
    private var firebaseDb: FirebaseDatabase? = null
    private var firebaseAuth: FirebaseAuth? = null
    private var arrayMarkers: HashMap<String, UserMark> = HashMap<String, UserMark>()
    private var emailUser: String? = null
    private var mapsMarkers: HashMap<String, Marker> = HashMap<String, Marker>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)

        MapsActivity@this.locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager

        checkPermissionsLoc()

        MapsActivity@this.firebaseDb = FirebaseDatabase.getInstance()
        MapsActivity@this.firebaseAuth = FirebaseAuth.getInstance()

        val currentUser = MapsActivity@this.firebaseAuth!!.currentUser

        if (currentUser == null) {
            finish()
        }

        MapsActivity@this.emailUser = Base64.encodeToString(currentUser?.email?.toByteArray(Charsets.UTF_8), Base64.NO_WRAP or Base64.URL_SAFE)

        MapsActivity@this.firebaseDb!!.getReference("/markers/").addValueEventListener(this)

        //MapsActivity@this.mapActThread.start()

    }

    override fun onResume() {
        super.onResume()
        checkPermissionsLoc()
    }

    override fun onPause() {
        super.onPause()
        MapsActivity@this.locationManager!!.removeUpdates(this)
        MapsActivity@this.firebaseDb!!
                .getReference("/markers/" + MapsActivity@this.emailUser)
                .child("state")
                .setValue("off")
    }


    override fun onCreateOptionsMenu(menu: Menu?): Boolean {

        val inflater = menuInflater

        inflater.inflate(R.menu.mapsmenu, menu)

        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.logout -> {
                removeAuths()
                return true
            }

            else -> return super.onOptionsItemSelected(item)
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {

        MapsActivity@this.mMap = googleMap

        MapsActivity@this.mMap!!.uiSettings.isCompassEnabled = true
        MapsActivity@this.mMap!!.mapType = GoogleMap.MAP_TYPE_NORMAL
        val cameraPosition = CameraPosition.Builder()
                .target(LatLng(-26.323115, -48.862916))
                .zoom(17f)
                .tilt(50f)
                .build()

        MapsActivity@this.mMap!!.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
    }

    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {

    }

    override fun onProviderEnabled(provider: String?) {

    }

    override fun onProviderDisabled(provider: String?) {

    }

    override fun onLocationChanged(location: Location?) {
        if (location != null && MapsActivity@this.mMap != null) {
            val cameraPosition = CameraPosition.Builder()
                    .target(LatLng(location.latitude, location.longitude))
                    .zoom(17f)
                    .tilt(50f)
                    .build()

            MapsActivity@this.userLocNow = LatLng(location.latitude, location.longitude)
            MapsActivity@this.firebaseDb!!
                    .getReference("/markers/" + MapsActivity@this.emailUser)
                    .setValue(UserMark(MapsActivity@this.emailUser!!, location.latitude, location.longitude, "on"))

            MapsActivity@this.mMap!!.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when (requestCode) {
            200 -> {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.size!! > 0 && grantResults[0] === PackageManager.PERMISSION_GRANTED) {
                    finish()
                    startActivity(intent)
                }
            }
        }
    }

    private fun checkPermissionsLoc() {

        val finePerm = ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarsePerm = ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

        if (!(finePerm && coarsePerm)) {
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION), 200)
        } else {
            val location = MapsActivity@ this.locationManager!!.getLastKnownLocation(NETWORK_PROVIDER)

            onLocationChanged(location)

            // Obtain the SupportMapFragment and get notified when the map is ready to be used.
            val mapFragment = supportFragmentManager
                    .findFragmentById(R.id.map) as SupportMapFragment
            mapFragment.getMapAsync(this)

            // getting GPS status
            val isGPSEnabled = MapsActivity@ this.locationManager!!.isProviderEnabled(LocationManager.GPS_PROVIDER)

            // getting network status
            val isNetworkEnabled = MapsActivity@ this.locationManager!!.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

            if (!isGPSEnabled && !isNetworkEnabled) {
                // no network provider is enabled
            } else {
                if (isGPSEnabled) {
                    MapsActivity@ this.locationManager!!.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0f, this)
                } else if (isNetworkEnabled) {
                    MapsActivity@ this.locationManager!!.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0f, this)
                }
            }
        }
    }

    override fun onCancelled(p0: DatabaseError?) {

    }

    override fun onDataChange(p0: DataSnapshot?) {
        checkSnap(p0)
        controlMarkers()
    }

    fun checkSnap(p0: DataSnapshot?) {

        var snapuid: String? = null
        var marker: UserMark? = null
        var state: String? = null

        if (p0 != null){
            for (snap: DataSnapshot in p0.children){

                marker = snap.getValue(UserMark::class.java)
                snapuid = marker!!.uid
                state = marker!!.state

                if (state == "on") {
                    if (MapsActivity@ this.arrayMarkers.isEmpty()) {
                        MapsActivity@ this.arrayMarkers!!.put(snapuid!!, marker!!)
                    } else {
                        val contem = MapsActivity@this.arrayMarkers.containsKey(snapuid)
                        if (contem) {
                            val userMarkUpd = MapsActivity@this.arrayMarkers!![snapuid!!]
                            userMarkUpd!!.uid = marker!!.uid
                            userMarkUpd!!.lat = marker!!.lat
                            userMarkUpd!!.lng = marker!!.lng
                            userMarkUpd!!.state = marker!!.state
                        } else {
                            MapsActivity@ this.arrayMarkers!!.put(snapuid!!, marker!!)
                        }
                    }
                } else {
                    val contem = MapsActivity@this.arrayMarkers.containsKey(snapuid)

                    if (contem) {
                        val userMarkUpd = MapsActivity@this.arrayMarkers!![snapuid!!]
                        userMarkUpd!!.state = "off"
                    }
                }

            }

        }
    }

    fun controlMarkers() {

        MapsActivity@this.mapActHandler.post({

            if (MapsActivity@this.mapsMarkers.keys == null ){
                Log.d("controlMarkers", "veio nulo")
            }

            for (key: String in MapsActivity@this.mapsMarkers.keys) {
                MapsActivity@ this.mapsMarkers[key]!!.remove()
            }

            MapsActivity@this.mapsMarkers.clear()

            for (key: String in MapsActivity@this.arrayMarkers.keys) {
                val usrmark = MapsActivity@ this.arrayMarkers[key]
                val latlng = LatLng(usrmark!!.lat!!, usrmark!!.lng!!)

                if (usrmark.state == "on") {
                    val mark = MapsActivity@ this.mMap!!.addMarker(MarkerOptions().position(latlng))
                    MapsActivity@ this.mapsMarkers[key] = mark
                }
            }

            Log.d("controlMarkers", MapsActivity@this.arrayMarkers.size.toString())
            Log.d("controlMarkersMaps", MapsActivity@ this.mapsMarkers.size.toString())
        })
    }

    fun removeAuths() {
        // remove o login com facebook
        LoginManager.getInstance().logOut()
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build()
        // remove o login com google
        GoogleSignIn.getClient(this@MapsActivity, gso).signOut()
        // remove os logins
        FirebaseAuth.getInstance().signOut()
        finish()
    }

}

class HandlerMaps : Handler {

    private var context: Context? = null

    constructor(context: Context){
        this.context = context
    }

    override fun handleMessage(msg: Message){
        Toast.makeText(context, msg.obj as String, Toast.LENGTH_SHORT).show()
    }
}

class ThreadMaps : Thread {

    var handler: Handler? = null

    constructor(handler: Handler){
        this.handler = handler
    }

    override fun run() {
        Thread.sleep(3000)
        val msg = Message()
        msg.what = 1
        msg.obj = "Deu bom"
        this.handler!!.sendMessage(msg)
    }
}

@IgnoreExtraProperties
class UserMark {

    var uid: String? = null
        get() = field
        set(value) {
            field = value
        }
    var lat: Double? = null
        get() = field
        set(value) {
            field = value
        }
    var lng: Double? = null
        get() = field
        set(value) {
            field = value
        }

    var state: String = "off"
        get() = field
        set(value) {
            field = value
        }

    constructor()

    constructor(uid: String, lat: Double, lng: Double, state: String) {
        this.uid = uid
        this.lat = lat
        this.lng = lng
        this.state = state
    }
}




























