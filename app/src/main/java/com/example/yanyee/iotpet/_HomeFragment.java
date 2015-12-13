package com.example.yanyee.iotpet;

import android.app.Activity;

import android.content.Context;
import android.content.SharedPreferences;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.github.mikephil.charting.charts.LineChart;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link _HomeFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link _HomeFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class _HomeFragment extends Fragment {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private OnFragmentInteractionListener mListener;

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment _HomeFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static _HomeFragment newInstance(String param1, String param2) {
        _HomeFragment fragment = new _HomeFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    public _HomeFragment() {
        // Required empty public constructor
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("newData", newData);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (savedInstanceState != null) {
            newData = savedInstanceState.getString("newData");
        }
    }
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }

    }


    private String data;

    public static int calculateInSampleSize(
            BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            // Calculate ratios of height and width to requested height and width
            final int heightRatio = Math.round((float) height / (float) reqHeight);
            final int widthRatio = Math.round((float) width / (float) reqWidth);

            // Choose the smallest ratio as inSampleSize value, this will guarantee
            // a final image with both dimensions larger than or equal to the
            // requested height and width.
            inSampleSize = heightRatio < widthRatio ? heightRatio : widthRatio;

        }

        return inSampleSize;
    }

    public static Bitmap decodeSampledBitmapFromResource(Resources res, int resId,
                                                         int reqWidth, int reqHeight) {

        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeResource(res, resId, options);


        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeResource(res, resId, options);
    }

    Button refresh;
    Button getStatus;
    TextView statusText;
    TextView amountOfWater;
    TextView waterData;

    SharedPreferences settings;
    String newData = "";
    String curStatus;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_home, container, false);
        /*********Mays's Part (Adding Images) ************/
        ImageView image = (ImageView) view.findViewById(R.id.imageView);
        image.setImageBitmap(
                decodeSampledBitmapFromResource(getResources(), R.drawable.image1, 500, 500));
                /*data="600";
        if(Integer.parseInt(data)<500){
            image.setImageResource(R.drawable.image2);
        }
        else{
            image.setImageResource(R.drawable.image1);
        }*/
        // Inflate the layout for this fragment

        /********Yanyee's Part (Testing Mqtt Transmission******/
        amountOfWater = (TextView) view.findViewById(R.id.amountOfWater);
        waterData = (TextView) view.findViewById(R.id.waterData);
        refresh = (Button) view.findViewById(R.id.Refresh);
        getStatus = (Button) view.findViewById(R.id.statusButton);
        statusText = (TextView) view.findViewById(R.id.statusText);
        settings = getPrefs(getActivity());
        waterData.setText(newData);

        getStatus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                curStatus = settings.getString("Mqtt Status", "Not Found");
                statusText.setText(curStatus);
            }
        });

        refresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view1) {
                newData = settings.getString("newData", "Not Found");
                waterData.setText(newData);

                Toast.makeText(getActivity(), newData + " " + System.currentTimeMillis(), Toast.LENGTH_LONG).show();
            }
        });
        return view;
    }

    public SharedPreferences getPrefs(Context context) {
        return context.getSharedPreferences(MqttService.APP_ID, 0);
    }

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnFragmentInteractionListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }


    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        public void onFragmentInteraction(Uri uri);
    }

    TextView amountDrank;


}
