package br.edu.denis.projeto_ifood;

import android.app.Dialog;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.OptionalPendingResult;

public class MapActivity extends AppCompatActivity {

    //constantes
    private static final String TAG = "noteMapActivity";
    private static final int ERRO_DIALOG_REQUeST = 9001;

    //variaveis aux
    String lat = "",lon = "",local_nome = "";

    //variaveis layout
    Button mapButton,menuButton,sugButton;
    EditText localEditText;

    //onCreate
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        //associa layouts
        mapButton = (Button)findViewById(R.id.mapButton);
        menuButton = (Button)findViewById(R.id.menuButton);
        localEditText = (EditText) findViewById(R.id.localEditText);
        sugButton = (Button)findViewById(R.id.sugButton);

        localEditText.setKeyListener(null);
        sugButton.setVisibility(View.INVISIBLE);


        //codigo
        if(isServiceOK()){
            init();
        }
    }

    //metodos
    private void init(){
        Log.d(TAG, "init: adicionando funcoes.");
        mapButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MapActivity.this,Map2Activity.class);
                startActivityForResult(intent, 501);//inicia com id 501
            }
        });

        menuButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MapActivity.this,MainActivity.class);
                startActivity(intent);
            }
        });

        sugButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MapActivity.this,SugestaoActivity.class);

                Bundle bundle =  new Bundle();
                bundle.putString("lat",lat);
                bundle.putString("lon",lon);
                bundle.putString("local_nome",local_nome);

                intent.putExtras(bundle);
                startActivity(intent);
            }
        });


    }

    public boolean isServiceOK(){
        Log.d(TAG,"isServiceOK: checando versao do google services.");
        int avaliable = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(MapActivity.this);
        if (avaliable == ConnectionResult.SUCCESS){
            //tudo ok, a versao esta correta
            Log.d(TAG,"isServiceOK: Google plays services esta funcionando.");
            return true;
        }
        else if(GoogleApiAvailability.getInstance().isUserResolvableError(avaliable)){
            //ocorreu um erro de versao
            Log.d(TAG,"isServiceOK: ocorreu um erro que pode ser arrumado.");
            Dialog dialog = GoogleApiAvailability.getInstance().getErrorDialog(MapActivity.this,avaliable, ERRO_DIALOG_REQUeST);
            dialog.show();
        }
        else{
            Toast.makeText(this,"Voce nao pode fazer requisicoes",Toast.LENGTH_SHORT).show();
        }

        return false;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);

        Bundle extras = data.getExtras();

        if(extras.getBoolean("att")) {
            local_nome = extras.getString("local_nome");
            localEditText.setText(local_nome);
            lat = extras.getString("lat");
            lon = extras.getString("lon");

            sugButton.setVisibility(View.VISIBLE);

        }

    }

}
