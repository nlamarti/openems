package io.openems.edge.timeofusetariff.edmij;

import com.google.common.collect.ImmutableSortedMap;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.utils.ThreadPoolUtils;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.timeofusetariff.api.TimeOfUsePrices;
import io.openems.edge.timeofusetariff.api.TimeOfUseTariff;
import io.openems.edge.timeofusetariff.api.utils.TimeOfUseTariffUtils;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Route;
import okhttp3.Response;
import okhttp3.RequestBody;
import okhttp3.Authenticator;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

@Designate(ocd = Config.class, factory = true)
@Component(//
    name = "TimeOfUseTariff.Edmij", //
    immediate = true, //
    configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class TimeOfUseTariffEdmijImpl extends AbstractOpenemsComponent
    implements TimeOfUseTariff, OpenemsComponent, TimeOfUseTariffEdmij {

  private static final String EDMIJ_API_URL = "https://trading.edmij.nl";

  private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
  private final AtomicReference<ImmutableSortedMap<ZonedDateTime, Float>> prices = new AtomicReference<>(
      ImmutableSortedMap.of());

  @Reference
  private ComponentManager componentManager;

  private Config config = null;
  private ZonedDateTime updateTimeStamp = null;

  private EdmijAuthRepository repository;
  private final Logger log = LoggerFactory.getLogger(this.getClass());

  public TimeOfUseTariffEdmijImpl() {
    super(//
        OpenemsComponent.ChannelId.values(), //
        TimeOfUseTariffEdmij.ChannelId.values() //
    );
  }

  @Activate
  private void activate(ComponentContext context, Config config) {
    super.activate(context, config.id(), config.alias(), config.enabled());

    if (!config.enabled()) {
      return;
    }
    this.config = config;
    this.repository = new EdmijAuthRepository(this.config.email(), this.config.pasword());
    this.executor.schedule(this.task, 0, TimeUnit.SECONDS);
  }

  @Override
  @Deactivate
  protected void deactivate() {
    super.deactivate();
    ThreadPoolUtils.shutdownAndAwaitTermination(this.executor, 0);
  }

  protected final Runnable task = () -> {
    /*
     * Update Map of prices
     */
    var client = new OkHttpClient.Builder()
        .addInterceptor(new OAuth2Interceptor(this.repository))
        .authenticator(new OAuth2Authenticator(this.repository))
        .build();
    var request = new Request.Builder()
        .url(EDMIJ_API_URL)
        .build();
    try (var response = client.newCall(request).execute()) {
      this.channel(TimeOfUseTariffEdmij.ChannelId.HTTP_STATUS_CODE).setNextValue(response.code());

      if (!response.isSuccessful()) {
        throw new IOException("Unexpected code " + response);
      }
      this.updateTimeStamp = ZonedDateTime.now();

    } catch (IOException e) {
      this.log.error("Failed to get Edmij tariff prices", e);
    }


		/*
		 * Schedule next price update for 2 pm
		 */
		var now = ZonedDateTime.now();
		var nextRun = now.withHour(14).truncatedTo(ChronoUnit.HOURS);
		if (now.isAfter(nextRun)) {
			nextRun = nextRun.plusDays(1);
		}

		var duration = Duration.between(now, nextRun);
		var delay = duration.getSeconds();

		this.executor.schedule(this.task, delay, TimeUnit.SECONDS);
	};

  @Override
  public TimeOfUsePrices getPrices() {
    // return empty TimeOfUsePrices if data is not yet available.
    if (this.updateTimeStamp == null) {
      return TimeOfUsePrices.empty(ZonedDateTime.now());
    }

    return TimeOfUseTariffUtils.getNext24HourPrices(Clock.systemDefaultZone() /* can be mocked for testing */,
        this.prices.get(), this.updateTimeStamp);
  }
}
