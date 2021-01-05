package com.sunnyweather.android.ui.weather

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import com.sunnyweather.andriod.CityManage.CityManage
import com.sunnyweather.android.R
import com.sunnyweather.android.logic.model.Location
import com.sunnyweather.android.logic.model.Place
import com.sunnyweather.android.logic.model.Weather
import com.sunnyweather.android.logic.model.getSky
import com.sunnyweather.android.ui.place.PlaceViewModel
import com.sunnyweather.android.ui.place.PreferencePlaceAdapter
import com.sunnyweather.android.ui.setting.SettingActivity
import kotlinx.android.synthetic.main.activity_weather.*
import kotlinx.android.synthetic.main.citymanage_layout.*
import kotlinx.android.synthetic.main.forecast.*
import kotlinx.android.synthetic.main.life_index.*
import kotlinx.android.synthetic.main.now.*
import kotlinx.android.synthetic.main.now.placeName
import kotlinx.android.synthetic.main.place_item.*
import kotlinx.android.synthetic.main.wind_index.*
import java.text.SimpleDateFormat
import java.util.*

class WeatherActivity : AppCompatActivity() {

    val viewModel by lazy { ViewModelProviders.of(this).get(WeatherViewModel::class.java) }
    val viewModel1 by lazy { ViewModelProviders.of(this).get(PlaceViewModel::class.java) }
    private lateinit var adapter: PreferencePlaceAdapter
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 21) {
            val decorView = window.decorView
            decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            window.statusBarColor = Color.TRANSPARENT
        }
        setContentView(R.layout.activity_weather)


        //获取地点经度
        if (viewModel.locationLng.isEmpty()) {
            viewModel.locationLng = intent.getStringExtra("location_lng") ?: ""
        }
        //获取地点纬度
        if (viewModel.locationLat.isEmpty()) {
            viewModel.locationLat = intent.getStringExtra("location_lat") ?: ""
        }
        //获取地名
        if (viewModel.placeName.isEmpty()) {
            viewModel.placeName = intent.getStringExtra("place_name") ?: ""
        }
        //调用WeatherViewModel类
        viewModel.weatherLiveData.observe(this, Observer { result ->
            val weather = result.getOrNull()
           // Log.d("weather",weather.toString())
            if (weather != null) {
                var preferenceList:MutableList<Place> = mutableListOf()
                if(viewModel1.isPreferencePlacesSaved()) {
                    preferenceList = viewModel1.getPreferencePlaces().toMutableList()
                }
                addList(viewModel1.getLocalPlace().name,preferenceList)
                val layoutManager = LinearLayoutManager(this)
                recyclerView.layoutManager = layoutManager
                adapter = PreferencePlaceAdapter(this, preferenceList)
                recyclerView.adapter = adapter
               //点击该天气卡片后被加入该天气自动加入城市布局
                addList(viewModel.placeName,preferenceList)
                viewModel1.savePreferencePlaces(preferenceList)
                showWeatherInfo(weather) //显示showWeatherInfo方法
            } else {
                Toast.makeText(this, "无法成功获取天气信息", Toast.LENGTH_SHORT).show()
                result.exceptionOrNull()?.printStackTrace()
            }
            swipeRefresh.isRefreshing = false
        })
        swipeRefresh.setColorSchemeResources(R.color.colorPrimary)
        refreshWeather()
        swipeRefresh.setOnRefreshListener {
            refreshWeather()
        }




        viewModel1.placeLiveData.observe(this, Observer{ result ->
            val places = result.getOrNull()
            if (places != null) {
                viewModel1.placeList.clear()
                viewModel1.placeList.addAll(places)
                val intent = Intent(this, WeatherActivity::class.java).apply {
                    val places = viewModel1.placeList
                    putExtra("location_lng", places[0].location.lng)
                    putExtra("location_lat", places[0].location.lat)
                    putExtra("place_name", places[0].name)
                }
                this.startActivity(intent)
            }
        })

        //设置按钮
        settingsBtn.setOnClickListener {
            val intent4 = Intent(this, SettingActivity::class.java)
            startActivity(intent4)

        }
        //滑动菜单按钮
        navBtn.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }
        //城市管理按钮
        navBtnr.setOnClickListener {
            var intent=Intent(this,CityManage::class.java)
            startActivity(intent)
        }

