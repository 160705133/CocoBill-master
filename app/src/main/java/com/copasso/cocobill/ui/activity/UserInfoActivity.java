package com.copasso.cocobill.ui.activity;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.afollestad.materialdialogs.GravityEnum;
import com.afollestad.materialdialogs.MaterialDialog;
import com.bumptech.glide.Glide;
import com.bumptech.glide.util.Util;
import com.copasso.cocobill.R;
import com.copasso.cocobill.base.BaseMVPActivity;
import com.copasso.cocobill.model.bean.remote.MyUser;
import com.copasso.cocobill.presenter.UserInfoPresenter;
import com.copasso.cocobill.presenter.contract.UserInfoContract;
import com.copasso.cocobill.utils.ImageUtils;
import com.copasso.cocobill.utils.ProgressUtils;
import com.copasso.cocobill.utils.SnackbarUtils;
import com.copasso.cocobill.utils.StringUtils;
import com.copasso.cocobill.utils.ToastUtils;
import com.copasso.cocobill.widget.CommonItemLayout;

import java.io.File;
import java.net.URL;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import cn.bmob.v3.BmobUser;
import cn.bmob.v3.datatype.BmobFile;
import cn.bmob.v3.exception.BmobException;
import cn.bmob.v3.listener.UpdateListener;
import cn.bmob.v3.listener.UploadFileListener;

/**
 * 用户中心activity
 */
public class UserInfoActivity extends BaseMVPActivity<UserInfoContract.Presenter>
        implements UserInfoContract.View, View.OnClickListener {

    private Toolbar toolbar;
    private RelativeLayout iconRL;
//    private ImageView iconIv;
    private CommonItemLayout usernameCL;
    private CommonItemLayout sexCL;
    private CommonItemLayout phoneCL;
    private CommonItemLayout emailCL;

//    protected static final int CHOOSE_PICTURE = 0;
//    protected static final int TAKE_PICTURE = 1;
//    protected static final int CROP_SMALL_PICTURE = 2;
//    //图片路径
    protected static Uri tempUri = null;

    private MyUser currentUser;

    /***********************************************************************/
    @Override
    protected int getLayoutId() {
        return R.layout.activity_user_info;
    }

    @Override
    protected void initData(Bundle savedInstanceState) {
        super.initData(savedInstanceState);
        currentUser= BmobUser.getCurrentUser(MyUser.class);
    }

    @Override
    protected void initWidget() {
        super.initWidget();
        toolbar = findViewById(R.id.toolbar);
//        iconRL = findViewById(R.id.rlt_update_icon);
//        iconIv = findViewById(R.id.img_icon);
        usernameCL = findViewById(R.id.cil_username);
        sexCL = findViewById(R.id.cil_sex);
        phoneCL = findViewById(R.id.cil_phone);
        emailCL = findViewById(R.id.cil_email);

        //初始化Toolbar
        toolbar.setTitle("账户");
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v->{
            //返回消息更新上个Activity数据
            setResult(RESULT_OK, new Intent());
            finish();
        });

//        //加载当前头像
//        Glide.with(mContext).load(currentUser.getImage()).into(iconIv);
        //添加用户信息
        usernameCL.setRightText(currentUser.getUsername());
        sexCL.setRightText(currentUser.getGender());
        phoneCL.setRightText(currentUser.getMobilePhoneNumber());
        emailCL.setRightText(currentUser.getEmail());
    }

    @Override
    protected void initClick() {
        super.initClick();
//        iconRL.setOnClickListener(this);
        usernameCL.setOnClickListener(this);
        sexCL.setOnClickListener(this);
        phoneCL.setOnClickListener(this);
        emailCL.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.cil_username:  //用户名
                SnackbarUtils.show(mContext, "江湖人行不更名，坐不改姓！");
                break;
            case R.id.cil_sex:  //性别
                showGenderDialog();
                break;
            case R.id.cil_phone:  //电话修改
                showPhoneDialog();
                break;
            case R.id.cil_email:  //邮箱修改
                showMailDialog();
                break;
            default:
                break;
        }
    }


    /**
     * 显示选择性别对话框
     */
    public void showGenderDialog() {

        new MaterialDialog.Builder(mContext)
                .title("选择性别")
                .titleGravity(GravityEnum.CENTER)
                .items(new String[]{"男", "女"})
                .positiveText("确定")
                .negativeText("取消")
                .itemsCallbackSingleChoice(0, (dialog, itemView, which, text) -> {
                    currentUser.setGender(text.toString());
                    doUpdate();
                    sexCL.setRightText(currentUser.getGender());
                    dialog.dismiss();
                    return false;
                }).show();

    }

    /**
     * 显示更换电话对话框
     */
    public void showPhoneDialog() {
        String phone = currentUser.getMobilePhoneNumber();
        new MaterialDialog.Builder(mContext)
                .title("电话")
                .inputType(InputType.TYPE_CLASS_TEXT)
                .inputRangeRes(0, 200, R.color.textRed)
                .input("请输入电话号码", phone, (dialog, input) -> {
                    String inputStr=input.toString();
                    if (inputStr.equals("")) {
                        ToastUtils.show(mContext,"内容不能为空！" + input);
                    } else {
                        if (StringUtils.checkPhoneNumber(inputStr)) {
                            currentUser.setMobilePhoneNumber(inputStr);
                            phoneCL.setRightText(inputStr);
                            doUpdate();
                        } else {
                            Toast.makeText(UserInfoActivity.this,
                                    "请输入正确的电话号码", Toast.LENGTH_LONG).show();
                        }
                    }
                })
                .positiveText("确定")
                .show();
    }

    /**
     * 显示更换邮箱对话框
     */
    public void showMailDialog() {
        String email = currentUser.getMobilePhoneNumber();
        new MaterialDialog.Builder(mContext)
                .title("邮箱")
                .inputType(InputType.TYPE_CLASS_TEXT)
                .inputRangeRes(0, 200, R.color.textRed)
                .input("请输入邮箱地址", email, (dialog, input) -> {
                    String inputStr=input.toString();
                    if (inputStr.equals("")) {
                        ToastUtils.show(mContext,"内容不能为空！" + input);
                    } else {
                        if (StringUtils.checkEmail(inputStr)) {
                            currentUser.setEmail(inputStr);
                            emailCL.setRightText(inputStr);
                            doUpdate();
                        } else {
                            Toast.makeText(UserInfoActivity.this,
                                    "请输入正确的邮箱格式", Toast.LENGTH_LONG).show();
                        }
                    }
                })
                .positiveText("确定")
                .show();
    }

    /**
     * 更新用户数据
     */
    public void doUpdate() {
        if (currentUser == null)
            return;
        ProgressUtils.show(UserInfoActivity.this, "正在修改...");
        mPresenter.updateUser(currentUser);

    }

    /**
     * 权限请求
     *
     * @param requestCode
     * @param permissions
     * @param grantResults
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

        } else {
            // 没有获取 到权限，从新请求，或者关闭app
            Toast.makeText(this, "需要存储权限", Toast.LENGTH_SHORT).show();
        }
    }


    /***********************************************************************/
    @Override
    protected UserInfoContract.Presenter bindPresenter() {
        return new UserInfoPresenter();
    }

    @Override
    public void onSuccess() {
        ProgressUtils.dismiss();
        currentUser=BmobUser.getCurrentUser(MyUser.class);
    }

    @Override
    public void onFailure(Throwable e) {
        ProgressUtils.dismiss();
        SnackbarUtils.show(mContext, e.getMessage());
    }
}
