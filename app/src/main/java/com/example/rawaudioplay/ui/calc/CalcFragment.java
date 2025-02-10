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
    private EditText monthlyInput;
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
        monthlyInput = view.findViewById(R.id.monthly_input);
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
            double monthlyAddition = monthlyInput.getText().toString().isEmpty() ? 0 : 
                Double.parseDouble(monthlyInput.getText().toString());
            
            // 修改计算参数
            double dailyRate = annualRate / 365; // 使用实际年天数
            int totalDays = years * 365;
            double currentPrincipal = principal;

            // 修改计息逻辑
            for (int day = 0; day < totalDays; day++) {
                // 每月定投处理保持不变
                if (day % 30 == 0 && day != 0) {
                    currentPrincipal += monthlyAddition;
                }
                
                // 每日复利计算（利息立即加入本金）
                currentPrincipal *= (1 + dailyRate); // 替换原来的计息方式
            }

            // 计算总投入（调整计算方式）
            int totalMonths = years * 12;
            double totalInvestment = principal + (monthlyAddition * totalMonths);
            
            // 显示结果（格式调整）
            DecimalFormat df = new DecimalFormat("#,##0.00");
            String result = "最终本息和：" + df.format(currentPrincipal) + "元\n"
                          + "总投入本金：" + df.format(totalInvestment) + "元\n"
                          + "累计利息：" + df.format(currentPrincipal - totalInvestment) + "元\n"
                          + "（按日计息，每日复利）";
            resultText.setText(result);
            
        } catch (NumberFormatException e) {
            resultText.setText("请输入有效的数字");
        }
    }
}