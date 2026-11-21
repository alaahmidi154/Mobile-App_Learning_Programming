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

public class LevelAdapter extends BaseAdapter {

    private final Context context;
    private final List<Level> levels;

    public LevelAdapter(Context context, List<Level> levels) {
        this.context = context;
        this.levels = levels;
    }

    @Override public int getCount() { return levels.size(); }
    @Override public Object getItem(int position) { return levels.get(position); }
    @Override public long getItemId(int position) { return levels.get(position).getLevel(); }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.item_level, parent, false);
        }
        Level l = levels.get(position);
        TextView name = convertView.findViewById(R.id.txtLevelName);
        name.setText("Niveau " + l.getLevel() + " · " + l.getName());
        name.setTextColor(Mission.difficultyColor(context, l.getName()));
        ((TextView) convertView.findViewById(R.id.txtLevelProgress))
                .setText(l.getCompleted() + "/" + l.getTotal() + " missions réussies");

        TextView status = convertView.findViewById(R.id.txtLevelStatus);
        if (l.isCompleted()) {
            status.setText("✓ Terminé");
            status.setTextColor(ContextCompat.getColor(context, R.color.verdict_success));
        } else {
            status.setText("En cours");
            status.setTextColor(MaterialColors.getColor(status,
                    com.google.android.material.R.attr.colorOnSurfaceVariant));
        }
        return convertView;
    }
}
