package com.example.codemissions;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.google.android.material.color.MaterialColors;

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

        TextView difficulty = convertView.findViewById(R.id.txtMissionDifficulty);
        difficulty.setText(m.getDifficulty());
        difficulty.setTextColor(Mission.difficultyColor(context, m.getDifficulty()));

        ((TextView) convertView.findViewById(R.id.txtMissionXp))
                .setText(" · +" + m.getXp() + " XP");

        TextView status = convertView.findViewById(R.id.txtMissionStatus);
        convertView.setAlpha(1f);
        if (m.isSolved()) {
            status.setText("✓ Réussie");
            status.setTextColor(ContextCompat.getColor(context, R.color.verdict_success));
        } else if (!m.isUnlocked()) {
            status.setText("🔒 Verrouillée");
            status.setTextColor(MaterialColors.getColor(status,
                    com.google.android.material.R.attr.colorOnSurfaceVariant));
            convertView.setAlpha(0.5f);
        } else {
            status.setText("À faire");
            status.setTextColor(MaterialColors.getColor(status,
                    com.google.android.material.R.attr.colorOnSurfaceVariant));
        }
        return convertView;
    }
}
