package io.openems.edge.bridge.mbus;

import io.openems.common.test.AbstractComponentConfig;

@SuppressWarnings("all")
public class MyConfig extends AbstractComponentConfig implements Config {

	protected static class Builder {
		private String id;
		private String portName;
		private int baudrate;

		private Builder() {
		}

		public Builder setId(String id) {
			this.id = id;
			return this;
		}

		public Builder setPortName(String portName) {
			this.portName = portName;
			return this;
		}

		public Builder setBaudrate(int baudrate) {
			this.baudrate = baudrate;
			return this;
		}

		public MyConfig build() {
			return new MyConfig(this);
		}
	}

	/**
	 * Create a Config builder.
	 *
	 * @return a {@link Builder}
	 */
	public static Builder create() {
		return new Builder();
	}

	private final Builder builder;

	private MyConfig(Builder builder) {
		super(Config.class, builder.id);
		this.builder = builder;
	}

	@Override
	public String portName() {
		return this.builder.portName;
	}

	@Override
	public int baudrate() {
		return this.builder.baudrate;
	}

}