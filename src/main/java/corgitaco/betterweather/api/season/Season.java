package corgitaco.betterweather.api.season;

import com.google.common.collect.Maps;
import com.mojang.serialization.Codec;
import net.minecraft.util.IStringSerializable;
import net.minecraft.util.Util;

import javax.annotation.Nullable;
import java.util.Map;

public interface Season {

    /**
     * @return the total year length.
     */
    int getYearLength();

    /**
     * @return the current year time.
     */
    int getCurrentYearTime();

    /**
     * @return the current season's key.
     */
    Key getKey();

    /**
     * @return the current season's phase.
     */
    Phase getPhase();

    /**
     * @return the current season's settings.
     */
    Settings getSettings();


    /**
     * @return the start time for this season in the given year.
     */
    default int getSeasonStartTime() {
        return getSeasonStartTime(getKey(), getYearLength());
    }

    /**
     * @return the start time for this season's phase in the given year.
     */
    default int getSeasonAndPhaseStartTime() {
        return getSeasonAndPhaseStartTime(getKey(), getPhase(), getYearLength());
    }

    enum Key implements IStringSerializable {
        SPRING,
        SUMMER,
        AUTUMN,
        WINTER;


        public static final Codec<Key> CODEC = IStringSerializable.createEnumCodec(Key::values, Key::getTypeFromId);

        private static final Map<String, Key> BY_ID = Util.make(Maps.newHashMap(), (nameToTypeMap) -> {
            for (Key key : values()) {
                nameToTypeMap.put(key.name(), key);
            }
        });


        @Nullable
        public static Key getTypeFromId(String idIn) {
            return BY_ID.get(idIn);
        }

        public static boolean hasType(String value) {
            return BY_ID.containsKey(value);
        }

        @Override
        public String getString() {
            return this.name();
        }
    }

    enum Phase implements IStringSerializable {
        START,
        MID,
        END;

        public static final Codec<Phase> CODEC = IStringSerializable.createEnumCodec(Phase::values, Phase::getTypeFromId);

        private static final Map<String, Phase> BY_ID = Util.make(Maps.newHashMap(), (nameToTypeMap) -> {
            for (Phase phase : values()) {
                nameToTypeMap.put(phase.name(), phase);
            }
        });

        @Nullable
        public static Phase getTypeFromId(String idIn) {
            return BY_ID.get(idIn);
        }

        public static boolean hasType(String value) {
            return BY_ID.containsKey(value);
        }

        @Override
        public String getString() {
            return this.name();
        }
    }


    /**
     * @return the total length of a given phase as determined by the given season length.
     */
    static int getPhaseLength(int seasonLength) {
        return seasonLength / Phase.values().length;
    }

    /**
     * @return the total length of a given season as determined by the given year length.
     */
    static int getSeasonLength(int yearLength) {
        return yearLength / (Key.values().length);
    }

    /**
     * @return the start time for the given season & year length.
     */
    static int getSeasonStartTime(Key seasonKey, int yearLength) {
        int perSubSeasonLength = getSeasonLength(yearLength);
        return perSubSeasonLength * seasonKey.ordinal();
    }

    /**
     * @return the start time of the given season's phase for the given year length.
     */
    static int getSeasonAndPhaseStartTime(Key seasonKey, Phase phase, int yearLength) {
        int timeInCycleForSeason = getSeasonStartTime(seasonKey, yearLength);
        int phaseTime = getSeasonLength(yearLength) / Phase.values().length;
        return timeInCycleForSeason + (phaseTime * phase.ordinal());
    }

    /**
     * @return the current season key determined by the year's current time and length.
     */
    static Key getSeasonFromTime(int currentYearTime, int yearLength) {
        int seasonLength = yearLength / Key.values().length;

        if (currentYearTime < seasonLength) {
            return Key.SPRING;
        } else if (currentYearTime < seasonLength * 2) {
            return Key.SUMMER;
        } else if (currentYearTime < seasonLength * 3) {
            return Key.AUTUMN;
        } else
            return Key.WINTER;
    }
}
