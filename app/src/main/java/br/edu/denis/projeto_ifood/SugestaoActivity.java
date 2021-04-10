package br.edu.denis.projeto_ifood;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class SugestaoActivity extends AppCompatActivity {

    //constantes
    public static final String WEATHER_API_KEY = "8626f21a29b527db33bf2b6312dc58c6";
    private static final String TAG = "noteMainActivity";

    //variaveis layout
    ImageView sugImageView;
    TextView descricaoTextView,climaTextView;
    Button voltarButton;

    //oncreate
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sugestao);

        //associacao layout
        sugImageView = (ImageView) findViewById(R.id.sugImageView);
        descricaoTextView = (TextView) findViewById(R.id.descricaoTextView);
        climaTextView = (TextView) findViewById(R.id.climaTextView);
        voltarButton = (Button) findViewById(R.id.voltarButton);

        //codigo
        init();

        Intent intent = getIntent();
        Bundle extras = intent.getExtras();

        Toast.makeText(SugestaoActivity.this, "Processando clima do local", Toast.LENGTH_SHORT).show();

        achaClima(extras);

    }

    //metodos
    public void montaInterface(){

        if(climaTextView.getText().toString().toLowerCase().contains("rain")){
            sugImageView.setImageResource(R.drawable.pizza1);
            descricaoTextView.setText("O clima está chuvoso! Que tal comer uma pizza hoje assistindo seu programa favorito?");
        }
        else{
            sugImageView.setImageResource(R.drawable.sorvete1);
            descricaoTextView.setText("Um belo sorvete é a recomendação de hoje!");
        }

    }

    public void achaClima(final Bundle extras){
        String lat = extras.getString("lat");
        String lon = extras.getString("lon");

        final String url = "https://api.openweathermap.org/data/2.5/weather?lat="+lat+"&lon="+lon+"&appid="+WEATHER_API_KEY;

        Log.d(TAG,"achaClima: "+url);

        JsonObjectRequest jor = new JsonObjectRequest(Request.Method.GET, url, null, new Response.Listener<JSONObject>()  {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    Log.d(TAG,"achaClima: clima encontrado");
                    JSONArray array = response.getJSONArray("weather");
                    JSONObject object = array.getJSONObject(0);
                    String main = object.getString("main");
                    climaTextView.setText("O clima em \""+ extras.getString("local_nome") +"\" está \""+main+"\"!");
                    montaInterface();

                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

            }
        }
        );
        RequestQueue queue = Volley.newRequestQueue(this);
        queue.add(jor);
    }

    public void init(){
        Log.d(TAG, "init: adicionando funcoes.");

        voltarButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                //Intent intent = new Intent(SugestaoActivity.this,MapActivity.class);
                //startActivity(intent);
                finish();
            }
        });

    }
}
