package br.edu.denis.projeto_ifood;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.AutocompletePrediction;
import com.google.android.gms.location.places.GeoDataClient;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.PlaceBuffer;
import com.google.android.gms.location.places.PlaceBufferResponse;
import com.google.android.gms.location.places.PlaceDetectionClient;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import br.edu.denis.models.PlaceInfo;
import br.edu.denis.util.PlaceAutoCompleteAdapter;

public class Map2Activity extends AppCompatActivity implements OnMapReadyCallback, GoogleApiClient.OnConnectionFailedListener {

    //constantes
    private static final String TAG = "noteMap2Activity";
    private static final String FINE_LOCATION = android.Manifest.permission.ACCESS_FINE_LOCATION;
    private static final String COURSE_LOCATION = android.Manifest.permission.ACCESS_COARSE_LOCATION;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1501;
    private static final float DEFAULT_ZOOM = 15f;
    private  static final LatLngBounds LAT_LNG_BOUNDS = new LatLngBounds(
            new LatLng(-40,-168), new LatLng(71,136)
    );
    private static final String MINHA_LOCALIACAO_ATUAL = "Minha localização atual";
    //variaveis
    private boolean mLocationPermissionsGranted = false;
    private GoogleMap mMap;
    private FusedLocationProviderClient mFusedLocationProviderClient;
    private ArrayList<String> historicoPesquisas = new ArrayList<String>();
    private PlaceAutoCompleteAdapter mPlaceAutoCompleteAdapter;
    private GoogleApiClient mGoogleApiClient;
    private GeoDataClient mGeoDataClient;
    private PlaceDetectionClient mPlaceDetectionClient;

    //variaveis aux
    String localDefinido = "";
    String latDefinida = "";
    String lonDefinida = "";
    boolean buscaFoiRealizada = false;

    //variaveis layout
    AutoCompleteTextView buscaEditText;
    ImageView gpsImageView,clearImageView;

