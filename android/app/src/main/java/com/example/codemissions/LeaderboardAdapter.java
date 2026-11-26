package com.example.codemissions;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.google.android.material.color.MaterialColors;

import java.util.List;

public class LeaderboardAdapter extends BaseAdapter {

    /** One line of GET /api/leaderboard: rang, nom, xpTotal, niveauJoueur. */
    public static class Row {
        public final int rang;
        public final String nom;
        public final int xp;
        public final int niveau;

        public Row(int rang, String nom, int xp, int niveau) {
            this.rang = rang;
            this.nom = nom;
            this.xp = xp;
            this.niveau = niveau;
        }
    }

    private final Context context;
    private final List<Row> rows;

    public LeaderboardAdapter(Context context, List<Row> rows) {
        this.context = context;
        this.rows = rows;
    }

    @Override
    public int getCount() { return rows.size(); }

    @Override
    public Object getItem(int position) { return rows.get(position); }

    @Override
    public long getItemId(int position) { return position; }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context)
                    .inflate(R.layout.item_leaderboard, parent, false);
        }
        Row row = rows.get(position);

        TextView rank = convertView.findViewById(R.id.txtRank);
        rank.setText(String.valueOf(row.rang));
        switch (row.rang) {
            case 1:  rank.setTextColor(ContextCompat.getColor(context, R.color.rank_gold));   break;
            case 2:  rank.setTextColor(ContextCompat.getColor(context, R.color.rank_silver)); break;
            case 3:  rank.setTextColor(ContextCompat.getColor(context, R.color.rank_bronze)); break;
            default: rank.setTextColor(MaterialColors.getColor(rank,
                    com.google.android.material.R.attr.colorOnSurfaceVariant));
        }

        ((TextView) convertView.findViewById(R.id.txtAvatar)).setText(
                row.nom == null || row.nom.isEmpty() ? "?" : row.nom.substring(0, 1).toUpperCase());
        ((TextView) convertView.findViewById(R.id.txtPlayerName)).setText(row.nom);
        ((TextView) convertView.findViewById(R.id.txtPlayerLevel)).setText("Niveau " + row.niveau);
        ((TextView) convertView.findViewById(R.id.txtPlayerXp)).setText(row.xp + " XP");

        // Highlight the current player (matched by name; names are not unique in the database)
        boolean me = row.nom != null && row.nom.equals(Session.getName(context));
        convertView.setBackgroundColor(me
                ? ContextCompat.getColor(context, R.color.highlight_me)
                : Color.TRANSPARENT);
        return convertView;
    }
}
