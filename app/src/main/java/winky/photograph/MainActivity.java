package winky.photograph;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.google.android.cameraview.AspectRatio;
import com.google.android.cameraview.CameraView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import winky.photograph.dialog.AspectRatioFragment;
import winky.photograph.dialog.ConfirmationDialogFragment;
import winky.photograph.dialog.VerificationDialog;

public class MainActivity extends AppCompatActivity implements AspectRatioFragment.Listener {

    private static final String FRAGMENT_DIALOG = "fragment_dialog";
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("_yyyyMMdd_HHmmss", Locale.getDefault());
    private static final String[] permissions = new String[]{
            Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_PHONE_STATE};
    private static final int REQUEST_CAMERA_PERMISSION = 1;
    private static final int REQUEST_CUT = 2;
    private CameraView cameraView;
    private Handler handler;
    private int currentFlash;
    private String picturePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).getAbsolutePath();
    private File tempFile;
    private static final int[] FLASH_OPTIONS = {
            CameraView.FLASH_AUTO,
            CameraView.FLASH_OFF,
            CameraView.FLASH_ON,
    };

    private static final int[] FLASH_ICONS = {
            R.drawable.ic_flash_auto,
            R.drawable.ic_flash_off,
            R.drawable.ic_flash_on,
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        cameraView = findViewById(R.id.camera);
        if (cameraView != null) {
            cameraView.addCallback(callback);
        }
        findViewById(R.id.to_picture).setOnClickListener(clickListener);
        findViewById(R.id.take_picture).setOnClickListener(clickListener);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowTitleEnabled(false);
        }
        File file = new File(picturePath, "Camera");
        if (file.isDirectory()) {
            picturePath = file.getAbsolutePath();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            cameraView.start();
        } else if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.CAMERA)) {
            ConfirmationDialogFragment.newInstance(R.string.camera_permission_confirmation, permissions,
                    REQUEST_CAMERA_PERMISSION,
                    R.string.camera_permission_not_granted)
                    .show(getSupportFragmentManager(), FRAGMENT_DIALOG);
        } else {
            ActivityCompat.requestPermissions(this, permissions, REQUEST_CAMERA_PERMISSION);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, R.string.camera_permission_not_granted, Toast.LENGTH_SHORT).show();
            }
            if (grantResults[1] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, R.string.sdcard_write_permission_not_granted, Toast.LENGTH_SHORT).show();
            }
            if (grantResults[2] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, R.string.read_device_permission_not_granted, Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onPause() {
        cameraView.stop();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (handler != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                handler.getLooper().quitSafely();
            } else {
                handler.getLooper().quit();
            }
            handler = null;
        }
    }

    private Handler getBackgroundHandler() {
        if (handler == null) {
            HandlerThread thread = new HandlerThread("background");
            thread.start();
            handler = new Handler(thread.getLooper());
        }
        return handler;
    }

    private View.OnClickListener clickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.take_picture:
                    if (cameraView != null) {
                        cameraView.takePicture();
                    }
                    break;
                case R.id.to_picture:
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("content://media/internal/images/media"));
                    startActivity(intent);
                    break;
            }
        }
    };

    private CameraView.Callback callback = new CameraView.Callback() {

        @Override
        public void onCameraOpened(CameraView cameraView) {
        }

        @Override
        public void onCameraClosed(CameraView cameraView) {
        }

        @Override
        public void onPictureTaken(CameraView cameraView, final byte[] data) {
            Toast.makeText(cameraView.getContext(), "保存成功", Toast.LENGTH_SHORT).show();
            getBackgroundHandler().post(new Runnable() {
                @Override
                public void run() {
                    String fileName = "IMG" + dateFormat.format(new Date()) + ".jpg";
                    final File file = new File(picturePath, fileName);
                    OutputStream os = null;
                    try {
                        os = new FileOutputStream(file);
                        os.write(data);
                        os.close();
                        sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(file)));
                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        if (os != null) {
                            try {
                                os.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    cropRawPhoto(file);
                                }
                            });
                        }
                    }
                }
            });
        }

    };

    public void cropRawPhoto(File file) {
        Intent intent = new Intent("com.android.camera.action.CROP");
        Uri imageUri;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            //添加这一句表示对目标应用临时授权该Uri所代表的文件
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            //通过FileProvider创建一个content类型的Uri
            imageUri = FileProvider.getUriForFile(this, "winky.photograph.fileprovider", file);
        } else {
            imageUri = Uri.fromFile(file);
        }
        intent.setDataAndType(imageUri, "image/*");
        intent.putExtra("crop", "true");
        //设置宽高比例
//        intent.putExtra("aspectX", 1);
//        intent.putExtra("aspectY", 1);
        //设置裁剪图片宽高
//        intent.putExtra("outputX", 300);
//        intent.putExtra("outputY", 300);
        intent.putExtra("scale", true);
        String fileName = "IMG_CROP" + dateFormat.format(new Date()) + ".jpg";
        intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(tempFile = new File(picturePath, fileName)));
        intent.putExtra("outputFormat", Bitmap.CompressFormat.JPEG.toString());
        intent.putExtra("noFaceDetection", true);
        startActivityForResult(intent, REQUEST_CUT);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
//        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CUT && resultCode == Activity.RESULT_OK) {
            sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(tempFile)));
            tempFile = null;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
//            case R.id.aspect_ratio:
//                FragmentManager fragmentManager = getSupportFragmentManager();
//                if (cameraView != null
//                        && fragmentManager.findFragmentByTag(FRAGMENT_DIALOG) == null) {
//                    final Set<AspectRatio> ratios = cameraView.getSupportedAspectRatios();
//                    final AspectRatio currentRatio = cameraView.getAspectRatio();
//                    AspectRatioFragment.newInstance(ratios, currentRatio)
//                            .show(fragmentManager, FRAGMENT_DIALOG);
//                }
//                return true;
            case R.id.switch_flash:
//                if (cameraView != null) {
//                    currentFlash = (currentFlash + 1) % FLASH_OPTIONS.length;
//                    item.setIcon(FLASH_ICONS[currentFlash]);
//                    cameraView.setFlash(FLASH_OPTIONS[currentFlash]);
//                }
                VerificationDialog.newInstance().show(getSupportFragmentManager(), "device_dialog");
                return true;
            case R.id.switch_camera:
                if (cameraView != null) {
                    int facing = cameraView.getFacing();
                    cameraView.setFacing(facing == CameraView.FACING_FRONT ? CameraView.FACING_BACK : CameraView.FACING_FRONT);
                }
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onAspectRatioSelected(@NonNull AspectRatio ratio) {
        if (cameraView != null) {
            Toast.makeText(this, ratio.toString(), Toast.LENGTH_SHORT).show();
            cameraView.setAspectRatio(ratio);
        }
    }
}
