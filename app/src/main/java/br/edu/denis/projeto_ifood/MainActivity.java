package br.edu.denis.projeto_ifood;

import com.facebook.AccessToken;
import com.facebook.AccessTokenTracker;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.OptionalPendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.squareup.picasso.Picasso;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.MalformedURLException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

public class MainActivity extends AppCompatActivity implements GoogleApiClient.OnConnectionFailedListener {

    //constantes
    private static final String TAG = "noteMainActivity";

    //variaveis
    CallbackManager callbackManager;
    ProgressDialog mDialog;
    AccessTokenTracker at;
    SharedPreferences sp;

    //variaveis layout
    TextView emailTextView,aniversarioTextView,amigosTextView;
    ImageView avatarImageView;
    LoginButton loginButton;
    Button cidadeButton, googleLogOutButton;
    SignInButton googleButton;

    //variaveis google
    GoogleApiClient googleApiClient;

    //onresult
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if(requestCode == 777){
            GoogleSignInResult resultado = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
            handleSignInResult(resultado);
        }
        else {
            super.onActivityResult(requestCode, resultCode, data);
            callbackManager.onActivityResult(requestCode, resultCode, data);
        }

    }

    //oncreate
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);//associa esse .java com um layout .xml

        //associaçao layout
        emailTextView = (TextView) findViewById(R.id.emailTextView);
        aniversarioTextView = (TextView)findViewById(R.id.aniversarioTextView);
        amigosTextView = (TextView)findViewById(R.id.amigosTextView);
        avatarImageView = (ImageView) findViewById(R.id.avatarImageView);
        loginButton = (LoginButton) findViewById(R.id.loginButton);
        cidadeButton =  (Button) findViewById(R.id.cidadeButton);
        googleButton =  (SignInButton) findViewById(R.id.googleButton);
        googleLogOutButton = (Button) findViewById(R.id.googleLogOutButton);

        cidadeButton.setVisibility(View.INVISIBLE);
        googleLogOutButton.setVisibility(View.INVISIBLE);
        googleLogOutButton.setX(googleButton.getX());
        googleLogOutButton.setY(googleButton.getY());

        sp = getSharedPreferences("mPre",MODE_PRIVATE);

        init();
        emailTextView.setText("Acesse pelo Facebook ou Google!");

        callbackManager = CallbackManager.Factory.create();
        loginButton.setReadPermissions(Arrays.asList("public_profile","email","user_birthday","user_friends"));
        loginButton.registerCallback(callbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                mDialog = new ProgressDialog(MainActivity.this);
                mDialog.setMessage("Retrieving data...");
                mDialog.show();

                String accesstoken = loginResult.getAccessToken().getToken();

                GraphRequest request = GraphRequest.newMeRequest(loginResult.getAccessToken(), new GraphRequest.GraphJSONObjectCallback() {
                    @Override
                    public void onCompleted(JSONObject object, GraphResponse response) {
                        mDialog.dismiss();
                        Log.d(TAG , object.toString());
                        getFacebookData(object);
                        cidadeButton.setVisibility(View.VISIBLE);
                        googleButton.setVisibility(View.INVISIBLE);
                    }
                });


                Bundle parameters= new Bundle();
                parameters.putString("fields","name,id,email,birthday,friends");
                request.setParameters(parameters);
                request.executeAsync();
            }

            @Override
            public void onCancel() {

            }

            @Override
            public void onError(FacebookException error) {

            }
        });

        //se o usuario ja esta logado via facebook
        if(AccessToken.getCurrentAccessToken() != null){

            String nome = sp.getString("user","");
            emailTextView.setText("Bem vindo "+nome+"!");
            cidadeButton.setVisibility(View.VISIBLE);
            googleButton.setVisibility(View.INVISIBLE);
        }

        //se o usuario deslogar via facebook
        at = new AccessTokenTracker() {
            @Override
            protected void onCurrentAccessTokenChanged(AccessToken oldAccessToken,AccessToken currentAccessToken) {
                if (currentAccessToken == null) {
                    avatarImageView.setImageResource(R.drawable.ifoodlogo1);
                    Toast.makeText(MainActivity.this,"Desconectado com sucesso", Toast.LENGTH_SHORT).show();
                    emailTextView.setText("Acesse pelo Facebook ou Google!");
                    aniversarioTextView.setText("");
                    amigosTextView.setText("");
                    cidadeButton.setVisibility(View.INVISIBLE);
                    googleButton.setVisibility(View.VISIBLE);
                }
            }
        };

        printKeyHash();

        //----------------------------- google login --------------------------------

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .build();

        googleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this,this)
                .addApi(Auth.GOOGLE_SIGN_IN_API,gso)
                .build();
    }

    //metodos
    private void printKeyHash(){
        try{
            PackageInfo info = getPackageManager().getPackageInfo("br.edu.denis.projeto_ifood", PackageManager.GET_SIGNATURES);
            for(Signature sig:info.signatures){
                MessageDigest md = MessageDigest.getInstance("SHA");
                md.update(sig.toByteArray());
                Log.d(TAG , Base64.encodeToString(md.digest(),Base64.DEFAULT));//printa no console
            }

        }
        catch(PackageManager.NameNotFoundException e){
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }


    private void getFacebookData(JSONObject object) {
        try{
            /*
            URL profile_picture = new URL("https://graph.facebook.com/"+object.getString("id")+"/picture?width=250&height=250");
            Picasso.with(this).load(profile_picture.toString()).into(avatarImageView);
            emailTextView.setText(object.getString("email"));
            aniversarioTextView.setText(object.getString("birthday"));
            amigosTextView.setText(object.getJSONObject("friends").getJSONObject("summary").getString("total_count")+" amigos");
            */
            emailTextView.setText("Bem vindo "+object.getString("name")+" !");
            sp.edit().putString("user",object.getString("name")).commit();

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void init(){
        Log.d(TAG, "init: adicionando funcoes.");

        cidadeButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this,MapActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            }
        });

        googleButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                Intent intent = Auth.GoogleSignInApi.getSignInIntent(googleApiClient);
                startActivityForResult(intent,777);
            }
        });

    }

    private void handleSignInResult(GoogleSignInResult resultado){
        //logou via google
        if(resultado.isSuccess()){
            cidadeButton.setVisibility(View.VISIBLE);

            GoogleSignInAccount acc = resultado.getSignInAccount();
            emailTextView.setText("Bem vindo "+acc.getDisplayName()+"!");
            cidadeButton.setVisibility(View.VISIBLE);
            loginButton.setVisibility(View.INVISIBLE);
            googleButton.setVisibility(View.INVISIBLE);
            googleLogOutButton.setVisibility(View.VISIBLE);
        }
        else{
            //Toast.makeText(this,"Ocorreu algum erro", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }


    @Override
    protected void onStart() {


        OptionalPendingResult<GoogleSignInResult> opr =  Auth.GoogleSignInApi.silentSignIn(googleApiClient);

        //se usuario ja estiver logado pelo google...
        if(opr.isDone()){
            GoogleSignInResult resultado = opr.get();
            handleSignInResult(resultado);
        }
        else{
            opr.setResultCallback(new ResultCallback<GoogleSignInResult>() {
                @Override
                public void onResult(@NonNull GoogleSignInResult googleSignInResult) {
                    handleSignInResult(googleSignInResult);
                }
            });
        }
        super.onStart();
    }

    public void logOutGoogle(View view){
        Auth.GoogleSignInApi.signOut(googleApiClient).setResultCallback(new ResultCallback<Status>() {
            @Override
            public void onResult(@NonNull Status status) {
                if(status.isSuccess()){
                    Toast.makeText(MainActivity.this,"Desconectado com sucesso", Toast.LENGTH_SHORT).show();
                    emailTextView.setText("Acesse pelo Facebook ou Google!");
                    cidadeButton.setVisibility(View.INVISIBLE);
                    loginButton.setVisibility(View.VISIBLE);
                    googleButton.setVisibility(View.VISIBLE);
                    googleLogOutButton.setVisibility(View.INVISIBLE);
                }
            }
        });
    }
}
