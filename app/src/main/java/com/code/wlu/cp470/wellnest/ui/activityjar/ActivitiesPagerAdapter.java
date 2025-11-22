package com.code.wlu.cp470.wellnest.ui.activityjar;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.code.wlu.cp470.wellnest.R;

import java.util.List;

public class ActivitiesPagerAdapter
        extends RecyclerView.Adapter<ActivitiesPagerAdapter.CardViewHolder> {

    public interface OnCardClickListener {
        void onCardClick(int position);
    }

    private final List<Integer> cardResIds;
    private final LayoutInflater inflater;
    private final OnCardClickListener clickListener;

    public ActivitiesPagerAdapter(Context context,
                                  List<Integer> cardResIds,
                                  OnCardClickListener clickListener) {
        this.cardResIds = cardResIds;
        this.inflater = LayoutInflater.from(context);
        this.clickListener = clickListener;
    }

    @NonNull
    @Override
    public CardViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = inflater.inflate(R.layout.item_activity_card, parent, false);
        return new CardViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CardViewHolder holder, int position) {
        int resId = cardResIds.get(position);
        holder.imgBackground.setImageResource(resId);

        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onCardClick(position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return cardResIds.size();
    }

    static class CardViewHolder extends RecyclerView.ViewHolder {
        final ImageView imgBackground;

        CardViewHolder(@NonNull View itemView) {
            super(itemView);
            imgBackground = itemView.findViewById(R.id.imgCardBackground);
        }
    }
}