//        navBtnl.setOnClickListener {
//            var intent1=Intent(this,PreferencePlaceActivity::class.java)
//            startActivity(intent1)
//        }
        //滑动窗口的设置
        drawerLayout.addDrawerListener(object : DrawerLayout.DrawerListener {
            override fun onDrawerStateChanged(newState: Int) {}

            override fun onDrawerSlide(drawerView: View, slideOffset: Float) {}

            override fun onDrawerOpened(drawerView: View) {}

            override fun onDrawerClosed(drawerView: View) {
                val manager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                manager.hideSoftInputFromWindow(drawerView.windowToken, InputMethodManager.HIDE_NOT_ALWAYS)
            }
        })
    }

    private fun addList(PlaceName: String, PreferenceList:MutableList<Place>){
        var flag = true
        for(it in PreferenceList){
            if(PlaceName == it.name) {
                flag = false
                break
            }
        }
        if(flag)
            PreferenceList.add(Place(PlaceName, Location("",""),""))
    }
    //刷新天气
    fun refreshWeather() {
        viewModel.refreshWeather(viewModel.locationLng, viewModel.locationLat)
        swipeRefresh.isRefreshing = true
    }
    //当前布局
    private fun showWeatherInfo(weather: Weather) {
        placeName.text = viewModel.placeName
        val realtime = weather.realtime
        val daily = weather.daily

        // 填充now.xml布局中数据
        val currentTempText = "${realtime.temperature.toInt()} ℃"
        currentTemp.text = currentTempText
        currentSky.text = getSky(realtime.skycon).info
        val currentPM25Text = "空气指数 ${realtime.airQuality.aqi.chn.toInt()}"
        currentAQI.text = currentPM25Text
        nowLayout.setBackgroundResource(getSky(realtime.skycon).bg)


        //填充wind_index布局中的数据
        speedText.text = "${realtime.wind.speed.toString()} km/h"
        directionText.text = "${realtime.wind.direction.toString()} N"

        // 填充forecast.xml布局中的数据
        forecastLayout.removeAllViews()
        val days = daily.skycon.size
        for (i in 0 until days) {
            val skycon = daily.skycon[i]
            val temperature = daily.temperature[i]
            val view = LayoutInflater.from(this).inflate(
                R.layout.forecast_item,
                forecastLayout,
                false
            )
            val dateInfo = view.findViewById(R.id.dateInfo) as TextView
            val skyIcon = view.findViewById(R.id.skyIcon) as ImageView
            val skyInfo = view.findViewById(R.id.skyInfo) as TextView
            val temperatureInfo = view.findViewById(R.id.temperatureInfo) as TextView
            val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            dateInfo.text = simpleDateFormat.format(skycon.date)
            val sky = getSky(skycon.value)
            skyIcon.setImageResource(sky.icon)
            skyInfo.text = sky.info
            val tempText = "${temperature.min.toInt()} ~ ${temperature.max.toInt()} ℃"
            temperatureInfo.text = tempText
            forecastLayout.addView(view)
        }
        // 填充life_index.xml布局中的数据
        val lifeIndex = daily.lifeIndex
        coldRiskText.text = lifeIndex.coldRisk[0].desc
        dressingText.text = lifeIndex.dressing[0].desc
        ultravioletText.text = lifeIndex.ultraviolet[0].desc
        carWashingText.text = lifeIndex.carWashing[0].desc
        weatherLayout.visibility = View.VISIBLE
        //分享按钮
        shareBtn.setOnClickListener {
            var share_intent = Intent()
            share_intent.action = Intent.ACTION_SEND //设置分享行为
            share_intent.type = "text/plain" //设置分享内容的类型
            share_intent.putExtra(Intent.EXTRA_SUBJECT, "share") //添加分享内 oid")
            share_intent.putExtra(
                Intent.EXTRA_TEXT, "${viewModel.placeName}的气温是" +
                        "${realtime.temperature.toInt()} ℃" +
                        "\n空气指数是${realtime.airQuality.aqi.chn.toInt()}"
                        + " \n感冒${lifeIndex.coldRisk[0].desc}" + "\n洗车${lifeIndex.carWashing[0].desc}"
            );//添加分享内容
            //创建分享的Dialog
            share_intent = Intent.createChooser(share_intent, "share")
            startActivity(share_intent)
        }

    }

}
