package com.example.codemissions;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

/** Vertical roadmap: a level header, then the missions of that level on a winding path. */
public class RoadmapAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public interface Listener {
        void onMissionClick(Mission m);
        void onLockedClick(Mission m);
    }

    private static final int HEADER = 0, MISSION = 1;
    /** Horizontal position of the nodes (0 = left, 1 = right), repeated in each level. */
    private static final float[] ZIGZAG = {0.28f, 0.72f, 0.34f, 0.70f};

    private static class Row {
        int level, done, total;      // header
        Mission mission;             // mission row
        int number;
        float x, prev = Float.NaN, next = Float.NaN;
        boolean current, aboveDone;
    }

    private final List<Row> rows = new ArrayList<>();
    private final Listener listener;
    private int currentPosition = -1;
    private Mission currentMission;

    public RoadmapAdapter(Listener listener) { this.listener = listener; }

    public int getCurrentPosition() { return currentPosition; }
    public Mission getCurrentMission() { return currentMission; }

    public void setMissions(List<Mission> missions) {
        rows.clear();
        currentPosition = -1;
        currentMission = null;
        int lastLevel = -1, indexInLevel = 0;
        Row previous = null;
        for (int i = 0; i < missions.size(); i++) {
            Mission m = missions.get(i);
            if (m.getLevel() != lastLevel) {
                lastLevel = m.getLevel();
                indexInLevel = 0;
                previous = null;
                Row h = new Row();
                h.level = lastLevel;
                for (Mission x : missions) {
                    if (x.getLevel() == lastLevel) { h.total++; if (x.isSolved()) h.done++; }
                }
                rows.add(h);
            }
            Row r = new Row();
            r.mission = m;
            r.number = i + 1;
            r.x = ZIGZAG[indexInLevel % ZIGZAG.length];
            if (previous != null) {
                r.prev = previous.x;
                previous.next = r.x;
                r.aboveDone = previous.mission.isSolved();
            }
            if (currentMission == null && m.isUnlocked() && !m.isSolved()) {
                r.current = true;
                currentMission = m;
                currentPosition = rows.size();
            }
            rows.add(r);
            previous = r;
            indexInLevel++;
        }
        notifyDataSetChanged();
    }

    @Override public int getItemViewType(int position) {
        return rows.get(position).mission == null ? HEADER : MISSION;
    }

    @Override public int getItemCount() { return rows.size(); }

    @NonNull @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == HEADER) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_road_header, parent, false);
            return new HeaderHolder(v);
        }
        RoadRowView v = new RoadRowView(parent.getContext());
        v.setLayoutParams(new RecyclerView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return new MissionHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Row r = rows.get(position);
        if (holder instanceof HeaderHolder) {
            HeaderHolder h = (HeaderHolder) holder;
            h.title.setText("Niveau " + r.level + " · " + Mission.levelName(r.level));
            h.count.setText(r.done + "/" + r.total);
        } else {
            MissionHolder h = (MissionHolder) holder;
            h.view.bind(r.mission, r.number, r.x, r.prev, r.next, r.current, r.aboveDone);
            h.view.setOnClickListener(v -> {
                if (r.mission.isUnlocked()) listener.onMissionClick(r.mission);
                else listener.onLockedClick(r.mission);
            });
        }
    }

    static class HeaderHolder extends RecyclerView.ViewHolder {
        final TextView title, count;
        HeaderHolder(View v) {
            super(v);
            title = v.findViewById(R.id.txtLevelTitle);
            count = v.findViewById(R.id.txtLevelCount);
        }
    }

    static class MissionHolder extends RecyclerView.ViewHolder {
        final RoadRowView view;
        MissionHolder(RoadRowView v) { super(v); view = v; }
    }
}
