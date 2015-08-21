package com.dong.howold;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.facepp.error.FaceppParseException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


public class MainActivity extends Activity implements View.OnClickListener {
    private ImageView mPhoto;
    private Button mSelectImage;
    private Button mDetectImage;
    private TextView mTip;
    private View mShowProcessBar;

    private String mCurrentPhotoStr;
    private Bitmap mPhotoImage;
    private Paint mPaint;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        initEvents();
        mPaint = new Paint();
        
    }

    private void initEvents() {
        mSelectImage.setOnClickListener(this);
        mDetectImage.setOnClickListener(this);
    }

    private void initView() {
        mPhoto = (ImageView) findViewById(R.id.id_photo);
        mDetectImage = (Button) findViewById(R.id.id_detectImage);
        mSelectImage = (Button) findViewById(R.id.id_selectImage);
        mTip = (TextView) findViewById(R.id.id_tip);
    }

    private static final int MSG_SUCCESS = 1;
    private static final int MSG_ERROR=0;

    private Handler handler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what){
                case MSG_SUCCESS:
                    //mShowProcessBar.setVisibility(View.GONE);
                    JSONObject jso = (JSONObject) msg.obj;
                    prePareBitmap(jso);
                    mPhoto.setImageBitmap(mPhotoImage);
                    break;
                case MSG_ERROR:
                    //mShowProcessBar.setVisibility(View.GONE);
                    String errorMessage = (String) msg.obj;
                    if(TextUtils.isEmpty(errorMessage)){
                        mTip.setText("error");
                    }else{
                        mTip.setText(errorMessage);
                    }
                    break;
            }
            super.handleMessage(msg);
        }
    };

    private void prePareBitmap(JSONObject jso) {
        Bitmap bitmap = Bitmap.createBitmap(mPhotoImage.getWidth(),
                mPhotoImage.getHeight(),mPhotoImage.getConfig());
        Canvas canvas = new Canvas(bitmap);
        canvas.drawBitmap(mPhotoImage,0,0,null);
        try {
            JSONArray jsonArray = jso.getJSONArray("face");
            int faceCount = jsonArray.length();
            mTip.setText("find "+faceCount);
            for(int i = 0; i < faceCount; i++){
                JSONObject face = jsonArray.getJSONObject(i);
                JSONObject position = face.getJSONObject("position");
                float x = (float) position.getJSONObject("center").getDouble("x");
                float y = (float) position.getJSONObject("center").getDouble("y");
                float w = (float) position.getDouble("width");
                float h = (float) position.getDouble("height");
                x = x / 100 * bitmap.getWidth();
                y = y / 100 * bitmap.getHeight();
                w = w / 100 * bitmap.getWidth();
                h = h / 100 * bitmap.getHeight();
                mPaint.setColor(0xffffffff);
                mPaint.setStrokeWidth(2);
                canvas.drawLine(x - w/2,y - h/2,x - w/2,y + h/2,mPaint);
                mPhotoImage = bitmap;
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

    }


    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.id_selectImage:
                Intent intent = new Intent(Intent.ACTION_PICK);
                intent.setType("image/*");
                startActivityForResult(intent,1);
                break;
            case R.id.id_detectImage:
                //mShowProcessBar.setVisibility(View.VISIBLE);
                FaceDetect.detect(mPhotoImage, new FaceDetect.CallBack() {
                    @Override
                    public void success(JSONObject result) {
                        Message msg = Message.obtain();
                        msg.what = MSG_SUCCESS;
                        msg.obj = result;
                        handler.sendMessage(msg);

                    }

                    @Override
                    public void error(FaceppParseException e) {
                        Message msg = Message.obtain();
                        msg.what = MSG_ERROR;
                        msg.obj = e.getErrorMessage();
                        handler.sendMessage(msg);

                    }
                });

                break;
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == 1){
            if(data != null){
                Uri uri = data.getData();
                Cursor cursor = getContentResolver().query(uri, null, null, null, null);
                cursor.moveToFirst();

                int index = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
                mCurrentPhotoStr = cursor.getString(index);
                cursor.close();
                reSizePhoto();
                mPhoto.setImageBitmap(mPhotoImage);
                mTip.setText("Click Detect -->");
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void reSizePhoto() {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;

        BitmapFactory.decodeFile(mCurrentPhotoStr, options);
        double ratio = Math.max(options.outWidth * 1.0d/1024f, options.outHeight * 1.0d/1024f);
        options.inSampleSize = (int) Math.ceil(ratio);
        options.inJustDecodeBounds = false;
        mPhotoImage = BitmapFactory.decodeFile(mCurrentPhotoStr, options);

    }
}
