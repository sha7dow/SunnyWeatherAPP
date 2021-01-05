package com.sunnyweather.android.logic.dao

import android.Manifest
import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.location.LocationManager
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.sunnyweather.android.SunnyWeatherApplication
import com.sunnyweather.android.logic.model.Location
import com.sunnyweather.android.logic.model.Place
import java.io.IOException


object PlaceDao {

    fun savePlace(place: Place) {
        sharedPreferences().edit {
            putString("place", Gson().toJson(place))
        }
    }

    fun savePreferencePlaces(places: MutableList<Place>){
        sharedPreferences().edit{
            putString("places",Gson().toJson(places));
        }
    }

    fun getSavedPlace(): Place {
        val placeJson = sharedPreferences().getString("place", "")
        return Gson().fromJson(placeJson, Place::class.java)
    }

    fun getPreferencePlaces(): Set<Place>{
        val placeJsonArray = sharedPreferences().getString("places","");
        return Gson().fromJson(placeJsonArray,
            object : TypeToken<Set<Place?>?>() {}.type
        )
    }
  //获取当前位置
    fun getLocalPlace():Place{
        val locMan = SunnyWeatherApplication.context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val checkCameraPermission = ContextCompat.checkSelfPermission(SunnyWeatherApplication.context, Manifest.permission.ACCESS_FINE_LOCATION)
        val checkCallPhonePermission =
            ContextCompat.checkSelfPermission(SunnyWeatherApplication.context, Manifest.permission.ACCESS_COARSE_LOCATION)

        var location = locMan.getLastKnownLocation(LocationManager.GPS_PROVIDER)
        if (location == null) {
            location = locMan.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
        }
        val ge = Geocoder(SunnyWeatherApplication.context)
        var addressList =ArrayList<Address>()
        try {
            if (location != null) {
                addressList = ge.getFromLocation(location.latitude,location.longitude,1) as ArrayList<Address>
            }
        }catch (e: IOException){
            e.printStackTrace()
        }



        val Location:Location = Location(location?.latitude.toString(), location?.longitude.toString());
        var LocalPlace:Place = Place(addressList[0].getAddressLine(0),Location,addressList[0].getAddressLine(0));

        return LocalPlace;
    }

    fun isPlaceSaved() = sharedPreferences().contains("place")

    fun isSavePreferencePlaces() = sharedPreferences().contains("places")

    private fun sharedPreferences() =
        SunnyWeatherApplication.context.getSharedPreferences("sunny_weather", Context.MODE_PRIVATE)

}