package com.example.codemissions;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.List;

public class MissionAdapter extends BaseAdapter {

    private final Context context;
    private final List<Mission> missions;

    public MissionAdapter(Context context, List<Mission> missions) {
        this.context = context;
        this.missions = missions;
    }

    @Override
    public int getCount() { return missions.size(); }

    @Override
    public Object getItem(int position) { return missions.get(position); }

    @Override
    public long getItemId(int position) { return missions.get(position).getId(); }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context)
                    .inflate(R.layout.item_mission, parent, false);
        }
        Mission m = missions.get(position);
        ((TextView) convertView.findViewById(R.id.txtMissionTitle)).setText(m.getTitle());
        ((TextView) convertView.findViewById(R.id.txtMissionInfo))
                .setText(m.getDifficulty() + " - " + m.getXp() + " XP");

        TextView status = convertView.findViewById(R.id.txtMissionStatus);
        if (m.isSolved()) {
            status.setText("\u2714 Completed");
            convertView.setAlpha(1f);
        } else if (!m.isUnlocked()) {
            status.setText("\uD83D\uDD12 Locked");
            convertView.setAlpha(0.5f);
        } else {
            status.setText("To do");
            convertView.setAlpha(1f);
        }
        return convertView;
    }
}