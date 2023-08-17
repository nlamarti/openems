package io.openems.edge.timeofusetariff.edmij;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.AttributeType;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
		name = "Time-Of-Use Tariff Edmij", //
		description = "Time-Of-Use Tariff implementation for Edmij.")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "timeOfUseTariff0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";
	@AttributeDefinition(name = "Email", description = "Email account to authenticate with the Edmij API")
	String email() default "";

	@AttributeDefinition(name = "Password", description = "Password to authenticate with the Edmij API", type = AttributeType.PASSWORD)
	String pasword() default "";
	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	String webconsole_configurationFactory_nameHint() default "Time-Of-Use Tariff Edmij [{id}]";
}
