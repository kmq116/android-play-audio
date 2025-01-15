package com.example.rawaudioplay.ui.vis;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.rawaudioplay.R;

public class VisFragment extends Fragment {

    private WebView webView;

    public class WebAppInterface {
        @JavascriptInterface
        public void onNodeClick(int nodeId) {
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> 
                    Toast.makeText(getActivity(), "点击了节点: " + nodeId, Toast.LENGTH_SHORT).show()
                );
            }
        }
    }

    public View onCreateView(@NonNull LayoutInflater inflater,
                            ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_vis, container, false);
        webView = root.findViewById(R.id.webView);
        
        // 配置WebView
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        
        // 添加JavaScript接口
        webView.addJavascriptInterface(new WebAppInterface(), "Android");
        
        // 从assets加载HTML文件
        webView.loadUrl("file:///android_asset/vis_network.html");
        
        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        webView = null;
    }
} 