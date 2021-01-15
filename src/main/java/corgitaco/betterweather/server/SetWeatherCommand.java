package corgitaco.betterweather.server;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import corgitaco.betterweather.BetterWeather;
import corgitaco.betterweather.api.BetterWeatherEntryPoint;
import corgitaco.betterweather.api.weatherevent.BetterWeatherID;
import corgitaco.betterweather.api.weatherevent.WeatherEvent;
import corgitaco.betterweather.weatherevent.WeatherEventSystem;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.command.ISuggestionProvider;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.server.ServerWorld;

import java.util.List;
import java.util.stream.Collectors;

public class SetWeatherCommand {

    public static ArgumentBuilder<CommandSource, ?> register(CommandDispatcher<CommandSource> dispatcher) {
        List<String> weatherTypes = BetterWeatherEntryPoint.WEATHER_EVENTS.stream().map(WeatherEvent::getID).map(BetterWeatherID::toString).collect(Collectors.toList());

        return Commands.literal("setweather").then(Commands.argument("weathertype", StringArgumentType.string()).suggests((ctx, sb) -> ISuggestionProvider.suggest(weatherTypes.stream(), sb))
                .executes((cs) -> betterWeatherSetWeatherType(cs.getSource().getWorld(), cs.getSource(), cs.getArgument("weathertype", String.class))));
    }

    public static int betterWeatherSetWeatherType(ServerWorld world, CommandSource source, String weatherType) {
        WeatherEvent weatherEvent = WeatherEventSystem.WEATHER_EVENTS.get(new BetterWeatherID(weatherType));

        if (weatherEvent != null) {
            BetterWeather.weatherData.setEvent(weatherEvent.getID().toString());
            world.func_241113_a_(0, 6000, weatherEvent.getID() != WeatherEventSystem.CLEAR, false);
            source.sendFeedback(weatherEvent.successTranslationTextComponent(), true);
        } else {
            source.sendFeedback(new TranslationTextComponent("commands.bw.setweather.failed", weatherType), true);
        }
        return 1;
    }
}
