package net.sytes.roneymaia.personalvr

import android.app.Activity
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
import android.content.Intent
import android.graphics.*
import android.location.LocationManager
import android.net.Uri
import com.google.android.gms.maps.model.*
import com.google.android.gms.maps.model.Marker
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.webkit.MimeTypeMap
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
import com.google.firebase.storage.FirebaseStorage
import com.squareup.picasso.Picasso
import com.squareup.picasso.Transformation
import java.io.File


class MapsActivity : AppCompatActivity(), OnMapReadyCallback, LocationListener, ValueEventListener {

    private var mMap: GoogleMap? = null
    private var markerPlayer: Marker? = null
    private var locationManager: LocationManager? = null
    private var userLocNow: LatLng? = null
    private var mapActHandler: Handler? = null
    private var mapBmpHandler: Handler? = null
    private var mapActChk: Handler? = null
    private var mapBmpThread: Thread? = null
    private var firebaseDb: FirebaseDatabase? = null
    private var firebaseAuth: FirebaseAuth? = null
    private var firebaseStorage: FirebaseStorage? = null
    private var arrayMarkers: HashMap<String, UserMark> = HashMap<String, UserMark>()
    private var uidUser: String? = ""
    private var imageUser: String? = ""
    private var mapsMarkers: HashMap<String, Marker> = HashMap<String, Marker>()
    private var bitMapsMrk: HashMap<String, Bitmap?> = HashMap<String, Bitmap?>()
    private var mLastLocation: Location? = null
    private var threadChk: Thread? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)

        MapsActivity@this.mapActHandler = object: Handler(Looper.getMainLooper()) {

            override fun handleMessage(msg: android.os.Message){

            }
        }

        MapsActivity@this.mapActChk = object: Handler(Looper.getMainLooper()) {

            override fun handleMessage(msg: android.os.Message){

            }
        }

        MapsActivity@this.mapBmpHandler = object: Handler(Looper.getMainLooper()) {

            override fun handleMessage(msg: android.os.Message){
                when (msg.what) {
                    1 -> {
                        val myobj = msg.obj as Array<*>
                        this@MapsActivity.bitMapsMrk.put(myobj[0] as String, myobj[1] as Bitmap)
                    }
                    2 -> {
                        val myobj = msg.obj as Array<*>
                        this@MapsActivity.bitMapsMrk.put(myobj[0] as String, myobj[1] as Bitmap)
                    }
                }
            }
        }

        MapsActivity@this.locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager

        checkPermissionsLoc()

        MapsActivity@this.firebaseDb = FirebaseDatabase.getInstance()
        MapsActivity@this.firebaseAuth = FirebaseAuth.getInstance()
        MapsActivity@this.firebaseStorage = FirebaseStorage.getInstance()

        val currentUser = MapsActivity@this.firebaseAuth!!.currentUser

        MapsActivity@this.uidUser = currentUser?.uid
        SingletonControlCanvas.uid = currentUser?.uid
        SingletonControlCanvas.firebaseDb = MapsActivity@this.firebaseDb

        if (currentUser?.uid == null) {
            finish()
        }

        MapsActivity@this.firebaseDb!!.getReference("/markers/").addValueEventListener(this)

        MapsActivity@this.threadChk = Thread(Runnable {

                while (true) {
                    MapsActivity@this.mapActChk!!.post(Runnable {
                        MapsActivity@this.firebaseDb!!
                                .getReference("/markers/" + MapsActivity@this.uidUser)
                                .child("state")
                                .setValue("off")
                        MapsActivity@this.firebaseDb!!
                                .getReference("/markers/" + MapsActivity@this.uidUser)
                                .child("state")
                                .setValue("on")


                    })
                    Thread.sleep(10000)
                }

        })

        MapsActivity@this.threadChk!!.start()

    }

    override fun onResume() {
        super.onResume()

        if(MapsActivity@this.mMap != null){ //prevent crashing if the map doesn't exist yet (eg. on starting activity)
            MapsActivity@this.mMap!!.clear()

            // add markers from database to the map
        }

        checkPermissionsLoc()

    }

    override fun onPause() {
        super.onPause()
        MapsActivity@this.locationManager!!.removeUpdates(this)
        MapsActivity@this.firebaseDb!!
                .getReference("/markers/" + MapsActivity@this.uidUser)
                .child("state")
                .setValue("off")
    }

    override fun onDestroy() {
        super.onDestroy()
        MapsActivity@this.locationManager!!.removeUpdates(this)
        MapsActivity@this.firebaseDb!!
                .getReference("/markers/" + MapsActivity@this.uidUser)
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
            R.id.camera -> {
                val intent = Intent().setType("image/*").setAction(Intent.ACTION_GET_CONTENT)
                startActivityForResult(intent, 100)
                return true
            }

            else -> return super.onOptionsItemSelected(item)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 100 && resultCode == Activity.RESULT_OK && data!!.data != null) {
            val fileUri = data.data
            val riversRef = MapsActivity@this.firebaseStorage!!.getReference("/uploads/" + MapsActivity@this.uidUser)
                    .child(System.currentTimeMillis().toString() + "." + getFileExtension(fileUri))

            riversRef.putFile(fileUri).addOnSuccessListener{ taskSnapshot ->
                MapsActivity@this.imageUser = taskSnapshot.downloadUrl.toString()
                MapsActivity@this.locationManager!!.removeUpdates(this)
                MapsActivity@this.firebaseDb!!
                        .getReference("/markers/" + MapsActivity@this.uidUser)
                        .child("uri")
                        .setValue(taskSnapshot.downloadUrl.toString())
                MapsActivity@this.locationManager!!.removeUpdates(this)
                MapsActivity@this.firebaseDb!!
                        .getReference("/markers/" + MapsActivity@this.uidUser)
                        .child("state")
                        .setValue("on")
            }.addOnFailureListener{ exception ->
                Toast.makeText(MapsActivity@this, "Falha em upload de imagem.", Toast.LENGTH_LONG).show()
            }

            if(MapsActivity@this.mMap != null){ //prevent crashing if the map doesn't exist yet (eg. on starting activity)
                MapsActivity@this.finish()
                startActivity(intent)
            }

        }
    }

    fun getFileExtension(uri: Uri): String? {
        val cr = contentResolver
        val mime = MimeTypeMap.getSingleton()
        return mime.getExtensionFromMimeType(cr.getType(uri))
    }

    override fun onMapReady(googleMap: GoogleMap) {

        MapsActivity@this.mMap = googleMap

        MapsActivity@this.mMap!!.uiSettings.isCompassEnabled = true
        MapsActivity@this.mMap!!.mapType = GoogleMap.MAP_TYPE_NORMAL

        if (MapsActivity@this.mLastLocation != null) {
            val cameraPosition = CameraPosition.Builder()
                    .target(LatLng(MapsActivity@this.mLastLocation!!.latitude, MapsActivity@this.mLastLocation!!.longitude))
                    .zoom(17f)
                    .tilt(50f)
                    .build()

            MapsActivity@this.mMap!!.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
        }

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

            val maps = HashMap<String, Any>()

            maps.put("lat", location.latitude)
            maps.put("lng", location.longitude)
            maps.put("state", "on")

            MapsActivity@this.userLocNow = LatLng(location.latitude, location.longitude)
            MapsActivity@this.firebaseDb!!
                    .getReference("/markers/" + MapsActivity@this.uidUser).updateChildren(maps as Map<String, Any>?)

            MapsActivity@this.mMap!!.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when (requestCode) {
            200 -> {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.size!! > 0 && grantResults[0] === PackageManager.PERMISSION_GRANTED) {
                    //MapsActivity@this.finish()
                    //startActivity(intent)

                    MapsActivity@this.locationManager!!.removeUpdates(this)
                    MapsActivity@this.firebaseDb!!
                            .getReference("/markers/" + MapsActivity@this.uidUser)
                            .child("state")
                            .setValue("on")
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
            MapsActivity@this.mLastLocation = MapsActivity@ this.locationManager!!.getLastKnownLocation(NETWORK_PROVIDER)

            onLocationChanged(MapsActivity@this.mLastLocation)

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
                    MapsActivity@this.locationManager!!.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0f, this)
                } else if (isNetworkEnabled) {
                    MapsActivity@this.locationManager!!.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0f, this)
                }
            }
        }
    }

    override fun onCancelled(p0: DatabaseError?) {

    }

    override fun onDataChange(p0: DataSnapshot?) {
        val upd = checkSnap(p0)
        getBitmaps(upd)
        controlMarkers()
    }

    fun checkSnap(p0: DataSnapshot?) : Boolean {

        var snapuid: String? = null
        var marker: UserMark? = null
        var state: String? = null
        var upd: Boolean = false

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

                            if (userMarkUpd!!.uri != marker!!.uri) {
                                upd = true
                            }

                            userMarkUpd!!.uid = marker!!.uid
                            userMarkUpd!!.lat = marker!!.lat
                            userMarkUpd!!.lng = marker!!.lng
                            userMarkUpd!!.state = marker!!.state
                            userMarkUpd!!.uri = marker!!.uri

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

        return upd
    }

    fun getBitmaps(upd: Boolean) {

        if (MapsActivity@this.arrayMarkers.size > 0) {
            for (key: String in MapsActivity@this.arrayMarkers.keys) {
                if (! MapsActivity@this.bitMapsMrk.containsKey(key) && !MapsActivity@this.arrayMarkers[key]!!.uri!!.isEmpty()) {
                    MapsActivity@ this.bitMapsMrk[key] = null
                    MapsActivity@this.mapBmpThread = ThreadMaps(MapsActivity@this.mapBmpHandler as Handler, MapsActivity@this.arrayMarkers[key]!!.uri!!, key)
                    MapsActivity@this.mapBmpThread!!.start()
                } else if (MapsActivity@this.bitMapsMrk.containsKey(key) && upd) {
                    MapsActivity@ this.bitMapsMrk.remove(key)
                    MapsActivity@this.mapBmpThread = ThreadMaps(MapsActivity@this.mapBmpHandler as Handler, MapsActivity@this.arrayMarkers[key]!!.uri!!, key)
                    MapsActivity@this.mapBmpThread!!.start()
                }
            }
        }

    }

    fun controlMarkers() {

        MapsActivity@this.mapActHandler!!.post({

            for (key: String in MapsActivity@this.mapsMarkers.keys) {
                MapsActivity@ this.mapsMarkers[key]!!.remove()
            }

            MapsActivity@this.mapsMarkers.clear()

            for (key: String in MapsActivity@this.arrayMarkers.keys) {
                val usrmark = MapsActivity@ this.arrayMarkers[key]
                val latlng = LatLng(usrmark!!.lat!!, usrmark!!.lng!!)
                val bmp = MapsActivity@this.bitMapsMrk[key]

                if (usrmark.state == "on" && bmp != null) {
                    val mark = MapsActivity@this.mMap!!.addMarker(MarkerOptions().position(latlng).icon(BitmapDescriptorFactory.fromBitmap(bmp)))
                    MapsActivity@ this.mapsMarkers[key] = mark
                }
            }


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

class ThreadMaps : Thread {

    var handler: Handler? = null
    var uri: String? = null
    var key: String? = null

    constructor(handler: Handler, uri: String, key: String){
        this.handler = handler
        this.uri = uri
        this.key = key
    }

    override fun run() {

        try {
            val bitmap = Picasso.get().load(uri).transform(CircleTransform()).resize(264, 264).get()

            val msg = Message()
            msg.what = 1
            msg.obj = arrayOf(
                    key,
                    bitmap
            )
            this.handler!!.sendMessage(msg)

        } catch (e: Exception) {

        }


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

    var uri: String? = null
        get() = field
        set(value) {
            field = value
        }

    constructor()

    constructor(uid: String?, lat: Double?, lng: Double?, state: String, uri: String?) {
        this.uid = uid
        this.lat = lat
        this.lng = lng
        this.state = state
        this.uri = uri
    }
}

class CircleTransform : Transformation {

    override fun transform(source: Bitmap): Bitmap {

        val size = Math.min(source.width, source.height)

        val x = (source.width - size) / 2
        val y = (source.height - size) / 2

        val squaredBitmap = Bitmap.createBitmap(source, x, y, size, size)
        if (squaredBitmap != source) {
            source.recycle()
        }

        val bitmap = Bitmap.createBitmap(size, size, source.config)

        val canvas = Canvas(bitmap)
        val paint = Paint()
        val shader = BitmapShader(squaredBitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
        paint.shader = shader
        paint.isAntiAlias = true

        val r = size / 2f
        canvas.drawCircle(r, r, r, paint)

        squaredBitmap.recycle()
        return bitmap
    }


    override fun key(): String {
        return "circle"
    }
}




























