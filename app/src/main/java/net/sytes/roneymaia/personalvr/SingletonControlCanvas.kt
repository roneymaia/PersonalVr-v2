package net.sytes.roneymaia.personalvr

import android.content.res.AssetManager
import com.google.android.gms.maps.model.Marker
import com.google.firebase.database.FirebaseDatabase

object SingletonControlCanvas{

    var imagensgif: Array<String?> = arrayOf<String?>()
        get() = field
        set(value) {
            field = value
        }

    var imageNumber: Int? = 0
        get() = field
        set(value) {
            field = value
        }

    var assets: AssetManager? = null
        get() = field
        set(value) {
            field = value
        }

    var markerPlayer: Marker? = null
        get() = field
        set(value) {
            field = value
        }

    var uid: String? = ""
        get() = field
        set(value) {
            field = value
        }

    var firebaseDb: FirebaseDatabase? = null
        get() = field
        set(value) {
            field = value
        }
}