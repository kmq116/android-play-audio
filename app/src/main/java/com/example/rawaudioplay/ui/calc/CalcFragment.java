package com.example.rawaudioplay.ui.calc;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import androidx.fragment.app.Fragment;

import com.example.rawaudioplay.R;

import java.text.DecimalFormat;

public class CalcFragment extends Fragment {
    private EditText principalInput;
    private EditText rateInput;
    private EditText yearsInput;
    private Spinner frequencySpinner;
    private TextView resultText;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_calc, container, false);
        
        // 初始化视图组件
        principalInput = view.findViewById(R.id.principal_input);
        rateInput = view.findViewById(R.id.rate_input);
        yearsInput = view.findViewById(R.id.years_input);
        frequencySpinner = view.findViewById(R.id.frequency_spinner);
        resultText = view.findViewById(R.id.result_text);
        
        // 设置计算按钮点击事件
        view.findViewById(R.id.calculate_button).setOnClickListener(v -> calculateCompoundInterest());
        
        return view;
    }

    private void calculateCompoundInterest() {
        try {
            // 获取输入值
            double principal = Double.parseDouble(principalInput.getText().toString());
            double annualRate = Double.parseDouble(rateInput.getText().toString()) / 100;
            int years = Integer.parseInt(yearsInput.getText().toString());
            
            // 获取复利频率（根据spinner位置映射到实际次数）
            int frequencyPosition = frequencySpinner.getSelectedItemPosition();
            int compoundTimes = getCompoundTimes(frequencyPosition);

            // 复利计算公式：A = P(1 + r/n)^(nt)
            double amount = principal * Math.pow(1 + (annualRate / compoundTimes), 
                compoundTimes * years);

            // 显示结果
            DecimalFormat df = new DecimalFormat("#,##0.00");
            resultText.setText("到期本息和：" + df.format(amount) + "元");
            
        } catch (NumberFormatException e) {
            resultText.setText("请输入有效的数字");
        }
    }

    // 根据spinner位置返回对应的复利次数
    private int getCompoundTimes(int position) {
        switch (position) {
            case 0: return 1;    // 年
            case 1: return 2;    // 半年
            case 2: return 4;    // 季度
            case 3: return 12;   // 月
            default: return 1;
        }
    }
}