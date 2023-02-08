package com.example.mymqtttest;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import com.baidu.mapapi.CoordType;
import com.baidu.mapapi.SDKInitializer;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.BitmapDescriptor;
import com.baidu.mapapi.map.BitmapDescriptorFactory;
import com.baidu.mapapi.map.MapStatusUpdate;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.MarkerOptions;
import com.baidu.mapapi.map.MyLocationConfiguration;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.map.OverlayOptions;
import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.search.core.SearchResult;
import com.baidu.mapapi.search.route.BikingRouteResult;
import com.baidu.mapapi.search.route.DrivingRouteResult;
import com.baidu.mapapi.search.route.IndoorRouteResult;
import com.baidu.mapapi.search.route.MassTransitRouteResult;
import com.baidu.mapapi.search.route.OnGetRoutePlanResultListener;
import com.baidu.mapapi.search.route.PlanNode;
import com.baidu.mapapi.search.route.RoutePlanSearch;
import com.baidu.mapapi.search.route.TransitRouteResult;
import com.baidu.mapapi.search.route.WalkingRoutePlanOption;
import com.baidu.mapapi.search.route.WalkingRouteResult;
import com.baidu.mapapi.utils.CoordinateConverter;

import org.json.JSONException;
import org.json.JSONObject;

import overlayutil.WalkingRouteOverlay;


public class MapActivity extends Activity implements IGetMessageCallBack{
    private final String TAG="MapActivity";
    double x1,y1,x2=112.71619,y2=26.902491;


    private MapView mapView;//声明地图组件
    private BaiduMap baiduMap;//定义百度地图对象
    private BaiduMap baiduMap1;
    private boolean isFirstLoc=true;//记录是否是第一次定位
    private MyLocationConfiguration.LocationMode locationMode;//当前定位模式
    private MyLocationConfiguration.LocationMode locationMode1;//当前定位模式
    private LatLng target;
    private MQTTService mqttService;
    private MyServiceConnection serviceConnection;

    RoutePlanSearch mSearch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        SDKInitializer.initialize(getApplicationContext());//初始化地图SDK
        setContentView(R.layout.activity_map);
        mapView = (MapView) findViewById(R.id.bmapView);
        baiduMap = mapView.getMap();//获取百度地图对象

        initSearch();

        baiduMap1=mapView.getMap();
        Intent intent=getIntent();
        Bundle bundle = intent.getBundleExtra("Message");
        x1=bundle.getDouble("Log1");
        x2=bundle.getDouble("Log2");
        y1=bundle.getDouble("Lat1");
        y2=bundle.getDouble("Lat2");
        Log.d("经度1",new String(String.valueOf(x1)));
        Log.d("纬度1",new String(String.valueOf(y1)));
        Log.d("经度2",new String(String.valueOf(x2)));
        Log.d("纬度2",new String(String.valueOf(y2)));
        serviceConnection = new MyServiceConnection();
        serviceConnection.setIGetMessageCallBack(MapActivity.this);
        Intent intent1 = new Intent(this, MQTTService.class);

        bindService(intent1, serviceConnection, Context.BIND_AUTO_CREATE);


        //获取系统的LocationManager对象
        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        //权限检查
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        //设置每一秒获取一次location信息


        locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                1000,//更新数据时间为1秒
                1,//位置间隔1米
                //位置监听器
                new LocationListener() {//GPS定位信息发生改变时触发，用于更新位置信息
                    @Override
                    public void onLocationChanged(@NonNull Location location) {
                        locationUpdates(location);
                    }

                    @Override
                    public void onStatusChanged(String provider, int status, Bundle extras) {

                    }
                }
        );
        Location location=locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

        locationUpdates(location);
    }

    private void initSearch(){
        mSearch= RoutePlanSearch.newInstance();
        OnGetRoutePlanResultListener listener =new OnGetRoutePlanResultListener() {
            @Override
            public void onGetWalkingRouteResult(WalkingRouteResult walkingRouteResult) {
                baiduMap.clear();
                Log.e(TAG, "onGetWalkingRouteResult: walkingRouteResult"+walkingRouteResult.toString() );
                if(walkingRouteResult==null /*|| walkingRouteResult.error!= SearchResult.ERRORNO.NO_ERROR*/){
                    Toast.makeText(MapActivity.this,"抱歉,未查找到合适路线",Toast.LENGTH_SHORT).show();
                }
                if(walkingRouteResult.error==SearchResult.ERRORNO.AMBIGUOUS_ROURE_ADDR){
                    //起终点或途经点地址有歧义,通过以下接口获取建议查询信息
                    //walkingRouteResult.getSuggestAddrInfo();
                    return;
                }
                if (walkingRouteResult.error==SearchResult.ERRORNO.NO_ERROR){
                    if (walkingRouteResult.getRouteLines().size()>=1){
                        WalkingRouteOverlay overlay=new WalkingRouteOverlay(baiduMap);
//                        baiduMap.setOnMarkerClickListener(overlay);
                        overlay.setData(walkingRouteResult.getRouteLines().get(0));//设置路线数据
                        overlay.addToMap();
                        overlay.zoomToSpan();//缩放地图
                    }else {
                        Toast.makeText(MapActivity.this,"暂无路径规划",Toast.LENGTH_SHORT).show();
                    }
                }

            }

            @Override
            public void onGetTransitRouteResult(TransitRouteResult transitRouteResult) {

            }

            @Override
            public void onGetMassTransitRouteResult(MassTransitRouteResult massTransitRouteResult) {

            }

            @Override
            public void onGetDrivingRouteResult(DrivingRouteResult drivingRouteResult) {

            }

            @Override
            public void onGetIndoorRouteResult(IndoorRouteResult indoorRouteResult) {

            }

            @Override
            public void onGetBikingRouteResult(BikingRouteResult bikingRouteResult) {

            }
        };
        mSearch.setOnGetRoutePlanResultListener(listener);
    }

    private void navigate(LatLng start,LatLng end){
        Log.e(TAG, "navigate: start"+start );
        Log.e(TAG, "navigate: end"+end );

        PlanNode startNode=PlanNode.withLocation(start);
        PlanNode endNode=PlanNode.withLocation(end);

        Log.e(TAG, "navigate: startNode"+startNode );
        Log.e(TAG, "navigate: endNode"+endNode );
        mSearch.walkingSearch((new WalkingRoutePlanOption())
                .from(startNode)
                .to(endNode));
    }


    public void locationUpdates(Location location){
        if(location!=null){
            LatLng ll=new LatLng(location.getLatitude(),location.getLongitude());//获取用户当前经纬度
            CoordinateConverter converter  = new CoordinateConverter()
                    .from(CoordinateConverter.CoordType.GPS)
                    .coord(ll);

            //desLatLng 转换后的坐标
            LatLng desLatLng = converter.convert();
            Log.i("Location","纬度："+desLatLng.latitude+"| 经度: "+desLatLng.longitude);
            if (isFirstLoc){
                MapStatusUpdate u= MapStatusUpdateFactory.newLatLng(desLatLng);//更新坐标位置
                baiduMap.animateMapStatus(u);//设置地图位置
                isFirstLoc=false;//取消第一次定位
            }
            //构造定位数据


                    MyLocationData locData=new MyLocationData.Builder().accuracy(location.getAccuracy())
                    .direction(100)//设置方向信息
                    .latitude(desLatLng.latitude)//设置纬度坐标
                    .longitude(desLatLng.longitude)//设置经度坐标
                    .build();


            baiduMap.setMyLocationData(locData);//设置定位数据
            BitmapDescriptor bitmapDescriptor= BitmapDescriptorFactory.fromResource(R.mipmap.icon_me);//设置自定义图标
            locationMode=MyLocationConfiguration.LocationMode.NORMAL;//设置定位模式
            MyLocationConfiguration config=new MyLocationConfiguration(locationMode,true,bitmapDescriptor);//设置构造方式
            baiduMap.setMyLocationConfiguration(config);//显示定位图标
            setTargetLocation(target,x2,y2);



            target = new LatLng(y2, x2);//定义目标坐标点
            LatLng end=new LatLng(target.latitude,target.longitude);//获取用户当前经纬度
            CoordinateConverter converters  = new CoordinateConverter()
                    .from(CoordinateConverter.CoordType.GPS)
                    .coord(end);

            //desLatLng 转换后的坐标
            LatLng endLatLng = converters.convert();

            navigate(desLatLng,endLatLng);

        }else {
            Log.i("Location","没有获取到GPS信息");
        }
    }


    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(serviceConnection);
        mSearch.destroy();
        mapView.onDestroy();
        mapView=null;

    }

    @Override
    protected void onStart() {
        super.onStart();
        baiduMap.setMyLocationEnabled(true);//开启定位图层
    }

    @Override
    protected void onStop() {
        super.onStop();
        baiduMap.setMyLocationEnabled(false);//停止定位图层
    }

    private void setTargetLocation(LatLng target,double x2,double y2){
        Log.d("单片机位置","更新");
        Log.d("dpj经度",new String(String.valueOf(x2)));
        Log.d("dpj纬度",new String(String.valueOf(y2)));

        target = new LatLng(y2, x2);//定义目标坐标点
        LatLng ll=new LatLng(target.latitude,target.longitude);//获取用户当前经纬度
        CoordinateConverter converter  = new CoordinateConverter()
                .from(CoordinateConverter.CoordType.GPS)
                .coord(ll);

        //desLatLng 转换后的坐标
        LatLng desLatLng = converter.convert();

        BitmapDescriptor bitmap=BitmapDescriptorFactory
                .fromResource(R.mipmap.icon_target);//构建目标图标
        //构建MarkerOption,用于地图上添加Maker
        OverlayOptions option=new MarkerOptions()
                .position(desLatLng)
                .icon(bitmap);
        //在地图上添加Marker并显示
        baiduMap.addOverlay(option);
    }

    @Override
    public void setMessage(String message) {

        try {
            Log.d("收到消息","1111111111111");

            JSONObject jsonObject=new JSONObject(String.valueOf(message));
            x2= jsonObject.getDouble("经度");
            y2= jsonObject.getDouble("纬度");
            Log.d("经度2",new String(String.valueOf(x2)));
            Log.d("纬度2",new String(String.valueOf(y2)));
            baiduMap.clear();
            setTargetLocation(target, x2, y2);

        } catch (JSONException e) {

            e.printStackTrace();
        }

        mqttService = serviceConnection.getMqttService();
        mqttService.toCreateNotification(message);
    }
}
