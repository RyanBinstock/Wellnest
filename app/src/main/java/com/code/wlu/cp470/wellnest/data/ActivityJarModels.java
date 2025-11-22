package com.code.wlu.cp470.wellnest.data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class ActivityJarModels {

    // One activity coming from backend
    public static class Activity implements Serializable {
        public final String emoji;
        public final String name;
        public final String description;
        public final String where;
        public final String when;

        public Activity(String emoji, String name, String description,
                        String where, String when) {
            this.emoji = emoji;
            this.name = name;
            this.description = description;
            this.where = where;
            this.when = when;
        }
    }

    // TEMP: mock data until backend is wired
    // categoryIndex: 0 = Explore, 1 = Nightlife, 2 = Play, 3 = Cozy, 4 = Culture
    public static List<Activity> getActivitiesForCategory(int categoryIndex) {
        List<Activity> list = new ArrayList<>();

        switch (categoryIndex) {
            case 0: // Explore
                list.add(new Activity(
                        "🧭",
                        "Trail Walk",
                        "Stretch your legs on nearby trails and chase hidden views just outside your doorstep.",
                        "Nearby trail / park",
                        "This afternoon"
                ));
                list.add(new Activity("🚲", "Sunset Bike Ride",
                        "Cruise quiet streets or riverside paths as the sun goes down.",
                        "Bike paths around your area",
                        "Around sunset"));
                break;

            case 1: // Nightlife
                list.add(new Activity("🎵", "Live Music Night",
                        "Find a cozy bar or café with live music and soak in the vibes.",
                        "Local bar / music venue",
                        "Tonight"));
                break;

            case 2: // Play
                list.add(new Activity("⚽", "Park Games",
                        "Kick a ball, toss a frisbee, or make up a goofy game with friends.",
                        "Nearest open field",
                        "Anytime"));
                break;

            case 3: // Cozy
                list.add(new Activity("🎬", "Movie Night",
                        "Pick a comfort movie, grab snacks, and make a cozy viewing nest.",
                        "Your living room",
                        "Evening"));
                break;

            case 4: // Culture
            default:
                list.add(new Activity("🖼️", "Gallery Stroll",
                        "Wander through a gallery or museum and let something unexpected catch your eye.",
                        "Local gallery / museum",
                        "Weekend afternoon"));
                break;
        }

        return list;
    }
}
