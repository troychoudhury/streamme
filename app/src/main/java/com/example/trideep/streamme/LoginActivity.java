package com.example.trideep.streamme;

import android.app.ProgressDialog;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.common.ConnectionResult;

import org.json.JSONException;

import java.util.Arrays;

public class LoginActivity extends AppCompatActivity implements View.OnClickListener, OnConnectionFailedListener{

    private static final String EMPTY_STRING = "";
    private static final String TAG = "LoginActivity";
    private static final int GG_RC_SIGN_IN = 0; // Request code to invoke sign in user interactions.
    private int mClickedButtonId = 0;
    private ProgressDialog mProgressDialog;
    // Facebook
    private CallbackManager mFbCallbackManager;
    private LoginManager mFbLoginManager;
    // Google
    private GoogleApiClient mGgGoogleApiClient;

    //
    // Activity overrides
    //
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        initialize();
        fbInitialize();
        ggInitialize();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);

        switch(mClickedButtonId) {
            case R.id.fb_login_button : {
                mFbCallbackManager.onActivityResult(requestCode, resultCode, data);
                break;
            }
            case R.id.gg_login_button : {
                if (requestCode == GG_RC_SIGN_IN) {
                    GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
                    ggHandleSignInResult(result);
                }
                break;
            }
        }
    }

    //
    // OnClickListener implementation
    //
    @Override
    public void onClick(View v) {
        mClickedButtonId = v.getId();

        switch(v.getId()) {
            case R.id.fb_login_button : {
                mFbLoginManager.logInWithReadPermissions(
                        this,
                        Arrays.asList("email", "public_profile", "user_friends"));
                break;
            }

            case R.id.gg_login_button : {
                Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGgGoogleApiClient);
                startActivityForResult(signInIntent, GG_RC_SIGN_IN);
                break;
            }
        }

        showProgressDialog();
    }

    //
    // onConnectionFailedListener implementation
    //
    @Override
    public void onConnectionFailed (ConnectionResult result) {
        // do nothing
    }

    //
    // Private methods
    //
    private void initialize()
    {
        mClickedButtonId = 0;
        setButtonOnClickListeners();
        setLoginStatus(EMPTY_STRING);
    }

    private void setButtonOnClickListeners() {
        findViewById(R.id.fb_login_button).setOnClickListener(this);
        findViewById(R.id.gg_login_button).setOnClickListener(this);
    }

    private void setLoginStatus(String status, boolean isSignInSuccessful) {
        hideProgressDialog();
        TextView loginStatus = (TextView)findViewById(R.id.login_status);
        loginStatus.setText(status.toCharArray(), 0, status.length());
        if(isSignInSuccessful) {
            startActivity();
        }
    }


    private void showProgressDialog() {
        if (mProgressDialog == null) {
            mProgressDialog = new ProgressDialog(this);
            mProgressDialog.setMessage(getString(R.string.loading));
            mProgressDialog.setIndeterminate(true);
        }
        mProgressDialog.show();
    }

    private void hideProgressDialog() {
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.hide();
        }
    }

    // Facebook
    private void fbInitialize()
    {
        FacebookSdk.sdkInitialize(getApplicationContext());
        mFbLoginManager = LoginManager.getInstance();
        mFbCallbackManager = CallbackManager.Factory.create();
        mFbLoginManager.registerCallback(
                mFbCallbackManager,
                new FacebookCallback<LoginResult>() {
                    @Override
                    public void onSuccess(LoginResult loginResult) {
                        fbHandleSignInResult(loginResult);
                    }

                    @Override
                    public void onCancel() {
                        setLoginStatus(EMPTY_STRING);
                    }

                    @Override
                    public void onError(FacebookException exception) {
                        String status = String.format(getString(R.string.sign_in_error_fmt), exception.getLocalizedMessage());
                        setLoginStatus(status);
                    }
                });
    }

    private void fbHandleSignInResult(LoginResult loginResult)
    {
        GraphRequest request = GraphRequest.newMeRequest(loginResult.getAccessToken(), null);
        Bundle parameters = new Bundle();
        parameters.putString("fields", "id,name,email");
        request.setParameters(parameters);
        GraphResponse response = request.executeAndWait();
        String email;
        try {
            email = response.getJSONObject().getString("email");
        } catch (JSONException e) {
            email = EMPTY_STRING;
        }
        String status = String.format(getString(R.string.signed_in_fmt), email);
        setLoginStatus(status);
    }

    // Google
    private void ggInitialize()
    {
        // Configure sign-in to request the user's ID, email address, and basic
        // profile. ID and basic profile are included in DEFAULT_SIGN_IN.
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .build();
        // Build a GoogleApiClient with access to the Google Sign-In API and the
        // options specified by gso.
        mGgGoogleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this /* FragmentActivity */, this /* OnConnectionFailedListener */)
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .build();
    }

    private void ggHandleSignInResult(GoogleSignInResult result) {
        Log.d(TAG, "ggHandleSignInResult:" + result.isSuccess());
        if (result.isSuccess()) {
            GoogleSignInAccount acct = result.getSignInAccount();
            String userId = acct.getDisplayName();
            String status = String.format(getString(R.string.signed_in_fmt), userId);
            setLoginStatus(status);
        } else {
            String status  = String.format(getString(R.string.sign_in_error_fmt), result.getStatus().toString());
            setLoginStatus(status);
        }
    }
}
