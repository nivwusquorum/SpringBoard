package uk.ac.cam.cl.ss958.huggler;

import java.io.Serializable;

public class HugglerIntroMessage implements Serializable {
	private static final long serialVersionUID = 1L;
	private String name;
	private String protocolName;
	
	public HugglerIntroMessage() {
		protocolName = "";
		name = "";
	}
	
	protected HugglerIntroMessage(HugglerIntroMessage copy) {
		this.name = copy.name;
		this.protocolName = copy.protocolName;
	}
	
	public String getName() {
		return name;
	}
	
	public String getProtocol() {
		return protocolName;
	}
	
	public HugglerIntroMessage(String protocol, String name) {
		this.protocolName = protocol;
		this.name = name;
	}
	
}
