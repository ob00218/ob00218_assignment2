package com1032.cw2.ob00218.ob00218_assignment2;

import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;

/**
 * Created by Ollie on 18/05/2017.
 */

public class Run {

    private double distance = 0;
    private String time = null;
    private double avgPace = 0;
    private String date = null;
    private ArrayList<String> latLngList = new ArrayList<String>();

    /**
     * Default constructor needed for FirebaseRecyclerAdapter
     */
    public Run() {
        super();
    }

    /**
     * Parameterised constructor setting the value of each of the fields
     * @param distance
     * @param time
     * @param avgPace
     * @param date
     * @param latLngList
     */
    public Run(double distance, String time, double avgPace, String date, ArrayList<String> latLngList) {
        super();
        this.distance = distance;
        this.time = time;
        this.avgPace = avgPace;
        this.date = date;
        this.latLngList = latLngList;
    }

    /**
     *
     * @return distance
     */
    public double getDistance() {
        return distance;
    }

    /**
     *
     * @return time
     */
    public String getTime() {
        return time;
    }

    /**
     *
     * @return avgPace
     */
    public double getAvgPace() {
        return avgPace;
    }

    /**
     *
     * @return date
     */
    public String getDate() {
        return date;
    }

    /**
     *
     * @return latLngList
     */
    public ArrayList<String> getLatLngList() {
        return latLngList;
    }
}
