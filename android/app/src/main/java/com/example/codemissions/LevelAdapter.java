package com.example.codemissions;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

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
        ((TextView) convertView.findViewById(R.id.txtLevelName))
                .setText("Level " + l.getLevel() + " - " + l.getName());
        ((TextView) convertView.findViewById(R.id.txtLevelProgress))
                .setText(l.getCompleted() + "/" + l.getTotal() + " missions");

        TextView status = convertView.findViewById(R.id.txtLevelStatus);
        convertView.setAlpha(1f);
        status.setText(l.isCompleted() ? "\u2714 Completed" : "In progress");
        return convertView;
    }
}