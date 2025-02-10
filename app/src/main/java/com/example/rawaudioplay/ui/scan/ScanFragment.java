package com.example.rawaudioplay.ui.scan;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.rawaudioplay.R;
import com.journeyapps.barcodescanner.DecoratedBarcodeView;
import com.journeyapps.barcodescanner.BarcodeCallback;
import com.journeyapps.barcodescanner.BarcodeResult;
import com.google.zxing.ResultPoint;

import java.util.List;

public class ScanFragment extends Fragment {
    private static final int CAMERA_PERMISSION_REQUEST = 100;
    private DecoratedBarcodeView barcodeView;

    public View onCreateView(@NonNull LayoutInflater inflater,
                            ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_scan, container, false);

        barcodeView = root.findViewById(R.id.barcode_scanner);
        
        if (checkCameraPermission()) {
            startScanning();
        } else {
            requestCameraPermission();
        }

        return root;
    }

    private boolean checkCameraPermission() {
        return ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void requestCameraPermission() {
        ActivityCompat.requestPermissions(requireActivity(),
                new String[]{Manifest.permission.CAMERA},
                CAMERA_PERMISSION_REQUEST);
    }

    private void startScanning() {
        barcodeView.decodeContinuous(new BarcodeCallback() {

            @Override
            public void barcodeResult(BarcodeResult result) {
                if (result.getText() != null) {
                    barcodeView.pause();
                    String scanResult = result.getText();
                    Log.d("ScanFragment", "扫描成功，结果: " + scanResult);
                    
                    // 显示更明显的成功提示
                    Toast.makeText(requireContext(), "扫描成功!\n结果: " + scanResult, 
                            Toast.LENGTH_LONG).show();
                    
                    // 可以在这里添加震动反馈
                    try {
                        android.os.Vibrator vibrator = (android.os.Vibrator) requireContext().getSystemService(android.content.Context.VIBRATOR_SERVICE);
                        if (vibrator != null && vibrator.hasVibrator()) {
                            vibrator.vibrate(200); // 震动200毫秒
                        }
                    } catch (Exception e) {
                        Log.e("ScanFragment", "震动反馈失败", e);
                    }
                    
                    // 3秒后恢复扫描
                    barcodeView.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            barcodeView.resume();
                        }
                    }, 3000);
                } else {
                    Log.w("ScanFragment", "扫描结果为空");
                }
            }

            @Override
            public void possibleResultPoints(List<ResultPoint> resultPoints) {
                // 添加扫描过程的日志
                if (resultPoints != null && !resultPoints.isEmpty()) {
                    Log.v("ScanFragment", "检测到可能的扫描点: " + resultPoints.size());
                }
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                         @NonNull int[] grantResults) {
        if (requestCode == CAMERA_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startScanning();
            } else {
                Toast.makeText(requireContext(), "需要相机权限才能扫描二维码", 
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        barcodeView.resume();
    }

    @Override
    public void onPause() {
        super.onPause();
        barcodeView.pause();
    }
} 