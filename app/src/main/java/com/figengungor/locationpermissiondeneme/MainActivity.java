package com.figengungor.locationpermissiondeneme;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;

public class MainActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {


    RelativeLayout container;
    TextView latitudeTV;
    TextView longitudeTV;
    Button locationButton;

    GoogleApiClient mGoogleApiClient; //Lokasyon API'sini kullanarak konum bilgisi çekeceğiz.
    LocationManager mLocationManager; //Cihazda lokasyon açık mı kapalı mı onu öğreneceğiz.
    private static final int ACCESS_FINE_LOCATION_PERMISSIONS_REQUEST = 1; //Lokasyona erişim izni alırken kullanacağız.
    private static final int LOCATION_SETTINGS_REQUEST = 2; //Ayarlar'a yönlendirip konum servisini açtırmak için kullanacağız.

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        container = (RelativeLayout) findViewById(R.id.container);
        latitudeTV = (TextView) findViewById(R.id.latitude);
        longitudeTV = (TextView) findViewById(R.id.longitude);
        locationButton = (Button) findViewById(R.id.locationButton);

        // Lokasyon API'sini ve callbackleri ekleyerek GoogleApiClient'ımızı oluşturuyoruz.
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        locationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED ) {
                    if(mGoogleApiClient.isConnected()) {
                        Location lastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);

                        if (lastLocation != null) {
                            double lat = lastLocation.getLatitude(), lon = lastLocation.getLongitude();
                            latitudeTV.setText(lat + "");
                            longitudeTV.setText(lon + "");
                            Toast.makeText(MainActivity.this, "lat: " + lat + " lon: " + lon, Toast.LENGTH_LONG).show();
                        }
                    }
                    else{
                        mGoogleApiClient.connect();
                    }

                }
                else {
                    checkLocationProvider();
                }
            }
        });

    }


    public void checkLocationProvider() {
        mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        //Gps ve Ag Saglayici kullan ayarları açık mı degil mi kontrol ediyoruz.
        // Hicbiri acik degilse, diyalogumuzu olusturuyoruz.
        if (!mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                && !mLocationManager
                .isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            AlertDialog.Builder dialog = new AlertDialog.Builder(this);
            dialog.setMessage(getString(R.string.open_location_settings));
            dialog.setTitle(getString(R.string.location_services_not_enabled));
            // İptal
            dialog.setPositiveButton(getString(R.string.cancel),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.cancel();
                        }
                    });
            // Konum servisleri ayar sayfasına yonlendiriyoruz.
            dialog.setNegativeButton(getString(R.string.OK),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            Intent myIntent = new Intent(
                                    Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                            startActivityForResult(myIntent, LOCATION_SETTINGS_REQUEST);
                            return;
                        }
                    });
            dialog.show();
        } else {
            getPermissionToAccessUserLocation();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {


        if (resultCode == RESULT_OK) {
            if (requestCode == LOCATION_SETTINGS_REQUEST) {
                getPermissionToAccessUserLocation();
            }
        }

        super.onActivityResult(requestCode, resultCode, data);
    }


    private void getPermissionToAccessUserLocation() {

        //Marshmallow'dan önceki sürümleride desteklemek için ContextCompat ve ActivityCompat
        //ile kontrol işlemlerini ve izin isteğinde bulunmayı gerçekleştiriyoruz.

        //Konum izninin verilip verilmediğini kontrol ediyoruz.
        //Konum izni verilmedi ise ActivityCompat.requestPermissions ile izni talep ediyoruz.
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            //ACCESS_FINE_LOCATION_PERMISSIONS_REQUEST integer kodumuz ile ACCESS_FINE_LOCATION konum izni talebinde
            //bulunuyoruz. Karşımıza izin isteyen bir diyalog çıkacak. onRequestPermissionsResult
            //methodu içinde verdiğiniz cevaba göre işlemler  yapılacaktır.
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    ACCESS_FINE_LOCATION_PERMISSIONS_REQUEST);

        }
        //Konum izni verildi ise, oluşturmuş olduğumuz GoogleApiClient objesine bağlanıyoruz.
        else {
            mGoogleApiClient.connect();

        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults) {
        if (requestCode == ACCESS_FINE_LOCATION_PERMISSIONS_REQUEST) {
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                mGoogleApiClient.connect(); //izni kapmışız, bağlanabilirz.
            } else if (grantResults[0] == PackageManager.PERMISSION_DENIED) {
                //izni alamamışız =( , kullanıcıyı neden bu izne ihtiyacımız olduğuna ikna etmemiz gerekir. Ama çokta ısrarcı olmamalıyız.
                //Bu durumda devreye PermissionRationale giriyor. shouldShowRequestPermissionRationale methodu mantıklı bir açıklama gösterilip gösterilmeyeceğine
                //dair boolean bir değer dönüyor.Eğer kullanıcı izin diyalogunda "Bir daha gösterme" seçeneğini seçip, reddederse, showRationale boolean değerimiz false dönecektir.
                //Ve bir daha gösterme seçeneğini seçtiği için artık izin alma diyalogu da çıkmayacaktır. Bunun için kullanıcının uygulama ayarlarına gidip, izinler kısmında lokasyon iznini aktif etmesi gerekir.
                boolean showRationale = ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.ACCESS_FINE_LOCATION);
                //Yukarıda bahsettiğimiz durum gerçekleştiği zaman, bir Snackbar ile kullanıcıyı uygulama ayarları sayfasına yönlendiriyoruz.
                if (!showRationale) {
                    Snackbar snackbar = Snackbar.make(container, "Konuma erişim izni vermek için Ayarlar sayfasına gidin.", Snackbar.LENGTH_LONG);
                    snackbar.setAction("Ayarlar", new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Intent intent = new Intent();
                            intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                            Uri uri = Uri.fromParts("package", getPackageName(), null);
                            intent.setData(uri);
                            startActivity(intent);
                        }
                    });
                    snackbar.show();
                }
                // Eğer showRationale true dönerse , kullanıcıyı ikna etmek üzere neden bu izne ihtiyacımız olduğunu açıkladığımız bir dialog oluşturuyoruz.
                // Eğer ikna olursa izin diyalogumuzu bir daha gösteriyoruz.İkna olmazsa yapacak bişi yok, bu izin ile yapılacak işlemi gerçekleştirmiyoruz.
                else {
                    showRationale();
                }
            }
        }
    }

    public void showRationale() {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
                this);

        // set title
        alertDialogBuilder.setTitle("");

        // set dialog message
        alertDialogBuilder
                .setMessage("Konumunuzu tespit etmek için konum bilgilerine erişime izin verin.")
                .setCancelable(false)
                .setPositiveButton("Devam Et", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // Request the permission
                        ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                ACCESS_FINE_LOCATION_PERMISSIONS_REQUEST);
                    }
                })
                .setNegativeButton("Şimdi Değil", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // if this button is clicked, just close
                        // the dialog box and do nothing
                        dialog.cancel();
                    }
                });

        // create alert dialog
        AlertDialog alertDialog = alertDialogBuilder.create();

        // show it
        alertDialog.show();
    }


    @Override
    public void onConnected(Bundle bundle) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            Location lastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);

            if (lastLocation != null) {
                double lat = lastLocation.getLatitude(), lon = lastLocation.getLongitude();
                latitudeTV.setText(lat+"");
                longitudeTV.setText(lon + "");
                Toast.makeText(this, "lat: " + lat + " lon: " + lon, Toast.LENGTH_LONG).show();
            }

        }
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }

    @Override
    protected void onStart() {
        super.onStart();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mGoogleApiClient.connect();
        }
    }

    @Override
    protected void onStop() {
        if (mGoogleApiClient.isConnected() || mGoogleApiClient.isConnecting())
            mGoogleApiClient.disconnect();
        super.onStop();
    }
}
