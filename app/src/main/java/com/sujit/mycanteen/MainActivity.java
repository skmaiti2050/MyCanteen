package com.sujit.mycanteen;

import android.content.Intent;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.facebook.accountkit.Account;
import com.facebook.accountkit.AccountKit;
import com.facebook.accountkit.AccountKitCallback;
import com.facebook.accountkit.AccountKitError;
import com.facebook.accountkit.AccountKitLoginResult;
import com.facebook.accountkit.ui.AccountKitActivity;
import com.facebook.accountkit.ui.AccountKitConfiguration;
import com.facebook.accountkit.ui.LoginType;
import com.rengwuxian.materialedittext.MaterialEditText;
import com.sujit.mycanteen.Model.CheckUserResponse;
import com.sujit.mycanteen.Model.User;
import com.sujit.mycanteen.Retrofit.IMyCanteenAPI;
import com.sujit.mycanteen.Utils.Common;
import com.szagurskii.patternedtextwatcher.PatternedTextWatcher;

import dmax.dialog.SpotsDialog;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_CODE = 1000;
    Button btn_continue;

    IMyCanteenAPI mService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mService = Common.getAPI();

        btn_continue = (Button) findViewById(R.id.btn_continue);
        btn_continue.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startLoginPage(LoginType.PHONE);
            }
        });
    }

    private void startLoginPage(LoginType loginType) {
        Intent intent =  new Intent(this, AccountKitActivity.class);
        AccountKitConfiguration.AccountKitConfigurationBuilder builder =
                new AccountKitConfiguration.AccountKitConfigurationBuilder(loginType,
                        AccountKitActivity.ResponseType.TOKEN);
        intent.putExtra(AccountKitActivity.ACCOUNT_KIT_ACTIVITY_CONFIGURATION,builder.build());
        startActivityForResult(intent,REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == REQUEST_CODE)
        {
            AccountKitLoginResult result = data.getParcelableExtra(AccountKitLoginResult.RESULT_KEY);
            
            if(result.getError() != null)
            {
                Toast.makeText(this, ""+result.getError().getErrorType().getMessage(), Toast.LENGTH_SHORT).show();
            }
            else if(result.wasCancelled())
            {
                Toast.makeText(this, "Cancelled", Toast.LENGTH_SHORT).show();
            }
            else
            {
                if(result.getAccessToken() != null)
                {
                    final android.app.AlertDialog alertDialog = new SpotsDialog.Builder()
                            .setContext(MainActivity.this)
                            .setTheme(R.style.Custom)
                            .setMessage("Please wait for a while...")
                            .setCancelable(false)
                            .build();
                    alertDialog.show();

                    AccountKit.getCurrentAccount(new AccountKitCallback<Account>() {
                        @Override
                        public void onSuccess(final Account account) {
                            mService.checkExistsUser(account.getPhoneNumber().toString())
                                    .enqueue(new Callback<CheckUserResponse>() {
                                        @Override
                                        public void onResponse(Call<CheckUserResponse> call, Response<CheckUserResponse> response) {
                                            CheckUserResponse userResponse = response.body();
                                            if(userResponse.isExists())
                                            {
                                                alertDialog.dismiss();
                                            }
                                            else
                                            {
                                                alertDialog.dismiss();

                                                showRegisterDialog(account.getPhoneNumber().toString());
                                            }
                                        }

                                        @Override
                                        public void onFailure(Call<CheckUserResponse> call, Throwable t) {
                                            alertDialog.dismiss();
                                        }
                                    });
                        }

                        @Override
                        public void onError(AccountKitError accountKitError) {
                            Log.d("ERROR",accountKitError.getErrorType().getMessage());
                        }
                    });
                }
            }
        }
    }

    private void showRegisterDialog(String phone) {

        final AlertDialog.Builder builder =  new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("REGISTER");

        LayoutInflater inflater = this.getLayoutInflater();
        View register_layout = inflater.inflate(R.layout.register_layout,null);

        final MaterialEditText name = (MaterialEditText)register_layout.findViewById(R.id.name);
        final MaterialEditText address = (MaterialEditText)register_layout.findViewById(R.id.address);
        final MaterialEditText birthday = (MaterialEditText)register_layout.findViewById(R.id.birthday);

        Button btn_register = (Button)register_layout.findViewById(R.id.btn_register);
        birthday.addTextChangedListener(new PatternedTextWatcher("####-##-##"));
        builder.setView(register_layout);
        final AlertDialog dialog = builder.create();
        btn_register.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                dialog.dismiss();

                if(TextUtils.isEmpty(name.getText().toString()));
                {
                    Toast.makeText(MainActivity.this, "Please enter your name!", Toast.LENGTH_SHORT).show();
                }

                if(TextUtils.isEmpty(address.getText().toString()));
                {
                    Toast.makeText(MainActivity.this, "Please enter your address!", Toast.LENGTH_SHORT).show();
                }

                if(TextUtils.isEmpty(birthday.getText().toString()));
                {
                    Toast.makeText(MainActivity.this, "Please enter your birthdate!", Toast.LENGTH_SHORT).show();
                }
            }
        });

        final android.app.AlertDialog waitdialog = new SpotsDialog.Builder()
                .setContext(MainActivity.this)
                .setTheme(R.style.Custom)
                .setMessage("Saving information in database...")
                .setCancelable(false)
                .build();

        waitdialog.show();

        mService.registerNewUser(phone, name.getText().toString(),
                address.getText().toString(),
                birthday.getText().toString())
                .enqueue(new Callback<User>() {
                    @Override
                    public void onResponse(Call<User> call, Response<User> response) {
                        waitdialog.dismiss();
                        User user = response.body();
                        if(TextUtils.isEmpty(user.getError_msg()))
                        {
                            Toast.makeText(MainActivity.this, "Registration Successful!", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<User> call, Throwable t) {
                        waitdialog.dismiss();
                    }
                });
        dialog.show();
    }
}
