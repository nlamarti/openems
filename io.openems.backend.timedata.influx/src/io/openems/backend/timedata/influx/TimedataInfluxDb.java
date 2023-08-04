package io.openems.backend.timedata.influx;

import java.net.URI;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.SortedMap;
import java.util.regex.Pattern;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.TreeBasedTable;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;
import com.influxdb.exceptions.BadRequestException;

import io.openems.backend.common.component.AbstractOpenemsBackendComponent;
import io.openems.backend.common.metadata.Metadata;
import io.openems.backend.common.timedata.Timedata;
import io.openems.common.OpenemsOEM;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.timedata.Resolution;
import io.openems.common.types.ChannelAddress;
import io.openems.common.utils.StringUtils;
import io.openems.shared.influxdb.InfluxConnector;

@Designate(ocd = Config.class, factory = false)
@Component(//
		name = "Timedata.InfluxDB", //
		configurationPolicy = ConfigurationPolicy.REQUIRE, //
		property = { //
				"service.ranking:Integer=1" // ranking order (highest first)
		}, //
		immediate = true //
)
public class TimedataInfluxDb extends AbstractOpenemsBackendComponent implements Timedata {

	private static final Pattern NAME_NUMBER_PATTERN = Pattern.compile("[^0-9]+([0-9]+)$");

	private final Logger log = LoggerFactory.getLogger(TimedataInfluxDb.class);
	private final FieldTypeConflictHandler fieldTypeConflictHandler;

	@Reference
	protected volatile Metadata metadata;

	private Config config;
	private InfluxConnector influxConnector = null;

	public TimedataInfluxDb() {
		super("Timedata.InfluxDB");
		this.fieldTypeConflictHandler = new FieldTypeConflictHandler(this);
	}

	@Activate
	private void activate(Config config) throws OpenemsException, IllegalArgumentException {
		this.config = config;

		this.logInfo(this.log, "Activate [" //
				+ "url=" + config.url() + ";"//
				+ "bucket=" + config.bucket() + ";"//
				+ "apiKey=" + (config.apiKey() != null ? "ok" : "NOT_SET") + ";"//
				+ "measurement=" + config.measurement() //
				+ (config.isReadOnly() ? ";READ_ONLY_MODE" : "") //
				+ "]");

		this.influxConnector = new InfluxConnector(config.queryLanguage(), URI.create(config.url()), config.org(),
				config.apiKey(), config.bucket(), config.isReadOnly(), config.poolSize(), config.maxQueueSize(), //
				(throwable) -> {
					if (throwable instanceof BadRequestException) {
						this.fieldTypeConflictHandler.handleException((BadRequestException) throwable);

					} else {
						this.logError(this.log, "Unable to write to InfluxDB. " + throwable.getClass().getSimpleName()
								+ ": " + throwable.getMessage());
					}
				});
	}

	@Deactivate
	private void deactivate() {
		this.logInfo(this.log, "Deactivate");
		if (this.influxConnector != null) {
			this.influxConnector.deactivate();
		}
	}

	@Override
	public void write(String edgeId, TreeBasedTable<Long, String, JsonElement> data) throws OpenemsException {
		// parse the numeric EdgeId
		int influxEdgeId = TimedataInfluxDb.parseNumberFromName(edgeId);

		// Write data to default location
		this.writeData(influxEdgeId, data);
	}

	/**
	 * Actually writes the data to InfluxDB.
	 *
	 * @param influxEdgeId the unique, numeric identifier of the Edge
	 * @param data         the data
	 * @throws OpenemsException on error
	 */
	private void writeData(int influxEdgeId, TreeBasedTable<Long, String, JsonElement> data) {
		var dataEntries = data.rowMap().entrySet();
		if (dataEntries.isEmpty()) {
			// no data to write
			return;
		}

		for (Entry<Long, Map<String, JsonElement>> dataEntry : dataEntries) {
			var channelEntries = dataEntry.getValue().entrySet();
			if (channelEntries.isEmpty()) {
				// no points to add
				continue;
			}

			var timestamp = dataEntry.getKey();
			// this builds an InfluxDB record ("point") for a given timestamp
			var point = Point //
					.measurement(InfluxConnector.MEASUREMENT) //
					.addTag(OpenemsOEM.INFLUXDB_TAG, String.valueOf(influxEdgeId)) //
					.time(timestamp, WritePrecision.MS);
			for (Entry<String, JsonElement> channelEntry : channelEntries) {
				this.addValue(point, channelEntry.getKey(), channelEntry.getValue());
			}
			if (point.hasFields()) {
				this.influxConnector.write(point);
			}
		}
	}

	/**
	 * Parses the number of an Edge from its name string.
	 *
	 * <p>
	 * e.g. translates "edge0" to "0".
	 *
	 * @param name the edge name
	 * @return the number
	 * @throws OpenemsException on error
	 */
	public static Integer parseNumberFromName(String name) throws OpenemsException {
		try {
			var matcher = TimedataInfluxDb.NAME_NUMBER_PATTERN.matcher(name);
			if (matcher.find()) {
				var nameNumberString = matcher.group(1);
				return Integer.parseInt(nameNumberString);
			}
		} catch (NullPointerException e) {
			/* ignore */
		}
		throw new OpenemsException("Unable to parse number from name [" + name + "]");
	}

