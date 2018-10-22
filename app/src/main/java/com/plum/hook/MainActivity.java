package com.plum.hook;

import android.Manifest;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

//        requestPermission();
    }

    public void jump2(View view) {
        Intent intent = new Intent();
        intent.setComponent(new ComponentName("com.plum.plugin", "com.plum.plugin.SecondActivity"));
//        系统里面做了手脚   --》newIntent   msg--->obj-->intent
        startActivity(intent);
    }

    public void jump3(View view) {
        Intent intent = new Intent();
        intent.setComponent(new ComponentName("com.plum.plugin", "com.plum.plugin.ThreeActivity"));
        startActivity(intent);
    }

    public void jump4(View view) {
        Intent intent = new Intent();
        intent.setComponent(new ComponentName("com.plum.plugin", "com.plum.plugin.ThirdActivity"));
        startActivity(intent);
    }

    public void logout(View view) {
        //实例化
        SharedPreferences share = this.getSharedPreferences("david", MODE_PRIVATE);
        //使处于可编辑状态
        SharedPreferences.Editor editor = share.edit();
        //设置保存的数据
        editor.putBoolean("login", false);
        Toast.makeText(this, "退出登录成功", Toast.LENGTH_SHORT).show();
        editor.commit();    //提交数据保存
    }

    private void requestPermission() {
        String[] permissions = new String[]{Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE};
        ActivityCompat.requestPermissions(this, permissions, 0);
    }
}
