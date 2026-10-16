package com.tarun.kahani.app;

import android.content.Context;

import com.tarun.kahani.core.Cloud;
import com.tarun.kahani.core.Film;
import com.tarun.kahani.core.SoundLib;
import com.tarun.kahani.core.SoundWords;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Real recordings for the film's backgrounds. When a story needs rain, a river, a market or a forest and the
 * library has no recording of it yet, a free-licence recording is fetched from Openverse (Freesound,
 * Wikimedia and other open collections; no key needed), saved to the library with its licence and creator,
 * and used from then on — also offline, and in every later story.
 */
final class FreeSounds {
    private FreeSounds() {}

    /** The sound's word (as SoundWords knows it) → what to search for and what to call it. */
    static final String[][] WANT = {
            {"storm", "thunderstorm rain", "Thunderstorm"}, {"rain", "rain ambience", "Rain"}, {"thunder", "thunder rumble", "Thunder"},
            {"wind", "wind ambience", "Wind"}, {"waterfall", "waterfall", "Waterfall"}, {"river", "river stream water", "River"},
            {"sea", "ocean waves beach", "Sea waves"}, {"fire", "campfire crackling", "Fire crackling"},
            {"crickets", "night crickets ambience", "Night crickets"}, {"frogs", "frogs pond night", "Frogs at a pond"},
            {"forest", "forest birds ambience", "Forest"}, {"birds", "birds chirping morning", "Birds"},
            {"village", "village ambience rural", "Village"}, {"market", "market crowd ambience", "Market crowd"},
            {"palace", "large hall ambience", "Palace hall"}, {"temple", "temple bells ambience", "Temple"},
            {"cave", "cave dripping ambience", "Cave"}, {"desert", "desert wind ambience", "Desert"},
            {"city", "city street traffic ambience", "City street"}, {"train", "train passing", "Train"},
            {"school", "children playing playground", "Children playing"}, {"garden", "garden birds ambience", "Garden"},
    };

    /** Keyless recordings on GitHub (remvze/moodist: CC0 or Pixabay Content Licence, per its README): the sound's word → the file. */
    static final String MOODIST = "https://raw.githubusercontent.com/remvze/moodist/main/public/sounds/";
    static final String[][] GITHUB = {
            {"storm", "rain/thunder"}, {"rain", "rain/light-rain"}, {"thunder", "rain/thunder"}, {"wind", "nature/wind"},
            {"waterfall", "nature/waterfall"}, {"river", "nature/river"}, {"sea", "nature/waves"}, {"fire", "nature/campfire"},
            {"crickets", "animals/crickets"}, {"forest", "nature/jungle"}, {"birds", "animals/birds"}, {"village", "places/night-village"},
            {"market", "urban/crowd"}, {"temple", "places/temple"}, {"cave", "nature/droplets"}, {"desert", "nature/howling-wind"},
            {"city", "urban/traffic"}, {"train", "transport/train"}, {"garden", "animals/birds"},
    };

    /** How many recordings were added (at most max per film, each a few MB at most). */
    static int fetchFor(Context ctx, Film film, Library lib, Cloud cloud, int max, List<String> notes) {
        if (cloud == null || film == null) return 0;
        // what the film's backgrounds need, in the order they first come
        Map<String, String[]> need = new LinkedHashMap<String, String[]>();
        for (Film.Amb a : film.ambience) {
            if (a.words == null || a.words.startsWith("#")) continue;
            Set<String> words = SoundWords.expand(a.words);
            for (String[] w : WANT) if (words.contains(w[0]) && !need.containsKey(w[0])) { need.put(w[0], w); break; }
        }
        if (need.isEmpty()) return 0;
        SoundLib have = lib.soundLib();
        int added = 0;
        for (String[] w : need.values()) {
            if (added >= max) break;
            if (have.bestUser(w[0], "amb") != null) continue;           // the library already has a recording of it
            // first the keyless recording on GitHub (no search, no key, a known file)
            String gh = null;
            for (String[] g : GITHUB) if (g[0].equals(w[0])) { gh = g[1]; break; }
            if (gh != null) {
                try {
                    byte[] b = cloud.download(MOODIST + gh + ".mp3");
                    if (b != null && b.length >= 20000 && b.length <= 12 * 1024 * 1024) {
                        Library.Item it = lib.addBytes(Library.SOUND, "amb", w[2] + " (free recording)", w[0] + ", " + gh.replace('/', ' ').replace('-', ' '), b, ".mp3",
                                "moodist (GitHub) CC0 / Pixabay licence");
                        if (AudioIO.decode(ctx, it.path) == null) lib.remove(it);
                        else {
                            it.setMeta("kindSet", "1");
                            it.kind = "amb";
                            added++;
                            notes.add(w[2].toLowerCase(Locale.US) + " sound (moodist on GitHub, CC0 / Pixabay licence)");
                            continue;
                        }
                    }
                } catch (Exception ignored) {
                    // not reachable: Openverse next
                }
            }
            try {
                List<Cloud.Found> fs = cloud.searchSounds(w[1], 10);
                Cloud.Found pick = null;
                for (Cloud.Found f : fs) {
                    String lic = f.license.toLowerCase(Locale.US);
                    boolean open = lic.equals("cc0") || lic.equals("by") || lic.equals("by-sa") || lic.equals("pdm") || lic.length() == 0;
                    if (open && (f.seconds == 0 || (f.seconds >= 12 && f.seconds <= 240))) { pick = f; break; }
                }
                if (pick == null) continue;
                byte[] b = cloud.download(pick.url);
                if (b.length < 20000 || b.length > 12 * 1024 * 1024) continue;
                String u = pick.url.toLowerCase(Locale.US);
                String ext = u.contains(".mp3") ? ".mp3" : u.contains(".ogg") ? ".ogg" : u.contains(".wav") ? ".wav" : u.contains(".flac") ? ".flac" : ".m4a";
                Library.Item it = lib.addBytes(Library.SOUND, "amb", w[2] + " (free recording)", w[0] + ", " + pick.title, b, ext,
                        pick.source + " " + pick.license + " " + pick.creator);
                if (AudioIO.decode(ctx, it.path) == null) { lib.remove(it); continue; }
                it.setMeta("kindSet", "1");
                it.kind = "amb";
                added++;
                notes.add(w[2].toLowerCase(Locale.US) + " sound (" + pick.source + ", " + (pick.license.length() > 0 ? pick.license.toUpperCase(Locale.US) : "free") + ")");
            } catch (Exception ignored) {
                // no internet or the service is busy: the built-in sound plays
            }
        }
        if (added > 0) lib.save();
        return added;
    }
}
