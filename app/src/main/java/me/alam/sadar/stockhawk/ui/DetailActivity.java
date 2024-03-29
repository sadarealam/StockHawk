package me.alam.sadar.stockhawk.ui;

import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.utils.Utils;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import me.alam.sadar.stockhawk.R;
import me.alam.sadar.stockhawk.datamodel.Meta;
import me.alam.sadar.stockhawk.datamodel.Quote;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;


public class DetailActivity extends AppCompatActivity implements Callback {

    private final String LOG_TAG = "DetailActivity" ;
    private final OkHttpClient client = new OkHttpClient();
    private String symbol ;
    LineChart mChart ;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        mChart = (LineChart) findViewById(R.id.chart) ;
        symbol = getIntent().getStringExtra("symbol").toUpperCase();
        try {
            getData(symbol);
        } catch (Exception e) {
            e.printStackTrace();
        }
        restoreActionBar();
    }

    private void getData(String symbol) throws Exception{
        StringBuilder urlbuilder = new StringBuilder("http://chartapi.finance.yahoo.com/instrument/1.0/");
        //todo later change the following line to fetch data for variable range .
        urlbuilder.append(symbol).append("/chartdata;type=quote;range=1y/json");
        String url = urlbuilder.toString() ;
        Log.d(LOG_TAG,url);
        final Request request = new Request.Builder()
                .url(url)
                .build();
        client.newCall(request).enqueue(this);
    }

    private void setData(List<Quote> quotes) {

        ArrayList<Entry> values = new ArrayList<Entry>();
        ArrayList<String> xvalues = new ArrayList<>() ;

        int i =0 ;
        for (Quote quote : quotes ) {
           //float val = (float) (Math.random() * range) + 3;
            values.add(new Entry(Float.parseFloat(quote.getClose()),i));
            xvalues.add(quote.getDate());
            i++ ;
        }

            LineDataSet dataSet;

            dataSet = new LineDataSet(values, symbol);

            // set the line to be drawn like this "- - - - - -"
            dataSet.setColor(Color.WHITE);
            dataSet.setDrawCircles(false);
            dataSet.setValueTextColor(Color.WHITE);
            dataSet.setDrawFilled(false);

            //TODO set the nice color for drawing.
          /*  if (Utils.getSDKInt() >= 18 && 5==6) {
                // fill drawable only supported on api level 18 and above
                Drawable drawable = ContextCompat.getDrawable(this, R.drawable.fade_red);
                dataSet.setFillDrawable(drawable);
            }
            else {
                dataSet.setFillColor(Color.BLACK);
            }*/

            ArrayList<ILineDataSet> dataSets = new ArrayList<ILineDataSet>();
            dataSets.add(dataSet); // add the datasets

            // create a data object with the datasets
            LineData data = new LineData(xvalues,dataSets);
            data.setValueTextColor(Color.WHITE);
            data.setDrawValues(false);

            XAxis xAxis = mChart.getXAxis();
            xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
            xAxis.setTextSize(10f);
            xAxis.setTextColor(Color.WHITE);

            YAxis yAxis = mChart.getAxisLeft();
            yAxis.setEnabled(true);
            yAxis.setLabelCount(5, true);
            yAxis.setTextColor(Color.WHITE);

            mChart.getAxisRight().setEnabled(false);
            mChart.getLegend().setTextSize(12f);
            mChart.setDescriptionColor(Color.WHITE);

            // set data
            mChart.setData(data);
            mChart.setDescription(getString(R.string.chart_descrioption));

            mChart.setNoDataText(getString(R.string.chart_fetching_data));
            mChart.setContentDescription(getString(R.string.chart_content_description)+symbol);

            callInvalidate();
        }

    private void callInvalidate(){
        Handler mainThread = new Handler(Looper.getMainLooper());
        mainThread.post(new Runnable() {
            @Override
            public void run() {
                mChart.invalidate();
                mChart.animateX(2000);
            }
        });
    }

    @Override
    public void onFailure(Call call, IOException e) {
        Handler mainThread = new Handler(Looper.getMainLooper());
        mainThread.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(DetailActivity.this, getString(R.string.detail_server_error), Toast.LENGTH_LONG).show();
                mChart.setNoDataText(getString(R.string.chart_fetching_error));
            }
        });

    }

    @Override
    public void onResponse(Call call, Response response) throws IOException {
        if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);
        else {
            String result = response.body().string();
            Log.d(LOG_TAG,result) ;
            String jsonresult = result.substring(result.indexOf("{"),result.lastIndexOf('}')+1);
            try {
                JSONObject resultobject = new JSONObject(jsonresult) ;
                JSONObject metaobject = resultobject.getJSONObject("meta") ;
                Meta meta = toMeta(metaobject) ;

                ArrayList<Quote> quotes = new ArrayList<>() ;
                JSONArray seriesarray = resultobject.getJSONArray("series");
                for(int i=0 ;i<seriesarray.length();i++){
                    JSONObject series = seriesarray.getJSONObject(i) ;
                    quotes.add(toQuote(series));
                }
                setData(quotes);
                setCardData(meta);
            } catch (JSONException e) {
                e.printStackTrace();
            }

        }
    }

    private Meta toMeta(JSONObject metaobject) throws JSONException {
        Meta meta = new Meta();
        meta.setCompanyName(metaobject.getString("Company-Name"));
        meta.setCurrency(metaobject.getString("currency"));
        meta.setExchangeName(metaobject.getString("Exchange-Name"));
        meta.setUri(metaobject.getString("uri"));
        meta.setTicker(metaobject.getString("ticker"));
        meta.setUnit(metaobject.getString("unit"));
        meta.setTimestamp(metaobject.getString("timestamp"));
        meta.setFirstTrade(metaobject.getString("first-trade"));
        meta.setLastTrade(metaobject.getString("last-trade"));
        meta.setPreviousClosePrice(metaobject.getString("previous_close_price"));
        return meta ;
    }

    private Quote toQuote(JSONObject series) throws JSONException {
        Quote quote = new Quote() ;
        quote.setClose(series.getString("close"));
        quote.setDate(series.getString("Date"));
        quote.setHigh(series.getString("high"));
        quote.setLow(series.getString("low"));
        quote.setOpen(series.getString("open"));
        quote.setVolume(series.getString("volume"));
        return  quote ;
    }

    private void setCardData(final Meta meta){
        final TextView symboltv = (TextView) findViewById(R.id.stock_symbol_detail);
        final TextView companytv = (TextView) findViewById(R.id.company_name_detail);
        final TextView firsttradetv = (TextView) findViewById(R.id.first_trade_detail);
        final TextView lasttradetv = (TextView) findViewById(R.id.last_trade_detail);
        final TextView currencytv = (TextView) findViewById(R.id.currency_detail);
        final TextView exchangename = (TextView) findViewById(R.id.stock_name_detail);
        final TextView bidpricetv = (TextView) findViewById(R.id.bid_price_detail);
        final CardView detailcard = (CardView) findViewById(R.id.detail_card) ;

        Handler mainThread = new Handler(Looper.getMainLooper());
        mainThread.post(new Runnable() {
            @Override
            public void run() {
                detailcard.setContentDescription("Detail desciption for "+meta.getTicker());
                symboltv.setText(meta.getTicker());
                companytv.setText(meta.getCompanyName());
                firsttradetv.setText(meta.getFirstTrade());
                lasttradetv.setText(meta.getLastTrade());
                currencytv.setText(meta.getCurrency());
                exchangename.setText(meta.getExchangeName());
                bidpricetv.setText(meta.getPreviousClosePrice());
            }
        });


    }

    public void restoreActionBar() {
        ActionBar actionBar = getSupportActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setTitle(symbol+" Detail");
    }
}
