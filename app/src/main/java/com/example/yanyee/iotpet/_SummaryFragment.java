package com.example.yanyee.iotpet;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.LimitLine;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;

import java.util.ArrayList;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.widget.RelativeLayout;

import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import com.github.mikephil.charting.utils.ColorTemplate;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link _SummaryFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link _SummaryFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class _SummaryFragment extends Fragment{
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
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment _SummaryFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static _SummaryFragment newInstance(String param1, String param2) {
        _SummaryFragment fragment = new _SummaryFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    public _SummaryFragment() {
        // Required empty public constructor
    }

    public BarChart chart;
    public RelativeLayout mainLayout;
    public LineChart mChart;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
       View view = inflater.inflate(R.layout.fragment_summary, container, false);

        //initialise linechart
        mChart = (LineChart) view.findViewById(R.id.lineChart);
        //mChart.setOnChartValueSelectedListener((OnChartValueSelectedListener) (this));
        mChart.setDescription("Amount of water consumed");

        // enable touch gestures
        mChart.setTouchEnabled(true);

        // enable scaling and dragging
        mChart.setDragEnabled(true);
        mChart.setScaleEnabled(true);
        mChart.setDrawGridBackground(false);

        // if disabled, scaling can be done on x- and y-axis separately
        mChart.setPinchZoom(true);

        // set an alternative background color
        mChart.setBackgroundColor(Color.LTGRAY);

        LineData data = new LineData();
        data.setValueTextColor(Color.WHITE);

        // add empty data
        mChart.setData(data);

        // get the legend (only possible after setting data)
        Legend l = mChart.getLegend();

        l.setForm(Legend.LegendForm.LINE);
        l.setTextColor(Color.WHITE);

        XAxis xl = mChart.getXAxis();
        xl.setTextColor(Color.WHITE);
        xl.setDrawGridLines(false);
        xl.setAvoidFirstLastClipping(true);

        YAxis yl = mChart.getAxisLeft();
        yl.setTextColor(Color.WHITE);
        yl.setAxisMaxValue(100f);
        yl.setDrawGridLines(true);

        YAxis yl2 = mChart.getAxisRight();
        yl2.setEnabled(false);



        /*chart = (BarChart) view.findViewById(R.id.chart);
        BarData data = new BarData(getXAxisValues(), getDataSet());
        chart.setData(data);
        chart.setDescription("Amount of water consumed(ml)");
        chart.animateXY(2000, 2000);
        //LimitLine line = new LimitLine(1000f);
        //data.addLimitLine(line);
        chart.invalidate();*/
        return view;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.fragment_summary);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }


    }

    private ArrayList<BarDataSet> getDataSet() {
        ArrayList<BarDataSet> dataSets = null;

        ArrayList<BarEntry> valueSet1 = new ArrayList<>();
        BarEntry v1e1 = new BarEntry(875.000f, 0); // MON
        valueSet1.add(v1e1);
        BarEntry v1e2 = new BarEntry(1020.000f, 1); // TUE
        valueSet1.add(v1e2);
        BarEntry v1e3 = new BarEntry(1200.000f, 2); // WED
        valueSet1.add(v1e3);
        BarEntry v1e4 = new BarEntry(1120.000f, 3); // THURS
        valueSet1.add(v1e4);
        BarEntry v1e5 = new BarEntry(900.000f, 4); // FRI
        valueSet1.add(v1e5);
        BarEntry v1e6 = new BarEntry(875.000f, 5); // SAT
        valueSet1.add(v1e6);
        BarEntry v1e7 = new BarEntry(980.000f, 6); // SUN
        valueSet1.add(v1e7);

        BarDataSet barDataSet1 = new BarDataSet(valueSet1, "Water Consumed");
        //barDataSet1.setColors(ColorTemplate.COLORFUL_COLORS);

        dataSets = new ArrayList<>();
        dataSets.add(barDataSet1);
        return dataSets;
    }

    private void addEntry(){
        LineData data = mChart.getData();

        if (data!= null){
            LineDataSet set = data.getDataSetByIndex(0);
            // set.addEntry(...); // can be called as well

            if (set ==null){
                set = createSet();
                data.addDataSet(set);
            }

            data.addXValue("");
            //add entry here
            data.addEntry(new Entry((float) (Math.random()), set.getEntryCount()), 0);

            mChart.notifyDataSetChanged();

            // limit the number of visible entries
            mChart.setVisibleXRangeMaximum(10);
            // mChart.setVisibleYRange(30, AxisDependency.LEFT);

            // move to the latest entry
            mChart.moveViewToX(data.getXValCount() - 121);

            // this automatically refreshes the chart (calls invalidate())
            // mChart.moveViewTo(data.getXValCount()-7, 55f,
            // AxisDependency.LEFT);
        }
    }

    private LineDataSet createSet(){
        LineDataSet set = new LineDataSet(null, "Data from sensor");
        set.setDrawCubic(true);
        set.setCubicIntensity(0.2f);
        set.setAxisDependency(YAxis.AxisDependency.LEFT);
        set.setColor(ColorTemplate.getHoloBlue());
        set.setCircleColor(ColorTemplate.getHoloBlue());
        set.setLineWidth(2f);
        set.setCircleSize(4f);
        set.setFillAlpha(65);
        set.setFillColor(ColorTemplate.getHoloBlue());
        set.setHighLightColor(Color.rgb(244, 117, 177));
        set.setValueTextColor(Color.WHITE);
        set.setValueTextSize(10f);
        set.setDrawValues(false);

        return set;
    }

    private boolean mustStop = false;

    @Override
    public void onPause() {
        super.onPause();
        mustStop=true; // Stop the infinite loop
    }

    @Override
    public void onResume() {
        super.onResume();

        new Thread(new Runnable() {

            @Override
            public void run() {
                mustStop=false;
                while (!mustStop) {

                    getActivity().runOnUiThread(new Runnable() {

                        @Override
                        public void run() {
                            addEntry(); //chart notified of update in addEntry method
                        }
                    });

                    try {
                        Thread.sleep(35);
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }

    private ArrayList<String> getXAxisValues() {
        ArrayList<String> xAxis = new ArrayList<>();
        xAxis.add("MON");
        xAxis.add("TUE");
        xAxis.add("WED");
        xAxis.add("THURS");
        xAxis.add("FRI");
        xAxis.add("SAT");
        xAxis.add("SUN");
        return xAxis;
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
        // TODO: Update argument type and na
        public void onFragmentInteraction(Uri uri);
    }

/*    public double calculateAmountDrank(String fromSensor){
        double currentAmount = 0.0; //set how much?
        double previousAmount = 0.0;
        double amountDrank;

        amountDrank = previousAmount - currentAmount;
        if (amountDrank > 800) { //amount drank too much
            //return some anomaly
            returnError();
        }else{
            newLineEntry();
        }
        return 0.0;
    }*/

}
