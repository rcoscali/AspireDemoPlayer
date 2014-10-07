package com.nagravision.aspiredemoplayer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.nagravision.aspiredemoplayer.drm.DrmAgent;

import android.app.Activity;
import android.content.Intent;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings.Secure;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;
import android.provider.Settings.Secure;

public class MainActivity extends Activity {
	private Button mLogin;
	private EditText mUsername;
	private EditText mPassword;
	private TextView mUserNameLabel;
	private TextView mPasswordLabel;
	private TextView mLoginDirectiveLabel;
	private static final String TAG = "MainPlayer";
	private DrmAgent mDrmAgent;

	@Override
	protected void onCreate(Bundle xBundle) {
		super.onCreate(xBundle);
		
		DrmAgent.gCreateInstance(this);
		mDrmAgent = DrmAgent.getInstance();
		String android_id = Secure.getString(MainActivity.this.getContentResolver(), Secure.ANDROID_ID); 		
		mDrmAgent.setAndroidId(android_id);		
		mDrmAgent.setDatabasePath(this.getExternalFilesDir(null).getAbsolutePath());
		
	    setContentView(R.layout.login);
	    mLogin = (Button) findViewById(R.id.login_button);
		mLogin.setOnClickListener((android.view.View.OnClickListener) mLogInListener);
		mUserNameLabel = (TextView) findViewById(R.id.userNameLabel);
		mPasswordLabel = (TextView) findViewById(R.id.passwordLabel);
		mLoginDirectiveLabel = (TextView) findViewById(R.id.loginDirectiveLabel);
		mUsername = (EditText) findViewById(R.id.userName);
		mPassword = (EditText) findViewById(R.id.password);
		mPassword.setOnEditorActionListener(mEnterPwdListener);
		//copyAssets();
	}

	@Override
	protected void onResume() {
		super.onResume();
		setWidgetVisibility(View.VISIBLE);
		if(mDrmAgent.isRegistered()){
			Intent intent = new Intent(MainActivity.this, Content.class);
			startActivity(intent);
			
		}
	}
	
	@Override
	protected void  onPause(){
		  super. onPause();
		  setWidgetVisibility(View.INVISIBLE);
		  finish();
	  }

	private void setWidgetVisibility(int xVisibility) {

		if(mLogin != null){
			mLogin.setVisibility(xVisibility);
			mUsername.setVisibility(xVisibility);
			mPassword.setVisibility(xVisibility);
			mUserNameLabel.setVisibility(xVisibility);
			mPasswordLabel.setVisibility(xVisibility);
			mLoginDirectiveLabel.setVisibility(xVisibility);
		}
	}
	
	private void doLogin()
	{
		mDrmAgent.setUserName(mUsername.getText().toString());
		mDrmAgent.setPassword(mPassword.getText().toString());

		if (mDrmAgent.loginSuccedded() && mDrmAgent.registerDevice()) {
			Intent intent = new Intent(MainActivity.this, Content.class);
			startActivity(intent);
		} else {
			Toast.makeText(MainActivity.this,
					"Incorrect login credentials", Toast.LENGTH_LONG)
					.show();
		}		
	}

	private OnClickListener mLogInListener = new OnClickListener() {
		public void onClick(View v) {
			doLogin();
		}
	};
	
	private OnEditorActionListener mEnterPwdListener = new TextView.OnEditorActionListener() {
		
		@Override
		public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
			if (actionId == EditorInfo.IME_NULL && event != null)
				doLogin();
			return false;
		}
	};

	// private void copyAssets() {
	// 	AssetManager assetManager = getAssets();
	// 	String[] files = null;
	// 	try {
	// 		files = assetManager.list("");
	// 	} catch (IOException e) {
	// 		Log.e("tag", "Failed to get asset file list.", e);
	// 	}
	// 	for (String filename : files) {
	// 		InputStream in = null;
	// 		OutputStream out = null;
	// 		try {
	// 			if (!filename.startsWith("images")
	// 					&& !filename.startsWith("sounds")
	// 					&& !filename.startsWith("webkit")) {
	// 				in = assetManager.open(filename);
	// 				File outFile = new File("/data/drm/contents/" + filename);
	// 				if (!outFile.exists()) {
	// 					out = new FileOutputStream(outFile);
	// 					copyFile(in, out);
	// 					in.close();
	// 					in = null;
	// 					out.flush();
	// 					out.close();
	// 					out = null;
	// 					Log.d(TAG, "Copy asset file to sdcard:" + filename);
	// 				}
	// 			}
	// 		} catch (IOException e) {
	// 			Log.d(TAG, "Failed to copy asset file: " + filename, e);
	// 		}
	// 	}
	// }

	// private void copyFile(InputStream in, OutputStream out) throws IOException {
	// 	byte[] buffer = new byte[1024];
	// 	int read;
	// 	while ((read = in.read(buffer)) != -1) {
	// 		out.write(buffer, 0, read);
	// 	}
	// }
}
