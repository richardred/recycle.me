package me.recycle;

import android.content.Context;
import android.content.Intent;
import android.hardware.Camera;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.support.v4.content.FileProvider;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;

import static android.content.ContentValues.TAG;
import static android.provider.MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE;

public class CameraFragment extends Fragment {
    private ImageView imageView;
    private ImageButton imageButton;
    private int imageRequestId = 0;
    private Uri[] imageUris = new Uri[256];
    static final int REQUEST_IMAGE_CAPTURE = 1;
    private Camera mCamera;
    private CameraPreview mPreview;

    public CameraFragment() {
        // Required empty public constructor
    }

    public static CameraFragment newInstance() {
        CameraFragment fragment = new CameraFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    public static Camera getCameraInstance(){
        Camera c = null;
        try {
            c = Camera.open(); // attempt to get a Camera instance
        }
        catch (Exception e){
            // Camera is not available (in use or does not exist)
        }
        return c; // returns null if camera is unavailable
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_camera, container, false);
        Bundle bundle = getArguments();

        imageView = (ImageView) view.findViewById(R.id.imageView);
        imageButton = (ImageButton) view.findViewById(R.id.button);
        mCamera = getCameraInstance();
        mCamera.setDisplayOrientation(90);
        mPreview = new CameraPreview(getActivity().getApplicationContext(), mCamera);
        FrameLayout preview = (FrameLayout) view.findViewById(R.id.camera_preview);
        preview.addView(mPreview);

        // Add a listener to the Capture button
        imageButton.setOnClickListener(
            new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // Calls onPictureTaken callback
                    mCamera.takePicture(null, null, mPicture);
                }
            }
        );

        return view;
    }

    private Camera.PictureCallback mPicture = new Camera.PictureCallback() {
        @Override
        public void onPictureTaken(final byte[] data, Camera camera) {
            File pictureFile;

            try {
                pictureFile = makeTempImageFile();
                FileOutputStream fos = new FileOutputStream(pictureFile);
                fos.write(data);
                fos.close();

                Uri img = Uri.fromFile(getActivity().getApplicationContext().getFileStreamPath("recycle.jpg"));
                setViewPicture(img);

                (new AsyncTask<Void, Void, Void>() {
                    @Override
                    protected Void doInBackground(Void... params) {
                        try {
                            detectRecycle(data);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        return null;
                    }
                }).execute();

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    };

    private File makeTempImageFile() throws IOException {
        File out = this.getActivity().getApplicationContext().getCacheDir();
        File recycleFolder = new File(out, "recycles");
        if (!recycleFolder.exists()) {
            recycleFolder.mkdir();
        }
        return File.createTempFile("recycle", ".jpg", recycleFolder);
    }

    private void setViewPicture(Uri uri) {
        imageView.setImageURI(uri);
    }

    private void detectRecycle(byte[] inBytes) throws IOException {
        URL serverUrl = new URL("http://35.231.68.240/testwiener");
        HttpURLConnection conn = (HttpURLConnection) serverUrl.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("content-type", "application/octet-stream");
        conn.setRequestProperty("Content-Length", inBytes.length + "");
        conn.setDoInput(true);
        conn.setDoOutput(true);
        conn.connect();

        OutputStream servOut = new BufferedOutputStream(conn.getOutputStream());
        servOut.write(inBytes);
        servOut.flush();
        servOut.close();

        InputStream servIn = new BufferedInputStream(conn.getInputStream());
        final File responseImageFile = makeTempImageFile();
        OutputStream recycleOut = new BufferedOutputStream(new FileOutputStream(responseImageFile));

        byte[] buf = new byte[4096];
        int n;
        try {
            while ((n = servIn.read(buf)) > 0) {
                recycleOut.write(buf, 0, n);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        recycleOut.flush();
        recycleOut.close();
        conn.disconnect();

        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                imageView.setImageURI(Uri.fromFile(responseImageFile));
            }
        });
    }

}