	@Override
	public SortedMap<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>> queryHistoricData(String edgeId,
			ZonedDateTime fromDate, ZonedDateTime toDate, Set<ChannelAddress> channels, Resolution resolution)
			throws OpenemsNamedException {
		// parse the numeric EdgeId
		Optional<Integer> influxEdgeId = Optional.of(TimedataInfluxDb.parseNumberFromName(edgeId));

		return this.influxConnector.queryHistoricData(influxEdgeId, fromDate, toDate, channels, resolution);
	}

	@Override
	public SortedMap<ChannelAddress, JsonElement> queryHistoricEnergy(String edgeId, ZonedDateTime fromDate,
			ZonedDateTime toDate, Set<ChannelAddress> channels) throws OpenemsNamedException {
		// parse the numeric EdgeId
		Optional<Integer> influxEdgeId = Optional.of(TimedataInfluxDb.parseNumberFromName(edgeId));
		return this.influxConnector.queryHistoricEnergy(influxEdgeId, fromDate, toDate, channels);
	}

	@Override
	public SortedMap<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>> queryHistoricEnergyPerPeriod(String edgeId,
			ZonedDateTime fromDate, ZonedDateTime toDate, Set<ChannelAddress> channels, Resolution resolution)
			throws OpenemsNamedException {
		// parse the numeric EdgeId
		Optional<Integer> influxEdgeId = Optional.of(TimedataInfluxDb.parseNumberFromName(edgeId));
		return this.influxConnector.queryHistoricEnergyPerPeriod(influxEdgeId, fromDate, toDate, channels, resolution);
	}

	/**
	 * Adds the value in the correct data format for InfluxDB.
	 *
	 * @param builder the Influx PointBuilder
	 * @param field   the field name
	 * @param element the value
	 */
	private void addValue(Point builder, String field, JsonElement element) {
		if (element == null || element.isJsonNull() || this.specialCaseFieldHandling(builder, field, element)) {
			// already handled by special case handling
			return;
		}

		if (element.isJsonPrimitive()) {
			var p = (JsonPrimitive) element;
			if (p.isNumber()) {
				// Numbers can be directly converted
				var n = p.getAsNumber();
				if (n.getClass().getName().equals("com.google.gson.internal.LazilyParsedNumber")) {
					// Avoid 'discouraged access'
					// LazilyParsedNumber stores value internally as String
					if (StringUtils.matchesFloatPattern(n.toString())) {
						builder.addField(field, n.doubleValue());
						return;
					}
					builder.addField(field, n.longValue());
					return;

				} else if (n instanceof Integer || n instanceof Long || n instanceof Short || n instanceof Byte) {
					builder.addField(field, n.longValue());
					return;

				}
				builder.addField(field, n.doubleValue());
				return;

			} else if (p.isBoolean()) {
				// Booleans are converted to integer (0/1)
				builder.addField(field, p.getAsBoolean());
				return;

			} else if (p.isString()) {
				// Strings are parsed if they start with a number or minus
				var s = p.getAsString();
				if (StringUtils.matchesFloatPattern(s)) {
					try {
						builder.addField(field, Double.parseDouble(s)); // try parsing to Double
						return;
					} catch (NumberFormatException e) {
						builder.addField(field, s);
						return;
					}

				} else if (StringUtils.matchesIntegerPattern(s)) {
					try {
						builder.addField(field, Long.parseLong(s)); // try parsing to Long
						return;
					} catch (NumberFormatException e) {
						builder.addField(field, s);
						return;
					}
				}
				builder.addField(field, s);
				return;
			}

		} else {
			builder.addField(field, element.toString());
			return;
		}
	}

	/**
	 * Handles some special cases for fields.
	 *
	 * <p>
	 * E.g. to avoid errors like "field type conflict: input field XYZ on
	 * measurement "data" is type integer, already exists as type string"
	 *
	 * @param builder the InfluxDB Builder
	 * @param field   the fieldName, i.e. the ChannelAddress
	 * @param value   the value, guaranteed to be not-null and not JsonNull.
	 * @return true if field was handled; false otherwise
	 */
	private boolean specialCaseFieldHandling(Point builder, String field, JsonElement value) {
		var handler = this.fieldTypeConflictHandler.getHandler(field);
		if (handler == null) {
			// no special handling exists for this field
			return false;
		}
		// call special handler
		handler.accept(builder, value);
		return true;
	}

	@Override
	protected void logInfo(Logger log, String message) {
		super.logInfo(log, message);
	}

	@Override
	protected void logWarn(Logger log, String message) {
		super.logWarn(log, message);
	}

	@Override
	public String id() {
		return this.config.id();
	}
}
