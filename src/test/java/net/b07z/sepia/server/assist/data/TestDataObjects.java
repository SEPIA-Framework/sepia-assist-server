package net.b07z.sepia.server.assist.data;

import static org.junit.Assert.*;

import org.json.simple.JSONObject;
import org.junit.Test;

import net.b07z.sepia.server.assist.assistant.LOCATION;
import net.b07z.sepia.server.core.tools.Converters;
import net.b07z.sepia.server.core.tools.JSON;

public class TestDataObjects {

	@Test
	public void test() {
		//Test Address
		JSONObject jo = JSON.make(LOCATION.COUNTRY, "Germany", LOCATION.CITY, "Berlin");
		Address adr = new Address(Converters.json2HashMap(jo));
		assertTrue(adr.city.equals("Berlin"));
		assertTrue(adr.country.equals("Germany"));
		
		//Test Name
		jo = JSON.make(Name.FIRST, "Joe", Name.LAST, "Average");
		Name name = new Name(Converters.json2HashMap(jo));
		assertTrue(name.first.equals("Joe"));
		assertTrue(name.last.equals("Average"));
	}

}
