package net.b07z.sepia.server.assist.parameters;

import java.util.ArrayList;
import java.util.HashMap;

import net.b07z.sepia.server.assist.interpreters.NluInput;
import net.b07z.sepia.server.core.assistant.PARAMETERS;
import net.b07z.sepia.server.core.tools.Debugger;

/**
 * Class to generate the initial parameter set for fashion shopping 
 * 
 * @author Florian Quirin
 *
 */
public class FashionShopping {
	
	//input
	NluInput nluInput;
	HashMap<String, String> preCheckedParameters;		//to avoid multiple runs of the same scripts you can pass this down to all the methods
	
	//output
	int score;
	ArrayList<String> guesses; 	//any parameter that could not be double-checked can be tried to "guess" and it's name should be tracked here then
	
	public FashionShopping setup(NluInput nluInput, HashMap<String, String> preCheckedParameters){
		this.nluInput = nluInput;
		this.preCheckedParameters = preCheckedParameters;
		this.score = 0;
		this.guesses = new ArrayList<>();
		return this;
	}
	
	public int getScore(){
		return score;
	}
	
	public ArrayList<String> getGuesses(){
		return guesses;
	}
	
	public HashMap<String, String> getParameters(){
		HashMap<String, String> pv = preCheckedParameters;
		if (pv == null){
			pv = new HashMap<>();
		}
		String thisText = nluInput.text; 		//extra normalization required?
		Parameter_Handler brandHandler = null;
		Parameter_Handler colorHandler = null;
		Parameter_Handler sizeHandler = null;
		Parameter_Handler genHandler = null;
		Parameter_Handler itemHandler = null;
		String brand = ""; 		//we need this further down
		
		//Brand
		String p = PARAMETERS.FASHION_BRAND;
		if (!pv.containsKey(p)){
			brandHandler = new FashionBrand();
			brandHandler.setup(nluInput);
			brand = brandHandler.extract(thisText);
			if (!brand.isEmpty()){
				score++;
			}
			pv.put(p, brand);
		}
		
		//Color
		p = PARAMETERS.COLOR;
		if (!pv.containsKey(p)){
			colorHandler = new Color();
			colorHandler.setup(nluInput);
			String color = colorHandler.extract(thisText);
			if (!color.isEmpty()){
				score++;
			}
			pv.put(p, color);
		}
		
		//Size
		p = PARAMETERS.FASHION_SIZE;
		if (!pv.containsKey(p)){
			sizeHandler = new FashionSize();
			sizeHandler.setup(nluInput);
			String size = sizeHandler.extract(thisText);
			if (!size.isEmpty()){
				score++;
			}
			pv.put(p, size);
		}
		
		//Gender
		p = PARAMETERS.GENDER;
		if (!pv.containsKey(p)){
			genHandler = new Gender();
			genHandler.setup(nluInput);
			String gen = genHandler.extract(thisText);
			if (!gen.isEmpty()){
				score++;
			}
			pv.put(p, gen);
		}
		
		//Item
		p = PARAMETERS.FASHION_ITEM;
		if (!pv.containsKey(p)){
			itemHandler = new FashionItem();
			itemHandler.setup(nluInput);
			String item = itemHandler.extract(thisText);
			boolean guess = false;
			if (item.isEmpty()){
				item = itemHandler.guess(thisText);
				guess = true;
			}
			if (!item.isEmpty()){
				if (guess){
					//clean up guessed item
					if (colorHandler != null) item = colorHandler.remove(item, colorHandler.getFound());
					if (genHandler != null) item = genHandler.remove(item, genHandler.getFound());
					if (sizeHandler != null) item = sizeHandler.remove(item, sizeHandler.getFound());
					item = item.trim();
					if (brandHandler != null && !item.equals(brandHandler.getFound())){
						item = brandHandler.remove(item, brandHandler.getFound());
					}
					Debugger.println("GUESS CLEAN: " + item + " = " + PARAMETERS.FASHION_ITEM + " (FashionItem)", 3);
					guesses.add(p);
				}
				score++;
			}
			if (item.isEmpty() && !brand.isEmpty()){
				item = brand;
			}
			pv.put(p, item);
		}else{
			if (!pv.get(p).isEmpty()){
				score++;
			}
		}
		
		return pv;
	}

}
