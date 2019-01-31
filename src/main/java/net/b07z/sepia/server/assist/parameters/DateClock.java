package net.b07z.sepia.server.assist.parameters;

import org.json.simple.JSONObject;

import net.b07z.sepia.server.assist.data.Parameter;
import net.b07z.sepia.server.assist.interpreters.NluInput;
import net.b07z.sepia.server.assist.interpreters.NluResult;
import net.b07z.sepia.server.assist.interviews.InterviewData;
import net.b07z.sepia.server.assist.tools.DateTimeConverters;
import net.b07z.sepia.server.assist.users.User;
import net.b07z.sepia.server.core.assistant.PARAMETERS;
import net.b07z.sepia.server.core.tools.JSON;

/**
 * Same as DateAndTime but just returns the clock. Useful when you want to ask for day and time separately.
 * 
 * @author Florian Quirin
 *
 */
public class DateClock implements ParameterHandler{
	
	User user;
	String language;
	boolean buildSuccess = false;
	NluInput nluInput;
	
	ParameterHandler masterHandler;
	
	private void setMaster(NluInput nluInput){
		masterHandler = new Parameter(PARAMETERS.TIME).getHandler();
		masterHandler.setup(nluInput);
	}
	private void setMaster(NluResult nluResult){
		masterHandler = new Parameter(PARAMETERS.TIME).getHandler();
		masterHandler.setup(nluResult);
	}
	
	String found = ""; 		//found raw text during extraction (that can be removed later)
	
	@Override
	public void setup(NluInput nluInput) {
		this.user = nluInput.user;
		this.language = nluInput.language;
		this.nluInput = nluInput;
		
		setMaster(nluInput);
	}
	@Override
	public void setup(NluResult nluResult) {
		this.user = nluResult.input.user;
		this.language = nluResult.language;
		this.nluInput = nluResult.input;
		
		setMaster(nluResult);
	}
	
	@Override
	public String extract(String input) {
		String date = masterHandler.extract(input);
		found = masterHandler.getFound();
		
		//check for time and overwrite date with today
		if (date.matches("<.*?>&&.*?&&.*")){
			//replace date with today - we keep this date so we can continue to use the master handler of DateAndTime
			date = date.replaceFirst("&&.*?&&", ("&&" + DateTimeConverters.getToday("yyyy.MM.dd", nluInput) + "&&"));
			return date;
			
		}else{
			return "";
		}
	}
	
	@Override
	public String guess(String input) {
		return masterHandler.guess(input);
	}

	@Override
	public String getFound() {
		return found;
	}

	@Override
	public String remove(String input, String found) {
		return masterHandler.remove(input, found);
	}
	
	@Override
	public String responseTweaker(String input){
		/*
		if (language.equals(LANGUAGES.DE)){
			if (input.matches("^(\\d|\\d\\d)$")){
				input = input + " uhr";
			}
		}else{
			if (input.matches("^(\\d|\\d\\d)$")){
				input = input + " oclock";
			}
		}
		//we need to convert the 'upgraded' input to right format OF THIS parameter again...
		nluInput.clearParameterResult(PARAMETERS.TIME); 	//although this is the CLOCK parameter we abuse TIME buffer
		input = extract(input);
		return input;
		*/
		return masterHandler.responseTweaker(input);
	}

	@Override
	public String build(String input) {
		String dateJsonString = masterHandler.build(input);
		if (!masterHandler.buildSuccess()){
			return "";
		}
		
		if (dateJsonString == null || dateJsonString.isEmpty()){
			return "";
		}
		
		JSONObject dateJson = JSON.parseString(dateJsonString);
		Object time = dateJson.get(InterviewData.DATE_TIME);
		if (time == null){
			return "";
		}
		Object diff = dateJson.get(InterviewData.TIME_DIFF);
	
		//build default result
		JSONObject timeResultJSON = new JSONObject();
			JSON.add(timeResultJSON, InterviewData.DATE_TIME, time);
			JSON.add(timeResultJSON, InterviewData.TIME_DIFF, diff);
		
		buildSuccess = true;
		return timeResultJSON.toJSONString();
	}

	@Override
	public boolean validate(String input) {
		return masterHandler.validate(input);
	}

	@Override
	public boolean buildSuccess() {
		return buildSuccess = true;
	}

}
