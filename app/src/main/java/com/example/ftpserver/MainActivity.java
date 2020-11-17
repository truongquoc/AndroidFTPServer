package com.example.ftpserver;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity implements ActivityCompat.OnRequestPermissionsResultCallback {
    public static final int SERVER_PORT = 3306;
    public static TextView infoMsg, userDisp, pwdDisp;
    public static TextInputEditText username;
    public static TextInputEditText password;
    private TextInputLayout userParent, pwdParent;
    private Switch togglePwd;
    String message;
    private static ArrayList<ServerSocketThread> serverSocketThreads = new ArrayList<>();
    private static ExecutorService pool = Executors.newFixedThreadPool(4);
    private static final int PERMISSION_REQUEST_CAMERA = 0;
    private static final int PERMISSION_REQUEST_NETWORK = 0;
    private static final int PERMISSION_REQUEST_WRITE_EXTERNAL_DISK = 0;
    private ServerSocket serverSocket;
    private ServerSocket dataServerSocket;
    private View mLayout;
    private boolean status = false;
    static final int PORT = 3306;
    Thread serverSocketThread;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        infoMsg = findViewById(R.id.msg);
        mLayout = findViewById(R.id.main_layout);
        username = findViewById(R.id.username);
        password = findViewById(R.id.password);
        userDisp = findViewById(R.id.usernameDisp);
        pwdDisp = findViewById(R.id.pwdDisp);
        userParent = findViewById(R.id.userParent);
        pwdParent = findViewById(R.id.pwdParent);
        togglePwd = findViewById(R.id.togglePwd);
        findViewById(R.id.submit).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                status = !status;
                if(status) {
                    username.setVisibility(View.INVISIBLE);
                    userParent.setVisibility(View.INVISIBLE);
                    pwdParent.setVisibility(View.INVISIBLE);
                    String txtUsername = String.valueOf(username.getText());
                    String txtPwd = String.valueOf(pwdDisp.getText());
                    if(txtUsername.isEmpty()) {
                        username.setText("ftp");
                        userDisp.setText("Username: ftp");
                    } else {
                        userDisp.setText(String.format("ftp: %s", username.getText()));
                    }
                    if(txtPwd.isEmpty()) {
                        password.setText("123456");
                    } else  {
                        pwdDisp.setText(password.getText());
                    }
                    StringBuilder stringBuilder = new StringBuilder("Password: ");
                    for(int i=0; i<password.getText().length(); i++) {
                        stringBuilder.append("*");
                    }
                    pwdDisp.setText(stringBuilder.toString());
                    userDisp.setVisibility(View.VISIBLE);
                    password.setVisibility(View.INVISIBLE);
                    pwdDisp.setVisibility(View.VISIBLE);

                    togglePwd.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                        @Override
                        public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                            if(b) {
                                pwdDisp.setText(String.format("Password: %s", password.getText()));
                            } else {
                                String pwd = "";
                                StringBuilder stringBuilder = new StringBuilder("Password: ");
                                for(int i=0; i< password.getText().length(); i++) {
                                    stringBuilder.append("*");
                                }
                                pwdDisp.setText(stringBuilder.toString());
                            }
                        }
                    });
                    Log.d("tag", "startServer");
                    ServerSocketThread.isRunning = true;
                    startServer();
                } else {
                    Log.d("tag", "stopServer");
                    ServerSocketThread.isRunning = false;
                  try {
                      ServerSocketThread.serverSocket.close();
                      ServerSocketThread.dataServerSocket.close();
                      ServerSocketThread.client.close();
                  } catch (Exception e) {
                      e.printStackTrace();
                  }
                }
            }
        });
        showExternalPreview();
    }

    void startServer()  {
            String txtUsername = String.valueOf(username.getText());
            String txtPassword = String.valueOf(password.getText());
            serverSocketThread = new Thread(new ServerSocketThread(txtUsername, txtPassword));
            serverSocketThread.start();
    }

    public static String getIPAddress(boolean useIPv4) {
        try {
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface intf : interfaces) {
                List<InetAddress> addrs = Collections.list(intf.getInetAddresses());
                for (InetAddress addr : addrs) {
                    if (!addr.isLoopbackAddress()) {
                        String sAddr = addr.getHostAddress();
                        //boolean isIPv4 = InetAddressUtils.isIPv4Address(sAddr);
                        boolean isIPv4 = sAddr.indexOf(':')<0;

                        if (useIPv4) {
                            if (isIPv4)
                                return sAddr;
                        } else {
                            if (!isIPv4) {
                                int delim = sAddr.indexOf('%'); // drop ip6 zone suffix
                                return delim<0 ? sAddr.toUpperCase() : sAddr.substring(0, delim).toUpperCase();
                            }
                        }
                    }
                }
            }
        } catch (Exception ignored) { } // for now eat exceptions
        return "";
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        // BEGIN_INCLUDE(onRequestPermissionsResult)
        if (requestCode == PERMISSION_REQUEST_NETWORK) {
            // Request for camera permission.
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission has been granted. Start camera preview Activity.
                Snackbar.make(mLayout, R.string.access_network_permission_granted,
                        Snackbar.LENGTH_SHORT)
                        .show();
            } else {
                // Permission request was denied.
                Snackbar.make(mLayout, R.string.access_network_permission_denied,
                        Snackbar.LENGTH_SHORT)
                        .show();
            }
        }
    }

    private void showExternalPreview() {
        //Check if the Network Permission has been granted
//        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED ) {
//            //Permission is already available start connect socket
//            Snackbar.make(mLayout,
//                    "Read external Disk is available",
//                    Snackbar.LENGTH_SHORT).show();
//        } else  {
//            requestNetworkPermission();
//        }

        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            Snackbar.make(mLayout,
                    "Write external Disk is available",
                    Snackbar.LENGTH_SHORT).show();
        } else {
            requestWriteExternal();
        }
    }

    private void requestWriteExternal() {
        if(ActivityCompat.shouldShowRequestPermissionRationale(this,  Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            Snackbar.make(mLayout, "Write to External Disk is required to access Your directory's phone",
                    Snackbar.LENGTH_INDEFINITE).setAction(R.string.ok, new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    // Request the permission
                    ActivityCompat.requestPermissions(MainActivity.this,
                            new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                            PERMISSION_REQUEST_WRITE_EXTERNAL_DISK);
                }
            }).show();
        } else {
            Snackbar.make(mLayout, "Access to Disk is unavailable", Snackbar.LENGTH_SHORT).show();
            // Request the permission. The result will be received in onRequestPermissionResult().
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_REQUEST_WRITE_EXTERNAL_DISK);
        }
    }
    private void requestNetworkPermission() {
        if(ActivityCompat.shouldShowRequestPermissionRationale(this,  Manifest.permission.READ_EXTERNAL_STORAGE)) {
            Snackbar.make(mLayout, R.string.network_access_required,
                    Snackbar.LENGTH_INDEFINITE).setAction(R.string.ok, new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    // Request the permission
                    ActivityCompat.requestPermissions(MainActivity.this,
                            new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                            PERMISSION_REQUEST_NETWORK);
                }
            }).show();
        } else {
            Snackbar.make(mLayout, R.string.access_network_permission_unavailable, Snackbar.LENGTH_SHORT).show();
            // Request the permission. The result will be received in onRequestPermissionResult().
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSION_REQUEST_NETWORK);
        }
    }


}

