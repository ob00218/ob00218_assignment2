package com1032.cw2.ob00218.ob00218_assignment2;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

import com.google.firebase.database.DatabaseReference;

/**
 * Created by Ollie on 21/05/2017.
 */

public class RunViewHolder extends RecyclerView.ViewHolder {

    public TextView time;
    public TextView distance;
    public TextView pace;
    public TextView date;
    private DatabaseReference mRef;

    /**
     * Constructor initializing the relevant TextViews
     * @param v
     */
    public RunViewHolder(View v) {
        super(v);
        time = (TextView)itemView.findViewById(R.id.timeTextViewRun);
        distance = (TextView)itemView.findViewById(R.id.distanceTextViewRun);
        pace = (TextView)itemView.findViewById(R.id.paceTextViewRun);
        date = (TextView)itemView.findViewById(R.id.dateTextViewRun);
    }

    /**
     * Set the database reference
     * @param ref
     */
    public void setRef(DatabaseReference ref) {
        mRef = ref;
    }
}

