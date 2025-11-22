package com.code.wlu.cp470.wellnest.ui.components;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.code.wlu.cp470.wellnest.R;

import java.util.ArrayList;
import java.util.List;

public class CarouselDemoActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_carousel_demo);

        WellnestCarouselView carouselView = findViewById(R.id.carouselView);

        List<DemoItem> items = new ArrayList<>();
        items.add(new DemoItem("🧘", "Yoga", "Relax your mind and body with some yoga poses.", R.drawable.activity_jar_explore_bg_blurred));
        items.add(new DemoItem("🏃", "Running", "Go for a run and clear your head.", R.drawable.activity_jar_explore_bg_blurred));
        items.add(new DemoItem("🎨", "Painting", "Express your creativity through painting.", R.drawable.activity_jar_explore_bg_blurred));
        items.add(new DemoItem("📚", "Reading", "Get lost in a good book.", R.drawable.activity_jar_explore_bg_blurred));
        items.add(new DemoItem("🍳", "Cooking", "Try a new recipe and enjoy a delicious meal.", R.drawable.activity_jar_explore_bg_blurred));

        DemoAdapter adapter = new DemoAdapter(items);
        carouselView.setAdapter(adapter);
    }

    private static class DemoItem {
        String emoji;
        String title;
        String description;
        int imageResId;

        public DemoItem(String emoji, String title, String description, int imageResId) {
            this.emoji = emoji;
            this.title = title;
            this.description = description;
            this.imageResId = imageResId;
        }
    }

    private static class DemoAdapter extends RecyclerView.Adapter<DemoAdapter.ViewHolder> {

        private final List<DemoItem> items;

        public DemoAdapter(List<DemoItem> items) {
            this.items = items;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_activity_card, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            DemoItem item = items.get(position);
            holder.emojiTv.setText(item.emoji);
            holder.titleTv.setText(item.title);
            holder.descriptionTv.setText(item.description);
            holder.backgroundImg.setImageResource(item.imageResId);
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView emojiTv;
            TextView titleTv;
            TextView descriptionTv;
            ImageView backgroundImg;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                emojiTv = itemView.findViewById(R.id.emoji_tv);
                titleTv = itemView.findViewById(R.id.activityName_tv);
                descriptionTv = itemView.findViewById(R.id.description_tv);
                backgroundImg = itemView.findViewById(R.id.imgCardBackground);
            }
        }
    }
}