package com.example.drhello.ui.writepost;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.GridLayoutManager;

import com.bumptech.glide.Glide;
import com.chaquo.python.PyObject;
import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;
import com.example.drhello.ShowDialogPython;
import com.example.drhello.medical.ChestActivity;
import com.example.drhello.ui.chats.StateOfUser;
import com.example.drhello.connectionnewtwork.CheckNetwork;
import com.example.drhello.firebaseinterface.MyCallbackUser;
import com.example.drhello.model.UserAccount;
import com.example.drhello.textclean.RequestPermissions;
import com.example.drhello.firebaseservice.FcmNotificationsSender;
import com.example.drhello.databinding.ActivityWritePostsBinding;
import com.example.drhello.model.Posts;
import com.example.drhello.ui.main.MainActivity;
import com.example.drhello.viewmodel.PostsViewModel;
import com.example.drhello.R;
import com.example.drhello.adapter.ImagePostsAdapter;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.ml.modeldownloader.CustomModel;
import com.google.firebase.ml.modeldownloader.CustomModelDownloadConditions;
import com.google.firebase.ml.modeldownloader.DownloadType;
import com.google.firebase.ml.modeldownloader.FirebaseModelDownloader;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import org.tensorflow.lite.Interpreter;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class WritePostsActivity extends AppCompatActivity {
    private final List<Bitmap> bitmaps = new ArrayList<>();
    private final List<String> uriImage = new ArrayList<>();
    private final List<Uri> uriImage2 = new ArrayList<>();
    private final List<byte[]> bytes = new ArrayList<>();
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private StorageReference storageReference;
    private Posts posts;
    private PostsViewModel postsViewModel;
    private static final String TAG = "Posts Activity";
    private ActivityWritePostsBinding activityWritePostsBinding;
    private RequestPermissions requestPermissions;
    private UserAccount userAccount;
    PyObject main_program;
    float prop;
    private ShowDialogPython showDialogPython;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_write_posts);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            getWindow().getDecorView().setSystemUiVisibility(getWindow().getDecorView().getSystemUiVisibility() | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        } else {
            getWindow().setStatusBarColor(Color.WHITE);
        }
        requestPermissions = new RequestPermissions(WritePostsActivity.this, WritePostsActivity.this);
        inti();

        AsyncTaskD asyncTaskDownload = new AsyncTaskD("post", "first");
        asyncTaskDownload.execute();

        activityWritePostsBinding = DataBindingUtil.setContentView(this, R.layout.activity_write_posts);
        FirebaseMessaging.getInstance().subscribeToTopic("all");

        readData(new MyCallbackUser() {
            @Override
            public void onCallback(DocumentSnapshot documentSnapshot) {
                if (!documentSnapshot.exists()) {
                    FirebaseAuth.getInstance().getCurrentUser().delete();
                    showDialogPython.dismissDialog();
                } else {
                    userAccount = documentSnapshot.toObject(UserAccount.class);
                    showDialogPython.dismissDialog();
                    posts.setNameUser(userAccount.getName());
                    posts.setImageUser(userAccount.getImg_profile());
                    posts.setDate(getDateTime());
                    posts.setTokneId(userAccount.getTokenID());
                    Log.e("posts.UserMu ", posts.getImageUser());
                    activityWritePostsBinding.userAddress.setText(userAccount.getUserInformation().getCity());
                    activityWritePostsBinding.userName.setText(userAccount.getName());
                    try {
                        Glide.with(WritePostsActivity.this).load(userAccount.getImg_profile()).
                                placeholder(R.drawable.user).
                                error(R.drawable.user).into(activityWritePostsBinding.imageUser);
                    } catch (Exception e) {
                        activityWritePostsBinding.imageUser.setImageResource(R.drawable.user);
                    }

                    if (getIntent().getSerializableExtra("post") != null) {
                        posts = (Posts) getIntent().getSerializableExtra("post");
                        posts.setNameUser(userAccount.getName());
                        posts.setImageUser(userAccount.getImg_profile());
                        posts.setDate(getDateTime());
                        posts.setTokneId(userAccount.getTokenID());
                        activityWritePostsBinding.editPost.setText(posts.getWritePost());
                        activityWritePostsBinding.addImage.setVisibility(View.GONE);
                        //to upload post
                        activityWritePostsBinding.imgPost.setOnClickListener(v -> {
                            if (CheckNetwork.getConnectivityStatusString(WritePostsActivity.this) == 1) {
                                String post = activityWritePostsBinding.editPost.getText().toString().trim();
                                AsyncTaskD asyncTaskDownload = new AsyncTaskD(post, "");
                                asyncTaskDownload.execute();
                            } else {
                                Toast.makeText(WritePostsActivity.this, "Please, Check Internet", Toast.LENGTH_SHORT).show();
                            }

                        });
                    } else {
                        //to upload post
                        activityWritePostsBinding.imgPost.setVisibility(View.VISIBLE);
                        activityWritePostsBinding.imgPost.setOnClickListener(v -> {
                            if (CheckNetwork.getConnectivityStatusString(WritePostsActivity.this) == 1) {
                                String post = activityWritePostsBinding.editPost.getText().toString().trim();
                                AsyncTaskD asyncTaskDownload = new AsyncTaskD(post, "uploadImages");
                                asyncTaskDownload.execute();
                            } else {
                                Toast.makeText(WritePostsActivity.this, "Please, Check Internet", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                }
            }
        });

        activityWritePostsBinding.imgBackPost.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(WritePostsActivity.this, MainActivity.class);
                intent.putExtra("postsView", "postsView");
                startActivity(intent);
            }
        });

        activityWritePostsBinding.addImage.setOnClickListener(v -> {
            if (requestPermissions.permissionStorageRead()) {
                ActivityCompat.requestPermissions(WritePostsActivity.this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 100);
            } else {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                intent.setType("image/*");
                startActivityForResult(intent, 1);
            }
        });
    }

    public void readData(MyCallbackUser myCallback) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            showDialogPython = new ShowDialogPython(WritePostsActivity.this,WritePostsActivity.this.getLayoutInflater(),"upload");
            FirebaseFirestore.getInstance().collection("users")
                    .document(currentUser.getUid()).get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                @Override
                public void onSuccess(DocumentSnapshot documentSnapshot) {
                    myCallback.onCallback(documentSnapshot);
                }
            });
        }
    }

    private String getDateTime() {
        DateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy hh:mm:ss a", Locale.US);
        Date date = new Date();
        return dateFormat.format(date);
    }

    private void inti() {
        Toolbar toolbar = findViewById(R.id.toolbar_posts);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setTitle("");
        posts = new Posts();
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        storageReference = FirebaseStorage.getInstance().getReference(Objects.requireNonNull(mAuth.getCurrentUser()).getUid());
        postsViewModel = new PostsViewModel();
        postsViewModel = ViewModelProviders.of(this).get(PostsViewModel.class);

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1 && resultCode == RESULT_OK) {

            assert data != null;
            ClipData clipData = data.getClipData();
            if (clipData != null) {
                for (int i = 0; i < clipData.getItemCount(); i++) {
                    Uri imgUri = clipData.getItemAt(i).getUri();
                    Bitmap bitmap;
                    uriImage2.add(imgUri);
                    try {
                        //To save in FirebaseStorage
                        bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imgUri);
                        ByteArrayOutputStream bytesStream = new ByteArrayOutputStream();
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, bytesStream);
                        byte[] bytesOutImg = bytesStream.toByteArray();
                        bytes.add(bytesOutImg);

                        //To show in the same activity
                        InputStream is = getContentResolver().openInputStream(imgUri);
                        Bitmap bitmap_really = BitmapFactory.decodeStream(is);
                        bitmaps.add(bitmap_really);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            } else {
                Uri imgUri = data.getData();
                Bitmap bitmap;
                try {
                    //To save in FirebaseStorage
                    bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imgUri);
                    ByteArrayOutputStream bytesStream = new ByteArrayOutputStream();
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bytesStream);
                    byte[] bytesOutImg = bytesStream.toByteArray();
                    bytes.add(bytesOutImg);

                    //To show in the same activity
                    InputStream is = getContentResolver().openInputStream(imgUri);
                    Bitmap bitmap_really = BitmapFactory.decodeStream(is);

                    bitmaps.add(bitmap_really);

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            if (bitmaps.size() == 1) {
                activityWritePostsBinding.editPost.setHint("Say something about this photo...");
            } else if (bitmaps.size() > 1) {
                activityWritePostsBinding.editPost.setHint("Say something about these photos...");
            } else {
                activityWritePostsBinding.editPost.setHint("What’s on your mind?");
            }
            ImagePostsAdapter imagePostsAdapter = new ImagePostsAdapter(WritePostsActivity.this, bitmaps);
            GridLayoutManager recycleLayoutManager = new GridLayoutManager(this, 2, GridLayoutManager.VERTICAL, false);
            recycleLayoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
                @Override
                public int getSpanSize(int position) {
                    // grid items to take 1 column
                    if (bitmaps.size() % 2 == 0) {
                        return 1;
                    } else {
                        return (position == bitmaps.size() - 1) ? 2 : 1;
                    }
                }
            });
            activityWritePostsBinding.recycleImages.setLayoutManager(recycleLayoutManager);
            Objects.requireNonNull(activityWritePostsBinding.recycleImages.getLayoutManager()).scrollToPosition(imagePostsAdapter.getItemCount() - 1);
            activityWritePostsBinding.recycleImages.setAdapter(imagePostsAdapter);

        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        StateOfUser stateOfUser = new StateOfUser();
        stateOfUser.changeState("Online");
    }

    @Override
    protected void onPause() {
        super.onPause();
        StateOfUser stateOfUser = new StateOfUser();
        stateOfUser.changeState("Offline");
    }

    public class AsyncTaskD extends AsyncTask<String, String, String> {

        String text;
        String action;

        public AsyncTaskD(String text, String action) {
            this.text = text;
            this.action = action;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            if (!action.equals("first")) {
                showDialogPython = new ShowDialogPython(WritePostsActivity.this,WritePostsActivity.this.getLayoutInflater(),"upload");
            }
        }

        @Override
        protected String doInBackground(String... f_url) {
            if (action.equals("first")) {
                if (!Python.isStarted()) {
                    Python.start(new AndroidPlatform(WritePostsActivity.this));//error is here!
                }
                final Python py = Python.getInstance();
                main_program = py.getModule("prolog");
            } else {
                if (!text.isEmpty()) {
                    modelFire(text);
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(String file_url) {
            if (action.equals("first")) {
                Log.e("first ", " first");
            } else if (action.equals("uploadImages")) {
                if (prop >= 0.5) {
                    Log.e("prop failed: ", prop + "");
                    showDialogPython.dismissDialog();
                } else {
                    Log.e("prop good: ", prop + "");
                    Log.e("posts.getnameuser ", posts.getImageUser());
                    posts.setReactions(new HashMap<>());
                    posts.setWritePost(text);
                    posts.setUserId(mAuth.getUid());
                    postsViewModel.uploadImages(db, storageReference, bytes, uriImage, posts);
                    postsViewModel.isfinish.observe(WritePostsActivity.this, integer -> {
                        Log.d(TAG, "Image: " + integer + "  uriImage.size() : " + bytes.size());
                        if (integer == bytes.size()) {
                            Log.d(TAG, "uploadImage: " + integer);
                            Log.e("int image ", "fcm");
                            FcmNotificationsSender fcmNotificationsSender = new FcmNotificationsSender("/topics/all",
                                    mAuth.getCurrentUser().getUid(),
                                    "Post",
                                    posts.getNameUser() + " Upload a new post ",
                                    getApplicationContext(),
                                    WritePostsActivity.this,
                                    posts.getImageUser());
                            fcmNotificationsSender.SendNotifications();
                            showDialogPython.dismissDialog();
                            Intent intent = new Intent(WritePostsActivity.this, MainActivity.class);
                            intent.putExtra("postsView", "postsView");
                            startActivity(intent);
                        }
                    });
                }
            } else {
                if (prop >= 0.5) {
                    Log.e("prop failed: ", prop + "");
                } else {
                    posts.setWritePost(text);
                    posts.setDate(getDateTime());
                    db.collection("posts")
                            .document(posts.getPostId()).set(posts).addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if (task.isSuccessful()) {
                                FcmNotificationsSender fcmNotificationsSender = new FcmNotificationsSender("/topics/all",
                                        mAuth.getCurrentUser().getUid(),
                                        "Post",
                                        posts.getNameUser() + " Upload a new post ",
                                        getApplicationContext(),
                                        WritePostsActivity.this,
                                        posts.getImageUser());
                                fcmNotificationsSender.SendNotifications();
                                showDialogPython.dismissDialog();
                                Intent intent = new Intent(WritePostsActivity.this, MainActivity.class);
                                intent.putExtra("postsView", "postsView");
                                startActivity(intent);
                                Log.d(TAG, "onComplete: save uri ");

                            } else {
                                //Toast.makeText(WritePostsActivity.this, "error ", Toast.LENGTH_SHORT).show();
                            }

                        }
                    });
                }
            }

        }


    }

    private void modelFire(String text) {
        String result = main_program.callAttr("predictComment", text,getKeyboardLanguage(text)).toString();
        result = result.replace("[", "").replace("]", "");
        String[] strings = result.split(", ");
        Log.e("result: ", result);
        float[][] input = new float[1][300];
        for (int i = 0; i < strings.length; i++) {
            input[0][i] = Float.parseFloat(strings[i]);
        }
        CustomModelDownloadConditions conditions = new CustomModelDownloadConditions.Builder()
                .requireWifi()
                .build();
        Task<CustomModel> model;
        if(getKeyboardLanguage(text).equals("EN")){
            Log.e("lang : ",   "EN");
            model = FirebaseModelDownloader.getInstance()
                    .getModel("HateAbusiveModelEN", DownloadType.LOCAL_MODEL_UPDATE_IN_BACKGROUND, conditions);
        }else{
            Log.e("lang : ",   "AR");
            model = FirebaseModelDownloader.getInstance()
                    .getModel("arabicHateOff", DownloadType.LOCAL_MODEL_UPDATE_IN_BACKGROUND, conditions);
        }

        model.addOnSuccessListener(new OnSuccessListener<CustomModel>() {
                    @Override
                    public void onSuccess(CustomModel model) {
                        File modelFile = model.getFile();
                        Log.e("modelFile : ", modelFile + "");
                        if (modelFile != null) {
                            Interpreter interpreter = new Interpreter(modelFile);
                            int bufferSize = 1 * java.lang.Float.SIZE / java.lang.Byte.SIZE;
                            ByteBuffer modelOutput = ByteBuffer.allocateDirect(bufferSize).order(ByteOrder.nativeOrder());
                            interpreter.run(input, modelOutput);
                            modelOutput.rewind();
                            FloatBuffer probabilities = modelOutput.asFloatBuffer();
                            prop = probabilities.get(0);
                            Log.e("MAX : ", prop * 100 + "");
                        }
                    }
                });
    }

    public static String getKeyboardLanguage(String s) {
        for (int i = 0; i < s.length();) {
            int c = s.codePointAt(i);
            if (c >= 0x0600 && c <= 0x06E0)
                return "AR";
            i += Character.charCount(c);
        }
        return "EN";
    }

}