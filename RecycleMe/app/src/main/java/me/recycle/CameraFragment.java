package me.recycle;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.support.v4.content.FileProvider;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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

public class CameraFragment extends Fragment {
    ViewPager viewPager;
    private ImageView imageView;
    private ImageButton imageButton;
    private int imageRequestId = 0;
    private Uri[] imageUris = new Uri[256];
    static final int REQUEST_IMAGE_CAPTURE = 1;

    private OnFragmentInteractionListener mListener;

    public CameraFragment() {
        // Required empty public constructor
    }

    public static CameraFragment newInstance() {
        CameraFragment fragment = new CameraFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewPager = (ViewPager) viewPager.findViewById(R.id.container);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_camera, container, false);
        imageView = (ImageView) view.findViewById(R.id.recycleView);
        imageButton = (ImageButton) view.findViewById(R.id.button);
        Bundle bundle = getArguments();

        return view;
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }


    public void onRecycleButtonClick(View v) {
        Log.i("Recycle", "Scanning for recycles");
        dispatchTakePictureIntent();
    }

    private void dispatchTakePictureIntent() {
        try {
            File tmp = makeTempImageFile();
            takePicture(tmp);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void takePicture(File pic) {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getActivity().getPackageManager()) != null) {
            int req = imageRequestId;
            imageRequestId = (imageRequestId + 1) % 256;
            imageUris[req] = FileProvider.getUriForFile(getActivity().getApplicationContext(), "me.recycle.fileprovider", pic);
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUris[req]);
            startActivityForResult(takePictureIntent, req | 0x100);
        }
    }

    private byte[] fullyReadInputStream(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[4096];

        int n;
        while ((n = in.read(buf)) > 0) {
            out.write(buf, 0, n);
        }
        return out.toByteArray();
    }

    private void detectRecycle(Uri recycleUri) throws IOException {
        InputStream in = new BufferedInputStream(getActivity().getContentResolver().openInputStream(recycleUri));

        byte[] inBytes = fullyReadInputStream(in);

        in.close();

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

    private File makeTempImageFile() throws IOException {
        File out = this.getActivity().getApplicationContext().getCacheDir();
        File recycleFolder = new File(out, "recycles");
        if (!recycleFolder.exists()) {
            recycleFolder.mkdir();
        }
        return File.createTempFile("recycle", ".jpg", recycleFolder);
    }

    private void setViewPicture(Uri uri) {imageView.setImageURI(uri);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (((requestCode & 0x100) == 0x100) && resultCode == -1) {
            final Uri pic = imageUris[requestCode & 0xFF];
            setViewPicture(pic);
            (new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... params) {
                    try {
                        detectRecycle(pic);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    return null;
                }
            }).execute();
        } else if (requestCode == REQUEST_IMAGE_CAPTURE){
            Log.e("Recycle", "ERROR: " + resultCode);
        }
    }
}
