package com.example.PetIssue;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

import static com.example.PetIssue.Util.INTENT_PATH;
import static com.example.PetIssue.Util.showToast;

public class ProfileActivity extends AppCompatActivity implements View.OnClickListener{
    private static final String TAG = "ProfileActivity";

    //firebase auth object
    private FirebaseAuth firebaseAuth;

    //view objects
    private TextView textViewUserEmail;
    private Button buttonLogout;
    private TextView textviewDelete;
    private ImageView profileImageView;
    private Button checkButton;
    private Button pictureButton;
    private Button galleryButton;
    private String profilePath;
    private FirebaseUser user;
    private RelativeLayout buttonBackgroundLayout;
    private RelativeLayout loaderLayout;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        //initializing views
        textViewUserEmail = findViewById(R.id.textviewUserEmail);
        buttonLogout = findViewById(R.id.buttonLogout);
        textviewDelete = findViewById(R.id.textviewDelete);
        checkButton = findViewById(R.id.checkButton);
        galleryButton = findViewById(R.id.galleryButton);
        pictureButton = findViewById(R.id.pictureButton);
        profileImageView = findViewById(R.id.profileImageView);
        loaderLayout = findViewById(R.id.loaderLyaout);
        buttonBackgroundLayout = findViewById(R.id.buttonsBackgroundLayout);



        //initializing firebase authentication object
        firebaseAuth = FirebaseAuth.getInstance();


        //유저가 있다면, null이 아니면 계속 진행
        FirebaseUser user = firebaseAuth.getCurrentUser();

        //textViewUserEmail의 내용을 변경해 준다.
        textViewUserEmail.setText("반갑습니다.\n"+ user.getEmail()+"으로 로그인 하였습니다.");

        //logout button event
        buttonLogout.setOnClickListener(this);
        textviewDelete.setOnClickListener(this);


    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case 0: {
                if (resultCode == Activity.RESULT_OK) {
                    profilePath = data.getStringExtra(INTENT_PATH);
                    Glide.with(this).load(profilePath).centerCrop().override(500).into(profileImageView);
                    buttonBackgroundLayout.setVisibility(View.GONE);
                }
                break;
            }
        }
    }

    @Override
    public void onClick(View view) {
        if (view == pictureButton){
            startActivity(new Intent(Intent.ACTION_CAMERA_BUTTON));
        }
        if (view == galleryButton){
            startActivity(new Intent(Intent.CATEGORY_APP_GALLERY));
            startActivity(new Intent(this, GalleryActivity.class));
        }
        if (view == checkButton){
            storageUploader();
        }
        if (view == buttonBackgroundLayout){
            buttonBackgroundLayout.setVisibility(View.GONE);
        }
        if (view == profileImageView){
            buttonBackgroundLayout.setVisibility(View.VISIBLE);
        }





        if (view == buttonLogout) {
            Toast.makeText(ProfileActivity.this,"로그아웃 완료", Toast.LENGTH_LONG).show();
            firebaseAuth.signOut();
            finish();
            startActivity(new Intent(this, LoginActivity.class));
        }
        //회원탈퇴를 클릭하면 회원정보를 삭제한다. 삭제전에 컨펌창을 하나 띄워야 겠다.
        if(view == textviewDelete) {
            AlertDialog.Builder alert_confirm = new AlertDialog.Builder(ProfileActivity.this);
            alert_confirm.setMessage("정말 계정을 삭제 할까요?").setCancelable(false).setPositiveButton("확인", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                            user.delete()
                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            Toast.makeText(ProfileActivity.this, "계정이 삭제 되었습니다.", Toast.LENGTH_LONG).show();
                                            finish();
                                            startActivity(new Intent(getApplicationContext(), MainActivity.class));
                                        }
                                    });
                        }
                    }
            );
            alert_confirm.setNegativeButton("취소", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    Toast.makeText(ProfileActivity.this, "취소", Toast.LENGTH_LONG).show();
                }
            });
            alert_confirm.show();
        }

    }

    private void storageUploader() {
        final String name = ((EditText) findViewById(R.id.nameEditText)).getText().toString();
        final String phoneNumber = ((EditText) findViewById(R.id.phoneNumberEditText)).getText().toString();
        final String birthDay = ((EditText) findViewById(R.id.birthDayEditText)).getText().toString();
        final String address = ((EditText) findViewById(R.id.addressEditText)).getText().toString();

        if (name.length() > 0 && phoneNumber.length() > 9 && birthDay.length() > 5 && address.length() > 0) {
            loaderLayout.setVisibility(View.VISIBLE);
            FirebaseStorage storage = FirebaseStorage.getInstance();
            StorageReference storageRef = storage.getReference();
            user = FirebaseAuth.getInstance().getCurrentUser();
            final StorageReference mountainImagesRef = storageRef.child("users/" + user.getUid() + "/profileImage.jpg");

            if (profilePath == null) {
                UserInfo userInfo = new UserInfo(name, phoneNumber, birthDay, address);
                storeUploader(userInfo);
            } else {
                try {
                    InputStream stream = new FileInputStream(new File(profilePath));
                    UploadTask uploadTask = mountainImagesRef.putStream(stream);
                    uploadTask.continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
                        @Override
                        public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {
                            if (!task.isSuccessful()) {
                                throw task.getException();
                            }
                            return mountainImagesRef.getDownloadUrl();
                        }
                    }).addOnCompleteListener(new OnCompleteListener<Uri>() {
                        @Override
                        public void onComplete(@NonNull Task<Uri> task) {
                            if (task.isSuccessful()) {
                                Uri downloadUri = task.getResult();

                                UserInfo userInfo = new UserInfo(name, phoneNumber, birthDay, address, downloadUri.toString());
                                storeUploader(userInfo);
                            } else {
                                showToast(ProfileActivity.this, "회원정보를 보내는데 실패하였습니다.");
                            }
                        }
                    });
                } catch (FileNotFoundException e) {
                    Log.e("로그", "에러: " + e.toString());
                }
            }
        } else {
            showToast(ProfileActivity.this, "회원정보를 입력해주세요.");
        }
    }

    private void storeUploader(UserInfo userInfo) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("users").document(user.getUid()).set(userInfo)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        showToast(ProfileActivity.this, "회원정보 등록을 성공하였습니다.");
                        loaderLayout.setVisibility(View.GONE);
                        finish();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        showToast(ProfileActivity.this, "회원정보 등록에 실패하였습니다.");
                        loaderLayout.setVisibility(View.GONE);
                        Log.w(TAG, "Error writing document", e);
                    }
                });
    }

}