    //oncreate
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map2);

        //associacao layout
        buscaEditText = (AutoCompleteTextView) findViewById(R.id.buscaEditText) ;
        gpsImageView = (ImageView) findViewById(R.id.gpsImageView);
        clearImageView = (ImageView) findViewById(R.id.clearImageView);

        //codigo
        registerReceiver(mGpsSwitchStateReceiver, new IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION));
        getLocationPermission();
    }

    //metodo
    private void getDeviceLocation() {
        Log.d(TAG, " getDeviceLocation: pegando a localizacao atual do usuario.");
        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        try {
            if (mLocationPermissionsGranted && checkIfLocationOpened()) {
                Task location = mFusedLocationProviderClient.getLastLocation();
                location.addOnCompleteListener(new OnCompleteListener() {
                    @Override
                    public void onComplete(@NonNull Task task) {
                        if (task.isSuccessful()) {
                            Log.d(TAG, " getDeviceLocation: localizacao atual identificada.");
                            Location currentLocation = (Location) task.getResult();

                            definirNovaLocalização(MINHA_LOCALIACAO_ATUAL,currentLocation.getLatitude(),currentLocation.getLongitude());
                            moveCamera(new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude()), DEFAULT_ZOOM,"Minha localização atual");

                        } else {
                            Log.d(TAG, " getDeviceLocation: localizacao atual nao pode ser identificada.");
                            Toast.makeText(Map2Activity.this, "Nao foi possivel identificar a localizacao atual", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }

        } catch (SecurityException e) {
            Log.d(TAG, " getDeviceLocation: SecurityException: " + e.getMessage());
        }
    }

    private void moveCamera(LatLng ll, float zoom, String title) {
        Log.d(TAG, " moveCamera: movendo a camera para: " + ll.latitude + "," + ll.longitude);

        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(ll, zoom));

        //nao vai colocar varrios amrcadores no mesmo lugar e nem na minha localziaçao atual
        if (/*!historicoPesquisas.contains(title) &&*/ (title != MINHA_LOCALIACAO_ATUAL)) {
            //historicoPesquisas.add(title);
            Log.d(TAG, " moveCamera: nova pesquisa foi feita");
            MarkerOptions options = new MarkerOptions().position(ll).title(title);

            mMap.clear();
            mMap.addMarker(options);
        }

        Toast.makeText(Map2Activity.this, "Nova localização definida", Toast.LENGTH_SHORT).show();
        hideSoftKeyboard();

    }

    private void initMap() {
        Log.d(TAG, "initMap: iniciando mapa.");
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);

        mapFragment.getMapAsync(Map2Activity.this);
    }

    private void getLocationPermission() {

        Log.d(TAG, "getLocationPermission: solcitando permissoes.");
        String[] permissions = {android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION};

        //verifica se as duas permissoes necessarias foram recebidas, pode iniciar o mapa
        if (ContextCompat.checkSelfPermission(this.getApplicationContext(), FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            if (ContextCompat.checkSelfPermission(this.getApplicationContext(), COURSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                mLocationPermissionsGranted = true;//permissoes recebidas, nao precisa solicita-las
                Log.d(TAG, "onRequestPermissionsResult: permissoes ja foram concedidas.");
                    initMap();
            }
        }

        if (!mLocationPermissionsGranted) {
            //se nao foram recebidas faz a solicitacao delas
            ActivityCompat.requestPermissions(this, permissions, LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    //aqui é processada a resposta da requisicao de permissoes feita
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        Log.d(TAG, "onRequestPermissionsResult: processando resultados.");
        mLocationPermissionsGranted = false;

        switch (requestCode) {
            case LOCATION_PERMISSION_REQUEST_CODE: {
                if (grantResults.length > 0) {

                    for (int i = 0; i < grantResults.length; i++) {
                        if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                            //uma das permissoes nao foi recebida
                            Log.d(TAG, "onRequestPermissionsResult: permissoes falharam.");
                            return;
                        }
                    }
                    //toadas as permissoes foram recebidas, pode iniciar o mapa
                    Log.d(TAG, "onRequestPermissionsResult: permissoes concedidas.");
                    mLocationPermissionsGranted = true;
                    initMap();
                }
            }
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        Toast.makeText(this, "Selecione uma localização", Toast.LENGTH_SHORT).show();
        Log.d(TAG, "onMapReady: mapa esta pronto.");
        mMap = googleMap;

        if (mLocationPermissionsGranted) {
            getDeviceLocation();
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            mMap.setMyLocationEnabled(true);//coloca uma marca na sua posicao e um botao apara ser redircionado para ela. O botao nao pode ser reposicionado.
            mMap.setMapType(1);
            mMap.getUiSettings().setMyLocationButtonEnabled(false);//desabilita o botao para nao bloquear a futura barra de pesquisa

            init();
        }

    }

    public void init(){
        Log.d(TAG, "init: adicionando funcoes.");
        buscaEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent keyEvent) {
                if((actionId == EditorInfo.IME_ACTION_SEARCH) ||
                        (actionId == EditorInfo.IME_ACTION_SEARCH) ||
                        (keyEvent.getAction() == KeyEvent.ACTION_DOWN) ||
                        (keyEvent.getAction() == KeyEvent.KEYCODE_ENTER)){
                    //se uma dessas acoes for executadas o metodo de busca abaixo sera ativado
                    geoLocate();
                }

                return false;
            }
        });

        gpsImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "onClick: clicou no icone de gps.");
                getDeviceLocation();
            }
        });

        clearImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "onClick: clicou no icone de limpar.");
                buscaEditText.setText("", TextView.BufferType.EDITABLE);
            }
        });


        hideSoftKeyboard();

        //auto suggestion google places
        //isso nao funcionou
        /*
        mGoogleApiClient = new GoogleApiClient
                .Builder(this)
                .addApi(Places.GEO_DATA_API)
                .addApi(Places.PLACE_DETECTION_API)
                .enableAutoManage(this,this)
                .build();
        */

        //isso arruma o problema do geoData
        mGeoDataClient = Places.getGeoDataClient(this,null);
        mPlaceDetectionClient = Places.getPlaceDetectionClient(this,null);

        mPlaceAutoCompleteAdapter = new PlaceAutoCompleteAdapter(
                this,mGeoDataClient,LAT_LNG_BOUNDS ,null);

        buscaEditText.setAdapter(mPlaceAutoCompleteAdapter);
        buscaEditText.setOnItemClickListener(mAutoCompleteClickListener);

    }



    public void geoLocate(){
        Log.d(TAG, "geoLocate: realziando busca.");

        String buscaSring = buscaEditText.getText().toString();
        Geocoder geocoder = new Geocoder((Map2Activity.this));
        List<Address> resultados = new ArrayList();

        try {
            resultados = geocoder.getFromLocationName(buscaSring, 1);//para essa busca somente 1 resultado é esperado
        }catch (IOException e){
            Log.d(TAG, "geoLocate: IOException: "+e.getMessage());
        }

        if(resultados.size() > 0){//se houve resultados
            Address lugar = resultados.get(0);
            Log.d(TAG, "geoLocate: endereco localizado: "+lugar.toString());

            definirNovaLocalização(lugar.getAddressLine(0),lugar.getLatitude(),lugar.getLongitude());
            moveCamera(new LatLng(lugar.getLatitude(),lugar.getLongitude()),DEFAULT_ZOOM, lugar.getAddressLine(0));
        }
        else{
            Toast.makeText(Map2Activity.this, "Nenhum resultado", Toast.LENGTH_SHORT).show();
        }
    }

    //sempre que chamar esse metodo o teclado era ocultado
    private void hideSoftKeyboard(){
        //metood original entrou em conflito com o google places
        //this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        //outro metodo... nao entra em conflito com o google places
        InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(buscaEditText.getWindowToken(),0);
    }


    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    public void onBackPressed() {

        Intent intent = new Intent();

        intent.putExtra("att",buscaFoiRealizada);
        intent.putExtra("local_nome", localDefinido);
        intent.putExtra("lat", latDefinida);
        intent.putExtra("lon", lonDefinida);
        setResult(501, intent);

        super.onBackPressed();

    }


    //------------------------------ google places ----------------------------------
    //execeto a implementaçao de sugestoes tudo acima é somente relativo a google maps

    //variaveis
    private PlaceInfo mPlace;

    private AdapterView.OnItemClickListener mAutoCompleteClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            hideSoftKeyboard();

            final AutocompletePrediction item = mPlaceAutoCompleteAdapter.getItem(position);
            final String placeId = item.getPlaceId();

            //deu errado porque nao usei geoDataApi e sim geoDataClient
            /*
            PendingResult<PlaceBuffer> placeResult = Places.GeoDataApi.getPlaceById(mGoogleApiClient,placeId);
            placeResult.setResultCallback(mUpdatePlaceDetailCallback);
            */

            Task<PlaceBufferResponse> placeResult = mGeoDataClient.getPlaceById(placeId);
            placeResult.addOnCompleteListener(mUpdatePlaceDetailsCallback);
        }
    };

    private OnCompleteListener<PlaceBufferResponse> mUpdatePlaceDetailsCallback = new OnCompleteListener<PlaceBufferResponse>() {
        @Override
        public void onComplete(@NonNull Task<PlaceBufferResponse> task) {
            if(task.isSuccessful()){
                PlaceBufferResponse places = task.getResult();

                final Place place = places.get(0);
                try{
                    mPlace = new PlaceInfo();
                    mPlace.setName(place.getName().toString());
                    mPlace.setAddress(place.getAddress().toString());
                    mPlace.setAttributions(place.getAttributions().toString());
                    mPlace.setId(place.getId());
                    mPlace.setLatlng(place.getLatLng());
                    mPlace.setRating(place.getRating());
                    mPlace.setPhoneNumber(place.getPhoneNumber().toString());
                    mPlace.setWebsiteUri(place.getWebsiteUri().toString());

                    Log.e(TAG,"mUpdatePlaceDetailsCallback: "+ mPlace.toString());
                }
                catch(NullPointerException e){
                    Log.e(TAG,"mUpdatePlaceDetailsCallback: "+e.getMessage());
                };

                definirNovaLocalização(place.getName().toString(),
                        place.getViewport().getCenter().latitude,
                        place.getViewport().getCenter().longitude);

                moveCamera(new LatLng(
                        place.getViewport().getCenter().latitude,
                        place.getViewport().getCenter().longitude),
                        DEFAULT_ZOOM,
                        mPlace.getName());

                places.release();//libera o buffer de memoria


            }else{
                Log.e(TAG,"mUpdatePlaceDetailsCallback: lugar nao encontrado.");
            }
        }
    };


    public void definirNovaLocalização(String local_nome, Double lat,Double lon){
        Log.d(TAG,"definirNovaLocalização: nova localizaçao definida.");

        localDefinido = local_nome;
        latDefinida = Double.toString(lat);
        lonDefinida = Double.toString(lon);
        buscaFoiRealizada = true;
    }

    //------------------------------------ verificando gps ---------------------------------

    private boolean checkIfLocationOpened() {
        final LocationManager manager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        if (manager.isProviderEnabled(LocationManager.GPS_PROVIDER) || manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)){
            return true;
        }
        buildAlertMessageNoGps();
        return false;
    }

    private void buildAlertMessageNoGps() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Seu GPS é necessario para prosseguir. Gostaria de ativa-lo?")
                .setCancelable(false)
                .setPositiveButton("Sim", new DialogInterface.OnClickListener() {
                    public void onClick(@SuppressWarnings("unused") final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
                        startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                    }
                })
                .setNegativeButton("Não", new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
                        dialog.cancel();

                        onBackPressed();
                    }
                });
        final AlertDialog alert = builder.create();
        alert.show();
    }


    private BroadcastReceiver mGpsSwitchStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            if (intent.getAction().matches("android.location.PROVIDERS_CHANGED")) {//houve mudanca
                checkIfLocationOpened();
            }
        }
    };


}
